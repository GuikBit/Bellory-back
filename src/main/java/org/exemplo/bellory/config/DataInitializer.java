package org.exemplo.bellory.config;

import org.exemplo.bellory.model.entity.ContentBlock;
import org.exemplo.bellory.model.entity.LandingPage;
import org.exemplo.bellory.model.entity.Section;
import org.exemplo.bellory.model.entity.User;
import org.exemplo.bellory.model.repository.LandingPageRepository;
import org.exemplo.bellory.model.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
public class DataInitializer {

    // @Transactional é importante aqui para garantir que todas as operações
    // com o banco de dados ocorram dentro de uma única transação.
    @Bean
    @Transactional
    public CommandLineRunner loadData(
            UserRepository userRepository,
            LandingPageRepository landingPageRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // --- 1. Cria o Usuário de Teste ---
            User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername("admin");
                newUser.setPassword(passwordEncoder.encode("password"));
                userRepository.save(newUser);
                System.out.println("Usuário de teste 'admin' criado.");
                return newUser;
            });

            // --- 2. Cria a Landing Page "home" se ela não existir ---
            if (landingPageRepository.findBySlug("home").isEmpty()) {
                System.out.println("Criando landing page de exemplo para o slug 'home'...");

                LandingPage homePage = new LandingPage();
                homePage.setSlug("home");
                homePage.setInternalTitle("Página Principal");
                homePage.setUser(adminUser);

                // --- Seção 1: HERO ---
                Section heroSection = new Section();
                heroSection.setLandingPage(homePage);
                heroSection.setSectionType("HERO");
                heroSection.setDisplayOrder(0);

                ContentBlock heroTitle = new ContentBlock();
                heroTitle.setSection(heroSection);
                heroTitle.setContentKey("title");
                heroTitle.setContentValue("Bem-vindo ao Bellory!");

                ContentBlock heroSubtitle = new ContentBlock();
                heroSubtitle.setSection(heroSection);
                heroSubtitle.setContentKey("subtitle");
                heroSubtitle.setContentValue("A sua plataforma completa para criar páginas incríveis.");

                ContentBlock heroButton = new ContentBlock();
                heroButton.setSection(heroSection);
                heroButton.setContentKey("buttonText");
                heroButton.setContentValue("Começar Agora");

                heroSection.setContentBlocks(List.of(heroTitle, heroSubtitle, heroButton));

                // --- Seção 2: FEATURES ---
                Section featuresSection = new Section();
                featuresSection.setLandingPage(homePage);
                featuresSection.setSectionType("FEATURES_GRID");
                featuresSection.setDisplayOrder(1);

                ContentBlock feature1Title = new ContentBlock();
                feature1Title.setSection(featuresSection);
                feature1Title.setContentKey("feature1_title");
                feature1Title.setContentValue("Editor Visual");

                ContentBlock feature1Text = new ContentBlock();
                feature1Text.setSection(featuresSection);
                feature1Text.setContentKey("feature1_text");
                feature1Text.setContentValue("Crie e personalize as suas páginas com um editor de arrastar e soltar.");

                ContentBlock feature2Title = new ContentBlock();
                feature2Title.setSection(featuresSection);
                feature2Title.setContentKey("feature2_title");
                feature2Title.setContentValue("Modelos Prontos");

                ContentBlock feature2Text = new ContentBlock();
                feature2Text.setSection(featuresSection);
                feature2Text.setContentKey("feature2_text");
                feature2Text.setContentValue("Comece rapidamente com a nossa biblioteca de modelos profissionais.");

                featuresSection.setContentBlocks(List.of(feature1Title, feature1Text, feature2Title, feature2Text));

                // Adiciona as seções à página
                homePage.setSections(List.of(heroSection, featuresSection));

                // Salva a página (e, por cascata, as seções e blocos de conteúdo)
                landingPageRepository.save(homePage);
                System.out.println("Landing page 'home' criada com sucesso.");
            }
        };
    }
}
