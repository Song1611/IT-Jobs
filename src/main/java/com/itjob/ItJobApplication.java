package com.itjob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ItJobApplication {

	public static void main(String[] args) {
		// Set default timezone to UTC to avoid PostgreSQL timezone issues
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ItJobApplication.class, args);
	}

}
