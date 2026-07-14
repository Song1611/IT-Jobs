package com.itjob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class ItJobApplication {

	public static void main(String[] args) {
		// Set default timezone to UTC to avoid PostgreSQL timezone issues
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ItJobApplication.class, args);
	}

}
