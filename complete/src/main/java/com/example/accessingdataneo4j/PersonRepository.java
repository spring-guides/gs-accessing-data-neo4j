package com.example.accessingdataneo4j;

import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends CrudRepository<Person, Long> {

	Person findByName(String name);
}
