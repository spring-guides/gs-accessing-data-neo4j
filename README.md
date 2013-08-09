This guide walks you through the process of using Spring Data to build an application with Neo4j.

What you'll build
-----------------

You'll learn how to persist objects and relationships in Neo4j's [NoSQL][u-nosql] graph-based data store.

What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Maven 3.0][mvn] or later

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[mvn]: http://maven.apache.org/download.cgi

How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/guides/gs), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [git](/understanding/git):
`git clone https://github.com/springframework-meta/gs-accessing-data-neo4j.git`
 - cd into `gs-accessing-data-neo4j/initial`.
 - Jump ahead to [Define a simple entity](#initial).

**When you're finished**, you can check your results against the code in `gs-accessing-data-neo4j/complete`.
[zip]: https://github.com/springframework-meta/gs-accessing-data-neo4j/archive/master.zip


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Maven](https://maven.apache.org) and [Gradle](http://gradle.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Maven](/guides/gs/maven) or [Building Java Projects with Gradle](/guides/gs/gradle/).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello

### Create a Maven POM

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.springframework</groupId>
    <artifactId>gs-acessing-data-neo4j</artifactId>
    <version>0.1.0</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>3.2.3.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
            <version>3.2.3.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-neo4j</artifactId>
            <version>2.2.0.RELEASE</version>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-context</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-tx</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-aspects</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
             <groupId>javax.validation</groupId>
             <artifactId>validation-api</artifactId>
             <version>1.0.0.GA</version>
        </dependency>
        <dependency>
             <groupId>org.slf4j</groupId>
             <artifactId>slf4j-log4j12</artifactId>
             <version>1.7.5</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>2.3.2</version>
            </plugin>
        </plugins>
    </build>
    
    <repositories>
        <repository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>http://repo.springsource.org/libs-snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>neo4j</id>
            <name>Neo4j</name>
            <url>http://m2.neo4j.org/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    <pluginRepositories>
        <pluginRepository>
            <id>spring-snapshots</id>
            <name>Spring Snapshots</name>
            <url>http://repo.springsource.org/libs-snapshot</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>
</project>
```

This guide also uses log4j with certain log levels turned up so that you can see what Neo4j and Spring Data are doing.

`src/main/resources/log4j.properties`
```properties
# Set root logger level to DEBUG and its only appender to A1.
log4j.rootLogger=WARN, A1

# A1 is set to be a ConsoleAppender.
log4j.appender.A1=org.apache.log4j.ConsoleAppender

# A1 uses PatternLayout.
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
log4j.appender.A1.layout.ConversionPattern=%-4r [%t] %-5p %c %x - %m%n

log4j.category.org.springframework=INFO
log4j.category.org.springframework.data.neo4j=DEBUG
```


<a name="initial"></a>
Define a simple entity
------------------------
Neo4j captures entities and their relationships, with both aspects being of equal importance. Imagine you are modeling a system where you store a record for each person. But you also want to track a person's co-workers (`teammates` in this example). With Neo4j, you can capture all that with some simple annotations.

`src/main/java/hello/Person.java`
```java
package hello;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class Person {

    @GraphId Long id;
    public String name;

    public Person() {}
    public Person(String name) { this.name = name; }

    @RelatedTo(type="TEAMMATE", direction=Direction.BOTH)
    public @Fetch Set<Person> teammates;

    public void worksWith(Person person) {
        if (teammates == null) {
            teammates = new HashSet<Person>();
        }
        teammates.add(person);
    }

    public String toString() {
        String results = name + "'s teammates include\n";
        if (teammates != null) {
            for (Person person : teammates) {
                results += "\t- " + person.name + "\n";
            }
        }
        return results;
    }

}
```

Here you have a `Person` class that has only one attribute, the `name`. You have two constructors, an empty one as well as one for the `name`. To use Neo4j later on, you need the empty constructor. The name-based one is for convenience.

> **Note:** In this guide, the typical getters and setters are omitted for brevity.

The `Person` class is annotated `@NodeEntity`. When Neo4j stores it, it results in the creation of a new node. This class also has an `id` marked `@GraphId`. Neo4j uses `@GraphId` internally to track the data.

The next important piece is the set of `teammates`. It is a simple `Set<Person>`, but marked up as `@RelatedTo`. This means that every member of this set is expected to also exist as a separate `Person` node. Note how the direction is set to `BOTH`. This means that when you generate a `TEAMMATE` relationship in one direction, it exists in the other direction as well.

With the `worksWith()` method, you can easily link people together.

Finally, you have a convenient `toString()` method to print out the person's name and that person's co-workers.

Create simple queries
---------------------
Spring Data Neo4j is focused on storing data in Neo4j. But it inherits functionality from the Spring Data Commons project, including the ability to derive queries. Essentially, you don't have to learn the query language of Neo4j, but can simply write a handful of methods and the queries are written for you.

To see how this works, create an interface that queries `Person` nodes.

`src/main/java/hello/PersonRepository.java`
```java
package hello;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface PersonRepository extends GraphRepository<Person> {

    Person findByName(String name);

    Iterable<Person> findByTeammatesName(String name);

}
```
    
`PersonRepository` extends the `GraphRepository` class and plugs in the type it operates on: `Person`. Out-of-the-box, this interface comes with many operations, including standard CRUD (change-replace-update-delete) operations.

But you can define other queries as needed by simply declaring their method signature. In this case, you added `findByName`, which seeks nodes of type `Person`and finds the one that matches on `name`. You also have `findByTeammatesName`, which looks for a `Person` node, drills into each entry of the `teammates` field, and matches based on the teammate's `name`.

Let's wire this up and see what it looks like!

Create an application class
---------------------------------
Create an Application class with all the components.

`src/main/java/hello/Application.java`
```java
package hello;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.core.GraphDatabase;

@Configuration
@EnableNeo4jRepositories
public class Application extends Neo4jConfiguration {

    @Bean
    EmbeddedGraphDatabase graphDatabaseService() {
        return new EmbeddedGraphDatabase("accessingdataneo4j.db");
    }

    @Autowired
    PersonRepository repository;

    public static void main(String[] args) throws IOException {
        FileUtils.deleteRecursively(new File("accessingdataneo4j.db"));

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Application.class);

        Person greg = new Person("Greg");
        Person roy = new Person("Roy");
        Person craig = new Person("Craig");

        System.out.println("Before linking up with Neo4j...");
        for (Person person : new Person[]{greg, roy, craig}) {
            System.out.println(person);
        }

        PersonRepository personRepository = ctx.getBean(PersonRepository.class);
        GraphDatabase graphDatabase = ctx.getBean(GraphDatabase.class);

        Transaction tx = graphDatabase.beginTx();
        try {
            personRepository.save(greg);
            personRepository.save(roy);
            personRepository.save(craig);
            
            greg = personRepository.findByName(greg.name);
            greg.worksWith(roy);
            greg.worksWith(craig);
            personRepository.save(greg);

            roy = personRepository.findByName(roy.name);
            roy.worksWith(craig);
            // We already know that roy works with greg
            personRepository.save(roy);
            
            // We already know craig works with roy and greg

            tx.success();
        } finally {
            tx.finish();
        }

        System.out.println("Lookup each person by name...");
        for (String name: new String[]{greg.name, roy.name, craig.name}) {
            System.out.println(personRepository.findByName(name));
        }

        System.out.println("Looking up who works with Greg...");
        for (Person person : personRepository.findByTeammatesName("Greg")) {
            System.out.println(person.name + " works with Greg.");
        }

        ctx.close();

    }

}
```

In the configuration, you need to add the `@EnableNeo4jRepositories` annotation as well as extend the `Neo4jConfiguration` class to conveniently spin up needed components.

One piece that's missing is the graph database service bean. In this case, you are using the `EmbeddedGraphDatabase`, which creates and reuses a file-based data store at **accessingdataneo4j.db**.

> **Note:** In a production environment, you would probably connect to a standalone, running Neo4j server instead.

You autowire an instance of `PersonRepository` that you defined earlier. Spring Data Neo4j will dynamically create a concrete class that implements that interface and will plug in the needed query code to meet the interface's obligations.

The `public static void main` includes code to create an application context and then define relationships.

In this case, you create three local `Person`s, **Greg**, **Roy**, and **Craig**.Initially, they only exist in memory. It's also important to note that no one is a teammate of anyone (yet).

To store anything in Neo4j, you must start a transaction using the `graphDatabase`. In there, you will save each person. Then, you fetch each person, and link them together.

At first, you find Greg and indicate that he works with Roy and Craig, then persist him again. Remember, the teammate relationship was marked as `BOTH`, that is, bidirectional. That means that Roy and Craig will have been updated as well.

That's why when you need to update Roy, it's critical that you fetch that record from Neo4j first. You need the latest status on Roy's teammates before adding **Craig** to the list.

Why is there no code that fetches Craig and adds any relationships? Because you already have! **Greg** earlier tagged Craig as a teammate, and so did Roy. That means there is no need to update Craig's relationships again. You can see it as you iterate over each team member and print their information to the console.

Finally, check out that other query where you look backwards, answering the question "who works with whom?"


Build the application
------------------------

To build this application, you need to add some extra bits to your pom.xml file.

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>2.1</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<transformers>
								<transformer
									implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
									<mainClass>hello.Application</mainClass>
								</transformer>
							</transformers>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

The [Maven Shade plugin][maven-shade-plugin] extracts classes from all jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.

Now run the following to produce a single executable JAR file containing all necessary dependency classes and resources:

```sh
$ mvn package
```

[maven-shade-plugin]: https://maven.apache.org/plugins/maven-shade-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/) instead.

Run the application
-------------------
Run your application with `java -jar` at the command line:

```sh
$ java -jar target/gs-accessing-data-neo4j-0.1.0.jar
```

    
You should see something like this (with other stuff like queries as well):
```
Before linking up with Neo4j...
Greg's teammates include

Roy's teammates include

Craig's teammates include

Lookup each person by name...
Greg's teammates include
	- Craig
	- Roy

Roy's teammates include
	- Craig
	- Greg

Craig's teammates include
	- Roy
	- Greg

Looking up who works with Greg...
Roy works with Greg.
Craig works with Greg.
```

You can see from the output that initially no one is connected by any relationship. Then after adding people in, they are tied together. Finally, you can see the handy query that looks up people based on teammate.

With the debug levels of Spring Data Neo4j turned up, you are also getting a glimpse of the query language used with Neo4j. 

Summary
-------
Congratulations! You just set up an embedded Neo4j server, stored some simple, related entities, and developed some quick queries.

[u-nosql]: /understanding/nosql
