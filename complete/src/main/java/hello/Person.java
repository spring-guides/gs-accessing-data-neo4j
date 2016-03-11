package hello;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class Person {

    @GraphId
    private Long id;

    private String name;

	/**
     * Neo4j requires a no-arg constructor much like JPA
     */
    private Person() {}

    public Person(String name) { this.name = name; }

	/**
     * Neo4j doesn't REALLY have bi-directional relationships.
     * It just means when querying to ignore the direction of
     * the relationship.
     * https://dzone.com/articles/modelling-data-neo4j
     */
    @RelatedTo(type="TEAMMATE", direction=Direction.BOTH)
    public @Fetch Set<Person> teammates;

    public void worksWith(Person person) {
        if (teammates == null) {
            teammates = new HashSet<>();
        }
        teammates.add(person);
    }

    public String toString() {

        return this.name + "'s teammates => " +
            Optional.ofNullable(this.teammates)
                .orElse(Collections.emptySet())
                .stream()
                    .map(person -> person.getName())
                    .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
