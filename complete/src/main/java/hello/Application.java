package hello;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.GraphDatabase;

@SpringBootApplication
public class Application {

	private final static Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) throws Exception {
		FileUtils.deleteRecursively(new File("accessingdataneo4j.db"));

		SpringApplication.run(Application.class, args);
	}

	@Bean
	CommandLineRunner demo(PersonRepository personRepository,
						   GraphDatabase graphDatabase) {
		return args -> {

			Person greg = new Person("Greg");
			Person roy = new Person("Roy");
			Person craig = new Person("Craig");

			List<Person> team = Arrays.asList(greg, roy, craig);

			log.info("Before linking up with Neo4j...");

			team.stream()
				.forEach(person -> log.info("\t" + person.toString()));

			Transaction tx = graphDatabase.beginTx();
			try {
				personRepository.save(greg);
				personRepository.save(roy);
				personRepository.save(craig);

				greg = personRepository.findByName(greg.getName());
				greg.worksWith(roy);
				greg.worksWith(craig);
				personRepository.save(greg);

				roy = personRepository.findByName(roy.getName());
				roy.worksWith(craig);
				// We already know that roy works with greg
				personRepository.save(roy);

				// We already know craig works with roy and greg


				log.info("Lookup each person by name...");
				team.stream()
					.forEach(person ->
						log.info("\t" + personRepository
								.findByName(person.getName()).toString()));


				log.info("Lookup each person by teammate...");
				for (Person person : team) {
					log.info(person.getName() + " is a teammate of...");
					personRepository.findByTeammatesName(person.getName()).stream()
						.forEach(teammate -> log.info("\t" + teammate.getName()));
				}

				tx.success();
			} finally {
				tx.close();
			}
		};
	}

}
