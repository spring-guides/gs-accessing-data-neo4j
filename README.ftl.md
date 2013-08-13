<#assign project_id="gs-accessing-data-neo4j">
This guide walks you through the process of using Spring Data to build an application with Neo4j.

What you'll build
-----------------

You'll use Neo4j's [NoSQL][u-nosql] graph-based data store to build an embedded Neo4j server, store entities and relationships, and develop queries.

What you'll need
----------------

 - About 15 minutes
 - <@prereq_editor_jdk_buildtools/>

## <@how_to_complete_this_guide jump_ahead='Define a simple entity'/>


<a name="scratch"></a>
Set up the project
------------------

<@build_system_intro/>

<@create_directory_structure_hello/>

### Create a Maven POM

    <@snippet path="pom.xml" prefix="initial"/>

This guide also uses log4j with certain log levels turned up so that you can see what Neo4j and Spring Data are doing.

    <@snippet path="src/main/resources/log4j.properties" prefix="initial"/>


<a name="initial"></a>
Define a simple entity
------------------------
Neo4j captures entities and their relationships, with both aspects being of equal importance. Imagine you are modeling a system where you store a record for each person. But you also want to track a person's co-workers (`teammates` in this example). With Neo4j, you can capture all that with some simple annotations.

    <@snippet path="src/main/java/hello/Person.java" prefix="complete"/>

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

    <@snippet path="src/main/java/hello/PersonRepository.java" prefix="complete"/>
    
`PersonRepository` extends the `GraphRepository` class and plugs in the type it operates on: `Person`. Out-of-the-box, this interface comes with many operations, including standard CRUD (change-replace-update-delete) operations.

But you can define other queries as needed by simply declaring their method signature. In this case, you added `findByName`, which seeks nodes of type `Person`and finds the one that matches on `name`. You also have `findByTeammatesName`, which looks for a `Person` node, drills into each entry of the `teammates` field, and matches based on the teammate's `name`.

Let's wire this up and see what it looks like!

Create an Application class
---------------------------------
Create an Application class with all the components.

    <@snippet path="src/main/java/hello/Application.java" prefix="complete"/>

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


## <@build_the_application/>

<@run_the_application/>
    
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

<@u_nosql/>
