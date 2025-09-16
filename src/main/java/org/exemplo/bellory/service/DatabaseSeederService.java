package org.exemplo.bellory.service;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.PageComponent;
import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.users.Role;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.tenant.PageComponentRepository;
import org.exemplo.bellory.model.repository.tenant.PageRepository;
import org.exemplo.bellory.model.repository.tenant.TenantRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.repository.users.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
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
    private final CategoriaRepository categoriaRepository;

    private final TenantRepository tenantRepository;
    private final PageRepository pageRepository;
    private final PageComponentRepository componentRepository;


    // Arrays com dados diversos para randomização
    private final String[] nomesFemininos = {"Ana", "Maria", "Julia", "Carla", "Fernanda", "Beatriz", "Camila", "Larissa", "Rafaela", "Amanda", "Gabriela", "Bruna", "Letícia", "Mariana", "Priscila", "Débora", "Tatiane", "Vanessa", "Patrícia", "Luciana"};
    private final String[] nomesMasculinos = {"Carlos", "João", "Pedro", "Lucas", "Rafael", "Bruno", "Diego", "Rodrigo", "Felipe", "Gustavo", "Thiago", "André", "Marcelo", "Vinícius", "Leonardo", "Daniel", "Eduardo", "Gabriel", "Fernando", "Ricardo"};
    private final String[] sobrenomes = {"Silva", "Santos", "Oliveira", "Souza", "Lima", "Ferreira", "Costa", "Rodrigues", "Martins", "Pereira", "Almeida", "Nascimento", "Carvalho", "Gomes", "Lopes", "Ribeiro", "Moreira", "Rocha", "Teixeira", "Dias"};
    private final String[] cargos = {"Cabeleireiro(a)", "Manicure", "Pedicure", "Esteticista", "Massagista", "Barbeiro", "Maquiador(a)", "Designer de Sobrancelhas", "Terapeuta Capilar", "Nail Artist"};
    private final String[] observacoes = {
            "Cliente prefere atendimento mais cedo",
            "Alergia a produtos com formol",
            "Primeira vez no salão",
            "Cliente VIP - atendimento especial",
            "Cabelo muito sensível",
            "Prefere profissionais experientes",
            "Cliente regular - já conhece procedimentos",
            "Solicita ambiente mais reservado",
            "Tem pressa - horário apertado",
            "Cliente com mobilidade reduzida"
    };

    public DatabaseSeederService(OrganizacaoRepository organizacaoRepository, RoleRepository roleRepository,
                                 FuncionarioRepository funcionarioRepository, ClienteRepository clienteRepository,
                                 ServicoRepository servicoRepository, AgendamentoRepository agendamentoRepository,
                                 PlanoRepository planoRepository, ProdutoRepository produtoRepository,
                                 PasswordEncoder passwordEncoder, CategoriaRepository categoriaRepository,
                                 PageComponentRepository componentRepository, PageRepository pageRepository, TenantRepository tenantRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.roleRepository = roleRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.planoRepository = planoRepository;
        this.produtoRepository = produtoRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaRepository = categoriaRepository;
        this.componentRepository = componentRepository;
        this.pageRepository = pageRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void seedDatabase() {
        System.out.println("🚀 Iniciando seeding completo do banco de dados...");

        // 1. PLANOS
        List<Plano> planos = criarPlanos();

        // 2. ORGANIZAÇÕES
        List<Organizacao> organizacoes = criarOrganizacoes(planos);
        Organizacao orgPrincipal = organizacoes.get(0);

        // 3. ROLES
        List<Role> roles = criarRoles();

        // 4. CATEGORIAS
        List<Categoria> categorias = criarCategorias();

        // 5. FUNCIONÁRIOS (15 funcionários)
        List<Funcionario> funcionarios = criarFuncionarios(orgPrincipal);

        // 6. CLIENTES (50 clientes)
        List<Cliente> clientes = criarClientes(orgPrincipal);

        // 7. SERVIÇOS (30 serviços)
        List<Servico> servicos = criarServicos(categorias, orgPrincipal);

        // 8. PRODUTOS (50 produtos)
        criarProdutos(orgPrincipal, categorias);

        // 9. AGENDAMENTOS (100 agendamentos com todos os status)
        criarAgendamentos(orgPrincipal, funcionarios, clientes, servicos);

        seedTenantData();

        System.out.println("✅ Seeding completo finalizado com sucesso!");
        System.out.println("📊 Dados criados:");
        System.out.println("   - Planos: " + planos.size());
        System.out.println("   - Organizações: " + organizacoes.size());
        System.out.println("   - Funcionários: " + funcionarios.size());
        System.out.println("   - Clientes: " + clientes.size());
        System.out.println("   - Serviços: " + servicos.size());
        System.out.println("   - Produtos: 50");
        System.out.println("   - Agendamentos: 100");
    }

    private List<Plano> criarPlanos() {
        System.out.println("📋 Criando planos...");
        List<Plano> planos = new ArrayList<>();

        String[][] planosData = {
                {"Plano Básico", "Funcionalidades essenciais para salões pequenos", "99.90", "mensal"},
                {"Plano Profissional", "Funcionalidades avançadas para salões médios", "199.90", "mensal"},
                {"Plano Premium", "Todas as funcionalidades para grandes salões", "399.90", "mensal"},
                {"Plano Anual Básico", "Plano básico com desconto anual", "999.00", "anual"},
                {"Plano Enterprise", "Solução completa para redes de salões", "799.90", "mensal"}
        };

        for (String[] data : planosData) {
            Plano plano = planoRepository.findByNome(data[0]).orElseGet(() -> {
                Plano p = new Plano();
                p.setNome(data[0]);
                p.setDescricao(data[1]);
                p.setValorUnitario(new BigDecimal(data[2]));
                p.setRecorrencia(data[3]);
                p.setAtivo(true);
                return planoRepository.save(p);
            });
            planos.add(plano);
        }

        return planos;
    }

    private List<Organizacao> criarOrganizacoes(List<Plano> planos) {
        System.out.println("🏢 Criando organizações...");
        List<Organizacao> organizacoes = new ArrayList<>();

        String[][] orgData = {
                {"Bellory Salon", "Bellory Salon & Spa", "00.000.000/0001-00", "Admin do Sistema", "admin@bellory.com", "000.000.000-00"},
                {"Studio Elegance", "Studio Elegance Premium", "11.111.111/0001-11", "Maria Fernanda", "contato@elegance.com", "111.111.111-11"},
                {"Salon Moderno", "Salon Moderno Hair & Beauty", "22.222.222/0001-22", "Carlos Roberto", "info@moderno.com", "222.222.222-22"}
        };

        for (int i = 0; i < orgData.length; i++) {
            String[] data = orgData[i];
            int finalI = i;
            Organizacao org = organizacaoRepository.findByNome(data[0]).orElseGet(() -> {
                Organizacao o = new Organizacao();
                o.setNome(data[0]);
                o.setNomeFantasia(data[1]);
                o.setCnpj(data[2]);
                o.setNomeResponsavel(data[3]);
                o.setEmailResponsavel(data[4]);
                o.setCpfResponsavel(data[5]);
                o.setPlano(planos.get(finalI % planos.size()));
                o.setDtCadastro(LocalDateTime.now());
                o.setAtivo(true);
                return organizacaoRepository.save(o);
            });
            organizacoes.add(org);
        }

        return organizacoes;
    }

    private List<Role> criarRoles() {
        System.out.println("🔐 Criando roles...");
        List<Role> roles = new ArrayList<>();
        String[] roleNames = {"ROLE_ADMIN", "ROLE_FUNCIONARIO", "ROLE_CLIENTE", "ROLE_GERENTE", "ROLE_RECEPCAO"};

        for (String roleName : roleNames) {
            Role role = roleRepository.findByNome(roleName).orElseGet(() -> {
                return roleRepository.save(new Role(roleName));
            });
            roles.add(role);
        }

        return roles;
    }

    private List<Categoria> criarCategorias() {
        System.out.println("📂 Criando categorias...");
        List<Categoria> categorias = new ArrayList<>();

        String[][] catData = {
                {"Cabelo", "cabelo", "SERVICO"},
                {"Mãos e Pés", "maos_pes", "SERVICO"},
                {"Estética Facial", "estetica_facial", "SERVICO"},
                {"Sobrancelhas", "sobrancelhas", "SERVICO"},
                {"Massagem", "massagem", "SERVICO"},
                {"Depilação", "depilacao", "SERVICO"},
                {"Maquiagem", "maquiagem", "SERVICO"},
                {"Tratamentos", "tratamentos", "SERVICO"},
                {"Barba", "barba", "SERVICO"},
                {"Noivas", "noivas", "SERVICO"}
        };

        for (String[] data : catData) {
            TipoCategoria tipo = TipoCategoria.valueOf(data[2]);
            Categoria categoria = categoriaRepository.findByTipo(tipo).stream()
                    .filter(c -> c.getLabel().equalsIgnoreCase(data[0]))
                    .findFirst()
                    .orElseGet(() -> {
                        Categoria c = new Categoria();
                        c.setLabel(data[0]);
                        c.setValue(data[1]);
                        c.setTipo(tipo);
                        c.setAtivo(true);
                        return categoriaRepository.save(c);
                    });
            categorias.add(categoria);
        }

        return categorias;
    }

    private List<Funcionario> criarFuncionarios(Organizacao org) {
        System.out.println("👥 Criando funcionários...");
        List<Funcionario> funcionarios = new ArrayList<>();

        String[] generos = {"Feminino", "Masculino"};
        String[] situacoes = {"Ativo", "Férias", "Licença"};
        String[] estadosCivis = {"Solteiro(a)", "Casado(a)", "Divorciado(a)", "Viúvo(a)"};
        String[] grausInstrucao = {"Ensino Médio", "Técnico", "Superior Incompleto", "Superior Completo", "Pós-graduação"};

        for (int i = 1; i <= 5; i++) {
            String username = "funcionario" + i;

            int finalI = i;
            Funcionario funcionario = funcionarioRepository.findByUsername(username)
                    .map(user -> (Funcionario) user)
                    .orElseGet(() -> {
                        boolean isFeminino = ThreadLocalRandom.current().nextBoolean();
                        String[] nomes = isFeminino ? nomesFemininos : nomesMasculinos;
                        String nome = nomes[ThreadLocalRandom.current().nextInt(nomes.length)];
                        String sobrenome = sobrenomes[ThreadLocalRandom.current().nextInt(sobrenomes.length)];
                        String nomeCompleto = nome + " " + sobrenome;

                        Funcionario f = new Funcionario();
                        f.setUsername(username);
                        f.setNomeCompleto(nomeCompleto);
                        f.setEmail(username + "@bellory.com");
                        f.setPassword(passwordEncoder.encode("password123"));
                        f.setCargo(cargos[ThreadLocalRandom.current().nextInt(cargos.length)]);
                        f.setRole(finalI <= 2 ? "ROLE_ADMIN" : (finalI <= 5 ? "ROLE_GERENTE" : "ROLE_FUNCIONARIO"));
                        f.setOrganizacao(org);
                        f.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.9); // 90% ativos

                        // Dados pessoais randomizados
                        f.setFoto("https://randomuser.me/api/portraits/" + (isFeminino ? "women/" : "men/") + finalI + ".jpg");
                        f.setCpf(String.format("%03d.%03d.%03d-%02d",
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(100)));
                        f.setTelefone(String.format("(11) 9%04d-%04d",
                                ThreadLocalRandom.current().nextInt(10000),
                                ThreadLocalRandom.current().nextInt(10000)));
                        f.setDataNasc(LocalDate.now().minusYears(ThreadLocalRandom.current().nextInt(20, 60)));
                        f.setSexo(isFeminino ? "Feminino" : "Masculino");
                        f.setNivel(ThreadLocalRandom.current().nextInt(1, 6));
                        f.setApelido(nome);
                        f.setSituacao(situacoes[ThreadLocalRandom.current().nextInt(situacoes.length)]);
                        f.setCep(String.format("%05d-%03d", ThreadLocalRandom.current().nextInt(100000), ThreadLocalRandom.current().nextInt(1000)));
                        f.setLogradouro("Rua " + sobrenomes[ThreadLocalRandom.current().nextInt(sobrenomes.length)]);
                        f.setNumero(String.valueOf(ThreadLocalRandom.current().nextInt(1, 9999)));
                        f.setBairro("Bairro " + (finalI <= 5 ? "Centro" : "Vila " + nome));
                        f.setCidade("São Paulo");
                        f.setUf("SP");
                        f.setRg(String.format("%02d.%03d.%03d-%01d",
                                ThreadLocalRandom.current().nextInt(100),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(10)));
                        f.setEstadoCivil(estadosCivis[ThreadLocalRandom.current().nextInt(estadosCivis.length)]);
                        f.setGrauInstrucao(grausInstrucao[ThreadLocalRandom.current().nextInt(grausInstrucao.length)]);
                        f.setSalario(new BigDecimal(ThreadLocalRandom.current().nextInt(2000, 8000)));
                        f.setJornadaSemanal("44 horas");
                        f.setNomeMae(nomesFemininos[ThreadLocalRandom.current().nextInt(nomesFemininos.length)] + " " +
                                sobrenomes[ThreadLocalRandom.current().nextInt(sobrenomes.length)]);
                        f.setDataContratacao(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(30, 1000)));
                        f.setDataCriacao(LocalDateTime.now());

                        return funcionarioRepository.save(f);
                    });

            // Criar jornada de trabalho se não existe
            if (funcionario.getJornadaDeTrabalho().isEmpty()) {
                criarJornadaParaFuncionario(funcionario);
                criarBloqueiosParaFuncionario(funcionario);
                funcionarioRepository.save(funcionario);
            }

            funcionarios.add(funcionario);
        }

        return funcionarios;
    }

    private List<Cliente> criarClientes(Organizacao org) {
        System.out.println("👤 Criando clientes...");
        List<Cliente> clientes = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            String username = "cliente" + i;

            Cliente cliente = clienteRepository.findByUsername(username).orElseGet(() -> {
                boolean isFeminino = ThreadLocalRandom.current().nextDouble() < 0.7; // 70% feminino
                String[] nomes = isFeminino ? nomesFemininos : nomesMasculinos;
                String nome = nomes[ThreadLocalRandom.current().nextInt(nomes.length)];
                String sobrenome = sobrenomes[ThreadLocalRandom.current().nextInt(sobrenomes.length)];
                String nomeCompleto = nome + " " + sobrenome;

                Cliente c = new Cliente();
                c.setUsername(username);
                c.setNomeCompleto(nomeCompleto);
                c.setEmail(username + "@email.com");
                c.setPassword(passwordEncoder.encode("password123"));
                c.setTelefone(String.format("(11) 9%04d-%04d",
                        ThreadLocalRandom.current().nextInt(10000),
                        ThreadLocalRandom.current().nextInt(10000)));
                c.setDataNascimento(LocalDate.now().minusYears(ThreadLocalRandom.current().nextInt(18, 70)));
                c.setRole("ROLE_CLIENTE");
                c.setOrganizacao(org);
                c.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.95); // 95% ativos

                return clienteRepository.save(c);
            });

            clientes.add(cliente);
        }

        return clientes;
    }

    private List<Servico> criarServicos(List<Categoria> categorias, Organizacao org) {
        System.out.println("💄 Criando serviços...");
        List<Servico> servicos = new ArrayList<>();

        String[][] servicosData = {
                // Cabelo
                {"Corte Feminino", "0", "Corte personalizado para cabelo feminino", "60", "129.90", "Feminino"},
                {"Corte Masculino", "0", "Corte clássico e moderno para homens", "45", "45.00", "Masculino"},
                {"Escova", "0", "Escova modeladora profissional", "45", "65.00", "Feminino"},
                {"Hidratação", "0", "Tratamento hidratante intensivo", "90", "89.90", "Unissex"},
                {"Coloração", "0", "Coloração completa dos cabelos", "180", "189.90", "Unissex"},
                {"Luzes", "0", "Mechas e luzes personalizadas", "120", "159.90", "Feminino"},
                {"Alisamento", "0", "Alisamento progressivo profissional", "240", "299.90", "Unissex"},
                {"Penteado", "0", "Penteados para eventos especiais", "90", "159.90", "Feminino"},

                // Mãos e Pés
//                {"Manicure Completa", "1", "Cutilagem, esmaltação e hidratação", "45", "45.00", "Feminino"},
//                {"Pedicure Completa", "1", "Cutilagem, esmaltação e esfoliação", "60", "55.00", "Feminino"},
//                {"Manicure Express", "1", "Esmaltação rápida", "20", "25.00", "Feminino"},
//                {"Unhas em Gel", "1", "Aplicação de gel nas unhas", "90", "89.90", "Feminino"},
//                {"Nail Art", "1", "Decoração artística das unhas", "60", "79.90", "Feminino"},
//                {"Spa dos Pés", "1", "Tratamento relaxante completo", "90", "119.90", "Unissex"},
//
//                // Estética Facial
//                {"Limpeza de Pele", "2", "Limpeza profunda e hidratação", "90", "129.90", "Unissex"},
//                {"Peeling", "2", "Renovação celular da pele", "60", "159.90", "Unissex"},
//                {"Máscara Facial", "2", "Tratamento com máscaras específicas", "45", "89.90", "Unissex"},
//                {"Microagulhamento", "2", "Tratamento anti-aging", "90", "299.90", "Unissex"},
//
//                // Sobrancelhas
//                {"Design de Sobrancelhas", "3", "Design personalizado com pinça", "30", "39.90", "Unissex"},
//                {"Henna", "3", "Coloração com henna natural", "45", "49.90", "Unissex"},
//                {"Micropigmentação", "3", "Pigmentação semipermanente", "120", "399.90", "Unissex"},
//
//                // Massagem
//                {"Massagem Relaxante", "4", "Massagem corporal relaxante", "60", "119.90", "Unissex"},
//                {"Massagem Modeladora", "4", "Massagem para modelar o corpo", "90", "159.90", "Unissex"},
//
//                // Depilação
//                {"Depilação Pernas", "5", "Depilação completa das pernas", "45", "79.90", "Feminino"},
//                {"Depilação Axilas", "5", "Depilação das axilas", "15", "29.90", "Feminino"},
//                {"Depilação Buço", "5", "Depilação do buço", "10", "19.90", "Feminino"},
//
//                // Maquiagem
//                {"Maquiagem Social", "6", "Make para eventos sociais", "60", "159.90", "Feminino"},
//                {"Maquiagem Noiva", "6", "Make especial para noivas", "90", "299.90", "Feminino"},
//                {"Automaquiagem", "6", "Aula de automaquiagem", "120", "199.90", "Feminino"},
//
//                // Barba
//                {"Barba Completa", "8", "Corte e modelagem da barba", "30", "39.90", "Masculino"}
        };

        for (String[] data : servicosData) {
            String nome = data[0];
            Categoria categoria = categorias.get(Integer.parseInt(data[1]));

            Servico servico = servicoRepository.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
                Servico s = new Servico();
                s.setNome(nome);
                s.setCategoria(categoria);
                s.setDescricao(data[2]);
                s.setTempoEstimadoMinutos(Integer.parseInt(data[3]));
                s.setPreco(new BigDecimal(data[4]));
                s.setGenero(data[5]);
                s.setOrganizacao(org);
                s.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.95); // 95% ativos
                s.adicionarUrlImagem("https://images.unsplash.com/photo-1562322140-8baeececf3df?w=400");
                return servicoRepository.save(s);
            });

            servicos.add(servico);
        }

        return servicos;
    }

    private void criarProdutos(Organizacao org, List<Categoria> categorias) {
        System.out.println("🛍️ Criando produtos...");

        // Criar mapa para facilitar busca de categorias por nome
        Map<String, Categoria> categoriaMap = categorias.stream()
                .collect(Collectors.toMap(Categoria::getLabel, categoria -> categoria));

        String[][] produtosData = {
                // nome, descricao, preco, quantidadeEstoque, nomeCategoria, codigoBarras, codigoInterno, marca, unidade, precoCusto, genero, avaliacao, totalAvaliacoes, descontoPercentual, destaque, ativo, modelo, peso
                {"Shampoo Premium", "Shampoo hidratante com óleos essenciais para todos os tipos de cabelo", "45.90", "100", "Cabelo", "7891234567890", "SHP001", "Beauty Pro", "UN", "25.00", "Feminino", "4.5", "127", "0", "true", "true", "Premium Line", "0.350"},
                {"Condicionador Reparador", "Condicionador para cabelos danificados com queratina", "39.90", "80", "Cabelo", "7891234567891", "CDR002", "Hair Care", "UN", "22.00", "Unissex", "4.3", "89", "15", "false", "true", "Repair", "0.300"},
                {"Máscara Capilar", "Tratamento intensivo semanal com óleos naturais", "89.90", "50", "Cabelo", "7891234567892", "MSC003", "Intensive Care", "UN", "45.00", "Unissex", "4.8", "203", "20", "true", "true", "Intensive", "0.250"},
                {"Óleo Argan", "Óleo puro de argan marroquino 100% natural", "129.90", "30", "Cabelo", "7891234567893", "ARG004", "Argan Gold", "ML", "65.00", "Unissex", "4.9", "156", "0", "true", "true", "Pure", "0.100"},
                {"Leave-in Protetor", "Proteção térmica e hidratação instantânea", "35.90", "120", "Cabelo", "7891234567894", "LEV005", "Thermo Pro", "ML", "18.00", "Unissex", "4.2", "94", "10", "false", "true", "Thermal", "0.150"},

                {"Esmalte Gel", "Esmalte com efeito gel duradouro até 15 dias", "19.90", "200", "Mãos e Pés", "7891234567895", "ESM006", "Nail Perfect", "UN", "8.00", "Feminino", "4.1", "78", "0", "false", "true", "Gel Effect", "0.015"},
                {"Base Fortalecedora", "Base que fortalece as unhas fracas", "29.90", "150", "Mãos e Pés", "7891234567896", "BSF007", "Strong Nails", "ML", "15.00", "Feminino", "4.4", "112", "0", "false", "true", "Fortifying", "0.012"},
                {"Kit Nail Art", "Kit completo para decoração de unhas", "79.90", "25", "Mãos e Pés", "7891234567897", "KNA008", "Art Nails", "KIT", "40.00", "Feminino", "4.7", "45", "25", "true", "true", "Professional", "0.200"},
                {"Removedor Suave", "Remove esmalte sem ressecar as unhas", "12.90", "180", "Mãos e Pés", "7891234567898", "REM009", "Gentle Care", "ML", "6.00", "Feminino", "4.0", "67", "0", "false", "true", "Gentle", "0.100"},
                {"Óleo Cutícula", "Hidrata e amacia as cutículas", "24.90", "100", "Mãos e Pés", "7891234567899", "CUT010", "Cuticle Soft", "ML", "12.00", "Feminino", "4.3", "88", "0", "false", "true", "Nourishing", "0.010"},

                {"Creme Anti-Idade", "Reduz rugas e linhas de expressão visíveis", "199.90", "40", "Estética Facial", "7891234567800", "CRA011", "Youth Formula", "G", "100.00", "Unissex", "4.8", "234", "30", "true", "true", "Anti-Age", "0.050"},
                {"Sérum Vitamina C", "Ilumina e revitaliza a pele com antioxidantes", "159.90", "60", "Estética Facial", "7891234567801", "SVC012", "Vitamin Boost", "ML", "80.00", "Unissex", "4.6", "189", "20", "true", "true", "Brightening", "0.030"},
                {"Protetor Solar Facial", "FPS 60 proteção UVA/UVB para rosto", "89.90", "80", "Estética Facial", "7891234567802", "PSF013", "Sun Shield", "ML", "45.00", "Unissex", "4.5", "145", "0", "false", "true", "FPS 60", "0.060"},
                {"Água Micelar", "Remove maquiagem suavemente sem agredir", "49.90", "120", "Estética Facial", "7891234567803", "AGM014", "Micellar Clean", "ML", "25.00", "Unissex", "4.2", "167", "15", "false", "true", "Micellar", "0.250"},
                {"Tônico Facial", "Equilibra pH da pele e minimiza poros", "39.90", "100", "Estética Facial", "7891234567804", "TON015", "Balance Tone", "ML", "20.00", "Unissex", "4.1", "134", "0", "false", "true", "Balancing", "0.200"},

                {"Lápis para Sobrancelha", "Define e preenche as sobrancelhas naturalmente", "29.90", "90", "Sobrancelhas", "7891234567805", "LPS016", "Perfect Brow", "UN", "15.00", "Feminino", "4.4", "98", "0", "false", "true", "Precision", "0.005"},
                {"Gel Fixador Sobrancelha", "Fixa e modela os fios por 12h", "35.90", "70", "Sobrancelhas", "7891234567806", "GFS017", "Brow Fix", "ML", "18.00", "Feminino", "4.2", "76", "0", "false", "true", "Long Lasting", "0.008"},
                {"Kit Sobrancelha", "Kit completo para design profissional", "69.90", "45", "Sobrancelhas", "7891234567807", "KSB018", "Brow Kit", "KIT", "35.00", "Feminino", "4.6", "123", "30", "true", "true", "Professional", "0.150"},
                {"Pinça Profissional", "Pinça de aço inoxidável ultra precisa", "45.90", "60", "Sobrancelhas", "7891234567808", "PIN019", "Steel Pro", "UN", "25.00", "Unissex", "4.7", "89", "0", "false", "true", "Precision", "0.020"},
                {"Cera Depilatória Sobrancelha", "Remove pelos indesejados suavemente", "19.90", "80", "Sobrancelhas", "7891234567809", "CDS020", "Wax Brow", "G", "10.00", "Feminino", "4.1", "65", "0", "false", "true", "Gentle", "0.025"},

                {"Óleo Relaxante", "Óleo essencial para massagem terapêutica", "89.90", "50", "Massagem", "7891234567810", "OLR021", "Relax Oil", "ML", "45.00", "Unissex", "4.8", "156", "0", "true", "true", "Therapeutic", "0.250"},
                {"Creme Massagem", "Creme hidratante para massagem corporal", "59.90", "60", "Massagem", "7891234567811", "CRM022", "Massage Cream", "G", "30.00", "Unissex", "4.5", "134", "15", "false", "true", "Moisturizing", "0.300"},
                {"Vela Aromática", "Vela para ambientação e relaxamento", "39.90", "80", "Massagem", "7891234567812", "VEA023", "Aroma Candle", "UN", "20.00", "Unissex", "4.3", "97", "0", "false", "true", "Aromatherapy", "0.180"},
                {"Pedras Quentes", "Kit de pedras vulcânicas para massagem", "159.90", "20", "Massagem", "7891234567813", "PQU024", "Hot Stones", "KIT", "80.00", "Unissex", "4.9", "67", "35", "true", "true", "Professional", "2.500"},
                {"CD Relaxante", "Música relaxante e sons da natureza", "29.90", "100", "Massagem", "7891234567814", "MUS025", "Nature Sounds", "UN", "15.00", "Unissex", "4.2", "78", "0", "false", "true", "Premium", "0.050"},

                {"Cera Quente", "Cera profissional para depilação corporal", "45.90", "80", "Depilação", "7891234567815", "CQU026", "Hot Wax", "G", "25.00", "Feminino", "4.4", "187", "0", "false", "true", "Professional", "0.400"},
                {"Cera Fria Roll-on", "Cera em roll-on para peles sensíveis", "29.90", "120", "Depilação", "7891234567816", "CFR027", "Cold Wax", "ML", "15.00", "Feminino", "4.1", "145", "0", "false", "true", "Sensitive", "0.100"},
                {"Pós Depilação", "Loção calmante e hidratante", "35.90", "90", "Depilação", "7891234567817", "POS028", "After Wax", "ML", "18.00", "Feminino", "4.3", "156", "20", "false", "true", "Soothing", "0.150"},
                {"Espátulas Descartáveis", "Pacote com 100 espátulas de madeira", "19.90", "200", "Depilação", "7891234567818", "ESP029", "Disposable", "PCT", "8.00", "Unissex", "4.0", "89", "0", "false", "true", "Eco Wood", "0.200"},
                {"Pó Talco Depilação", "Talco mineral para preparar a pele", "24.90", "150", "Depilação", "7891234567819", "TAL030", "Wax Talc", "G", "12.00", "Feminino", "4.2", "123", "0", "false", "true", "Mineral", "0.100"},

                {"Base Líquida HD", "Cobertura natural duradoura alta definição", "69.90", "90", "Maquiagem", "7891234567820", "BLI031", "Perfect Skin", "ML", "35.00", "Feminino", "4.4", "234", "0", "false", "true", "HD Formula", "0.030"},
                {"Paleta Sombras Profissional", "48 cores vibrantes e pigmentadas", "119.90", "45", "Maquiagem", "7891234567821", "PAL032", "Color Palette", "UN", "60.00", "Feminino", "4.7", "178", "35", "true", "true", "Professional", "0.180"},
                {"Batom Matte Longa Duração", "Acabamento matte resistente a 12h", "29.90", "150", "Maquiagem", "7891234567822", "BAT033", "Matte Kiss", "UN", "15.00", "Feminino", "4.3", "198", "0", "false", "true", "Long Wear", "0.004"},
                {"Rímel à Prova d'Água", "Alonga e volumiza resistente à água", "45.90", "80", "Maquiagem", "7891234567823", "RIM034", "Lash Volume", "UN", "25.00", "Feminino", "4.5", "167", "0", "false", "true", "Waterproof", "0.010"},
                {"Blush Compacto Natural", "Cor natural duradoura para bochechas", "39.90", "70", "Maquiagem", "7891234567824", "BLU035", "Natural Glow", "UN", "20.00", "Feminino", "4.2", "145", "20", "false", "true", "Natural", "0.008"},

                {"Peeling Químico Facial", "Remove impurezas profundas com AHA", "129.90", "30", "Tratamentos", "7891234567825", "PEE036", "Deep Clean", "ML", "65.00", "Unissex", "4.6", "98", "25", "true", "true", "Professional", "0.050"},
                {"Máscara de Ouro 24k", "Tratamento luxuoso anti-idade premium", "299.90", "15", "Tratamentos", "7891234567826", "MDO037", "Gold Mask", "UN", "150.00", "Unissex", "4.9", "67", "40", "true", "true", "Luxury", "0.025"},
                {"Hidrogel Ácido Hialurônico", "Hidratação intensiva profunda", "89.90", "40", "Tratamentos", "7891234567827", "HGF038", "Hydro Gel", "UN", "45.00", "Unissex", "4.7", "134", "30", "true", "true", "Intensive", "0.030"},
                {"Ampola Vitamina E", "Concentrado revitalizante antioxidante", "59.90", "60", "Tratamentos", "7891234567828", "AMP039", "Vita Boost", "UN", "30.00", "Unissex", "4.5", "189", "0", "false", "true", "Concentrate", "0.010"},
                {"Aparelho Led Terapia", "Fototerapia LED profissional", "1299.90", "5", "Tratamentos", "7891234567829", "LED040", "Photo Therapy", "UN", "650.00", "Unissex", "4.8", "23", "50", "true", "true", "Professional", "0.800"},

                {"Óleo para Barba Premium", "Hidrata e perfuma a barba masculina", "49.90", "80", "Barba", "7891234567830", "OLB041", "Beard Oil", "ML", "25.00", "Masculino", "4.6", "156", "0", "false", "true", "Premium", "0.050"},
                {"Balm para Barba Natural", "Modela e condiciona naturalmente", "39.90", "90", "Barba", "7891234567831", "BAL042", "Beard Balm", "G", "20.00", "Masculino", "4.4", "134", "15", "false", "true", "Natural", "0.060"},
                {"Shampoo Específico Barba", "Limpeza específica para pelos faciais", "35.90", "100", "Barba", "7891234567832", "SHB043", "Beard Wash", "ML", "18.00", "Masculino", "4.3", "198", "0", "false", "true", "Specialized", "0.250"},
                {"Pente Madeira Artesanal", "Pente de madeira nobre feito à mão", "29.90", "60", "Barba", "7891234567833", "PTM044", "Wood Comb", "UN", "15.00", "Masculino", "4.5", "87", "0", "false", "true", "Handmade", "0.025"},
                {"Kit Barba Completo", "Todos os produtos essenciais", "159.90", "25", "Barba", "7891234567834", "KBC045", "Complete Kit", "KIT", "80.00", "Masculino", "4.8", "78", "40", "true", "true", "Premium", "0.400"},

                {"Véu de Noiva Bordado", "Véu tradicional com bordado à mão", "299.90", "20", "Noivas", "7891234567835", "VNO046", "Bridal Veil", "UN", "150.00", "Feminino", "4.9", "45", "0", "true", "true", "Luxury", "0.100"},
                {"Kit Maquiagem Noiva", "Maquiagem completa para o grande dia", "399.90", "15", "Noivas", "7891234567836", "MNO047", "Bridal Makeup", "KIT", "200.00", "Feminino", "4.8", "67", "35", "true", "true", "Professional", "0.500"},
                {"Acessórios Cabelo Noiva", "Tiaras, presilhas e ornamentos", "199.90", "30", "Noivas", "7891234567837", "PNO048", "Hair Accessories", "KIT", "100.00", "Feminino", "4.7", "89", "30", "true", "true", "Elegant", "0.150"},
                {"Perfume Exclusivo Noiva", "Fragrância especial e única", "259.90", "25", "Noivas", "7891234567838", "PRN049", "Bridal Scent", "ML", "130.00", "Feminino", "4.6", "34", "25", "true", "true", "Exclusive", "0.100"},
                {"Kit Spa Relaxante Noiva", "Tratamentos pré-casamento completos", "499.90", "10", "Noivas", "7891234567839", "SPN050", "Bridal Spa", "KIT", "250.00", "Feminino", "4.9", "23", "45", "true", "true", "Luxury", "1.200"}
        };

        for (String[] data : produtosData) {
            // Buscar categoria por nome
            Categoria categoria = categoriaMap.get(data[4]);

            if (categoria == null) {
                System.err.println("⚠️ Categoria não encontrada: " + data[4]);
                continue;
            }

            String nome = data[0];
            produtoRepository.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
                try {
                    Produto p = new Produto();
                    p.setOrganizacao(org);
                    p.setNome(data[0]);                                      // nome
                    p.setDescricao(data[1]);                                 // descricao
                    p.setPreco(new BigDecimal(data[2]));                     // preco
                    p.setQuantidadeEstoque(Integer.parseInt(data[3]));       // quantidadeEstoque
                    p.setCategoria(categoria);                               // categoria
                    p.setCodigoBarras(data[5]);                             // codigoBarras
                    p.setCodigoInterno(data[6]);                            // codigoInterno
                    p.setMarca(data[7]);                                    // marca
                    p.setUnidade(data[8]);                                  // unidade
                    p.setPrecoCusto(new BigDecimal(data[9]));               // precoCusto
                    p.setGenero(data[10]);                                  // genero
                    p.setAvaliacao(new BigDecimal(data[11]));               // avaliacao
                    p.setTotalAvaliacoes(Integer.parseInt(data[12]));       // totalAvaliacoes
                    p.setDescontoPercentual(Integer.parseInt(data[13]) > 0 ? Integer.parseInt(data[13]) : null); // descontoPercentual
                    p.setDestaque(Boolean.parseBoolean(data[14]));          // destaque
                    p.setAtivo(Boolean.parseBoolean(data[15]));             // ativo
                    p.setModelo(data[16]);                                  // modelo
                    p.setPeso(new BigDecimal(data[17]));                    // peso

                    // Configurações padrão
                    p.setEstoqueMinimo(10);
                    p.setStatus(Produto.StatusProduto.ATIVO);

                    // Adicionar imagens variadas por categoria
                    adicionarImagensPorCategoria(p, data[4]);

                    // Adicionar ingredientes básicos
                    p.setIngredientes(Arrays.asList(
                            "Água purificada",
                            "Ingredientes ativos específicos",
                            "Conservantes naturais",
                            "Fragrância suave",
                            "Vitaminas e antioxidantes"
                    ));

                    // Adicionar instruções de uso
                    p.setComoUsar(Arrays.asList(
                            "Limpe bem a área antes da aplicação",
                            "Aplique o produto conforme necessário",
                            "Massageie suavemente até completa absorção",
                            "Use conforme orientação profissional"
                    ));

                    // Adicionar especificações técnicas
                    Map<String, String> specs = new HashMap<>();
                    specs.put("Tipo de Pele", "Todos os tipos");
                    specs.put("Validade", "36 meses");
                    specs.put("Origem", "Nacional");
                    specs.put("Certificação", "ANVISA");
                    specs.put("Testado", "Dermatologicamente");
                    p.setEspecificacoes(specs);

                    return produtoRepository.save(p);

                } catch (NumberFormatException e) {
                    System.err.println("❌ Erro ao converter dados numéricos para produto: " + data[0]);
                    System.err.println("Dados problemáticos: " + Arrays.toString(data));
                    return null;
                } catch (Exception e) {
                    System.err.println("❌ Erro geral ao criar produto: " + data[0] + " - " + e.getMessage());
                    return null;
                }
            });
        }

        System.out.println("✅ Produtos criados com sucesso!");
    }

    private void adicionarImagensPorCategoria(Produto produto, String nomeCategoria) {
        List<String> imagens = new ArrayList<>();

        switch (nomeCategoria) {
            case "Cabelo":
                imagens.add("https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400");
                imagens.add("https://images.unsplash.com/photo-1559599101-f09722fb4948?w=400");
                break;
            case "Mãos e Pés":
                imagens.add("https://images.unsplash.com/photo-1604654894610-df63bc536371?w=400");
                imagens.add("https://images.unsplash.com/photo-1583001931096-959e9a1a6223?w=400");
                break;
            case "Estética Facial":
                imagens.add("https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=400");
                imagens.add("https://images.unsplash.com/photo-1570194065650-d99fb4bedf0a?w=400");
                break;
            case "Sobrancelhas":
                imagens.add("https://images.unsplash.com/photo-1487412947147-5cebf100ffc2?w=400");
                imagens.add("https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=400");
                break;
            case "Massagem":
                imagens.add("https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=400");
                imagens.add("https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400");
                break;
            case "Depilação":
                imagens.add("https://images.unsplash.com/photo-1570194065650-d99fb4bedf0a?w=400");
                imagens.add("https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?w=400");
                break;
            case "Maquiagem":
                imagens.add("https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=400");
                imagens.add("https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400");
                break;
            case "Tratamentos":
                imagens.add("https://images.unsplash.com/photo-1570194065650-d99fb4bedf0a?w=400");
                imagens.add("https://images.unsplash.com/photo-1583001931096-959e9a1a6223?w=400");
                break;
            case "Barba":
                imagens.add("https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=400");
                imagens.add("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400");
                break;
            case "Noivas":
                imagens.add("https://images.unsplash.com/photo-1594736797933-d0401ba2fe65?w=400");
                imagens.add("https://images.unsplash.com/photo-1519741497674-611481863552?w=400");
                break;
            default:
                imagens.add("https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=400");
                imagens.add("https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400");
        }

        produto.setUrlsImagens(imagens);
    }

    private void criarAgendamentos(Organizacao org, List<Funcionario> funcionarios, List<Cliente> clientes, List<Servico> servicos) {
        System.out.println("📅 Criando agendamentos com todos os status...");

        Status[] todosStatus = Status.values();
        int agendamentosPorStatus = 100 / todosStatus.length;
        int contador = 0;

        for (Status status : todosStatus) {
            System.out.println("   Criando agendamentos com status: " + status);

            for (int i = 0; i < agendamentosPorStatus + (status == Status.AGENDADO ? 100 % todosStatus.length : 0); i++) {
                LocalDateTime dataAgendamento = gerarDataAgendamento(status);

                // Evitar duplicatas verificando se já existe agendamento similar
                Funcionario funcionario = funcionarios.get(ThreadLocalRandom.current().nextInt(funcionarios.size()));
                if (agendamentoRepository.findByFuncionariosContainingAndDtAgendamento(funcionario, dataAgendamento).isEmpty()) {

                    Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
                    List<Servico> servicosEscolhidos = escolherServicosAleatorios(servicos);
                    int duracaoTotal = servicosEscolhidos.stream().mapToInt(Servico::getTempoEstimadoMinutos).sum();

                    Agendamento agendamento = new Agendamento();
                    agendamento.setOrganizacao(org);
                    agendamento.setCliente(cliente);
                    agendamento.setFuncionarios(Collections.singletonList(funcionario));
                    agendamento.setServicos(servicosEscolhidos);
                    agendamento.setDtAgendamento(dataAgendamento);
                    agendamento.setStatus(status);
                    agendamento.setObservacao(gerarObservacaoAleatoria(status));

                    // Criar bloqueio na agenda do funcionário
                    LocalDateTime fimAgendamento = dataAgendamento.plusMinutes(duracaoTotal);
                    BloqueioAgenda bloqueio = new BloqueioAgenda(
                            funcionario,
                            dataAgendamento,
                            fimAgendamento,
                            "Agendamento: " + cliente.getNomeCompleto(),
                            TipoBloqueio.AGENDAMENTO,
                            agendamento
                    );

                    funcionario.addBloqueio(bloqueio);
                    agendamento.setBloqueioAgenda(bloqueio);

                    agendamentoRepository.save(agendamento);
                    contador++;
                }
            }
        }

        System.out.println("   Total de agendamentos criados: " + contador);
    }

    private LocalDateTime gerarDataAgendamento(Status status) {
        LocalDateTime base = LocalDateTime.now();

        switch (status) {
            case AGENDADO:
                // Futuro (próximos 30 dias)
                return base.plusDays(ThreadLocalRandom.current().nextInt(1, 31))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case CONFIRMADO:
                // Futuro próximo (próximos 7 dias)
                return base.plusDays(ThreadLocalRandom.current().nextInt(1, 8))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case EM_ANDAMENTO:
                // Hoje, horário atual próximo
                int horaAtual = base.getHour();
                int horaMinima = Math.max(9, horaAtual - 1);
                int horaMaxima = Math.min(18, horaAtual + 2);

                // Garantir que horaMaxima seja sempre maior que horaMinima
                if (horaMaxima <= horaMinima) {
                    horaMaxima = horaMinima + 1;
                }

                return base.withHour(ThreadLocalRandom.current().nextInt(horaMinima, horaMaxima))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case CONCLUIDO:
                // Passado (últimos 60 dias)
                return base.minusDays(ThreadLocalRandom.current().nextInt(1, 61))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case CANCELADO:
                // Passado ou futuro
                return base.plusDays(ThreadLocalRandom.current().nextInt(-30, 31))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case NAO_COMPARECEU:
                // Passado recente (últimos 15 dias)
                return base.minusDays(ThreadLocalRandom.current().nextInt(1, 16))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            case REAGENDADO:
                // Futuro (próximos 15 dias)
                return base.plusDays(ThreadLocalRandom.current().nextInt(1, 16))
                        .withHour(ThreadLocalRandom.current().nextInt(9, 18))
                        .withMinute(ThreadLocalRandom.current().nextBoolean() ? 0 : 30)
                        .withSecond(0).withNano(0);

            default:
                return base.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        }
    }
    private List<Servico> escolherServicosAleatorios(List<Servico> servicos) {
        List<Servico> servicosAtivos = servicos.stream()
                .filter(Servico::isAtivo)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        int quantidade = ThreadLocalRandom.current().nextDouble() < 0.7 ? 1 :
                (ThreadLocalRandom.current().nextDouble() < 0.9 ? 2 : 3);

        List<Servico> escolhidos = new ArrayList<>();
        for (int i = 0; i < quantidade && !servicosAtivos.isEmpty(); i++) {
            int index = ThreadLocalRandom.current().nextInt(servicosAtivos.size());
            escolhidos.add(servicosAtivos.remove(index));
        }

        return escolhidos.isEmpty() ? Arrays.asList(servicos.get(0)) : escolhidos;
    }

    private String gerarObservacaoAleatoria(Status status) {
        String[] observacoesPorStatus = {
                // Observações gerais
                "Cliente regular, conhece os procedimentos",
                "Primeira vez no salão",
                "Cliente VIP - atendimento especial",
                "Alergia a produtos com formol",
                "Prefere profissionais experientes"
        };

        String[] observacoesEspecificas;
        switch (status) {
            case CANCELADO:
                observacoesEspecificas = new String[]{
                        "Cliente cancelou por motivos pessoais",
                        "Reagendamento solicitado pelo cliente",
                        "Cancelado por emergência médica",
                        "Cliente viajou inesperadamente",
                        "Cancelado devido ao trânsito"
                };
                break;

            case NAO_COMPARECEU:
                observacoesEspecificas = new String[]{
                        "Cliente não compareceu sem aviso",
                        "Não atendeu ligações de confirmação",
                        "Esqueceu do agendamento",
                        "Possível problema de saúde",
                        "Cliente não justificou ausência"
                };
                break;

            case CONCLUIDO:
                observacoesEspecificas = new String[]{
                        "Serviço realizado com sucesso",
                        "Cliente muito satisfeita com resultado",
                        "Solicitou agendamento de retorno",
                        "Elogiou atendimento da equipe",
                        "Resultado superou expectativas"
                };
                break;

            case EM_ANDAMENTO:
                observacoesEspecificas = new String[]{
                        "Serviço em execução",
                        "Cliente relaxada, sem pressa",
                        "Procedimento dentro do prazo",
                        "Cliente aprovando resultado parcial",
                        "Serviço transcorrendo normalmente"
                };
                break;

            default:
                observacoesEspecificas = observacoesPorStatus;
        }

        // 70% chance de usar observação específica do status
        String[] observacoesParaUsar = ThreadLocalRandom.current().nextDouble() < 0.7 ?
                observacoesEspecificas : observacoesPorStatus;

        return observacoesParaUsar[ThreadLocalRandom.current().nextInt(observacoesParaUsar.length)];
    }

    private void criarJornadaParaFuncionario(Funcionario funcionario) {
        // Jornadas variadas para diferentes funcionários
        boolean temSabado = ThreadLocalRandom.current().nextDouble() < 0.8; // 80% trabalham sábado
        boolean temDomingo = ThreadLocalRandom.current().nextDouble() < 0.3; // 30% trabalham domingo

        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.TERCA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.QUARTA, LocalTime.of(9, 0), LocalTime.of(18, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.QUINTA, LocalTime.of(10, 0), LocalTime.of(20, 0), true));
        funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SEXTA, LocalTime.of(10, 0), LocalTime.of(20, 0), true));

        if (temSabado) {
            funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.SABADO, LocalTime.of(8, 0), LocalTime.of(16, 0), true));
        }

        if (temDomingo) {
            funcionario.addJornada(new JornadaTrabalho(null, funcionario, DiaSemana.DOMINGO, LocalTime.of(9, 0), LocalTime.of(15, 0), true));
        }
    }

    private void criarBloqueiosParaFuncionario(Funcionario funcionario) {
        // Bloqueio de almoço
        LocalDateTime inicioAlmoco = LocalDate.now().atTime(12, 0);
        LocalDateTime fimAlmoco = LocalDate.now().atTime(13, 0);
        BloqueioAgenda bloqueioAlmoco = new BloqueioAgenda(
                funcionario,
                inicioAlmoco,
                fimAlmoco,
                "Horário de Almoço",
                TipoBloqueio.ALMOCO,
                null
        );
        funcionario.addBloqueio(bloqueioAlmoco);

        // Alguns funcionários podem ter outros bloqueios
        if (ThreadLocalRandom.current().nextDouble() < 0.3) { // 30% chance
            LocalDateTime inicioReuniao = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 8))
                    .atTime(15, 0);
            LocalDateTime fimReuniao = inicioReuniao.plusHours(1);
            BloqueioAgenda bloqueioReuniao = new BloqueioAgenda(
                    funcionario,
                    inicioReuniao,
                    fimReuniao,
                    "Reunião de Equipe",
                    TipoBloqueio.REUNIAO,
                    null
            );
            funcionario.addBloqueio(bloqueioReuniao);
        }

        // Férias ou licenças para alguns funcionários
        if (ThreadLocalRandom.current().nextDouble() < 0.2) { // 20% chance
            LocalDateTime inicioFerias = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(30, 90))
                    .atTime(0, 0);
            LocalDateTime fimFerias = inicioFerias.plusDays(ThreadLocalRandom.current().nextInt(7, 21));
            BloqueioAgenda bloqueioFerias = new BloqueioAgenda(
                    funcionario,
                    inicioFerias,
                    fimFerias,
                    "Período de Férias",
                    TipoBloqueio.FERIAS,
                    null
            );
            funcionario.addBloqueio(bloqueioFerias);
        }
    }

    // Adicione este método ao seu DatabaseSeederService existente para popular dados de exemplo

    /**
     * Popula dados de exemplo para a arquitetura multi-tenant.
     * Este método deve ser chamado após a criação das organizações.
     */
    @Transactional
    public void seedTenantData() {
        System.out.println("=== Populando dados multi-tenant ===");

        try {
            // Criar tenants de exemplo
            createSampleTenants();

            System.out.println("Dados multi-tenant populados com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao popular dados multi-tenant: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cria tenants de exemplo com páginas e componentes.
     */
    private void createSampleTenants() {
        // Tenant 1 - Salão de Beleza
        Tenant salaoBeleza = Tenant.builder()
                .name("Salão Bella Vista")
                .subdomain("bella")
                .theme("beauty")
                .active(true)
                .email("contato@bella.bellory.com.br")
                .description("Salão de beleza especializado em cortes modernos e tratamentos capilares")
                .themeConfig("{\"primaryColor\":\"#ff69b4\",\"secondaryColor\":\"#ffffff\",\"fontFamily\":\"Poppins\"}")
                .build();

        salaoBeleza = tenantRepository.save(salaoBeleza);

        // Criar página inicial para o salão
        createHomePageForSalon(salaoBeleza);

        // Tenant 2 - Barbearia
        Tenant barbearia = Tenant.builder()
                .name("Barbearia Vintage")
                .subdomain("vintage")
                .theme("masculine")
                .active(true)
                .email("contato@vintage.bellory.com.br")
                .description("Barbearia tradicional com cortes clássicos e modernos")
                .themeConfig("{\"primaryColor\":\"#8b4513\",\"secondaryColor\":\"#f4f4f4\",\"fontFamily\":\"Roboto\"}")
                .build();

        barbearia = tenantRepository.save(barbearia);

        // Criar página inicial para a barbearia
        createHomePageForBarber(barbearia);

        // Tenant 3 - Spa
        Tenant spa = Tenant.builder()
                .name("Spa Relax")
                .subdomain("relax")
                .theme("wellness")
                .active(true)
                .email("contato@relax.bellory.com.br")
                .description("Spa completo com tratamentos relaxantes e terapêuticos")
                .themeConfig("{\"primaryColor\":\"#20b2aa\",\"secondaryColor\":\"#f0f8ff\",\"fontFamily\":\"Lato\"}")
                .build();

        spa = tenantRepository.save(spa);

        // Criar página inicial para o spa
        createHomePageForSpa(spa);

        System.out.println("Criados 3 tenants de exemplo com suas respectivas páginas");
    }

    /**
     * Cria página inicial para o salão de beleza.
     */
    private void createHomePageForSalon(Tenant tenant) {
        Page homePage = Page.builder()
                .tenant(tenant)
                .slug("home")
                .title("Bella Vista - Sua Beleza, Nossa Paixão")
                .description("Descubra os melhores tratamentos de beleza e cortes modernos")
                .active(true)
                .metaTitle("Salão Bella Vista - Tratamentos de Beleza Premium")
                .metaDescription("Salão de beleza especializado em cortes, coloração e tratamentos capilares. Agende já!")
                .metaKeywords("salão de beleza, cortes femininos, coloração, tratamentos capilares")
                .build();

        homePage = pageRepository.save(homePage);

        // Componente Hero
        PageComponent heroComponent = PageComponent.builder()
                .page(homePage)
                .type("HERO")
                .orderIndex(0)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Bella Vista Salão\",\n" +
                        "  \"subtitle\": \"Sua beleza é nossa paixão\",\n" +
                        "  \"description\": \"Oferecemos os melhores tratamentos de beleza com profissionais qualificados e produtos de primeira linha.\",\n" +
                        "  \"backgroundImage\": \"https://images.unsplash.com/photo-1560066984-138dadb4c035?ixlib=rb-4.0.3\",\n" +
                        "  \"ctaText\": \"Agendar Horário\",\n" +
                        "  \"ctaLink\": \"/agendamento\"\n" +
                        "}")
                .build();

        componentRepository.save(heroComponent);

        // Componente Serviços
        PageComponent servicesComponent = PageComponent.builder()
                .page(homePage)
                .type("SERVICES_GRID")
                .orderIndex(1)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Nossos Serviços\",\n" +
                        "  \"services\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Corte Feminino\",\n" +
                        "      \"description\": \"Cortes modernos e clássicos\",\n" +
                        "      \"price\": \"R$ 80,00\",\n" +
                        "      \"image\": \"https://images.unsplash.com/photo-1522337660859-02fbefca4702?ixlib=rb-4.0.3\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"name\": \"Coloração\",\n" +
                        "      \"description\": \"Coloração completa com produtos premium\",\n" +
                        "      \"price\": \"R$ 150,00\",\n" +
                        "      \"image\": \"https://images.unsplash.com/photo-1487412912498-0447578fcca8?ixlib=rb-4.0.3\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"name\": \"Tratamentos\",\n" +
                        "      \"description\": \"Hidratação e reconstrução capilar\",\n" +
                        "      \"price\": \"R$ 120,00\",\n" +
                        "      \"image\": \"https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?ixlib=rb-4.0.3\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}")
                .build();

        componentRepository.save(servicesComponent);

        // Componente Sobre
        PageComponent aboutComponent = PageComponent.builder()
                .page(homePage)
                .type("ABOUT")
                .orderIndex(2)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Sobre Nós\",\n" +
                        "  \"description\": \"Há mais de 10 anos cuidando da sua beleza com carinho e profissionalismo. Nossa equipe é formada por profissionais especializados que estão sempre se atualizando com as últimas tendências do mercado.\",\n" +
                        "  \"image\": \"https://images.unsplash.com/photo-1521590832167-7bcbfaa6381f?ixlib=rb-4.0.3\",\n" +
                        "  \"highlights\": [\n" +
                        "    \"Mais de 10 anos de experiência\",\n" +
                        "    \"Profissionais qualificados\",\n" +
                        "    \"Produtos de primeira linha\",\n" +
                        "    \"Ambiente acolhedor\"\n" +
                        "  ]\n" +
                        "}")
                .build();

        componentRepository.save(aboutComponent);

        // Componente Contato
        PageComponent contactComponent = PageComponent.builder()
                .page(homePage)
                .type("CONTACT_FORM")
                .orderIndex(3)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Entre em Contato\",\n" +
                        "  \"address\": \"Rua das Flores, 123 - Centro\",\n" +
                        "  \"phone\": \"(11) 9999-9999\",\n" +
                        "  \"email\": \"contato@bella.bellory.com.br\",\n" +
                        "  \"hours\": \"Segunda a Sexta: 9h às 18h | Sábado: 9h às 16h\",\n" +
                        "  \"showForm\": true\n" +
                        "}")
                .build();

        componentRepository.save(contactComponent);
    }

    /**
     * Cria página inicial para a barbearia.
     */
    private void createHomePageForBarber(Tenant tenant) {
        Page homePage = Page.builder()
                .tenant(tenant)
                .slug("home")
                .title("Barbearia Vintage - Tradição e Estilo")
                .description("Cortes clássicos e modernos em uma barbearia tradicional")
                .active(true)
                .metaTitle("Barbearia Vintage - Cortes Masculinos Premium")
                .metaDescription("Barbearia tradicional com cortes clássicos e modernos. Ambiente masculino e acolhedor.")
                .metaKeywords("barbearia, cortes masculinos, barba, bigode, estilo vintage")
                .build();

        homePage = pageRepository.save(homePage);

        // Componente Hero para barbearia
        PageComponent heroComponent = PageComponent.builder()
                .page(homePage)
                .type("HERO")
                .orderIndex(0)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Barbearia Vintage\",\n" +
                        "  \"subtitle\": \"Tradição, estilo e qualidade\",\n" +
                        "  \"description\": \"Uma barbearia tradicional que combina técnicas clássicas com o melhor da modernidade.\",\n" +
                        "  \"backgroundImage\": \"https://images.unsplash.com/photo-1585747860715-2ba37e788b70?ixlib=rb-4.0.3\",\n" +
                        "  \"ctaText\": \"Agendar Corte\",\n" +
                        "  \"ctaLink\": \"/agendamento\"\n" +
                        "}")
                .build();

        componentRepository.save(heroComponent);

        // Outros componentes similares...
    }

    /**
     * Cria página inicial para o spa.
     */
    private void createHomePageForSpa(Tenant tenant) {
        Page homePage = Page.builder()
                .tenant(tenant)
                .slug("home")
                .title("Spa Relax - Bem-estar e Relaxamento")
                .description("Tratamentos relaxantes e terapêuticos para seu bem-estar")
                .active(true)
                .metaTitle("Spa Relax - Tratamentos de Bem-estar")
                .metaDescription("Spa completo com massagens, tratamentos faciais e terapias relaxantes.")
                .metaKeywords("spa, massagens, relaxamento, bem-estar, tratamentos faciais")
                .build();

        homePage = pageRepository.save(homePage);

        // Componente Hero para spa
        PageComponent heroComponent = PageComponent.builder()
                .page(homePage)
                .type("HERO")
                .orderIndex(0)
                .active(true)
                .propsJson("{\n" +
                        "  \"title\": \"Spa Relax\",\n" +
                        "  \"subtitle\": \"Sua oasis de tranquilidade\",\n" +
                        "  \"description\": \"Desconecte-se do mundo e reconecte-se com você mesmo em nosso spa completo.\",\n" +
                        "  \"backgroundImage\": \"https://images.unsplash.com/photo-1544161515-4ab6ce6db874?ixlib=rb-4.0.3\",\n" +
                        "  \"ctaText\": \"Agendar Tratamento\",\n" +
                        "  \"ctaLink\": \"/agendamento\"\n" +
                        "}")
                .build();

        componentRepository.save(heroComponent);

        // Outros componentes similares...
    }

}
