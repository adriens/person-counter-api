package com.adriens.personcounterapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAutoConfiguration
@EnableScheduling
@SpringBootApplication
/**
 * Spring Boot starter class
 */
public class PersonCounterApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(PersonCounterApiApplication.class, args);
	}

}
