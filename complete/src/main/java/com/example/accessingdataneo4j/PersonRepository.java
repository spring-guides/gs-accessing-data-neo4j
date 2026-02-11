package com.example.accessingdataneo4j;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface PersonRepository extends Neo4jRepository<Person, Long> {

	@Nullable
	Person findByName(String name);

	List<Person> findByTeammatesName(String name);
}
