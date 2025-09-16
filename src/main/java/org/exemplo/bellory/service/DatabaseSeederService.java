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
        criarProdutos(orgPrincipal);

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

    private void criarProdutos(Organizacao org) {
        System.out.println("🛍️ Criando produtos...");

        String[][] produtosData = {
                {"Shampoo Premium", "Shampoo hidratante com óleos essenciais", "45.90", "100", "Cabelo", "Feminino", "Beauty Pro", "4.5", "0", "false"},
                {"Condicionador Reparador", "Condicionador para cabelos danificados", "39.90", "80", "Cabelo", "Unissex", "Hair Care", "4.3", "15", "true"},
                {"Máscara Capilar", "Tratamento intensivo semanal", "89.90", "50", "Cabelo", "Unissex", "Intensive Care", "4.8", "20", "true"},
                {"Óleo Argan", "Óleo puro de argan marroquino", "129.90", "30", "Cabelo", "Unissex", "Argan Gold", "4.9", "0", "false"},
                {"Leave-in Protetor", "Proteção térmica e hidratação", "35.90", "120", "Cabelo", "Unissex", "Thermo Pro", "4.2", "10", "false"},

                {"Esmalte Gel", "Esmalte com efeito gel duradouro", "19.90", "200", "Unhas", "Feminino", "Nail Perfect", "4.1", "0", "false"},
                {"Base Fortalecedora", "Base que fortalece as unhas", "29.90", "150", "Unhas", "Feminino", "Strong Nails", "4.4", "0", "false"},
                {"Kit Nail Art", "Kit completo para decoração", "79.90", "25", "Unhas", "Feminino", "Art Nails", "4.7", "25", "true"},
                {"Removedor Suave", "Remove esmalte sem ressecar", "12.90", "180", "Unhas", "Feminino", "Gentle Care", "4.0", "0", "false"},
                {"Óleo Cutícula", "Hidrata e amacia as cutículas", "24.90", "100", "Unhas", "Feminino", "Cuticle Soft", "4.3", "0", "false"},

                {"Creme Anti-Idade", "Reduz rugas e linhas de expressão", "199.90", "40", "Skincare", "Unissex", "Youth Formula", "4.8", "30", "true"},
                {"Sérum Vitamina C", "Ilumina e revitaliza a pele", "159.90", "60", "Skincare", "Unissex", "Vitamin Boost", "4.6", "20", "true"},
                {"Protetor Solar Facial", "FPS 60 para rosto", "89.90", "80", "Skincare", "Unissex", "Sun Shield", "4.5", "0", "false"},
                {"Água Micelar", "Remove maquiagem suavemente", "49.90", "120", "Skincare", "Unissex", "Micellar Clean", "4.2", "15", "false"},
                {"Tônico Facial", "Equilibra pH da pele", "39.90", "100", "Skincare", "Unissex", "Balance Tone", "4.1", "0", "false"},

                {"Base Líquida", "Cobertura natural duradoura", "69.90", "90", "Maquiagem", "Feminino", "Perfect Skin", "4.4", "0", "false"},
                {"Paleta Sombras", "12 cores vibrantes", "119.90", "45", "Maquiagem", "Feminino", "Color Palette", "4.7", "35", "true"},
                {"Batom Matte", "Acabamento matte duradouro", "29.90", "150", "Maquiagem", "Feminino", "Matte Kiss", "4.3", "0", "false"},
                {"Rímel Alongador", "Alonga e volumiza os cílios", "45.90", "80", "Maquiagem", "Feminino", "Lash Volume", "4.5", "0", "false"},
                {"Blush Compacto", "Cor natural para as bochechas", "39.90", "70", "Maquiagem", "Feminino", "Natural Glow", "4.2", "20", "false"},

                {"Cera Modeladora", "Fixa e modela o cabelo", "35.90", "60", "Styling", "Masculino", "Style Fix", "4.0", "0", "false"},
                {"Gel Fixador", "Fixação forte sem ressecamento", "25.90", "90", "Styling", "Masculino", "Strong Hold", "4.1", "0", "false"},
                {"Pomada Capilar", "Brilho e fixação moderada", "42.90", "55", "Styling", "Masculino", "Shine Wax", "4.3", "15", "false"},
                {"Spray Texturizador", "Textura e movimento", "38.90", "75", "Styling", "Unissex", "Texture Boost", "4.4", "0", "false"},
                {"Mousse Volumizador", "Volume e leveza", "46.90", "65", "Styling", "Feminino", "Volume Up", "4.2", "25", "true"},

                {"Kit Coloração", "Kit completo para colorir em casa", "89.90", "35", "Kits", "Unissex", "Home Color", "4.0", "40", "true"},
                {"Kit Hidratação", "Tratamento completo hidratante", "129.90", "40", "Kits", "Unissex", "Hydra Kit", "4.6", "30", "true"},
                {"Kit Manicure", "Ferramentas profissionais", "159.90", "20", "Kits", "Feminino", "Nail Pro Kit", "4.8", "45", "true"},
                {"Kit Barba", "Cuidados completos para barba", "199.90", "25", "Kits", "Masculino", "Beard Care", "4.7", "35", "true"},
                {"Kit Noiva", "Produtos especiais para noivas", "399.90", "15", "Kits", "Feminino", "Bridal Kit", "4.9", "50", "true"},

                {"Perfume Floral", "Fragrância delicada e feminina", "159.90", "50", "Perfumaria", "Feminino", "Flower Essence", "4.6", "0", "false"},
                {"Perfume Amadeirado", "Fragrância masculina marcante", "179.90", "45", "Perfumaria", "Masculino", "Wood Scent", "4.5", "20", "false"},
                {"Body Splash", "Fragrância suave para o corpo", "49.90", "100", "Perfumaria", "Feminino", "Fresh Body", "4.2", "0", "false"},
                {"Desodorante Roll-on", "Proteção 48h sem álcool", "19.90", "150", "Perfumaria", "Unissex", "Dry Care", "4.1", "0", "false"},
                {"Água Perfumada", "Fragrância leve e refrescante", "79.90", "80", "Perfumaria", "Unissex", "Light Scent", "4.3", "25", "false"},

                {"Escova Profissional", "Cerdas naturais para alisamento", "89.90", "30", "Acessórios", "Unissex", "Pro Brush", "4.7", "0", "false"},
                {"Secador Íons", "Tecnologia íons para brilho", "299.90", "15", "Acessórios", "Unissex", "Ion Dryer", "4.8", "25", "true"},
                {"Chapinha Cerâmica", "Placas de cerâmica profissional", "199.90", "20", "Acessórios", "Unissex", "Ceramic Pro", "4.6", "30", "true"},
                {"Babyliss", "Modelador de cachos", "149.90", "25", "Acessórios", "Feminino", "Curl Master", "4.5", "20", "false"},
                {"Kit Pincéis", "Pincéis profissionais maquiagem", "119.90", "35", "Acessórios", "Feminino", "Brush Set", "4.4", "35", "true"},

                {"Sabonete Esfoliante", "Remove células mortas", "29.90", "120", "Corpo", "Unissex", "Exfoliant Care", "4.1", "0", "false"},
                {"Hidratante Corporal", "Nutrição intensa 24h", "45.90", "100", "Corpo", "Unissex", "Body Moist", "4.3", "15", "false"},
                {"Óleo Corporal", "Hidratação profunda com óleos", "69.90", "60", "Corpo", "Feminino", "Body Oil", "4.5", "20", "false"},
                {"Creme Anticelulite", "Reduz aparência da celulite", "89.90", "40", "Corpo", "Feminino", "Slim Body", "4.2", "25", "true"},
                {"Protetor Solar Corporal", "FPS 50 resistente à água", "59.90", "80", "Corpo", "Unissex", "Sun Body", "4.4", "0", "false"},

                {"Suplemento Capilar", "Vitaminas para crescimento", "129.90", "50", "Suplementos", "Unissex", "Hair Growth", "4.6", "0", "false"},
                {"Colágeno Hidrolisado", "Beleza de dentro para fora", "89.90", "60", "Suplementos", "Unissex", "Beauty Collagen", "4.7", "30", "true"},
                {"Vitamina E", "Antioxidante natural", "39.90", "100", "Suplementos", "Unissex", "Vitamin E", "4.3", "0", "false"},
                {"Biotina", "Fortalece cabelos e unhas", "49.90", "80", "Suplementos", "Unissex", "Biotin Plus", "4.4", "20", "false"}
        };

        for (String[] data : produtosData) {
            String nome = data[0];
            produtoRepository.findByNomeAndOrganizacao(nome, org).orElseGet(() -> {
                Produto p = new Produto();
                p.setOrganizacao(org);
                p.setNome(data[0]);
                p.setDescricao(data[1]);
                p.setPreco(new BigDecimal(data[2]));
                p.setQtdEstoque(Integer.parseInt(data[3]));
                p.setCategoria(data[4]);
                p.setGenero(data[5]);
                p.setMarca(data[6]);
                p.setAvaliacao(new BigDecimal(data[7]));
                p.setDescontoPercentual(Integer.parseInt(data[8]) > 0 ? Integer.parseInt(data[8]) : null);
                p.setDestaque(Boolean.parseBoolean(data[9]));
                p.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.95);
                p.setUrlsImagens(Arrays.asList(
                        "https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=400",
                        "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=400"
                ));
                p.setIngredientes(Arrays.asList(
                        "Água", "Ingrediente Ativo", "Conservantes", "Fragrância", "Vitaminas"
                ));
                p.setTotalAvaliacoes(ThreadLocalRandom.current().nextInt(5, 50));
                return produtoRepository.save(p);
            });
        }
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
