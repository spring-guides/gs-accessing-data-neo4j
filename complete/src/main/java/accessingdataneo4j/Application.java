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
			
			greg = personRepository.findOne(greg.id);
			greg.worksWith(roy);
			greg.worksWith(craig);
			personRepository.save(greg);

			roy = personRepository.findOne(roy.id);
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
