package org.exemplo.bellory.config;

import org.exemplo.bellory.service.DatabaseSeederService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    public CommandLineRunner loadData(DatabaseSeederService seeder) {
        return args -> {
            try {
               seeder.seedDatabase();
            } catch (Exception e) {
                System.err.println("[dev] DataInitializer: falha no seed — " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
