This guide walks you through the process of using Spring Data to build an application with Neo4j.

What you'll build
-----------------

You'll use Neo4j's [NoSQL][u-nosql] graph-based data store to build an embedded Neo4j server, store entities and relationships, and develop queries.

What you'll need
----------------

 - About 15 minutes
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Gradle 1.8+][gradle] or [Maven 3.0+][mvn]
 - You can also import the code from this guide as well as view the web page directly into [Spring Tool Suite (STS)][gs-sts] and work your way through it from there.

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[gradle]: http://www.gradle.org/
[mvn]: http://maven.apache.org/download.cgi
[gs-sts]: /guides/gs/sts

How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/guides/gs), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [Git][u-git]:
`git clone https://github.com/spring-guides/gs-accessing-data-neo4j.git`
 - cd into `gs-accessing-data-neo4j/initial`.
 - Jump ahead to [Define a simple entity](#initial).

**When you're finished**, you can check your results against the code in `gs-accessing-data-neo4j/complete`.
[zip]: https://github.com/spring-guides/gs-accessing-data-neo4j/archive/master.zip
[u-git]: /understanding/Git


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Gradle](http://gradle.org) and [Maven](https://maven.apache.org) is included here. If you're not familiar with either, refer to [Building Java Projects with Gradle](/guides/gs/gradle/) or [Building Java Projects with Maven](/guides/gs/maven).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello


### Create a Maven build file

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.springframework</groupId>
    <artifactId>gs-acessing-data-neo4j</artifactId>
    <version>0.1.0</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>0.5.0.M6</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-neo4j</artifactId>
            <version>2.3.2.RELEASE</version>
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
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin> 
                <artifactId>maven-compiler-plugin</artifactId> 
                <version>3.1</version> 
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>http://repo.spring.io/libs-milestone</url>
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
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>http://repo.spring.io/libs-milestone</url>
        </pluginRepository>
    </pluginRepositories>

</project>
```
    

> **Note:** This guide is using [Spring Boot](/guides/gs/spring-boot/).

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

Create an Application class
---------------------------------
Create an Application class with all the components.

`src/main/java/hello/Application.java`
```java
package hello;

import java.io.File;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

@Configuration
@EnableNeo4jRepositories
public class Application extends Neo4jConfiguration implements CommandLineRunner {

    @Bean
    EmbeddedGraphDatabase graphDatabaseService() {
        return new EmbeddedGraphDatabase("accessingdataneo4j.db");
    }

    @Autowired
    PersonRepository personRepository;

    @Autowired
    GraphDatabase graphDatabase;

    public void run(String... args) throws Exception {
        Person greg = new Person("Greg");
        Person roy = new Person("Roy");
        Person craig = new Person("Craig");

        System.out.println("Before linking up with Neo4j...");
        for (Person person : new Person[]{greg, roy, craig}) {
            System.out.println(person);
        }

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
    }

    public static void main(String[] args) throws Exception {
        FileUtils.deleteRecursively(new File("accessingdataneo4j.db"));

        SpringApplication.run(Application.class, args);
    }

}
```

In the configuration, you need to add the `@EnableNeo4jRepositories` annotation as well as extend the `Neo4jConfiguration` class to conveniently spin up needed components.

One piece that's missing is the graph database service bean. In this case, you are using the `EmbeddedGraphDatabase`, which creates and reuses a file-based data store at **accessingdataneo4j.db**.

> **Note:** In a production environment, you would probably connect to a standalone, running Neo4j server instead.

You autowire an instance of `PersonRepository` that you defined earlier. Spring Data Neo4j will dynamically create a concrete class that implements that interface and will plug in the needed query code to meet the interface's obligations.

The `public statuc void main` uses Spring Boot's `SpringApplication.run()` to launch the application and invoke the `CommandLineRunner` that builds the relationships.

In this case, you create three local `Person`s, **Greg**, **Roy**, and **Craig**.Initially, they only exist in memory. It's also important to note that no one is a teammate of anyone (yet).

To store anything in Neo4j, you must start a transaction using the `graphDatabase`. In there, you will save each person. Then, you fetch each person, and link them together.

At first, you find Greg and indicate that he works with Roy and Craig, then persist him again. Remember, the teammate relationship was marked as `BOTH`, that is, bidirectional. That means that Roy and Craig will have been updated as well.

That's why when you need to update Roy, it's critical that you fetch that record from Neo4j first. You need the latest status on Roy's teammates before adding **Craig** to the list.

Why is there no code that fetches Craig and adds any relationships? Because you already have! **Greg** earlier tagged Craig as a teammate, and so did Roy. That means there is no need to update Craig's relationships again. You can see it as you iterate over each team member and print their information to the console.

Finally, check out that other query where you look backwards, answering the question "who works with whom?"


### Build an executable JAR

Now that your `Application` class is ready, you simply instruct the build system to create a single, executable jar containing everything. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.

Add the following configuration to your existing Maven POM:

`pom.xml`
```xml
    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

The `start-class` property tells Maven to create a `META-INF/MANIFEST.MF` file with a `Main-Class: hello.Application` entry. This entry enables you to run it with `mvn spring-boot:run` (or simply run the jar itself with `java -jar`).

The [Spring Boot maven plugin][spring-boot-maven-plugin] collects all the jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.

Now run the following command to produce a single executable JAR file containing all necessary dependency classes and resources:

```sh
$ mvn package
```

[spring-boot-maven-plugin]: https://github.com/spring-projects/spring-boot/tree/master/spring-boot-tools/spring-boot-maven-plugin

> **Note:** The procedure above will create a runnable JAR. You can also opt to [build a classic WAR file](/guides/gs/convert-jar-to-war/) instead.

Run the service
-------------------
Run your service using the spring-boot plugin at the command line:

```sh
$ mvn spring-boot:run
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

[u-nosql]: /understanding/NoSQL
