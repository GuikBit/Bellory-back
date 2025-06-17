package org.exemplo.bellory.config;

import org.exemplo.bellory.model.entity.*;
import org.exemplo.bellory.model.entity.landingPage.ContentBlock;
import org.exemplo.bellory.model.entity.landingPage.LandingPage;
import org.exemplo.bellory.model.entity.landingPage.Section;
import org.exemplo.bellory.model.entity.users.*;
import org.exemplo.bellory.model.repository.*;
import org.exemplo.bellory.model.repository.users.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    public CommandLineRunner loadData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            AdminRepository adminRepository,
            FuncionarioRepository funcionarioRepository,
            ClienteRepository clienteRepository,
            LandingPageRepository landingPageRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // --- 1. Cria as Roles Básicas se não existirem ---
            Role roleAdmin = roleRepository.findByNome("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

            Role roleFuncionario = roleRepository.findByNome("ROLE_FUNCIONARIO")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_FUNCIONARIO")));

            Role roleCliente = roleRepository.findByNome("ROLE_CLIENTE")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE")));

            System.out.println("Roles básicas verificadas/criadas.");

            // --- 2. Cria os Utilizadores de Teste com as suas Roles ---

            // Utilizador Admin
            if (userRepository.findByUsername("admin").isEmpty()) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setEmail("admin@bellory.com");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setRoles(Set.of(roleAdmin));
                adminRepository.save(admin);
                System.out.println("Utilizador Admin criado.");
            }

            // Utilizador Funcionário
            if (userRepository.findByUsername("funcionario").isEmpty()) {
                Funcionario funcionario = new Funcionario();
                funcionario.setUsername("funcionario");
                funcionario.setEmail("func@bellory.com");
                funcionario.setPassword(passwordEncoder.encode("password"));
                funcionario.setCargo("Cabeleireiro");
                funcionario.setRoles(Set.of(roleFuncionario));
                funcionarioRepository.save(funcionario);
                System.out.println("Utilizador Funcionário criado.");
            }

            // Utilizador Cliente
            if (userRepository.findByUsername("cliente").isEmpty()) {
                Cliente cliente = new Cliente();
                cliente.setUsername("cliente");
                cliente.setEmail("cliente@email.com");
                cliente.setPassword(passwordEncoder.encode("password"));
                cliente.setNomeCompleto("Ana Silva");
                cliente.setTelefone("99999-8888");
                cliente.setDataNascimento(LocalDate.of(1995, 5, 15));
                cliente.setRoles(Set.of(roleCliente));
                clienteRepository.save(cliente);
                System.out.println("Utilizador Cliente criado.");
            }

            // --- 3. Cria a Landing Page "home" associada ao Admin ---
            User adminUser = userRepository.findByUsername("admin").get();
            if (landingPageRepository.findBySlug("home").isEmpty()) {
                System.out.println("Criando landing page de exemplo para o slug 'home'...");

                LandingPage homePage = new LandingPage();
                homePage.setSlug("home");
                homePage.setInternalTitle("Página Principal");
                homePage.setUser(adminUser); // Associa ao Admin

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
