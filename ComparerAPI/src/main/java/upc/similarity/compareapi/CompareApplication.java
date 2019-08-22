package upc.similarity.compareapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CompareApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompareApplication.class, args);
	}
}
