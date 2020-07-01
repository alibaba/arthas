package com.example.arthasspringbootstarterexample;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArthasSpringBootStarterExampleApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(ArthasSpringBootStarterExampleApplication.class, args);
		System.out.println("xxxxxxxxxxxxxxxxxx");
		TimeUnit.SECONDS.sleep(3);
		System.exit(0);
	}

}
