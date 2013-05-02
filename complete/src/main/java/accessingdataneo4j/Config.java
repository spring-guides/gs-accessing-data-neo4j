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
