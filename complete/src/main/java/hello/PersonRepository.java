package hello;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface PersonRepository extends GraphRepository<Person> {
	
	Person findByName(String name);
	
	Iterable<Person> findByTeammatesName(String name);

}
