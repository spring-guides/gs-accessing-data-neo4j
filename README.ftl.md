<#assign project_id="gs-accessing-data-neo4j">

Getting Started: Accessing Data with Neo4j
==========================================

What you'll build
-----------------

This guide will walk you through the process of building an application using Neo4j's graph-based data store.

What you'll need
----------------

 - About 15 minutes
 - <@prereq_editor_jdk_buildtools/>

## <@how_to_complete_this_guide jump_ahead='Defining a simple entity'/>


<a name="scratch"></a>
Set up the project
------------------

<@build_system_intro/>

<@create_directory_structure_hello/>

### Create a Maven POM

    <@snippet path="pom.xml" prefix="complete"/>

This guide also uses log4j with certain log levels turned up so you can see what Neo4j and Spring Data are doing.

    <@snippet path="src/main/resources/log4j.properties" prefix="initial"/>


<a name="initial"></a>
Defining a simple entity
------------------------
Neo4j is designed to capture entities and their relationships, with both aspects being of equal importance. Imagine you are modeling a system where you store a record for each person. But you also want to track a person's teammates. With Neo4j, you can capture all that with some simple annotations.

    <@snippet path="src/main/java/hello/Person.java" prefix="complete"/>

Here you have a `Person` class that has only one attribute, the `name`. You have two constructors, an empty one as well as one for the `name`. To use Neo4j later on, you need the empty constructor. The name-based one is for convenience.

> In this guide, the typical getters and setters have been left out for brevity.

You'll notice this class is annotated `@NodeEntity`. When Neo4j stores it, it will result in the creation of a new node. This class also has an `id` marked as `@GraphId`. This is for internal usage to help Neo4j track the data.

The next important piece is the set of `teammates`. It is a simple `Set<Person>`, but marked up as `@RelatedTo`. This means that every member of this set is expected to also exist as a separate `Person` node. An important part to notice is how the direction is set to `BOTH`. This means that when you generate a `TEAMMATE` relationship in one direction, it exists in the other direction as well.

As a convenience method, you have the `worksWith()` method. This way, you can easily link people together.

Finally, you have a convenient `toString()` method to print out the person's name and the people he or she works with.

Creating some simple queries
----------------------------
Spring Data Neo4j is focused on storing data in Neo4j. But it inherits much functionality from the Spring Data Commons project. This includes it's powerful ability to derive queries. Essentially, you don't have to learn the query language of Neo4j, but can simply write a handful of methods and the queries are written for you.

To see how this works, create an interface that is focused on querying `Person` nodes.

    <@snippet path="src/main/java/hello/PersonRepository.java" prefix="complete"/>
    
`PersonRepository` extends the `GraphRepository` class and plugs in the type it operates on: `Person`. Out-of-the-box, this interface comes with a lot of operations, including standard CRUD (change-replace-update-delete) operations.

But you can define other queries as needed by simply declaring their method signature. In this case, you added `findByName`, which essentially will seek nodes of type `Person` and find the one that matches on `name`. You also have `findByTeammatesName`, which looks for a `Person` node, drills into each entry of the `teammates` field, and matches based on the teammate's `name`.

Let's wire this up and see what it looks like!

Wiring the application components
---------------------------------
You need to create an Application class with all the components.

    <@snippet path="src/main/java/hello/Application.java" prefix="complete"/>

In the configuration, you need to add the `@EnableNeo4jRepositories` annotation as well as extend the `Neo4jConfiguration` class to conveniently spin up needed components.

One piece that's missing is the graph database service bean. In this case, you are picking the `EmbeddedGraphDatabase` which creates and re-uses a file-based data store at **accessingdataneo4j.db**.

> For production solutions, you would probably replace this with an alternative to connect to a running Neo4j server.

Finally, you autowire an instance of `PersonRepository` that you just defined up above. Spring Data Neo4j will dynamically create a concrete class that implements that interface and will plugin the needed query code to meet the interface's obligations.

Running the application
-----------------------
The `public static void main` includes code to create an application context and then define some relationships.

In this case, you  are creating three local `Person`s, **Greg**, **Roy**, and **Craig**. Initially, they only exist in memory. It's also important to note that no one is a teammate to anyone (yet).

To store anything in Neo4j, you must start a transaction using the `graphDatabase`. In there, you will save each person. Then, you need to fetch each person, and link them together.

At first, you find **Greg** and indicate that he works with **Roy** and **Craig**, then persist him again. Remember, the teammate relationship was marked as `BOTH`, i.e. bidirectional. That means that **Roy** and **Craig** will have been updated as well.

That's why when you need to update **Roy**, it's critical that you fetch that record from Neo4j first. You need the latest status on **Roy's** teammates before adding **Craig** to the list.

Why is there no code that fetches **Craig** and adds any relationships? Because you already have! **Greg** earlier tagged **Craig** as a teammate, and so did **Roy**. That means there is no need to update that again. You can see it as you iterate over each team member and print their information to the console.

Finally, check out that other query where you look backwards, answering the question "who works with whom?"


## <@build_the_application/>
    
Running the Application
-----------------------

Run your service with `java -jar` at the command line:

    java -jar target/${project_id}-complete-0.1.0.jar
    
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

With the debug levels of Spring Data Neo4j turned up, you are also getting a glimpse of the query language used with Neo4j. This guide won't delve into that, but if you like, you can investigate that in some of the other Getting Started Guides.

Summary
-------
Congratulations! You just setup an embedded Neo4j server, stored some simple, related entities, and developed some quick queries.
