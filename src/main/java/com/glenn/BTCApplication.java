package com.glenn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BTCApplication {


    public static void main(String[] args) {
        SpringApplication.run(BTCApplication.class, args);
    }

}
