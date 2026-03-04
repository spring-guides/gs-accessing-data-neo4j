package com.example.accessingdataneo4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node
public class Person {

	@Id
	@GeneratedValue
	private @Nullable Long id;

	private String name;

	private Person() {
		// Empty constructor required as of Neo4j API 2.0.5
	};

	public Person(String name) {
		this.name = name;
	}

	@Relationship(type = "REFERRED")
	public Set<Person> referrals;

	public void referred(Person person) {
		if (referrals == null) {
			referrals = new HashSet<>();
		}
		referrals.add(person);
	}

	public String toString() {

		return this.name + " referred => "
			+ Optional.ofNullable(this.referrals).orElse(
					Collections.emptySet()).stream()
						.map(Person::getName)
						.collect(Collectors.toList());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
