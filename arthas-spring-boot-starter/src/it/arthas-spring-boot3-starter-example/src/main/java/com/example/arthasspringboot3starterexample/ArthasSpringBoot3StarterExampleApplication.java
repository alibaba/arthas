package com.example.arthasspringboot3starterexample;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ArthasSpringBoot3StarterExampleApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(ArthasSpringBoot3StarterExampleApplication.class, args);
		System.out.println("xxxxxxxxxxxxxxxxxx");
		TimeUnit.SECONDS.sleep(3);
		System.exit(0);
	}

}
