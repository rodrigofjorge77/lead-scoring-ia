package org.projeto.mockcrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockCrmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockCrmServiceApplication.class, args);
    }
}
