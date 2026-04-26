package com.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class Application {

	public static void main(String[] args) {
        System.out.println(java.util.TimeZone.getDefault());
		SpringApplication.run(Application.class, args);
	}
}
