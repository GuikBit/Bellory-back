package org.exemplo.bellory;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BelloryApplication {


    public static void main(String[] args) {
        SpringApplication Bellory = new SpringApplication(BelloryApplication.class);
        Bellory.run(args);
    }
}
