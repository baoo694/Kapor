package com.kapor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KaporApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaporApplication.class, args);
    }
}
