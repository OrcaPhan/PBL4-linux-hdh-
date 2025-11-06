package com.orca.pbl4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point của ORCA System Monitor - Spring Boot REST API
 */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("ORCA System Monitor API started on http://localhost:8080");
    }
}
