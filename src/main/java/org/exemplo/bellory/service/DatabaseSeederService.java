package org.exemplo.bellory.service;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.users.Role;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.repository.users.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service // Anotação chave: esta é uma classe de serviço
public class DatabaseSeederService {

    private final OrganizacaoRepository organizacaoRepository;
    private final RoleRepository roleRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PlanoRepository planoRepository;
    private final ProdutoRepository produtoRepository;
    private final PasswordEncoder passwordEncoder;

    // Injeção de dependência via construtor (boa prática)
    public DatabaseSeederService(OrganizacaoRepository organizacaoRepository, RoleRepository roleRepository,
                                 FuncionarioRepository funcionarioRepository, ClienteRepository clienteRepository,
                                 ServicoRepository servicoRepository, AgendamentoRepository agendamentoRepository,
                                 PlanoRepository planoRepository, ProdutoRepository produtoRepository,
                                 PasswordEncoder passwordEncoder) {
        this.organizacaoRepository = organizacaoRepository;
        this.roleRepository = roleRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.planoRepository = planoRepository;
        this.produtoRepository = produtoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional // A anotação garante que tudo aqui dentro execute em uma única transação
    public void seedDatabase() {
        // --- 1. Cria ou verifica o Plano Padrão ---
        Plano planoPadrao = planoRepository.findByNome("Plano Básico").orElseGet(() -> {
            Plano p = new Plano();
            p.setNome("Plano Básico");
            p.setDescricao("Plano de funcionalidades essenciais.");
            p.setValorUnitario(new BigDecimal("99.90"));
            p.setRecorrencia("mensal");
            p.setAtivo(true);
            System.out.println("Criado Plano: " + p.getNome());
            return planoRepository.save(p);
        });

        // --- 2. Cria a Organização Principal ---
        Organizacao organizacaoPrincipal = organizacaoRepository.findByNome("Bellory Salon").orElseGet(() -> {
            Organizacao org = new Organizacao();
            org.setNome("Bellory Salon");
            org.setNomeFantasia("Bellory Salon & Spa");
            org.setCnpj("00.000.000/0001-00");
            org.setNomeResponsavel("Admin do Sistema");
            org.setEmailResponsavel("admin@bellory.com");
            org.setCpfResponsavel("000.000.000-00");
            org.setPlano(planoPadrao);
            org.setDtCadastro(LocalDateTime.now());
            org.setAtivo(true);
            System.out.println("Criada Organização: " + org.getNome());
            return organizacaoRepository.save(org);
        });

        // --- 3. Cria as Roles (Permissões) ---
        Role roleAdmin = criarRoleSeNaoExistir(roleRepository, "ROLE_ADMIN");
        Role roleFuncionario = criarRoleSeNaoExistir(roleRepository, "ROLE_FUNCIONARIO");
        Role roleCliente = criarRoleSeNaoExistir(roleRepository, "ROLE_CLIENTE");

        // --- 4. Cria os Usuários de Teste ---
        Funcionario funcionario1 = criarFuncionarioSeNaoExistir(funcionarioRepository, "funcionario1", "Julia Almeida", "julia@bellory.com", "Cabeleireira", "ROLE_ADMIN", organizacaoPrincipal, passwordEncoder, LocalDate.now().toString());
        Funcionario funcionario2 = criarFuncionarioSeNaoExistir(funcionarioRepository, "funcionario2", "Carlos Mendes", "carlos@bellory.com", "Manicure", "ROLE_FUNCIONARIO", organizacaoPrincipal, passwordEncoder, LocalDate.now().toString());
        Cliente cliente1 = criarClienteSeNaoExistir(clienteRepository, "cliente1", "Ana Silva", "ana.silva@email.com", "99999-8888", LocalDate.of(1995, 5, 15), "ROLE_CLIENTE", organizacaoPrincipal, passwordEncoder);

        // --- 5. Cria os Serviços de Teste ---
        Servico servicoCorte = criarServicoSeNaoExistir(servicoRepository, "Corte Feminino", "Cabelo", "Corte personalizado...", 60, new BigDecimal("129.90"), "Feminino","https://images.unsplash.com/photo-1647140655214-e4a2d914971f?q=80&w=1965&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D", organizacaoPrincipal);
        Servico servicoManicure = criarServicoSeNaoExistir(servicoRepository, "Manicure Completa", "Mãos", "Cutilagem, esmaltação...", 45, new BigDecimal("45.00"),"Feminino","https://s1-unimed-dev.us-southeast-1.linodeobjects.com/images/products/seller_143/Modelagem-e-design-de-sobrancelha-masculina_cfac09e2_7d31_40ce_97ab_629fd41641a0.webp", organizacaoPrincipal);

        // --- 6. Define Jornada de Trabalho e Bloqueios ---
        if (funcionario1.getJornadaDeTrabalho().isEmpty()) {
            criarJornadaParaFuncionario(funcionario1);
            criarBloqueiosAlmocoParaFuncionario(funcionario1);
            funcionarioRepository.save(funcionario1);
            System.out.println("Jornada e bloqueios de almoço para " + funcionario1.getNomeCompleto() + " criados.");
        }
        if (funcionario2.getJornadaDeTrabalho().isEmpty()) {
            criarJornadaParaFuncionario(funcionario2);
            criarBloqueiosAlmocoParaFuncionario(funcionario2);
            funcionarioRepository.save(funcionario2);
            System.out.println("Jornada e bloqueios de almoço para " + funcionario2.getNomeCompleto() + " criados.");
        }

        // --- 7. Cria um Agendamento de Teste ---
        LocalDateTime dataHoraAgendamento = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (agendamentoRepository.findByFuncionariosContainingAndDtAgendamento(funcionario1, dataHoraAgendamento).isEmpty()) {
            System.out.println("Criando agendamento de teste...");
            Agendamento agendamento = new Agendamento();
            agendamento.setOrganizacao(organizacaoPrincipal);
            agendamento.setCliente(cliente1);
            agendamento.setFuncionarios(Collections.singletonList(funcionario1));
            agendamento.setServicos(Collections.singletonList(servicoCorte));
            agendamento.setDtAgendamento(dataHoraAgendamento);
            agendamento.setStatus(Status.AGENDADO);
            agendamento.setObservacao("Cliente deseja um corte moderno.");

            LocalDateTime fimAgendamento = dataHoraAgendamento.plusMinutes(servicoCorte.getTempoEstimadoMinutos());
            BloqueioAgenda bloqueio = new BloqueioAgenda(funcionario1, dataHoraAgendamento, fimAgendamento, "Agendamento: " + cliente1.getNomeCompleto(), TipoBloqueio.AGENDAMENTO, agendamento);

            funcionario1.addBloqueio(bloqueio);
            agendamento.setBloqueioAgenda(bloqueio);
            agendamentoRepository.save(agendamento);
            System.out.println("Agendamento de teste e bloqueio correspondente criados com sucesso.");
        }

        // --- 8. Cria Produtos de Teste ---
        System.out.println("Criando produtos de teste...");
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Máscara Neon Glow", "Máscara com efeito neon que revitaliza e ilumina os cabelos instantaneamente.", new BigDecimal("75.90"), 100, "Tratamentos", "Feminino", "Neon Beauty", new BigDecimal("4.9"), 16, true, List.of("https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?q=80&w=400"), List.of("Proteínas Fluorescentes", "Ácidos Frutais", "Vitamina B12", "Colágeno Vegetal"));
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Esmalte Holográfico", "Esmalte com efeito holográfico que muda de cor conforme a luz.", new BigDecimal("35.50"), 100, "Unhas", "Feminino", "Holo Nails", new BigDecimal("4.8"), 0, false, List.of("https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=400"), List.of("Pigmentos Holográficos", "Base Magnética", "Top Coat 3D"));
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Shampoo Color Blast", "Shampoo que deposita cor temporária enquanto limpa os cabelos.", new BigDecimal("52.90"), 100, "Cabelo", "Feminino", "Color Revolution", new BigDecimal("4.7"), 0, false, List.of("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?q=80&w=400"), List.of("Pigmentos Temporários", "Extratos Naturais", "Proteínas Vegetais"));
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Sérum Anti-Gravity", "Sérum facial com tecnologia anti-gravidade para lifting instantâneo.", new BigDecimal("95.00"), 0, "Skincare", "Feminino", "Future Skin", new BigDecimal("5.0"), 0, false, List.of("https://images.unsplash.com/photo-1556228578-8c89e6adf883?q=80&w=400"), List.of("Peptídeos Tensores", "Ácido Hialurônico", "Nanopartículas", "Vitamina C"));
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Kit Nail Art Futurista", "Kit completo para nail art com produtos inovadores e ferramentas tech.", new BigDecimal("129.90"), 100, "Kits", "Feminino", "Tech Nails", new BigDecimal("4.9"), 28, true, List.of("https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=400"), List.of("Gel UV", "Glitters Holográficos", "Adesivos 3D", "LED Pen", "Base Magnética"));
        criarProdutoSeNaoExistir(produtoRepository, organizacaoPrincipal, "Spray Texturizador Neon", "Spray que cria texturas incríveis e brilho neon nos cabelos.", new BigDecimal("48.90"), 100, "Styling", "Feminino", "Texture Lab", new BigDecimal("4.6"), 0, false, List.of("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?q=80&w=400"), List.of("Polímeros Texturizantes", "Pigmentos Neon", "Óleos Essenciais"));
    }

    // --- MÉTODOS AUXILIARES ---
    private Role criarRoleSeNaoExistir(RoleRepository roleRepository, String nome) {
        return roleRepository.findByNome(nome).orElseGet(() -> {
            System.out.println("Criando Role: " + nome);
            return roleRepository.save(new Role(nome));
        });
    }

    private Funcionario criarFuncionarioSeNaoExistir(FuncionarioRepository funcionarioRepository, String username, String nomeCompleto, String email, String cargo, String role, Organizacao org, PasswordEncoder encoder, String dataContracao) {
        return funcionarioRepository.findByUsername(username)
                .map(user -> (Funcionario) user)
                .orElseGet(() -> {
                    Funcionario f = new Funcionario();
                    // --- Dados Essenciais (já existentes) ---
                    f.setUsername(username);
                    f.setNomeCompleto(nomeCompleto);
                    f.setEmail(email);
                    f.setPassword(encoder.encode("password"));
                    f.setCargo(cargo);
                    f.setRole(role);
                    f.setOrganizacao(org);
                    f.setAtivo(true);
                    f.setDataContratacao(LocalDateTime.now());
                    f.setDataCriacao(LocalDateTime.now());

                    // --- Novos Campos Populados ---
                    f.setFoto("https://exemplo.com/foto/" + username + ".jpg");
                    f.setCpf("123.456.789-" + (username.endsWith("1") ? "10" : "20"));
                    f.setTelefone("(11) 98765-4321");
                    f.setDataNasc(LocalDate.of(1990, 1, 15));
                    f.setSexo("Feminino");
                    f.setNivel(username.contains("admin") ? 1 : 2); // Exemplo de lógica
                    f.setApelido(nomeCompleto.split(" ")[0]);
                    f.setSituacao("Ativo");
                    f.setCep("12345-678");
                    f.setLogradouro("Rua das Flores");
                    f.setNumero("123");
                    f.setBairro("Centro");
                    f.setCidade("Cidade Exemplo");
                    f.setUf("SP");
                    f.setRg("12.345.678-9");
                    f.setEstadoCivil("Solteiro(a)");
                    f.setGrauInstrucao("Ensino Superior Completo");
                    f.setSalario(new BigDecimal("3500.00"));
                    f.setJornadaSemanal("44 horas");
                    f.setNomeMae("Maria da Silva");

                    System.out.println("Criado Funcionario: " + nomeCompleto);
                    return funcionarioRepository.save(f);
                });
    }

    private Cliente criarClienteSeNaoExistir(ClienteRepository clienteRepository, String username, String nomeCompleto, String email, String telefone, LocalDate dtNasc, String role, Organizacao org, PasswordEncoder encoder) {
        return clienteRepository.findByUsername(username).orElseGet(() -> {
            Cliente c = new Cliente();
            c.setUsername(username);
            c.setNomeCompleto(nomeCompleto);
            c.setEmail(email);
            c.setPassword(encoder.encode("password"));
            c.setTelefone(telefone);
            c.setDataNascimento(dtNasc);
            c.setRole(role); // Atribui a role como String
            c.setOrganizacao(org);
            c.setAtivo(true);
            System.out.println("Criado Cliente: " + nomeCompleto);
            return clienteRepository.save(c);
        });
    }


    private Servico criarServicoSeNaoExistir(ServicoRepository repo, String nome, String categoria, String descricao, int duracao, BigDecimal preco, String genero, String urlImage, Organizacao org) {
        return repo.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
            Servico s = new Servico();
            s.setNome(nome);
            s.setCategoria(categoria);
            s.setDescricao(descricao);
            s.setTempoEstimadoMinutos(duracao);
            s.setPreco(preco);
            s.setOrganizacao(org);
            s.setGenero(genero);
            s.adicionarUrlImagem(urlImage);
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
        LocalDateTime inicioAlmoco = LocalDate.now().atTime(12, 0);
        LocalDateTime fimAlmoco = LocalDate.now().atTime(13, 0);
        BloqueioAgenda bloqueioAlmoco = new BloqueioAgenda(funcionario, inicioAlmoco, fimAlmoco, "Horário de Almoço", TipoBloqueio.ALMOCO, null);
        funcionario.addBloqueio(bloqueioAlmoco);
    }

    private void criarProdutoSeNaoExistir(ProdutoRepository produtoRepository, Organizacao org, String nome, String descricao, BigDecimal preco, int qtdEstoque, String categoria, String genero, String marca, BigDecimal avaliacao, int descontoPercentual, boolean destaque, List<String> urlsImagens, List<String> ingredientes) {
        produtoRepository.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
            Produto p = new Produto();
            p.setOrganizacao(org);
            p.setNome(nome);
            p.setDescricao(descricao);
            p.setPreco(preco);
            p.setQtdEstoque(qtdEstoque);
            p.setCategoria(categoria);
            p.setGenero(genero);
            p.setMarca(marca);
            p.setAvaliacao(avaliacao);
            p.setDescontoPercentual(descontoPercentual > 0 ? descontoPercentual : null);
            p.setDestaque(destaque);
            p.setAtivo(true);
            p.setUrlsImagens(urlsImagens);
            p.setIngredientes(ingredientes);
            p.setTotalAvaliacoes(avaliacao.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
            System.out.println("Criado Produto: " + nome);
            return produtoRepository.save(p);
        });
    }
}
