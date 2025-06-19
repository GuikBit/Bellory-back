package org.exemplo.bellory.config;

import org.exemplo.bellory.model.entity.agendamento.Agendamento; // Importar Agendamento
import org.exemplo.bellory.model.entity.agendamento.Status; // Importar Status
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.landingPage.ContentBlock;
import org.exemplo.bellory.model.entity.landingPage.LandingPage;
import org.exemplo.bellory.model.entity.landingPage.Section;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.*;
import org.exemplo.bellory.model.repository.*;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository; // Novo repositório
import org.exemplo.bellory.model.repository.funcionario.DisponibilidadeRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.funcionario.JornadaTrabalhoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime; // Para agendamentos
import java.time.LocalTime;    // Para jornada de trabalho
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    public CommandLineRunner loadData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            ServicoRepository servicoRepository,
            //AdminRepository adminRepository,
            FuncionarioRepository funcionarioRepository,
            ClienteRepository clienteRepository,
            LandingPageRepository landingPageRepository,
            AgendamentoRepository agendamentoRepository, // <-- NOVO: Repositório de Agendamento
            JornadaTrabalhoRepository jornadaTrabalhoRepository, // <-- NOVO: Repositório de Jornada de Trabalho
            DisponibilidadeRepository bloqueioAgendaRepository, // <-- NOVO: Repositório de Bloqueio de Agenda
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

            // Utilizador Funcionário (ajustado para ter nomeCompleto)
            Funcionario funcionario1;
            if (userRepository.findByUsername("funcionario").isEmpty()) {
                funcionario1 = new Funcionario();
                funcionario1.setUsername("funcionario");
                funcionario1.setNomeCompleto("Julia Almeida"); // Nome completo para o funcionário
                funcionario1.setEmail("func@bellory.com");
                funcionario1.setPassword(passwordEncoder.encode("password"));
                funcionario1.setCargo("Cabeleireira");
                funcionario1.setRoles(Set.of(roleFuncionario, roleAdmin)); // Pode ser ROLE_FUNCIONARIO ou ambos
                funcionarioRepository.save(funcionario1);
                System.out.println("Utilizador Funcionário criado.");
            } else {
                funcionario1 = (Funcionario) userRepository.findByUsername("funcionario").get();
            }

            // Outro Funcionário (opcional)
            Funcionario funcionario2;
            if (userRepository.findByUsername("funcionario2").isEmpty()) {
                funcionario2 = new Funcionario();
                funcionario2.setUsername("funcionario2");
                funcionario2.setNomeCompleto("Carlos Mendes");
                funcionario2.setEmail("func2@bellory.com");
                funcionario2.setPassword(passwordEncoder.encode("password2"));
                funcionario2.setCargo("Manicure");
                funcionario2.setRoles(Set.of(roleFuncionario));
                funcionarioRepository.save(funcionario2);
                System.out.println("Utilizador Funcionário 2 criado.");
            } else {
                funcionario2 = (Funcionario) userRepository.findByUsername("funcionario2").get();
            }


            // Utilizador Cliente
            Cliente cliente1;
            if (userRepository.findByUsername("cliente").isEmpty()) {
                cliente1 = new Cliente();
                cliente1.setUsername("cliente");
                cliente1.setEmail("cliente@email.com");
                cliente1.setPassword(passwordEncoder.encode("password"));
                cliente1.setNomeCompleto("Ana Silva");
                cliente1.setTelefone("99999-8888");
                cliente1.setDataNascimento(LocalDate.of(1995, 5, 15));
                cliente1.setRoles(Set.of(roleCliente));
                clienteRepository.save(cliente1);
                System.out.println("Utilizador Cliente criado.");
            } else {
                cliente1 = (Cliente) userRepository.findByUsername("cliente").get();
            }

            // --- 3. Cria a Landing Page "home" associada ao Admin ---
            User adminUser = userRepository.findByUsername("funcionario").get(); // Assumindo que 'funcionario' também tem ROLE_ADMIN para gerenciar LP

            if (landingPageRepository.findBySlug("home").isEmpty()) {
                System.out.println("Criando landing page de exemplo para o slug 'home'...");

                LandingPage homePage = new LandingPage();
                homePage.setSlug("home");
                homePage.setInternalTitle("Página Principal");
                homePage.setUser(adminUser);

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

                homePage.setSections(List.of(heroSection, featuresSection));

                landingPageRepository.save(homePage);
                System.out.println("Landing page 'home' criada com sucesso.");
            }

            // --- 4. Cria os Serviços de Teste ---
            Servico servicoCorteFeminino = new Servico();
            servicoCorteFeminino.setNome("Corte Feminino");
            servicoCorteFeminino.setOrganizacao_id(1); // Use Long para organizacaoId
            servicoCorteFeminino.setCategoria("Cabelo");
            servicoCorteFeminino.setGenero("Feminino");
            servicoCorteFeminino.setDescricao("Corte personalizado com técnicas modernas e acabamento impecável. Inclui lavagem e styling.");
            servicoCorteFeminino.setDuracaoEstimadaMinutos(90);
            servicoCorteFeminino.setPreco(BigDecimal.valueOf(129.90));
            servicoCorteFeminino.setProdutos(List.of("Shampoo L'Oréal Professionnel", "Condicionador Kerastase", "Sérum Moroccanoil"));
            servicoCorteFeminino.setUrlsImagens(List.of("https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop"));
            servicoCorteFeminino.setAtivo(true);
            servicoRepository.save(servicoCorteFeminino);

            Servico servicoColoracaoPremium = new Servico();
            servicoColoracaoPremium.setNome("Coloração Premium");
            servicoColoracaoPremium.setOrganizacao_id(1);
            servicoColoracaoPremium.setCategoria("Pintura");
            servicoColoracaoPremium.setGenero("Feminino");
            servicoColoracaoPremium.setDescricao("Coloração profissional com produtos de alta qualidade. Cores vibrantes e duradouras.");
            servicoColoracaoPremium.setDuracaoEstimadaMinutos(180);
            servicoColoracaoPremium.setPreco(BigDecimal.valueOf(249.90));
            servicoColoracaoPremium.setProdutos(List.of("Tintura Wella Professionals", "Oxidante Schwarzkopf", "Tratamento Olaplex"));
            servicoColoracaoPremium.setUrlsImagens(List.of("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?q=80&w=1000&auto=format&fit=crop"));
            servicoColoracaoPremium.setAtivo(true);
            servicoRepository.save(servicoColoracaoPremium);

            Servico servicoPenteadoEventos = new Servico();
            servicoPenteadoEventos.setNome("Penteado para Eventos");
            servicoPenteadoEventos.setOrganizacao_id(1);
            servicoPenteadoEventos.setCategoria("Penteados");
            servicoPenteadoEventos.setGenero("Feminino");
            servicoPenteadoEventos.setDescricao("Penteados sofisticados para ocasiões especiais. Elegância e charme garantidos.");
            servicoPenteadoEventos.setDuracaoEstimadaMinutos(120);
            servicoPenteadoEventos.setPreco(BigDecimal.valueOf(199.90));
            servicoPenteadoEventos.setProdutos(List.of("Spray Fixador Tresemmé", "Mousse Volumizador", "Acessórios Exclusivos"));
            servicoPenteadoEventos.setUrlsImagens(List.of("https://images.unsplash.com/photo-1487412947147-5cebf100ffc2?q=80&w=1000&auto=format&fit=crop"));
            servicoPenteadoEventos.setAtivo(true);
            servicoRepository.save(servicoPenteadoEventos);

            // --- 5. Cria as Jornadas de Trabalho para o Funcionário 1 (Julia Almeida) ---
            if (jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(funcionario1, DiaSemana.SEGUNDA).isEmpty()) {
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario1, DiaSemana.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario1, DiaSemana.TERCA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario1, DiaSemana.QUARTA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario1, DiaSemana.QUINTA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario1, DiaSemana.SEXTA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
                System.out.println("Jornada de trabalho para Funcionário 1 criada.");
            }

            // --- 6. Cria Bloqueios de Agenda (Ex: Almoço, Reunião) ---
            // Exemplo de almoço diário para o funcionário 1
            if (bloqueioAgendaRepository.findByFuncionarioAndInicioBloqueioBetween(
                    funcionario1,
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0)),
                    LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(13, 0)) // Procura na proxima 24h
            ).isEmpty()) {
                for (int i = 0; i < 7; i++) { // Bloqueia almoço para os próximos 7 dias
                    LocalDate today = LocalDate.now().plusDays(i);
                    // Apenas bloqueia se for um dia de trabalho da Julia (Seg-Sex)
                    DayOfWeek dayOfWeek = today.getDayOfWeek();
                    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                        bloqueioAgendaRepository.save(new BloqueioAgenda(
                                funcionario1,
                                LocalDateTime.of(today, LocalTime.of(12, 0)),
                                LocalDateTime.of(today, LocalTime.of(13, 0)),
                                "Horário de Almoço",
                                TipoBloqueio.ALMOCO,
                                null // Não associado a um agendamento específico
                        ));
                    }
                }
                System.out.println("Bloqueios de almoço para Funcionário 1 criados para os próximos 7 dias úteis.");
            }

            // --- 5. Cria as Jornadas de Trabalho para o Funcionário 1 (Julia Almeida) ---
            if (jornadaTrabalhoRepository.findByFuncionarioAndDiaSemana(funcionario1, DiaSemana.SEGUNDA).isEmpty()) {
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario2, DiaSemana.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario2, DiaSemana.TERCA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario2, DiaSemana.QUARTA, LocalTime.of(8, 0), LocalTime.of(14, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario2, DiaSemana.QUINTA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
                jornadaTrabalhoRepository.save(new JornadaTrabalho(funcionario2, DiaSemana.SEXTA, LocalTime.of(8, 0), LocalTime.of(18, 0)));
                System.out.println("Jornada de trabalho para Funcionário 1 criada.");
            }

            // --- 6. Cria Bloqueios de Agenda (Ex: Almoço, Reunião) ---
            // Exemplo de almoço diário para o funcionário 1
            if (bloqueioAgendaRepository.findByFuncionarioAndInicioBloqueioBetween(
                    funcionario2,
                    LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0)),
                    LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(13, 0)) // Procura na proxima 24h
            ).isEmpty()) {
                for (int i = 0; i < 7; i++) { // Bloqueia almoço para os próximos 7 dias
                    LocalDate today = LocalDate.now().plusDays(i);
                    // Apenas bloqueia se for um dia de trabalho da Julia (Seg-Sex)
                    DayOfWeek dayOfWeek = today.getDayOfWeek();
                    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                        bloqueioAgendaRepository.save(new BloqueioAgenda(
                                funcionario2,
                                LocalDateTime.of(today, LocalTime.of(12, 0)),
                                LocalDateTime.of(today, LocalTime.of(13, 0)),
                                "Horário de Almoço",
                                TipoBloqueio.ALMOCO,
                                null // Não associado a um agendamento específico
                        ));
                    }
                }
                System.out.println("Bloqueios de almoço para Funcionário 2 criados para os próximos 7 dias úteis.");
            }


            // --- 7. Cria um Agendamento de Teste ---
            // Agendamento para amanhã, 10:00 (ajuste a data/hora para um horário livre)
            LocalDateTime dataHoraAgendamentoExemplo = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

            // Verifique se já existe um agendamento para evitar duplicidade
            if (agendamentoRepository.findByClienteAndDataHoraAgendamento(cliente1, dataHoraAgendamentoExemplo).isEmpty()) {

                // Crie o Agendamento
                Agendamento agendamentoExemplo = new Agendamento(
                        1L, // organizacaoId
                        cliente1,
                        List.of(servicoCorteFeminino, servicoPenteadoEventos), // Serviços para o agendamento
                        List.of(funcionario1), // Funcionário responsável
                        dataHoraAgendamentoExemplo,
                        "Agendamento de teste para corte e penteado."
                );
                agendamentoExemplo.setStatus(Status.AGENDADO); // Define o status
                agendamentoRepository.save(agendamentoExemplo);
                System.out.println("Agendamento de teste criado.");

                // Crie o Bloqueio de Agenda correspondente ao agendamento
                // Calcule a duração total do agendamento
                int duracaoTotalAgendamento = agendamentoExemplo.getServicos().stream()
                        .mapToInt(Servico::getDuracaoEstimadaMinutos)
                        .sum();
                LocalDateTime fimAgendamentoExemplo = dataHoraAgendamentoExemplo.plusMinutes(duracaoTotalAgendamento);

                bloqueioAgendaRepository.save(new BloqueioAgenda(
                        funcionario1,
                        dataHoraAgendamentoExemplo,
                        fimAgendamentoExemplo,
                        "Agendamento de Serviço: " + cliente1.getNomeCompleto(),
                        TipoBloqueio.AGENDAMENTO,
                        agendamentoExemplo
                ));
                System.out.println("Bloqueio de agenda para o agendamento de teste criado.");
            }
        };
    }
}
