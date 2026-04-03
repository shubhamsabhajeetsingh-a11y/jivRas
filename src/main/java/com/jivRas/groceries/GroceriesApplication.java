package com.jivRas.groceries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaRepositories
@EnableKafka
@EnableCaching
public class GroceriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GroceriesApplication.class, args);
	}

}
