package com.portofolio.medical_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicalAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicalAppApplication.class, args);
	}

}
