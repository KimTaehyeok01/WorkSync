package com.worksync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WorkSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkSyncApplication.class, args);
    }
}
