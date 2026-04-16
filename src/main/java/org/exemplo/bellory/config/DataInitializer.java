package org.exemplo.bellory.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DataInitializer {

    /**
     * Placeholder de inicializacao para o profile dev. Deixa um gancho para
     * eventuais seeds futuros — o seeder legado foi removido junto com o ciclo
     * Asaas/PlanoBellory local.
     */
    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            System.out.println("[dev] DataInitializer: sem seeders ativos.");
        };
    }
}
