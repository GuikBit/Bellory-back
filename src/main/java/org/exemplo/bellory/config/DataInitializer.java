package org.exemplo.bellory.config;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao; // Importe a entidade Organizacao
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.*;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.BloqueioAgendaRepository; // Nome corrigido
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository; // Importe o repositório da Organizacao
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    public CommandLineRunner loadData(
            OrganizacaoRepository organizacaoRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            FuncionarioRepository funcionarioRepository,
            ClienteRepository clienteRepository,
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository,
            BloqueioAgendaRepository bloqueioAgendaRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // --- 1. Cria a Organização Principal ---
            Organizacao organizacaoPrincipal = organizacaoRepository.findByNome("Bellory Salon")
                    .orElseGet(() -> {
                        Organizacao org = new Organizacao();
                        org.setNome("Bellory Salon");
                        org.setNomeFantasia("Bellory Salon & Spa");
                        // Preencha outros campos obrigatórios da organização
                        return organizacaoRepository.save(org);
                    });
            System.out.println("Organização principal verificada/criada.");

            // --- 2. Cria as Roles Básicas se não existirem ---
            Role roleAdmin = roleRepository.findByNome("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            Role roleFuncionario = roleRepository.findByNome("ROLE_FUNCIONARIO").orElseGet(() -> roleRepository.save(new Role("ROLE_FUNCIONARIO")));
            Role roleCliente = roleRepository.findByNome("ROLE_CLIENTE").orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE")));
            System.out.println("Roles básicas verificadas/criadas.");

            // --- 3. Cria os Usuários de Teste ---
            Funcionario funcionario1 = criarFuncionarioSeNaoExistir("funcionario1", "Julia Almeida", "julia@bellory.com", "Cabeleireira", Set.of(roleFuncionario, roleAdmin), organizacaoPrincipal, userRepository, passwordEncoder);
            Funcionario funcionario2 = criarFuncionarioSeNaoExistir("funcionario2", "Carlos Mendes", "carlos@bellory.com", "Manicure", Set.of(roleFuncionario), organizacaoPrincipal, userRepository, passwordEncoder);
            Cliente cliente1 = criarClienteSeNaoExistir("cliente1", "Ana Silva", "ana.silva@email.com", "99999-8888", LocalDate.of(1995, 5, 15), Set.of(roleCliente), organizacaoPrincipal, userRepository, passwordEncoder);

            // --- 4. Cria os Serviços de Teste ---
            Servico servicoCorte = criarServicoSeNaoExistir("Corte Feminino", "Cabelo", "Corte personalizado...", 60, BigDecimal.valueOf(129.90), organizacaoPrincipal, servicoRepository);
            Servico servicoManicure = criarServicoSeNaoExistir("Manicure Completa", "Mãos", "Cutilagem, esmaltação...", 45, BigDecimal.valueOf(45.00), organizacaoPrincipal, servicoRepository);

            // --- 5. Define Jornada de Trabalho e Bloqueios ---
            // Usando a relação na entidade Funcionario para gerenciar a jornada e os bloqueios
            if (funcionario1.getJornadaDeTrabalho().isEmpty()) {
                criarJornadaParaFuncionario(funcionario1);
                criarBloqueiosAlmocoParaFuncionario(funcionario1);
                funcionarioRepository.save(funcionario1); // Salva o funcionário com sua nova jornada e bloqueios
                System.out.println("Jornada e bloqueios de almoço para " + funcionario1.getNomeCompleto() + " criados.");
            }
            if (funcionario2.getJornadaDeTrabalho().isEmpty()) {
                criarJornadaParaFuncionario(funcionario2);
                criarBloqueiosAlmocoParaFuncionario(funcionario2);
                funcionarioRepository.save(funcionario2); // Salva o funcionário com sua nova jornada e bloqueios
                System.out.println("Jornada e bloqueios de almoço para " + funcionario2.getNomeCompleto() + " criados.");
            }

            // --- 6. Cria um Agendamento de Teste ---
            LocalDateTime dataHoraAgendamento = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

            if (agendamentoRepository.findByFuncionarioAndDtAgendamento(funcionario1, dataHoraAgendamento).isEmpty()) {
                System.out.println("Criando agendamento de teste...");

                // 1. Cria o Agendamento
                Agendamento agendamento = new Agendamento();
                agendamento.setOrganizacao(organizacaoPrincipal);
                agendamento.setCliente(cliente1);
                agendamento.adicionarFuncionario(funcionario1);
                agendamento.adicionarServico(servicoCorte);
                agendamento.setDtAgendamento(dataHoraAgendamento);
                agendamento.setStatus(Status.AGENDADO);

                // 2. Cria o Bloqueio correspondente
                LocalDateTime fimAgendamento = dataHoraAgendamento.plusMinutes(servicoCorte.getTempoEstimadoMinutos());
                BloqueioAgenda bloqueio = new BloqueioAgenda(
                        funcionario1,
                        dataHoraAgendamento,
                        fimAgendamento,
                        "Agendamento: " + cliente1.getNomeCompleto(),
                        TipoBloqueio.AGENDAMENTO,
                        null // O vínculo será feito pelo agendamento
                );

                // 3. Vincula o agendamento e o bloqueio de forma bidirecional
                agendamento.setBloqueioAgenda(bloqueio);

                // 4. Salva o Agendamento (o Bloqueio será salvo em cascata)
                agendamentoRepository.save(agendamento);
                System.out.println("Agendamento de teste e bloqueio correspondente criados com sucesso.");
            }
        };
    }

    // --- MÉTODOS AUXILIARES PARA ORGANIZAÇÃO ---

    private Funcionario criarFuncionarioSeNaoExistir(String username, String nomeCompleto, String email, String cargo, Set<Role> roles, Organizacao org, UserRepository userRepo, PasswordEncoder encoder) {
        return (Funcionario) userRepo.findByUsername(username).orElseGet(() -> {
            Funcionario f = new Funcionario();
            f.setUsername(username);
            f.setNomeCompleto(nomeCompleto);
            f.setEmail(email);
            f.setPassword(encoder.encode("password"));
            f.setCargo(cargo);
            f.setRoles(roles);
            f.setOrganizacao(org);
            System.out.println("Criado Funcionario: " + nomeCompleto);
            return userRepo.save(f);
        });
    }

    private Cliente criarClienteSeNaoExistir(String username, String nomeCompleto, String email, String telefone, LocalDate dtNasc, Set<Role> roles, Organizacao org, UserRepository userRepo, PasswordEncoder encoder) {
        return (Cliente) userRepo.findByUsername(username).orElseGet(() -> {
            Cliente c = new Cliente();
            c.setUsername(username);
            c.setNomeCompleto(nomeCompleto);
            c.setEmail(email);
            c.setPassword(encoder.encode("password"));
            c.setTelefone(telefone);
            c.setDataNascimento(dtNasc);
            c.setRoles(roles);
            c.setOrganizacao(org);
            System.out.println("Criado Cliente: " + nomeCompleto);
            return userRepo.save(c);
        });
    }

    private Servico criarServicoSeNaoExistir(String nome, String categoria, String descricao, int duracao, BigDecimal preco, Organizacao org, ServicoRepository repo) {
        return repo.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
            Servico s = new Servico();
            s.setNome(nome);
            s.setCategoria(categoria);
            s.setDescricao(descricao);
            s.setTempoEstimadoMinutos(duracao);
            s.setPreco(preco);
            s.setOrganizacao(org);
            s.setAtivo(true);
            System.out.println("Criado Serviço: " + nome);
            return repo.save(s);
        });
    }

    private void criarJornadaParaFuncionario(Funcionario funcionario) {
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.TERCA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.QUARTA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.QUINTA, LocalTime.of(10, 0), LocalTime.of(20, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SEXTA, LocalTime.of(10, 0), LocalTime.of(20, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SABADO, LocalTime.of(8, 0), LocalTime.of(14, 0), true));
    }

    private void criarBloqueiosAlmocoParaFuncionario(Funcionario funcionario) {
        funcionario.addBloqueio(new BloqueioAgenda(funcionario, null, null, "Horário de Almoço", TipoBloqueio.ALMOCO, null));
    }
}