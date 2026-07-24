package com.digitalheroes.urlaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalHeroesUrlAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalHeroesUrlAuditApplication.class, args);
    }
}
