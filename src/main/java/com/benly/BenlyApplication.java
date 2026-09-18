package com.benly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class BenlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenlyApplication.class, args);
    }

}
