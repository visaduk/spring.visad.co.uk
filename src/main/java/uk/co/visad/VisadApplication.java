package uk.co.visad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class VisadApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisadApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Set default timezone to Europe/London
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }
}
