package org.exemplo.bellory.config;

import org.exemplo.bellory.service.DatabaseSeederService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DataInitializer {

    /**
     * Este Bean executa na inicialização do Spring.
     * Sua única responsabilidade agora é chamar o DatabaseSeederService,
     * que contém toda a lógica de negócio e o controle transacional.
     * @param seederService O serviço que contém a lógica de popular o banco.
     * @return um CommandLineRunner.
     */
    @Bean
    public CommandLineRunner loadData(DatabaseSeederService seederService) {
        return args -> {
            System.out.println("Iniciando a carga de dados iniciais via Seeder Service...");
            seederService.seedDatabase();
            System.out.println("Carga de dados iniciais finalizada com sucesso.");
        };
    }
}
