package org.exemplo.bellory;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BelloryApplication {


    public static void main(String[] args) {
        SpringApplication Bellory = new SpringApplication(BelloryApplication.class);
        Bellory.run(args);
    }
}
