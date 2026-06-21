package com.jinlee.devtroublemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DevtroublemateApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevtroublemateApplication.class, args);
	}

}