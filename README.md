Getting Started: Accessing Data with Neo4j
==========================================

This Getting Started guide will walk you through the process of building an application using Neo4j's graph-based data store.

To help you get started, we've provide an initial project structure as well as the completed project for you in GitHub:

```sh
$ git clone https://github.com/springframework-meta/gs-accessing-data-neo4j.git
```

In the `start` folder, you'll find a bare project, ready for you to copy-n-paste code snippets from this document. In the `complete` folder, you'll find the complete project code.

Before we start storing and querying objects with Neo4j, there is some initial project setup that's required. Or, you can skip straight to the [fun part]().

Selecting Dependencies
----------------------
The sample in this Getting Started Guide will leverage Spring Data Neo4j and JSR 303's validation. Therefore, the following dependencies are needed in the project's build configuration:

- org.springframework.data:spring-data-neo4j:2.2.0.RELEASE
- javax.validation:validation-api:1.0.0.GA
- org.slf4j:slf4j-log4j12:1.7.5

Refer to the [Gradle Getting Started Guide]() of the [Maven Getting Started Guide]() for details on how to include these dependencies in your build.

Configuring a runnable application
----------------------------------
First of all, we need to create a basic runnable application.

```java
package accessingdataneo4j;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Application {
	
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
	}
	
}
```

This application will load an application context from the `Config` class. Let's define that next:

```java
package accessingdataneo4j;

import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

}
```

Our configuration can't get much simpler. We essentially don't have any components defined yet. But we'll be adding some soon.

To finish setting things up, let's configure some logging options in **log4j.properties**.

```txt
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

This will print detailed messages about Spring Data Neo4j to provide insight into what's happening. Now let's run our barebone application.

```sh
$ ./gradlew run
```

We should see something like this:

```sh
0    [main] INFO  org.springframework.context.annotation.AnnotationConfigApplicationContext  - Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@622d8a59: startup date [Thu May 02 15:42:19 CDT 2013]; root of context hierarchy
145  [main] INFO  org.springframework.beans.factory.support.DefaultListableBeanFactory  - Pre-instantiating singletons in org.springframework.beans.factory.support.DefaultListableBeanFactory@25b13009: defining beans [org.springframework.context.annotation.internalConfigurationAnnotationProcessor,org.springframework.context.annotation.internalAutowiredAnnotationProcessor,org.springframework.context.annotation.internalRequiredAnnotationProcessor,org.springframework.context.annotation.internalCommonAnnotationProcessor,config,org.springframework.context.annotation.ConfigurationClassPostProcessor.importAwareProcessor]; root of factory hierarchy
```

With all this setup, let's dive into creating some basic relationships.

Defining a simple entity
------------------------
Neo4j is designed to capture entities and their relationships, with both aspects being of equal importance. Let's imagine we are modeling a system where we store a record for each person. But we also want to track a person's teammates. With Neo4j, we can capture all that with some simple annotations.

```java
package accessingdataneo4j;

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

Here we have a `Person` class that has only one attribute, their `name`. We have two constructors, an empty one as well as one for the `name`. To use Neo4j later on, we need the empty constructor. The name-based one is for convenience.

> In this guide, the typical getters and setters have been left out for brevity.

You'll notice this class is annotated `@NodeEntity`. When Neo4j stores it, it will result in the creation of a new node. This class also has an `id` marked as `@GraphId`. This is for internal usage to help Neo4j track the data.

The next important piece is our set of `teammates`. It is a simple `Set<Person>`, but marked up as `@RelatedTo`. This means that every member of this set is expected to also exist as a separate `Person` node. An important part to notice is how the direction is set to `BOTH`. This means that when we generate a `TEAMMATE` relationship in one direction, it exists in the other direction as well.

As a convenience method, we have the `worksWith()` method. This way, we can easily link people together.

Finally, we have a convenient `toString()` method to print out the person's name and the people he or she works with.

Creating some simple queries
----------------------------
Spring Data Neo4j is focused on storing data in Neo4j. But it inherits much functionality from the Spring Data Commons project. This includes it's powerful ability to derive queries. Essentially, we don't have to learn the query language of Neo4j, but can simply write a handful of methods and the queries are written for us.

To show how, let's create an interface that is focused on querying `Person` nodes.

```java
package accessingdataneo4j;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface PersonRepository extends GraphRepository<Person> {
	
	Person findByName(String name);
	
	Iterable<Person> findByTeammatesName(String name);

}
```

`PersonRepository` extends the `GraphRepository` class and plugs in the type it operates on: `Person`. Out-of-the-box, this interface comes with a lot of operations, including standard CRUD (change-replace-update-delete) operations.

But we can define other queries as needed by simply declaring their method signature. In this case, we added `findByName`, which essentially will seek nodes of type `Person` and find the one that matches on `name`. We also have `findByTeammatesName`, which looks for a `Person` node, drills into each `teammate`, and matches based on the teammate's `name`.

Let's wire this up and see what it looks like!

Wiring the application components
---------------------------------
We need to update our `Config` class with some new components.

```java
package accessingdataneo4j;

import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

@Configuration
@EnableNeo4jRepositories
public class Config extends Neo4jConfiguration {

	@Bean
	EmbeddedGraphDatabase graphDatabaseService() {
		return new EmbeddedGraphDatabase("accessingdataneo4j.db");
	}

	@Autowired
	PersonRepository repository;

}
```

In our configuration, we need to add the `@EnableNeo4jRepositories` as well as extend the `Neo4jConfiguration` class to conveniently spin up needed components.

One piece that's missing and requires us to choose, is providing a graph database service. In this case, we are picking the `EmbeddedGraphDatabase` which creates and re-uses a file-based data store at **accessingdataneo4j.db**.

Finally, we autowire an instance of `PersonRepository` that we just defined up above. Spring Data Neo4j will dynamically create a concrete class that implements our interface and will plugin the needed query code to meet the interface's obligations.

Now let's build an application to use it!

Building an application
-----------------------
For the purposes of this guide, we will create three people, and link them together. Then we can query them. This is an incredibly simple set of relationships to demonstrate the power of Neo4j. 

```java
package accessingdataneo4j;

import org.neo4j.graphdb.Transaction;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.core.GraphDatabase;

public class Application {
	
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);

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
		
	}
	
}
```

In this case, we are creating three local `Person`s, Greg, Roy, and Craig. Initially, they only exist in memory. It's also important to note that no one is a teammate to anyone (yet).

To store anything in Neo4j, we must start a transaction using the `graphDatabase`. In there, we will save each person. Then, we need to fetch each person, and link them together.

At first, we find Greg and indicate that he works with Roy and Craig, then persist him again. Remember, the teammate relationship was marked at `BOTH`, i.e. bidirectional. That means that Roy and Craig will have been updated as well.

That's why, when we need to update Roy, it's critical we fetch from Neo4j first. We need the latest status on Roy's teammates before adding Craig to the list.

Why didn't we fetch Craig and add any relationships? Because we already have! Greg earlier tagged Craig as a teammate, and so did Roy. That means there is no need to update that again. We can see it as we iterate over each team member and print their information to the console.

Finally, let's check out that other query where we look backwards, answering the question "who works with whom?"

Running the Application
-----------------------
Now that we have coded everything up, let's run it!

```sh
$ ./gradlew run
```

We should see something like:

```sh
0    [main] INFO  org.springframework.context.annotation.AnnotationConfigApplicationContext  - Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@761b22ed: startup date [Thu May 02 16:11:04 CDT 2013]; root of context hierarchy
342  [main] INFO  org.springframework.beans.factory.support.DefaultListableBeanFactory  - Pre-instantiating singletons in org.springframework.beans.factory.support.DefaultListableBeanFactory@63d1e70a: defining beans [org.springframework.context.annotation.internalConfigurationAnnotationProcessor,org.springframework.context.annotation.internalAutowiredAnnotationProcessor,org.springframework.context.annotation.internalRequiredAnnotationProcessor,org.springframework.context.annotation.internalCommonAnnotationProcessor,config,org.springframework.context.annotation.ConfigurationClassPostProcessor.importAwareProcessor,org.springframework.data.repository.core.support.RepositoryInterfaceAwareBeanPostProcessor#0,personRepository,graphDatabaseService,graphDatabase,mappingInfrastructure,isNewStrategyFactory,neo4jTemplate,relationshipTypeRepresentationStrategy,nodeTypeRepresentationStrategy,typeRepresentationStrategyFactory,entityStateHandler,nodeTypeMapper,relationshipTypeMapper,entityFetchHandler,nodeStateTransmitter,neo4jConversionService,graphRelationshipInstantiator,graphEntityInstantiator,neo4jMappingContext,entityAlias,relationshipEntityStateFactory,nodeEntityStateFactory,nodeDelegatingFieldAccessorFactory,relationshipDelegatingFieldAccessorFactory,neo4jTransactionManager,indexCreationMappingEventListener,configurationCheck,persistenceExceptionTranslator,indexProvider]; root of factory hierarchy
935  [main] INFO  org.springframework.transaction.jta.JtaTransactionManager  - Using JTA UserTransaction: org.neo4j.kernel.impl.transaction.UserTransactionImpl@7c4214de
935  [main] INFO  org.springframework.transaction.jta.JtaTransactionManager  - Using JTA TransactionManager: org.neo4j.kernel.impl.transaction.SpringTransactionManager@56683a8d
1338 [main] DEBUG org.springframework.data.neo4j.repository.query.DerivedCypherRepositoryQuery  - Derived query: START `person`=node:__types__(className="accessingdataneo4j.Person") WHERE `person`.`name`! = {0} RETURN `person`from method Repository-Graph-Query-Method for public abstract accessingdataneo4j.Person accessingdataneo4j.PersonRepository.findByName(java.lang.String)
1340 [main] DEBUG org.springframework.data.neo4j.repository.query.DerivedCypherRepositoryQuery  - Derived query: START `person`=node:__types__(className="accessingdataneo4j.Person") MATCH `person`-[:`TEAMMATE`]-`person_teammates` WHERE `person_teammates`.`name`! = {0} RETURN `person`from method Repository-Graph-Query-Method for public abstract java.lang.Iterable accessingdataneo4j.PersonRepository.findByTeammatesName(java.lang.String)
Before linking up with Neo4j...
Greg's teammates include

Roy's teammates include

Craig's teammates include

1403 [main] DEBUG org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory  - Factory org.springframework.data.neo4j.fieldaccess.IdFieldAccessorFactory@62455eba used for field: class java.lang.Long id rel: false idx: false
1405 [main] DEBUG org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory  - Factory org.springframework.data.neo4j.fieldaccess.PropertyFieldAccessorFactory@1e28a987 used for field: class java.lang.String name rel: false idx: false
1405 [main] DEBUG org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory  - Factory org.springframework.data.neo4j.fieldaccess.RelatedToCollectionFieldAccessorFactory@40bde86b used for field: interface java.util.Set teammates rel: true idx: false
1410 [main] DEBUG org.springframework.data.neo4j.mapping.EntityInstantiator  - Using class accessingdataneo4j.Person no-arg constructor
Lookup each person by name...
1538 [main] DEBUG org.springframework.data.neo4j.support.query.CypherQueryEngine  - Executing cypher query: START `person`=node:__types__(className="accessingdataneo4j.Person") WHERE `person`.`name`! = {0} RETURN `person` params {0=Greg}
Greg's teammates include
	- Craig
	- Roy

2129 [main] DEBUG org.springframework.data.neo4j.support.query.CypherQueryEngine  - Executing cypher query: START `person`=node:__types__(className="accessingdataneo4j.Person") WHERE `person`.`name`! = {0} RETURN `person` params {0=Roy}
Roy's teammates include
	- Craig
	- Greg

2225 [main] DEBUG org.springframework.data.neo4j.support.query.CypherQueryEngine  - Executing cypher query: START `person`=node:__types__(className="accessingdataneo4j.Person") WHERE `person`.`name`! = {0} RETURN `person` params {0=Craig}
Craig's teammates include
	- Roy
	- Greg

Looking up who works with Greg...
2229 [main] DEBUG org.springframework.data.neo4j.support.query.CypherQueryEngine  - Executing cypher query: START `person`=node:__types__(className="accessingdataneo4j.Person") MATCH `person`-[:`TEAMMATE`]-`person_teammates` WHERE `person_teammates`.`name`! = {0} RETURN `person` params {0=Greg}
Roy works with Greg.
Craig works with Greg.
```

We can see from the output that initially no one is connected by any relationship. Then as we add people in, they are tied together. Finally, we can see our handy query that looks up people based on teammate.

With the debug levels of Spring Data Neo4j turned up, we are also getting a glimpse of the query language used with Neo4j. We won't delve into that, but if you like, you can investigate that in some of our other Getting Started Guides.

Next Steps
----------
Congratulations! You just setup an embedded Neo4j server, stored some simple, related entities, and developed some quick queries.