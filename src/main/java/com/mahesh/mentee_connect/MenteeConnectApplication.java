package com.mahesh.mentee_connect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class MenteeConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(MenteeConnectApplication.class, args);
    }
}
