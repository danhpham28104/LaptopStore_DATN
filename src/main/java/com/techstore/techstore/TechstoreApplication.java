package com.techstore.techstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 🔹 Bật scheduled tasks (OTP cleanup, Payment status check)
public class TechstoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechstoreApplication.class, args);
	}

}

