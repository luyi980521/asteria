package io.asteria.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.asteria")
@EnableScheduling
public class AsteriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsteriaApplication.class, args);
    }
}
