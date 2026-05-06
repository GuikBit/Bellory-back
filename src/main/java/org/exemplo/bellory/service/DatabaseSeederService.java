package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.instancia.InstanceCreateDTO;
import org.exemplo.bellory.model.dto.organizacao.*;
import org.exemplo.bellory.model.dto.tema.*;
import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.agendamento.Status;
import org.exemplo.bellory.model.entity.cobranca.Cobranca;
import org.exemplo.bellory.model.entity.config.*;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.funcionario.*;
import org.exemplo.bellory.model.entity.organizacao.AcessoAdm;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.entity.organizacao.Responsavel;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.tema.*;
import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.PageComponent;
import org.exemplo.bellory.model.entity.tenant.Tenant;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.users.Role;
import org.exemplo.bellory.model.entity.compra.Compra;
import org.exemplo.bellory.model.entity.compra.CompraProduto;
import org.exemplo.bellory.model.entity.financeiro.*;
import org.exemplo.bellory.model.entity.notificacao.TipoNotificacao;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.exemplo.bellory.model.entity.questionario.OpcaoResposta;
import org.exemplo.bellory.model.entity.questionario.Pergunta;
import org.exemplo.bellory.model.entity.questionario.Questionario;
import org.exemplo.bellory.model.entity.questionario.enums.TipoPergunta;
import org.exemplo.bellory.model.entity.questionario.enums.TipoQuestionario;
import org.exemplo.bellory.model.repository.Transacao.CobrancaRepository;
import org.exemplo.bellory.model.repository.Transacao.CompraRepository;
import org.exemplo.bellory.model.repository.Transacao.PagamentoRepository;
import org.exemplo.bellory.client.payment.PaymentApiClient;
import org.exemplo.bellory.client.payment.dto.PlanResponse;
import org.exemplo.bellory.exception.PaymentApiException;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.config.ConfigSistemaRepository;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.exemplo.bellory.model.repository.tenant.PageComponentRepository;
import org.exemplo.bellory.model.repository.tenant.PageRepository;
import org.exemplo.bellory.model.repository.tenant.TenantRepository;
import org.exemplo.bellory.model.repository.financeiro.*;
import org.exemplo.bellory.model.repository.notificacao.ConfigNotificacaoRepository;
import org.exemplo.bellory.model.repository.questionario.QuestionarioRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.exemplo.bellory.model.repository.users.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.CoderResult;
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
    private final CobrancaRepository cobrancaRepository;

    private final ProdutoRepository produtoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;
    private final AdminRepository adminRepository;
    private final ConfigSistemaRepository configSistemaRepository;

    private final TenantRepository tenantRepository;
    private final PageRepository pageRepository;
    private final PageComponentRepository componentRepository;
    private final ApiKeyService apiKeyService;

    private final InstanceService instanceService;
    private final OrganizacaoService organizacaoService;

    // Novos repositories para módulos expandidos
    private final ContaBancariaRepository contaBancariaRepository;
    private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final CompraRepository compraRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ConfigNotificacaoRepository notificacaoConfigRepository;
    private final QuestionarioRepository questionarioRepository;

    // Payment API (planos vivem no serviço externo)
    private final PaymentApiClient paymentApiClient;

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
    private final CargoRepository cargoRepository;

    public DatabaseSeederService(OrganizacaoRepository organizacaoRepository, RoleRepository roleRepository,
                                 FuncionarioRepository funcionarioRepository, ClienteRepository clienteRepository,
                                 ServicoRepository servicoRepository, AgendamentoRepository agendamentoRepository,
                                 ProdutoRepository produtoRepository,
                                 PasswordEncoder passwordEncoder, CategoriaRepository categoriaRepository,
                                 PageComponentRepository componentRepository, PageRepository pageRepository, TenantRepository tenantRepository,
                                 AdminRepository adminRepository, CargoRepository cargoRepository,
                                 ConfigSistemaRepository configSistemaRepository, ApiKeyService apiKeyService, InstanceService instanceService,
                                 CobrancaRepository cobrancaRepository,
                                 ContaBancariaRepository contaBancariaRepository,
                                 CategoriaFinanceiraRepository categoriaFinanceiraRepository,
                                 CentroCustoRepository centroCustoRepository,
                                 ContaPagarRepository contaPagarRepository,
                                 ContaReceberRepository contaReceberRepository,
                                 LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
                                 CompraRepository compraRepository,
                                 PagamentoRepository pagamentoRepository,
                                 ConfigNotificacaoRepository notificacaoConfigRepository,
                                 QuestionarioRepository questionarioRepository,
                                 OrganizacaoService organizacaoService,
                                 PaymentApiClient paymentApiClient) {
        this.organizacaoRepository = organizacaoRepository;
        this.roleRepository = roleRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.produtoRepository = produtoRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaRepository = categoriaRepository;
        this.componentRepository = componentRepository;
        this.pageRepository = pageRepository;
        this.tenantRepository = tenantRepository;
        this.adminRepository = adminRepository;
        this.cargoRepository = cargoRepository;
        this.configSistemaRepository = configSistemaRepository;
        this.apiKeyService = apiKeyService;
        this.instanceService = instanceService;
        this.cobrancaRepository = cobrancaRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
        this.compraRepository = compraRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.notificacaoConfigRepository = notificacaoConfigRepository;
        this.questionarioRepository = questionarioRepository;
        this.organizacaoService = organizacaoService;
        this.paymentApiClient = paymentApiClient;
    }

    @Transactional
    public void seedDatabase() {
        System.out.println("🚀 Iniciando seeding completo do banco de dados...");

        // 1. PLANOS — vivem na Payment API. Validar que estao acessiveis antes de prosseguir.
        List<PlanResponse> planosPayment = validarPlanosPaymentApi();

        // 2. ORGANIZAÇÕES (OrganizacaoService.create() cria customer + subscription na Payment API)
        List<Organizacao> organizacoes = criarOrganizacoes();
        Organizacao orgPrincipal = organizacoes.get(0);

        // 3. ROLES
        List<Role> roles = criarRoles();

        // 4. CATEGORIAS
        List<Categoria> categorias = criarCategorias(orgPrincipal);

        // 5. FUNCIONÁRIOS (15 funcionários)
        List<Funcionario> funcionarios = criarFuncionarios(orgPrincipal);

        // 6. CLIENTES (50 clientes)
        List<Cliente> clientes = criarClientes(orgPrincipal);

        // 7. SERVIÇOS (30 serviços)
        List<Servico> servicos = criarServicos(categorias, orgPrincipal);

        // 8. VINCULAÇÃO SERVIÇOS-FUNCIONÁRIOS
        vincularServicosComFuncionarios(funcionarios, servicos);

        // 8. PRODUTOS (50 produtos)
        List<Produto> produtos = criarProdutos(orgPrincipal, categorias);

        // 9. AGENDAMENTOS (100 agendamentos com todos os status)
        criarAgendamentos(orgPrincipal, funcionarios, clientes, servicos);

        // 10. MÓDULO FINANCEIRO
        List<ContaBancaria> contasBancarias = criarContasBancarias(orgPrincipal);
        List<CategoriaFinanceira> categoriasFinanceiras = criarCategoriasFinanceiras(orgPrincipal);
        List<CentroCusto> centrosCusto = criarCentrosCusto(orgPrincipal);
        criarContasPagar(orgPrincipal, contasBancarias, categoriasFinanceiras, centrosCusto);
        criarContasReceber(orgPrincipal, clientes, contasBancarias, categoriasFinanceiras, centrosCusto);
        criarLancamentosFinanceiros(orgPrincipal, contasBancarias, categoriasFinanceiras, centrosCusto);

        // 11. COMPRAS DE PRODUTOS
        criarCompras(orgPrincipal, clientes, produtos);

        // 12. PAGAMENTOS COMPLETOS PARA COBRANÇAS DE AGENDAMENTOS
        criarPagamentosParaCobrancas(orgPrincipal);

        // 13. CONFIGURAÇÕES DE NOTIFICAÇÃO
        criarConfigNotificacoesOrganizacao(orgPrincipal);

        // 14. QUESTIONÁRIOS
        criarQuestionarios(orgPrincipal);

        // Assinaturas (vinculo customer/subscription) ja foram criadas por OrganizacaoService.create().
        // Nao populamos cobrancas/cupons/webhooks localmente - esses estados vivem na Payment API.

        System.out.println("✅ Seeding completo finalizado com sucesso!");
        System.out.println("📊 Dados criados:");
        System.out.println("   - Planos validados na Payment API: " + planosPayment.size());
        System.out.println("   - Organizações: " + organizacoes.size());
        System.out.println("   - Funcionários: " + funcionarios.size());
        System.out.println("   - Clientes: " + clientes.size());
        System.out.println("   - Serviços: " + servicos.size());
        System.out.println("   - Produtos: " + produtos.size());
        System.out.println("   - Agendamentos: 100");
        System.out.println("   - Contas Bancárias: " + contasBancarias.size());
        System.out.println("   - Categorias Financeiras: " + categoriasFinanceiras.size());
        System.out.println("   - Centros de Custo: " + centrosCusto.size());
        System.out.println("   - Módulo Financeiro: Contas a pagar/receber + Lançamentos");
        System.out.println("   - Compras de Produtos: com cobranças e pagamentos");
        System.out.println("   - Pagamentos de Agendamentos: com fluxo completo");
        System.out.println("   - Configurações de Notificação: 4 regras");
        System.out.println("   - Questionários: 3 com perguntas e opções");
        System.out.println("   - Assinaturas: " + organizacoes.size() + " (vinculadas a Payment API)");
    }

    /**
     * Valida que os planos esperados existem na Payment API antes de criar organizacoes.
     * Se a Payment API estiver fora ou algum plano estiver faltando, falha rapido (em vez de
     * dar erro crytico no meio do seed).
     */
    private List<PlanResponse> validarPlanosPaymentApi() {
        System.out.println("📋 Validando planos na Payment API...");
        String[] codigosEsperados = {"gratuito", "basico", "plus", "premium"};
        List<PlanResponse> planos = new ArrayList<>();
        for (String codigo : codigosEsperados) {
            try {
                PlanResponse plano = paymentApiClient.getPlanByCodigo(codigo);
                if (plano == null || !Boolean.TRUE.equals(plano.getActive())) {
                    throw new IllegalStateException("Plano '" + codigo + "' nao esta ativo na Payment API");
                }
                planos.add(plano);
                System.out.println("   ✓ Plano '" + codigo + "' (id=" + plano.getId() + ", " + plano.getName() + ")");
            } catch (PaymentApiException e) {
                throw new IllegalStateException(
                        "Falha ao consultar plano '" + codigo + "' na Payment API. " +
                        "Garanta que a Payment API esta no ar e que os 4 planos (gratuito/basico/plus/premium) " +
                        "estao cadastrados e ativos. Detalhe: " + e.getMessage(), e);
            }
        }
        return planos;
    }

    private void vincularServicosComFuncionarios(List<Funcionario> funcionarios, List<Servico> servicos) {
        System.out.println("🔗 Vinculando serviços com funcionários...");

        Map<String, List<String>> especialidadesPorFuncionario = Map.of(
                "funcionario1", List.of("cabelo", "sobrancelhas", "maquiagem"),
                "funcionario2", List.of("cabelo", "barba"),
                "funcionario3", List.of("estetica_facial", "tratamentos", "sobrancelhas"),
                "funcionario4", List.of("cabelo"),
                "funcionario5", List.of("maos_pes", "sobrancelhas")
        );

        Map<String, List<Servico>> servicosPorCategoria = servicos.stream()
                .collect(Collectors.groupingBy(s -> s.getCategoria().getValue()));

        int vinculosCriados = 0;

        for (Funcionario funcionario : funcionarios) {
            List<String> especialidades = especialidadesPorFuncionario.get(funcionario.getUsername());
            if (especialidades == null) continue;

            List<Servico> servicosDoFuncionario = new ArrayList<>();

            for (String especialidade : especialidades) {
                List<Servico> servicosCategoria = servicosPorCategoria.get(especialidade);
                if (servicosCategoria != null) {
                    Collections.shuffle(servicosCategoria);
                    int quantidade = Math.min(servicosCategoria.size(), ThreadLocalRandom.current().nextInt(1, 4));
                    servicosDoFuncionario.addAll(servicosCategoria.subList(0, quantidade));
                }
            }

            for (Servico servico : servicos) {
                if (!servicosDoFuncionario.contains(servico) && ThreadLocalRandom.current().nextDouble() < 0.1) {
                    servicosDoFuncionario.add(servico);
                }
            }

            if (funcionario.getServicos() == null) {
                funcionario.setServicos(new ArrayList<>());
            }
            funcionario.getServicos().clear();
            funcionario.getServicos().addAll(servicosDoFuncionario);

            funcionarioRepository.save(funcionario);
            vinculosCriados += servicosDoFuncionario.size();

            System.out.println("   " + funcionario.getNomeCompleto() + " vinculado a " +
                    servicosDoFuncionario.size() + " serviços: " +
                    servicosDoFuncionario.stream().map(Servico::getNome).collect(Collectors.joining(", ")));
        }

        System.out.println("✅ Total de vínculos serviço-funcionário criados: " + vinculosCriados);
    }

    private List<Organizacao> criarOrganizacoes() {
        System.out.println("🏢 Criando organizações via OrganizacaoService.create()...");
        List<Organizacao> organizacoes = new ArrayList<>();

        // Dados de exemplo para organizações
        // {nomeFantasia, razaoSocial, cnpj (válido), telefone1, telefone2, whatsapp, emailPrincipal,
        //  inscricaoEstadual, responsavelNome, responsavelEmail, responsavelTelefone,
        //  adminLogin, adminSenha, planoCodigo}
        String[][] orgData = {
                {
                        "Bellory Salon",
                        "Bellory Salon & Spa LTDA",
                        "12.345.678/0001-95",
                        "(11) 3000-1000",
                        "(11) 3000-1001",
                        "(11) 99000-1000",
                        "contato@bellorysalon.com.br",
                        "110.042.490.114",
                        "Admin do Sistema",
                        "admin@bellory.com",
                        "(11) 99999-0001",
                        "admin",
                        "admin123",
                        "gratuito"
                },
                {
                        "Studio Elegance",
                        "Studio Elegance Premium LTDA",
                        "98.765.432/0001-98",
                        "(11) 3100-2000",
                        "(11) 3100-2001",
                        "(11) 99100-2000",
                        "contato@elegance.com.br",
                        "110.042.490.115",
                        "Maria Fernanda Silva",
                        "maria@elegance.com.br",
                        "(11) 99999-0002",
                        "elegance_admin",
                        "elegance123",
                        "basico"
                },
                {
                        "Salon Moderno",
                        "Salon Moderno Hair & Beauty LTDA",
                        "11.222.333/0001-81",
                        "(11) 3200-3000",
                        "(11) 3200-3001",
                        "(11) 99200-3000",
                        "info@moderno.com.br",
                        "110.042.490.116",
                        "Carlos Roberto Santos",
                        "carlos@moderno.com.br",
                        "(11) 99999-0003",
                        "moderno_admin",
                        "moderno123",
                        "plus"
                },
                {
                        "Beauty Premium Spa",
                        "Beauty Premium Spa LTDA",
                        "71.444.156/0001-00",
                        "(21) 3300-4000",
                        "(21) 3300-4001",
                        "(21) 99300-4000",
                        "contato@beautypremium.com.br",
                        "110.042.490.117",
                        "Ana Paula Ribeiro",
                        "ana@beautypremium.com.br",
                        "(21) 99999-0004",
                        "beauty_admin",
                        "beauty123",
                        "premium"
                },
                {
                        "Corte e Arte",
                        "Corte e Arte Cabeleireiros LTDA",
                        "58.245.412/0001-71",
                        "(31) 3400-5000",
                        "(31) 3400-5001",
                        "(31) 99400-5000",
                        "contato@corteearte.com.br",
                        "110.042.490.118",
                        "Rodrigo Mendes",
                        "rodrigo@corteearte.com.br",
                        "(31) 99999-0005",
                        "corte_admin",
                        "corte123",
                        "basico"
                },
                {
                        "Glamour Hair Studio",
                        "Glamour Hair Studio LTDA",
                        "84.367.482/0001-02",
                        "(41) 3500-6000",
                        "(41) 3500-6001",
                        "(41) 99500-6000",
                        "contato@glamourhair.com.br",
                        "110.042.490.119",
                        "Fernanda Costa",
                        "fernanda@glamourhair.com.br",
                        "(41) 99999-0006",
                        "glamour_admin",
                        "glamour123",
                        "plus"
                },
                {
                        "Barbearia Vintage",
                        "Barbearia Vintage LTDA",
                        "31.576.854/0001-44",
                        "(51) 3600-7000",
                        "(51) 3600-7001",
                        "(51) 99600-7000",
                        "contato@barbeariavintage.com.br",
                        "110.042.490.120",
                        "Marcos Oliveira",
                        "marcos@barbeariavintage.com.br",
                        "(51) 99999-0007",
                        "vintage_admin",
                        "vintage123",
                        "basico"
                },
                {
                        "Espaco Zen Beauty",
                        "Espaco Zen Beauty LTDA",
                        "47.382.915/0001-72",
                        "(61) 3700-8000",
                        "(61) 3700-8001",
                        "(61) 99700-8000",
                        "contato@espacozen.com.br",
                        "110.042.490.121",
                        "Juliana Almeida",
                        "juliana@espacozen.com.br",
                        "(61) 99999-0008",
                        "zen_admin",
                        "zen123",
                        "gratuito"
                }
        };

        for (int i = 0; i < orgData.length; i++) {
            String[] data = orgData[i];
            int finalI = i;

            // Verifica se a organização já existe (idempotência)
            Optional<Organizacao> existente = organizacaoRepository.findByNomeFantasia(data[0]);
            if (existente.isPresent()) {
                organizacoes.add(existente.get());
                System.out.println("   ✓ Organização já existe: " + data[0]);
                continue;
            }

            // === Monta o DTO completo para criação via service ===
            CreateOrganizacaoDTO dto = new CreateOrganizacaoDTO();
            dto.setNomeFantasia(data[0]);
            dto.setRazaoSocial(data[1]);
            dto.setCnpj(data[2]);
            dto.setTelefone1(data[3]);
            dto.setTelefone2(data[4]);
            dto.setWhatsapp(data[5]);
            dto.setEmail(data[6]);
            dto.setInscricaoEstadual(data[7]);

            ResponsavelDTO responsavel = new ResponsavelDTO();
            responsavel.setNome(data[8]);
            responsavel.setEmail(data[9]);
            responsavel.setTelefone(data[10]);
            dto.setResponsavel(responsavel);

            AcessoAdmDTO acesso = new AcessoAdmDTO();
            acesso.setLogin(data[11]);
            acesso.setSenha(data[12]);
            dto.setAcessoAdm(acesso);

            PlanoDTO planoDTO = new PlanoDTO();
            planoDTO.setId(data[13]);
            dto.setPlano(planoDTO);

            EnderecoDTO endereco = new EnderecoDTO();
            endereco.setCep("36048310");
            endereco.setLogradouro("Rua Diomar Monteiro");
            endereco.setNumero("1509");
            endereco.setComplemento("Rua Diomar Monteiro");
            endereco.setBairro("Grama");
            endereco.setCidade("Juiz de Fora");
            endereco.setUf("MG");
            endereco.setLatitude(BigDecimal.valueOf(1));
            endereco.setLongitude(BigDecimal.valueOf(32.34234));
            dto.setEndereco(endereco);

            String[] nomesTemas = {"Tema Elegante", "Tema Moderno", "Tema Clássico"};
            dto.setTema(nomesTemas[finalI % nomesTemas.length]);

            // Cria organização via service (valida CNPJ, cria Funcionário ADMIN, Admin suporte, ConfigSistema, Cargo,
            // e chama Payment API pra criar customer + subscription).
            // Se a Payment API recusar (ex: CNPJ/customer ja cadastrado de um seed anterior),
            // logamos e pulamos essa org pra nao abortar o seed inteiro.
            OrganizacaoResponseDTO response;
            try {
                response = organizacaoService.create(dto);
            } catch (Exception e) {
                System.err.println("   ✗ Falha ao criar org '" + data[0] + "' (cnpj=" + data[2] + " plano=" + data[13] + "): " + e.getMessage());
                System.err.println("     -> Se o erro for 'Documento ja cadastrado', limpe o customer correspondente na Payment API e rode novamente.");
                continue;
            }

            // Busca a entidade para operações subsequentes do seeder
            Organizacao org = organizacaoRepository.findById(response.getId()).get();

            // Redes sociais (não faz parte do CreateDTO)
            RedesSociais redesSociais = new RedesSociais();
            redesSociais.setInstagram("@" + data[0].toLowerCase().replaceAll(" ", ""));
            redesSociais.setFacebook("facebook.com/" + data[0].toLowerCase().replaceAll(" ", ""));
            redesSociais.setWhatsapp(data[5]);
            redesSociais.setLinkedin("linkedin.com/company/" + data[0].toLowerCase().replaceAll(" ", ""));
            redesSociais.setSite("www." + data[0].toLowerCase().replaceAll(" ", "") + ".com.br");
            org.setRedesSociais(redesSociais);
            organizacaoRepository.save(org);

            // API Key já é criada pelo OrganizacaoService.create() junto com o admin suporte
            Admin adminSuporte = adminRepository.findByUsernameAndOrganizacao_Id("bellory_suporte", org.getId())
                    .orElse(null);
            if (adminSuporte != null) {
                System.out.println("   ✓ Admin suporte encontrado: " + adminSuporte.getUsername());
            }

            organizacoes.add(org);
            System.out.println("   ✓ Organização criada via service: " + org.getNomeFantasia() + " (slug: " + org.getSlug() + ")");
        }

        if (organizacoes.isEmpty()) {
            throw new IllegalStateException("Nenhuma organizacao foi criada. Verifique os logs acima e o estado da Payment API.");
        }

        return organizacoes;
    }

    private Tema criarTemaPersonalizado(int indice) {
        Tema tema = new Tema();

        // Definir nome e tipo do tema baseado no índice
        String[] nomesTemas = {"Tema Elegante", "Tema Moderno", "Tema Clássico"};
        String[] tiposTemas = {"LIGHT", "DARK", "LIGHT"};

        tema.setNome(nomesTemas[indice % nomesTemas.length]);
        tema.setTipo(tiposTemas[indice % tiposTemas.length]);

        // === CORES ===
        Cores cores = new Cores();

        // Paletas de cores diferentes por tema
        if (indice == 0) { // Tema Elegante (Rosa/Pink)
            cores.setPrimary("#E91E63");
            cores.setSecondary("#F48FB1");
            cores.setAccent("#C2185B");
        } else if (indice == 1) { // Tema Moderno (Azul)
            cores.setPrimary("#2196F3");
            cores.setSecondary("#64B5F6");
            cores.setAccent("#1976D2");
        } else { // Tema Clássico (Roxo)
            cores.setPrimary("#9C27B0");
            cores.setSecondary("#BA68C8");
            cores.setAccent("#7B1FA2");
        }

        // Cores comuns
        cores.setBackground("#FFFFFF");
        cores.setText("#212121");
        cores.setTextSecondary("#757575");
        cores.setCardBackground("#FAFAFA");
        cores.setCardBackgroundSecondary("#F5F5F5");
        cores.setButtonText("#FFFFFF");
        cores.setBackgroundLinear("linear-gradient(135deg, " + cores.getPrimary() + " 0%, " + cores.getSecondary() + " 100%)");
        cores.setSuccess("#4CAF50");
        cores.setWarning("#FF9800");
        cores.setError("#F44336");
        cores.setInfo("#2196F3");
        cores.setBorder("#E0E0E0");
        cores.setBorderLight("#F5F5F5");
        cores.setDivider("#BDBDBD");
        cores.setOverlay("rgba(0, 0, 0, 0.5)");
        cores.setModalBackground("#FFFFFF");
        cores.setInputBackground("#FAFAFA");
        cores.setInputBorder("#E0E0E0");
        cores.setInputFocus(cores.getPrimary());
        cores.setPlaceholder("#9E9E9E");
        cores.setNavBackground("#FFFFFF");
        cores.setNavHover("#F5F5F5");
        cores.setNavActive(cores.getPrimary());
        cores.setOnline("#4CAF50");
        cores.setOffline("#9E9E9E");
        cores.setAway("#FF9800");
        cores.setBusy("#F44336");

        tema.setCores(cores);

        // === FONTS ===
        Fonts fonts = new Fonts();
        fonts.setHeading("Poppins, sans-serif");
        fonts.setBody("Inter, sans-serif");
        fonts.setMono("JetBrains Mono, monospace");
        tema.setFonts(fonts);

        // === BORDER RADIUS ===
        BorderRadius borderRadius = new BorderRadius();
        borderRadius.setSmall("4px");
        borderRadius.setMedium("8px");
        borderRadius.setLarge("12px");
        borderRadius.setXl("16px");
        borderRadius.setFull("9999px");
        tema.setBorderRadius(borderRadius);

        // === SHADOWS ===
        Shadows shadows = new Shadows();
        shadows.setBase("0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)");
        shadows.setMd("0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06)");
        shadows.setLg("0 10px 15px rgba(0, 0, 0, 0.1), 0 4px 6px rgba(0, 0, 0, 0.05)");
        shadows.setPrimaryGlow("0 0 20px " + cores.getPrimary() + "80");
        shadows.setAccentGlow("0 0 20px " + cores.getAccent() + "80");
        tema.setShadows(shadows);

        return tema;
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

    private List<Categoria> criarCategorias(Organizacao org) {
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
            Categoria categoria = categoriaRepository.findByOrganizacao_IdAndTipo(org.getId(), tipo).stream()
                    .filter(c -> c.getLabel().equalsIgnoreCase(data[0]))
                    .findFirst()
                    .orElseGet(() -> {
                        Categoria c = new Categoria();
                        c.setLabel(data[0]);
                        c.setValue(data[1]);
                        c.setTipo(tipo);
                        c.setOrganizacao(org);
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

        // Definir especialidades por funcionário
        String[][] especialidadesPorFuncionario = {
                {"Ana Silva", "Feminino", "Cabeleireiro(a)", "cabelo,sobrancelhas,maquiagem"}, // Especialista em beleza feminina
                {"Carlos Santos", "Masculino", "Barbeiro", "cabelo,barba"}, // Barbeiro especializado
                {"Maria Oliveira", "Feminino", "Esteticista", "estetica_facial,tratamentos,sobrancelhas"}, // Estética facial
                {"João Costa", "Masculino", "Cabeleireiro(a)", "cabelo"}, // Cabelo masculino e feminino
                {"Fernanda Lima", "Feminino", "Manicure", "maos_pes,sobrancelhas"} // Mãos, pés e sobrancelhas
        };

        for (int i = 1; i <= 5; i++) {
            String username = "funcionario" + i;
            int finalI = i;

            Funcionario funcionario = funcionarioRepository.findByUsernameAndOrganizacao_Id(username, org.getId())
                    .orElseGet(() -> {
                        String[] dados = especialidadesPorFuncionario[finalI - 1];
                        String nomeCompleto = dados[0];
                        String sexo = dados[1];
                        String cargo = dados[2];

                        Cargo c = new Cargo();
                        c.setNome(dados[2]);
                        c.setOrganizacao(org);
                        cargoRepository.save(c);

                        Funcionario f = new Funcionario();
                        f.setUsername(username);
                        f.setNomeCompleto(nomeCompleto);
                        f.setEmail(username + "@bellory.com");
                        f.setPassword(passwordEncoder.encode("password123"));
                        f.setCargo(c);
                        f.setRole(finalI <= 2 ? "ROLE_ADMIN" : (finalI <= 5 ? "ROLE_GERENTE" : "ROLE_FUNCIONARIO"));
                        f.setOrganizacao(org);
                        f.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.9); // 90% ativos

                        // Dados pessoais baseados na especialidade
                        boolean isFeminino = "Feminino".equals(sexo);
                        //f.getFotoPerfil("https://randomuser.me/api/portraits/" + (isFeminino ? "women/" : "men/") + finalI + ".jpg");
                        f.setCpf(String.format("%03d.%03d.%03d-%02d",
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(1000),
                                ThreadLocalRandom.current().nextInt(100)));
                        f.setTelefone(String.format("(11) 9%04d-%04d",
                                ThreadLocalRandom.current().nextInt(10000),
                                ThreadLocalRandom.current().nextInt(10000)));
                        f.setDataNasc(LocalDate.now().minusYears(ThreadLocalRandom.current().nextInt(20, 60)));
                        f.setSexo(sexo);
                        f.setNivel(ThreadLocalRandom.current().nextInt(1, 6));
                        f.setApelido(nomeCompleto.split(" ")[0]);
                        f.setSituacao(situacoes[ThreadLocalRandom.current().nextInt(situacoes.length)]);
                        f.setCep(String.format("%05d-%03d", ThreadLocalRandom.current().nextInt(100000), ThreadLocalRandom.current().nextInt(1000)));
                        f.setLogradouro("Rua " + sobrenomes[ThreadLocalRandom.current().nextInt(sobrenomes.length)]);
                        f.setNumero(String.valueOf(ThreadLocalRandom.current().nextInt(1, 9999)));
                        f.setBairro("Bairro " + (finalI <= 5 ? "Centro" : "Vila " + nomeCompleto.split(" ")[0]));
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
                        f.setVisivelExterno(ThreadLocalRandom.current().nextDouble() < 0.8); // 80% visível externamente

                        return funcionarioRepository.save(f);
                    });

            // Criar jornada de trabalho se não existe
            if (funcionario.getJornadasDia().isEmpty()) {
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

            Cliente cliente = clienteRepository.findByUsernameAndOrganizacao_Id(username, org.getId()).orElseGet(() -> {
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
                // Cabelo (categoria index 0)
                {"Corte Feminino", "0", "Corte personalizado para cabelo feminino", "60", "129.90", "Feminino"},
                {"Corte Masculino", "0", "Corte clássico e moderno para homens", "45", "45.00", "Masculino"},
                {"Corte Infantil", "0", "Corte especial para crianças", "30", "35.00", "Unissex"},
                {"Escova", "0", "Escova modeladora profissional", "45", "65.00", "Feminino"},
                {"Hidratação", "0", "Tratamento hidratante intensivo", "90", "89.90", "Unissex"},
                {"Coloração", "0", "Coloração completa dos cabelos", "180", "189.90", "Unissex"},
                {"Luzes", "0", "Mechas e luzes personalizadas", "120", "159.90", "Feminino"},
                {"Alisamento", "0", "Alisamento progressivo profissional", "240", "299.90", "Unissex"},
                {"Penteado", "0", "Penteados para eventos especiais", "90", "159.90", "Feminino"},

                // Mãos e Pés (categoria index 1)
                {"Manicure Completa", "1", "Cutilagem, esmaltação e hidratação", "45", "45.00", "Feminino"},
                {"Pedicure Completa", "1", "Cutilagem, esmaltação e esfoliação", "60", "55.00", "Feminino"},
                {"Manicure Express", "1", "Esmaltação rápida", "20", "25.00", "Feminino"},
                {"Unhas em Gel", "1", "Aplicação de gel nas unhas", "90", "89.90", "Feminino"},
                {"Nail Art", "1", "Decoração artística das unhas", "60", "79.90", "Feminino"},
                {"Spa dos Pés", "1", "Tratamento relaxante completo", "90", "119.90", "Unissex"},

                // Estética Facial (categoria index 2)
                {"Limpeza de Pele", "2", "Limpeza profunda e hidratação", "90", "129.90", "Unissex"},
                {"Peeling", "2", "Renovação celular da pele", "60", "159.90", "Unissex"},
                {"Máscara Facial", "2", "Tratamento com máscaras específicas", "45", "89.90", "Unissex"},
                {"Microagulhamento", "2", "Tratamento anti-aging", "90", "299.90", "Unissex"},

                // Sobrancelhas (categoria index 3)
                {"Design de Sobrancelhas", "3", "Design personalizado com pinça", "30", "39.90", "Unissex"},
                {"Henna", "3", "Coloração com henna natural", "45", "49.90", "Unissex"},
                {"Micropigmentação", "3", "Pigmentação semipermanente", "120", "399.90", "Unissex"},

                // Massagem (categoria index 4)
                {"Massagem Relaxante", "4", "Massagem corporal relaxante", "60", "119.90", "Unissex"},
                {"Massagem Modeladora", "4", "Massagem para modelar o corpo", "90", "159.90", "Unissex"},

                // Depilação (categoria index 5)
                {"Depilação Pernas", "5", "Depilação completa das pernas", "45", "79.90", "Feminino"},
                {"Depilação Axilas", "5", "Depilação das axilas", "15", "29.90", "Feminino"},
                {"Depilação Buço", "5", "Depilação do buço", "10", "19.90", "Feminino"},

                // Maquiagem (categoria index 6)
                {"Maquiagem Social", "6", "Make para eventos sociais", "60", "159.90", "Feminino"},
                {"Maquiagem Noiva", "6", "Make especial para noivas", "90", "299.90", "Feminino"},
                {"Automaquiagem", "6", "Aula de automaquiagem", "120", "199.90", "Feminino"},

                // Tratamentos (categoria index 7)
                {"Botox Capilar", "7", "Tratamento intensivo para cabelos danificados", "120", "199.90", "Unissex"},
                {"Cauterização", "7", "Reparação profunda dos fios", "90", "149.90", "Unissex"},

                // Barba (categoria index 8)
                {"Barba Completa", "8", "Corte e modelagem da barba", "30", "39.90", "Masculino"},
                {"Barboterapia", "8", "Tratamento relaxante para barba", "45", "59.90", "Masculino"},

                // Noivas (categoria index 9)
                {"Pacote Noiva", "9", "Serviço completo para noivas", "240", "899.90", "Feminino"},
                {"Teste de Noiva", "9", "Teste de maquiagem e penteado", "120", "299.90", "Feminino"}
        };

        for (String[] data : servicosData) {
            String nome = data[0];
            Categoria categoria = categorias.get(Integer.parseInt(data[1]));

            Servico servico = servicoRepository.findFirstByNomeAndOrganizacao(nome, org).orElseGet(() -> {
                Servico s = new Servico();
                s.setNome(nome);
                s.setCategoria(categoria);
                s.setDescricao(data[2]);
                s.setTempoEstimadoMinutos(Integer.parseInt(data[3]));
                s.setPreco(new BigDecimal(data[4]));
                s.setDesconto(new BigDecimal(0));
                s.setPrecoFinal(new BigDecimal(data[4]));
                s.setGenero(data[5]);
                s.setOrganizacao(org);
                s.setAtivo(ThreadLocalRandom.current().nextDouble() < 0.95); // 95% ativos
                s.setAvaliacao(ThreadLocalRandom.current().nextDouble() < 0.8); // 80% permitem avaliação
                s.setHome(ThreadLocalRandom.current().nextDouble() < 0.3); // 30% aparecem na home
                s.adicionarUrlImagem("https://images.unsplash.com/photo-1562322140-8baeececf3df?w=400");

                // Adicionar alguns produtos para os serviços
                if (ThreadLocalRandom.current().nextDouble() < 0.7) { // 70% dos serviços têm produtos
                    List<String> produtos = new ArrayList<>();
                    produtos.add("Shampoo Premium");
                    produtos.add("Condicionador Reparador");
                    if (categoria.getValue().equals("cabelo")) {
                        produtos.add("Óleo Argan");
                        produtos.add("Leave-in Protetor");
                    }
                    s.setProdutos(produtos);
                }

                return servicoRepository.save(s);
            });

            servicos.add(servico);
        }

        return servicos;
    }

    private List<Produto> criarProdutos(Organizacao org, List<Categoria> categorias) {
        System.out.println("🛍️ Criando produtos...");
        List<Produto> produtosCriados = new ArrayList<>();

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

                    Produto saved = produtoRepository.save(p);
                    produtosCriados.add(saved);
                    return saved;

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
        return produtosCriados;
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

                    Agendamento saveAgen = agendamentoRepository.save(agendamento);

                    Cobranca cob = new Cobranca();

                    cob.setAgendamento(saveAgen);
                    cob.setCliente(cliente);
                    cob.setOrganizacao(org);

                    BigDecimal valorTotal = servicosEscolhidos.stream()
                            .map(Servico::getPreco)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    cob.setValor(valorTotal);
                    cob.setValorPago(BigDecimal.valueOf(0));
                    cob.setValorPendente(valorTotal);
                    cob.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
                    cob.setTipoCobranca(Cobranca.TipoCobranca.AGENDAMENTO);
                    cob.setDtVencimento(saveAgen.getDtAgendamento().toLocalDate());

                    cobrancaRepository.save(cob);

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
        boolean temIntervaloAlmoco = ThreadLocalRandom.current().nextDouble() < 0.7; // 70% têm intervalo de almoço

        // SEGUNDA-FEIRA - Horário comercial
        JornadaDia segunda = new JornadaDia();
        segunda.setFuncionario(funcionario);
        segunda.setDiaSemana(DiaSemana.SEGUNDA);
        segunda.setAtivo(true);

        if (temIntervaloAlmoco) {
            // Com intervalo de almoço (2 horários)
            HorarioTrabalho manha = new HorarioTrabalho(segunda, LocalTime.of(9, 0), LocalTime.of(12, 0));
            HorarioTrabalho tarde = new HorarioTrabalho(segunda, LocalTime.of(13, 0), LocalTime.of(18, 0));
            segunda.addHorario(manha);
            segunda.addHorario(tarde);
        } else {
            // Sem intervalo (1 horário corrido)
            HorarioTrabalho corrido = new HorarioTrabalho(segunda, LocalTime.of(9, 0), LocalTime.of(18, 0));
            segunda.addHorario(corrido);
        }
        funcionario.addJornadaDia(segunda);

        // TERÇA-FEIRA - Similar à segunda
        JornadaDia terca = new JornadaDia();
        terca.setFuncionario(funcionario);
        terca.setDiaSemana(DiaSemana.TERCA);
        terca.setAtivo(true);

        if (temIntervaloAlmoco) {
            terca.addHorario(new HorarioTrabalho(terca, LocalTime.of(9, 0), LocalTime.of(12, 0)));
            terca.addHorario(new HorarioTrabalho(terca, LocalTime.of(13, 0), LocalTime.of(18, 0)));
        } else {
            terca.addHorario(new HorarioTrabalho(terca, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        }
        funcionario.addJornadaDia(terca);

        // QUARTA-FEIRA
        JornadaDia quarta = new JornadaDia();
        quarta.setFuncionario(funcionario);
        quarta.setDiaSemana(DiaSemana.QUARTA);
        quarta.setAtivo(true);

        if (temIntervaloAlmoco) {
            quarta.addHorario(new HorarioTrabalho(quarta, LocalTime.of(9, 0), LocalTime.of(12, 0)));
            quarta.addHorario(new HorarioTrabalho(quarta, LocalTime.of(13, 0), LocalTime.of(18, 0)));
        } else {
            quarta.addHorario(new HorarioTrabalho(quarta, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        }
        funcionario.addJornadaDia(quarta);

        // QUINTA-FEIRA - Horário estendido (até 20h)
        JornadaDia quinta = new JornadaDia();
        quinta.setFuncionario(funcionario);
        quinta.setDiaSemana(DiaSemana.QUINTA);
        quinta.setAtivo(true);

        if (temIntervaloAlmoco) {
            quinta.addHorario(new HorarioTrabalho(quinta, LocalTime.of(10, 0), LocalTime.of(13, 0)));
            quinta.addHorario(new HorarioTrabalho(quinta, LocalTime.of(14, 0), LocalTime.of(20, 0)));
        } else {
            quinta.addHorario(new HorarioTrabalho(quinta, LocalTime.of(10, 0), LocalTime.of(20, 0)));
        }
        funcionario.addJornadaDia(quinta);

        // SEXTA-FEIRA - Horário estendido
        JornadaDia sexta = new JornadaDia();
        sexta.setFuncionario(funcionario);
        sexta.setDiaSemana(DiaSemana.SEXTA);
        sexta.setAtivo(true);

        if (temIntervaloAlmoco) {
            sexta.addHorario(new HorarioTrabalho(sexta, LocalTime.of(10, 0), LocalTime.of(13, 0)));
            sexta.addHorario(new HorarioTrabalho(sexta, LocalTime.of(14, 0), LocalTime.of(20, 0)));
        } else {
            sexta.addHorario(new HorarioTrabalho(sexta, LocalTime.of(10, 0), LocalTime.of(20, 0)));
        }
        funcionario.addJornadaDia(sexta);

        // SÁBADO (80% dos funcionários)
        if (temSabado) {
            JornadaDia sabado = new JornadaDia();
            sabado.setFuncionario(funcionario);
            sabado.setDiaSemana(DiaSemana.SABADO);
            sabado.setAtivo(true);

            // Sábado geralmente é meio período
            sabado.addHorario(new HorarioTrabalho(sabado, LocalTime.of(8, 0), LocalTime.of(16, 0)));

            funcionario.addJornadaDia(sabado);
        }

        // DOMINGO (30% dos funcionários)
        if (temDomingo) {
            JornadaDia domingo = new JornadaDia();
            domingo.setFuncionario(funcionario);
            domingo.setDiaSemana(DiaSemana.DOMINGO);
            domingo.setAtivo(true);

            // Domingo geralmente é período reduzido
            domingo.addHorario(new HorarioTrabalho(domingo, LocalTime.of(9, 0), LocalTime.of(15, 0)));

            funcionario.addJornadaDia(domingo);
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

    // ===========================================================================================
    // ========================= NOVOS MÉTODOS - MÓDULOS EXPANDIDOS ==============================
    // ===========================================================================================

    /**
     * Cria contas bancárias da organização para o módulo financeiro.
     */
    private List<ContaBancaria> criarContasBancarias(Organizacao org) {
        System.out.println("🏦 Criando contas bancárias...");
        List<ContaBancaria> contas = new ArrayList<>();

        Object[][] contasData = {
                {"Conta Principal Bradesco", ContaBancaria.TipoConta.CONTA_CORRENTE, "Bradesco", "1234", "56789-0", 50000.00, 47523.45, true, "#E91E63", "bank"},
                {"Poupança Itaú", ContaBancaria.TipoConta.POUPANCA, "Itaú", "4567", "12345-6", 30000.00, 32150.00, false, "#FF9800", "savings"},
                {"Caixa do Salão", ContaBancaria.TipoConta.CAIXA, null, null, null, 5000.00, 3275.50, false, "#4CAF50", "cash"},
                {"PIX Nubank", ContaBancaria.TipoConta.CARTEIRA_DIGITAL, "Nubank", "0001", "98765-4", 15000.00, 18930.75, false, "#9C27B0", "pix"}
        };

        for (Object[] data : contasData) {
            ContaBancaria conta = new ContaBancaria();
            conta.setOrganizacao(org);
            conta.setNome((String) data[0]);
            conta.setTipoConta((ContaBancaria.TipoConta) data[1]);
            conta.setBanco((String) data[2]);
            conta.setAgencia((String) data[3]);
            conta.setNumeroConta((String) data[4]);
            conta.setSaldoInicial(BigDecimal.valueOf((double) data[5]));
            conta.setSaldoAtual(BigDecimal.valueOf((double) data[6]));
            conta.setPrincipal((boolean) data[7]);
            conta.setAtivo(true);
            conta.setCor((String) data[8]);
            conta.setIcone((String) data[9]);
            conta.setDtCriacao(LocalDateTime.now().minusDays(90));
            contas.add(contaBancariaRepository.save(conta));
        }

        System.out.println("✅ " + contas.size() + " contas bancárias criadas!");
        return contas;
    }

    /**
     * Cria categorias financeiras (receitas e despesas) para o módulo financeiro.
     */
    private List<CategoriaFinanceira> criarCategoriasFinanceiras(Organizacao org) {
        System.out.println("📂 Criando categorias financeiras...");
        List<CategoriaFinanceira> categorias = new ArrayList<>();

        // === CATEGORIAS DE RECEITA ===
        CategoriaFinanceira servicosPrestados = criarCategoriaFinanceira(org, "Serviços Prestados", "Receitas de serviços realizados", CategoriaFinanceira.TipoCategoria.RECEITA, "#4CAF50", "scissors", null);
        categorias.add(servicosPrestados);
        categorias.add(criarCategoriaFinanceira(org, "Cortes e Penteados", "Receitas com cortes e penteados", CategoriaFinanceira.TipoCategoria.RECEITA, "#66BB6A", "cut", servicosPrestados));
        categorias.add(criarCategoriaFinanceira(org, "Tratamentos Capilares", "Receitas com tratamentos", CategoriaFinanceira.TipoCategoria.RECEITA, "#81C784", "spa", servicosPrestados));
        categorias.add(criarCategoriaFinanceira(org, "Estética e Beleza", "Receitas com serviços estéticos", CategoriaFinanceira.TipoCategoria.RECEITA, "#A5D6A7", "star", servicosPrestados));

        CategoriaFinanceira vendaProdutos = criarCategoriaFinanceira(org, "Venda de Produtos", "Receitas com venda de produtos", CategoriaFinanceira.TipoCategoria.RECEITA, "#2196F3", "shopping-bag", null);
        categorias.add(vendaProdutos);

        categorias.add(criarCategoriaFinanceira(org, "Outros Recebimentos", "Outras receitas", CategoriaFinanceira.TipoCategoria.RECEITA, "#00BCD4", "plus-circle", null));

        // === CATEGORIAS DE DESPESA ===
        CategoriaFinanceira folhaPagamento = criarCategoriaFinanceira(org, "Folha de Pagamento", "Gastos com pessoal", CategoriaFinanceira.TipoCategoria.DESPESA, "#F44336", "users", null);
        categorias.add(folhaPagamento);
        categorias.add(criarCategoriaFinanceira(org, "Salários", "Pagamento de salários", CategoriaFinanceira.TipoCategoria.DESPESA, "#EF5350", "dollar-sign", folhaPagamento));
        categorias.add(criarCategoriaFinanceira(org, "Encargos Sociais", "INSS, FGTS e encargos", CategoriaFinanceira.TipoCategoria.DESPESA, "#E57373", "file-text", folhaPagamento));

        categorias.add(criarCategoriaFinanceira(org, "Aluguel e Condomínio", "Despesas com imóvel", CategoriaFinanceira.TipoCategoria.DESPESA, "#FF9800", "home", null));
        categorias.add(criarCategoriaFinanceira(org, "Produtos e Insumos", "Compra de materiais", CategoriaFinanceira.TipoCategoria.DESPESA, "#9C27B0", "package", null));
        categorias.add(criarCategoriaFinanceira(org, "Marketing e Publicidade", "Gastos com marketing", CategoriaFinanceira.TipoCategoria.DESPESA, "#E91E63", "trending-up", null));

        CategoriaFinanceira utilidades = criarCategoriaFinanceira(org, "Utilidades", "Contas de consumo", CategoriaFinanceira.TipoCategoria.DESPESA, "#795548", "zap", null);
        categorias.add(utilidades);
        categorias.add(criarCategoriaFinanceira(org, "Energia Elétrica", "Conta de energia", CategoriaFinanceira.TipoCategoria.DESPESA, "#8D6E63", "zap", utilidades));
        categorias.add(criarCategoriaFinanceira(org, "Água e Esgoto", "Conta de água", CategoriaFinanceira.TipoCategoria.DESPESA, "#A1887F", "droplet", utilidades));
        categorias.add(criarCategoriaFinanceira(org, "Internet e Telefone", "Telecomunicações", CategoriaFinanceira.TipoCategoria.DESPESA, "#BCAAA4", "wifi", utilidades));

        categorias.add(criarCategoriaFinanceira(org, "Manutenção", "Reparos e manutenção", CategoriaFinanceira.TipoCategoria.DESPESA, "#607D8B", "tool", null));
        categorias.add(criarCategoriaFinanceira(org, "Impostos e Taxas", "Tributos e taxas", CategoriaFinanceira.TipoCategoria.DESPESA, "#455A64", "percent", null));

        System.out.println("✅ " + categorias.size() + " categorias financeiras criadas!");
        return categorias;
    }

    private CategoriaFinanceira criarCategoriaFinanceira(Organizacao org, String nome, String descricao,
                                                          CategoriaFinanceira.TipoCategoria tipo, String cor,
                                                          String icone, CategoriaFinanceira categoriaPai) {
        CategoriaFinanceira cat = new CategoriaFinanceira();
        cat.setOrganizacao(org);
        cat.setNome(nome);
        cat.setDescricao(descricao);
        cat.setTipo(tipo);
        cat.setCor(cor);
        cat.setIcone(icone);
        cat.setAtivo(true);
        cat.setCategoriaPai(categoriaPai);
        cat.setDtCriacao(LocalDateTime.now().minusDays(90));
        return categoriaFinanceiraRepository.save(cat);
    }

    /**
     * Cria centros de custo da organização.
     */
    private List<CentroCusto> criarCentrosCusto(Organizacao org) {
        System.out.println("🏢 Criando centros de custo...");
        List<CentroCusto> centros = new ArrayList<>();

        String[][] centrosData = {
                {"Operacional", "CC001", "Custos operacionais do salão"},
                {"Administrativo", "CC002", "Custos administrativos gerais"},
                {"Marketing", "CC003", "Investimentos em marketing e publicidade"},
                {"Financeiro", "CC004", "Custos do setor financeiro"},
                {"Recursos Humanos", "CC005", "Custos com pessoal e treinamento"}
        };

        for (String[] data : centrosData) {
            CentroCusto centro = new CentroCusto();
            centro.setOrganizacao(org);
            centro.setNome(data[0]);
            centro.setCodigo(data[1]);
            centro.setDescricao(data[2]);
            centro.setAtivo(true);
            centro.setDtCriacao(LocalDateTime.now().minusDays(90));
            centros.add(centroCustoRepository.save(centro));
        }

        System.out.println("✅ " + centros.size() + " centros de custo criados!");
        return centros;
    }

    /**
     * Cria contas a pagar com diversos status e cenários.
     */
    private void criarContasPagar(Organizacao org, List<ContaBancaria> contas,
                                   List<CategoriaFinanceira> categorias, List<CentroCusto> centros) {
        System.out.println("💸 Criando contas a pagar...");

        ContaBancaria contaPrincipal = contas.get(0);
        CentroCusto centroOperacional = centros.get(0);
        CentroCusto centroAdmin = centros.get(1);
        CentroCusto centroMarketing = centros.get(2);
        CentroCusto centroRH = centros.get(4);

        // Buscar categorias de despesa
        CategoriaFinanceira catAluguel = categorias.stream().filter(c -> c.getNome().equals("Aluguel e Condomínio")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catProdutos = categorias.stream().filter(c -> c.getNome().equals("Produtos e Insumos")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catMarketing = categorias.stream().filter(c -> c.getNome().equals("Marketing e Publicidade")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catSalarios = categorias.stream().filter(c -> c.getNome().equals("Salários")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catEnergia = categorias.stream().filter(c -> c.getNome().equals("Energia Elétrica")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catAgua = categorias.stream().filter(c -> c.getNome().equals("Água e Esgoto")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catInternet = categorias.stream().filter(c -> c.getNome().equals("Internet e Telefone")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catManutencao = categorias.stream().filter(c -> c.getNome().equals("Manutenção")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catImpostos = categorias.stream().filter(c -> c.getNome().equals("Impostos e Taxas")).findFirst().orElse(categorias.get(0));

        int contador = 0;

        // --- CONTAS PAGAS (passado) ---
        for (int i = 1; i <= 3; i++) {
            ContaPagar cp = criarContaPagarBase(org, contaPrincipal, catAluguel, centroAdmin,
                    "Aluguel do Salão - Mês " + i, "Imobiliária Central",
                    BigDecimal.valueOf(4500.00), LocalDate.now().minusMonths(i));
            cp.setStatus(ContaPagar.StatusContaPagar.PAGA);
            cp.setValorPago(BigDecimal.valueOf(4500.00));
            cp.setDtPagamento(cp.getDtVencimento().minusDays(2));
            cp.setRecorrente(true);
            cp.setPeriodicidade(ContaPagar.Periodicidade.MENSAL);
            cp.setFormaPagamento("TRANSFERENCIA");
            contaPagarRepository.save(cp);
            contador++;
        }

        for (int i = 1; i <= 2; i++) {
            ContaPagar cp = criarContaPagarBase(org, contaPrincipal, catEnergia, centroOperacional,
                    "Conta de Energia - Mês " + i, "CEMIG",
                    BigDecimal.valueOf(850.00 + ThreadLocalRandom.current().nextInt(0, 200)), LocalDate.now().minusMonths(i));
            cp.setStatus(ContaPagar.StatusContaPagar.PAGA);
            cp.setValorPago(cp.getValor());
            cp.setDtPagamento(cp.getDtVencimento());
            cp.setRecorrente(true);
            cp.setPeriodicidade(ContaPagar.Periodicidade.MENSAL);
            cp.setFormaPagamento("PIX");
            contaPagarRepository.save(cp);
            contador++;
        }

        // Fornecedor de produtos - pago
        ContaPagar cpProdutos1 = criarContaPagarBase(org, contaPrincipal, catProdutos, centroOperacional,
                "Compra de Shampoos e Condicionadores", "Beauty Distribuidora LTDA",
                BigDecimal.valueOf(3200.00), LocalDate.now().minusDays(20));
        cpProdutos1.setStatus(ContaPagar.StatusContaPagar.PAGA);
        cpProdutos1.setValorPago(BigDecimal.valueOf(3200.00));
        cpProdutos1.setDtPagamento(LocalDate.now().minusDays(19));
        cpProdutos1.setNumeroNota("NF-2024-001234");
        cpProdutos1.setFormaPagamento("CARTAO_CREDITO");
        contaPagarRepository.save(cpProdutos1);
        contador++;

        ContaPagar cpProdutos2 = criarContaPagarBase(org, contaPrincipal, catProdutos, centroOperacional,
                "Esmaltes e Material de Manicure", "Nail Supply Brasil",
                BigDecimal.valueOf(1850.00), LocalDate.now().minusDays(15));
        cpProdutos2.setStatus(ContaPagar.StatusContaPagar.PAGA);
        cpProdutos2.setValorPago(BigDecimal.valueOf(1850.00));
        cpProdutos2.setDtPagamento(LocalDate.now().minusDays(14));
        cpProdutos2.setFormaPagamento("PIX");
        contaPagarRepository.save(cpProdutos2);
        contador++;

        // Marketing pago
        ContaPagar cpMkt = criarContaPagarBase(org, contaPrincipal, catMarketing, centroMarketing,
                "Campanha Instagram Ads - Janeiro", "Meta Platforms",
                BigDecimal.valueOf(1500.00), LocalDate.now().minusDays(10));
        cpMkt.setStatus(ContaPagar.StatusContaPagar.PAGA);
        cpMkt.setValorPago(BigDecimal.valueOf(1500.00));
        cpMkt.setDtPagamento(LocalDate.now().minusDays(10));
        cpMkt.setFormaPagamento("CARTAO_CREDITO");
        contaPagarRepository.save(cpMkt);
        contador++;

        // Salários pagos
        for (int i = 0; i < 3; i++) {
            ContaPagar cpSalario = criarContaPagarBase(org, contaPrincipal, catSalarios, centroRH,
                    "Salário Funcionário " + (i + 1) + " - Mês Anterior", "Folha de Pagamento",
                    BigDecimal.valueOf(2500.00 + i * 500), LocalDate.now().minusDays(5));
            cpSalario.setStatus(ContaPagar.StatusContaPagar.PAGA);
            cpSalario.setValorPago(cpSalario.getValor());
            cpSalario.setDtPagamento(LocalDate.now().minusDays(5));
            cpSalario.setRecorrente(true);
            cpSalario.setPeriodicidade(ContaPagar.Periodicidade.MENSAL);
            cpSalario.setFormaPagamento("TRANSFERENCIA");
            contaPagarRepository.save(cpSalario);
            contador++;
        }

        // --- CONTAS PENDENTES (futuras) ---
        ContaPagar cpAluguelFuturo = criarContaPagarBase(org, contaPrincipal, catAluguel, centroAdmin,
                "Aluguel do Salão - Mês Atual", "Imobiliária Central",
                BigDecimal.valueOf(4500.00), LocalDate.now().plusDays(10));
        cpAluguelFuturo.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        cpAluguelFuturo.setRecorrente(true);
        cpAluguelFuturo.setPeriodicidade(ContaPagar.Periodicidade.MENSAL);
        contaPagarRepository.save(cpAluguelFuturo);
        contador++;

        ContaPagar cpEnergiaFutura = criarContaPagarBase(org, contaPrincipal, catEnergia, centroOperacional,
                "Conta de Energia - Mês Atual", "CEMIG",
                BigDecimal.valueOf(920.00), LocalDate.now().plusDays(15));
        cpEnergiaFutura.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        cpEnergiaFutura.setFormaPagamento("PIX");
        contaPagarRepository.save(cpEnergiaFutura);
        contador++;

        ContaPagar cpAguaFutura = criarContaPagarBase(org, contaPrincipal, catAgua, centroOperacional,
                "Conta de Água - Mês Atual", "COPASA",
                BigDecimal.valueOf(380.00), LocalDate.now().plusDays(12));
        cpAguaFutura.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        contaPagarRepository.save(cpAguaFutura);
        contador++;

        ContaPagar cpInternetFutura = criarContaPagarBase(org, contaPrincipal, catInternet, centroAdmin,
                "Internet Fibra 500MB", "Vivo Empresas",
                BigDecimal.valueOf(249.90), LocalDate.now().plusDays(8));
        cpInternetFutura.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        cpInternetFutura.setRecorrente(true);
        cpInternetFutura.setPeriodicidade(ContaPagar.Periodicidade.MENSAL);
        contaPagarRepository.save(cpInternetFutura);
        contador++;

        ContaPagar cpProdutosFuturo = criarContaPagarBase(org, contaPrincipal, catProdutos, centroOperacional,
                "Reposição de Tintas e Colorações", "Color Pro Distribuidora",
                BigDecimal.valueOf(4800.00), LocalDate.now().plusDays(20));
        cpProdutosFuturo.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        cpProdutosFuturo.setParcelaAtual(1);
        cpProdutosFuturo.setTotalParcelas(3);
        contaPagarRepository.save(cpProdutosFuturo);
        contador++;

        // --- CONTAS VENCIDAS ---
        ContaPagar cpImpostoVencido = criarContaPagarBase(org, contaPrincipal, catImpostos, centroAdmin,
                "DAS Simples Nacional - Dezembro", "Receita Federal",
                BigDecimal.valueOf(1890.00), LocalDate.now().minusDays(15));
        cpImpostoVencido.setStatus(ContaPagar.StatusContaPagar.VENCIDA);
        cpImpostoVencido.setValorJuros(BigDecimal.valueOf(28.35));
        cpImpostoVencido.setValorMulta(BigDecimal.valueOf(37.80));
        cpImpostoVencido.setObservacoes("Verificar com contador possibilidade de parcelamento");
        contaPagarRepository.save(cpImpostoVencido);
        contador++;

        ContaPagar cpFornecedorVencido = criarContaPagarBase(org, contaPrincipal, catProdutos, centroOperacional,
                "Material de Limpeza - Lote 47", "Higiene Total LTDA",
                BigDecimal.valueOf(650.00), LocalDate.now().minusDays(8));
        cpFornecedorVencido.setStatus(ContaPagar.StatusContaPagar.VENCIDA);
        cpFornecedorVencido.setObservacoes("Fornecedor cobrando juros de mora");
        contaPagarRepository.save(cpFornecedorVencido);
        contador++;

        ContaPagar cpManutencaoVencida = criarContaPagarBase(org, contaPrincipal, catManutencao, centroOperacional,
                "Reparo do Ar Condicionado", "Cool Service Refrigeração",
                BigDecimal.valueOf(1200.00), LocalDate.now().minusDays(5));
        cpManutencaoVencida.setStatus(ContaPagar.StatusContaPagar.VENCIDA);
        contaPagarRepository.save(cpManutencaoVencida);
        contador++;

        // Contas parcialmente pagas
        ContaPagar cpParcelada1 = criarContaPagarBase(org, contaPrincipal, catProdutos, centroOperacional,
                "Equipamento de Secagem Profissional", "Salon Equipment Brasil",
                BigDecimal.valueOf(5600.00), LocalDate.now().minusDays(3));
        cpParcelada1.setStatus(ContaPagar.StatusContaPagar.PARCIALMENTE_PAGA);
        cpParcelada1.setValorPago(BigDecimal.valueOf(2800.00));
        cpParcelada1.setParcelaAtual(1);
        cpParcelada1.setTotalParcelas(2);
        cpParcelada1.setObservacoes("Primeira parcela paga, segunda parcela em 30 dias");
        contaPagarRepository.save(cpParcelada1);
        contador++;

        ContaPagar cpParcelada2 = criarContaPagarBase(org, contaPrincipal, catManutencao, centroOperacional,
                "Reforma do Banheiro Social", "Construtora JR",
                BigDecimal.valueOf(8500.00), LocalDate.now().minusDays(10));
        cpParcelada2.setStatus(ContaPagar.StatusContaPagar.PARCIALMENTE_PAGA);
        cpParcelada2.setValorPago(BigDecimal.valueOf(4250.00));
        cpParcelada2.setParcelaAtual(2);
        cpParcelada2.setTotalParcelas(4);
        contaPagarRepository.save(cpParcelada2);
        contador++;

        // Conta cancelada
        ContaPagar cpCancelada = criarContaPagarBase(org, contaPrincipal, catMarketing, centroMarketing,
                "Panfletos Promocionais - Cancelado", "Gráfica Express",
                BigDecimal.valueOf(800.00), LocalDate.now().plusDays(5));
        cpCancelada.setStatus(ContaPagar.StatusContaPagar.CANCELADA);
        cpCancelada.setObservacoes("Cancelado - optamos por marketing digital");
        contaPagarRepository.save(cpCancelada);
        contador++;

        System.out.println("✅ " + contador + " contas a pagar criadas!");
    }

    private ContaPagar criarContaPagarBase(Organizacao org, ContaBancaria conta, CategoriaFinanceira categoria,
                                            CentroCusto centro, String descricao, String fornecedor,
                                            BigDecimal valor, LocalDate dtVencimento) {
        ContaPagar cp = new ContaPagar();
        cp.setOrganizacao(org);
        cp.setContaBancaria(conta);
        cp.setCategoriaFinanceira(categoria);
        cp.setCentroCusto(centro);
        cp.setDescricao(descricao);
        cp.setFornecedor(fornecedor);
        cp.setValor(valor);
        cp.setValorPago(BigDecimal.ZERO);
        cp.setValorDesconto(BigDecimal.ZERO);
        cp.setValorJuros(BigDecimal.ZERO);
        cp.setValorMulta(BigDecimal.ZERO);
        cp.setDtEmissao(dtVencimento.minusDays(15));
        cp.setDtVencimento(dtVencimento);
        cp.setDtCompetencia(dtVencimento.withDayOfMonth(1));
        cp.setStatus(ContaPagar.StatusContaPagar.PENDENTE);
        cp.setRecorrente(false);
        cp.setDtCriacao(LocalDateTime.now());
        return cp;
    }

    /**
     * Cria contas a receber com diversos status e cenários.
     */
    private void criarContasReceber(Organizacao org, List<Cliente> clientes, List<ContaBancaria> contas,
                                     List<CategoriaFinanceira> categorias, List<CentroCusto> centros) {
        System.out.println("💰 Criando contas a receber...");

        ContaBancaria contaPrincipal = contas.get(0);
        ContaBancaria contaPix = contas.size() > 3 ? contas.get(3) : contas.get(0);
        CentroCusto centroOperacional = centros.get(0);

        CategoriaFinanceira catServicos = categorias.stream().filter(c -> c.getNome().equals("Serviços Prestados")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catVendas = categorias.stream().filter(c -> c.getNome().equals("Venda de Produtos")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catOutros = categorias.stream().filter(c -> c.getNome().equals("Outros Recebimentos")).findFirst().orElse(categorias.get(0));

        int contador = 0;
        String[] servicosDescricao = {
                "Corte e Escova", "Coloração Completa", "Progressiva", "Manicure e Pedicure",
                "Design de Sobrancelhas", "Pacote Noiva", "Massagem Relaxante", "Limpeza de Pele",
                "Barba e Corte", "Hidratação Capilar"
        };
        String[] formaPagamento = {"PIX", "CARTAO_CREDITO", "CARTAO_DEBITO", "DINHEIRO", "TRANSFERENCIA"};

        // --- RECEBIDAS (8 entradas) ---
        for (int i = 0; i < 8; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            String servDesc = servicosDescricao[i % servicosDescricao.length];
            BigDecimal valor = BigDecimal.valueOf(80 + ThreadLocalRandom.current().nextInt(300));
            LocalDate dtVenc = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(1, 45));

            ContaReceber cr = criarContaReceberBase(org, i < 6 ? contaPrincipal : contaPix,
                    i < 6 ? catServicos : catVendas, centroOperacional, cliente,
                    servDesc + " - " + cliente.getNomeCompleto(), valor, dtVenc);
            cr.setStatus(ContaReceber.StatusContaReceber.RECEBIDA);
            cr.setValorRecebido(valor);
            cr.setDtRecebimento(dtVenc);
            cr.setFormaPagamento(formaPagamento[ThreadLocalRandom.current().nextInt(formaPagamento.length)]);
            contaReceberRepository.save(cr);
            contador++;
        }

        // --- PENDENTES (5 entradas) ---
        for (int i = 0; i < 5; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            String servDesc = servicosDescricao[(i + 3) % servicosDescricao.length];
            BigDecimal valor = BigDecimal.valueOf(100 + ThreadLocalRandom.current().nextInt(500));
            LocalDate dtVenc = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 20));

            ContaReceber cr = criarContaReceberBase(org, contaPrincipal, catServicos, centroOperacional,
                    cliente, servDesc + " - " + cliente.getNomeCompleto(), valor, dtVenc);
            cr.setStatus(ContaReceber.StatusContaReceber.PENDENTE);
            contaReceberRepository.save(cr);
            contador++;
        }

        // --- VENCIDAS (3 entradas) ---
        for (int i = 0; i < 3; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            BigDecimal valor = BigDecimal.valueOf(150 + ThreadLocalRandom.current().nextInt(400));
            LocalDate dtVenc = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(5, 30));

            ContaReceber cr = criarContaReceberBase(org, contaPrincipal, catServicos, centroOperacional,
                    cliente, "Serviço pendente - " + cliente.getNomeCompleto(), valor, dtVenc);
            cr.setStatus(ContaReceber.StatusContaReceber.VENCIDA);
            cr.setValorJuros(valor.multiply(BigDecimal.valueOf(0.02)));
            cr.setObservacoes("Cliente não realizou o pagamento. Entrar em contato.");
            contaReceberRepository.save(cr);
            contador++;
        }

        // --- PARCIALMENTE RECEBIDAS (2 entradas) ---
        for (int i = 0; i < 2; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            BigDecimal valor = BigDecimal.valueOf(500 + ThreadLocalRandom.current().nextInt(500));
            LocalDate dtVenc = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(1, 10));
            BigDecimal valorRecebido = valor.divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);

            ContaReceber cr = criarContaReceberBase(org, contaPrincipal, catServicos, centroOperacional,
                    cliente, "Pacote de Serviços - " + cliente.getNomeCompleto(), valor, dtVenc);
            cr.setStatus(ContaReceber.StatusContaReceber.PARCIALMENTE_RECEBIDA);
            cr.setValorRecebido(valorRecebido);
            cr.setDtRecebimento(dtVenc);
            cr.setParcelaAtual(1);
            cr.setTotalParcelas(2);
            cr.setObservacoes("Pagamento parcial realizado. Restante em " + dtVenc.plusDays(30));
            contaReceberRepository.save(cr);
            contador++;
        }

        // --- CANCELADAS (2 entradas) ---
        for (int i = 0; i < 2; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            BigDecimal valor = BigDecimal.valueOf(200 + ThreadLocalRandom.current().nextInt(200));
            LocalDate dtVenc = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(5, 15));

            ContaReceber cr = criarContaReceberBase(org, contaPrincipal, catServicos, centroOperacional,
                    cliente, "Serviço cancelado - " + cliente.getNomeCompleto(), valor, dtVenc);
            cr.setStatus(ContaReceber.StatusContaReceber.CANCELADA);
            cr.setObservacoes("Cliente cancelou o agendamento");
            contaReceberRepository.save(cr);
            contador++;
        }

        System.out.println("✅ " + contador + " contas a receber criadas!");
    }

    private ContaReceber criarContaReceberBase(Organizacao org, ContaBancaria conta, CategoriaFinanceira categoria,
                                                CentroCusto centro, Cliente cliente,
                                                String descricao, BigDecimal valor, LocalDate dtVencimento) {
        ContaReceber cr = new ContaReceber();
        cr.setOrganizacao(org);
        cr.setContaBancaria(conta);
        cr.setCategoriaFinanceira(categoria);
        cr.setCentroCusto(centro);
        cr.setCliente(cliente);
        cr.setDescricao(descricao);
        cr.setValor(valor);
        cr.setValorRecebido(BigDecimal.ZERO);
        cr.setValorDesconto(BigDecimal.ZERO);
        cr.setValorJuros(BigDecimal.ZERO);
        cr.setValorMulta(BigDecimal.ZERO);
        cr.setDtEmissao(dtVencimento.minusDays(5));
        cr.setDtVencimento(dtVencimento);
        cr.setDtCompetencia(dtVencimento.withDayOfMonth(1));
        cr.setStatus(ContaReceber.StatusContaReceber.PENDENTE);
        cr.setRecorrente(false);
        cr.setDtCriacao(LocalDateTime.now());
        return cr;
    }

    /**
     * Cria lançamentos financeiros com diversos tipos e status.
     */
    private void criarLancamentosFinanceiros(Organizacao org, List<ContaBancaria> contas,
                                              List<CategoriaFinanceira> categorias, List<CentroCusto> centros) {
        System.out.println("📒 Criando lançamentos financeiros...");

        ContaBancaria contaPrincipal = contas.get(0);
        ContaBancaria contaPoupanca = contas.size() > 1 ? contas.get(1) : contas.get(0);
        ContaBancaria contaCaixa = contas.size() > 2 ? contas.get(2) : contas.get(0);
        ContaBancaria contaPix = contas.size() > 3 ? contas.get(3) : contas.get(0);
        CentroCusto centroOp = centros.get(0);
        CentroCusto centroAdmin = centros.get(1);

        CategoriaFinanceira catServicos = categorias.stream().filter(c -> c.getNome().equals("Serviços Prestados")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catCortes = categorias.stream().filter(c -> c.getNome().equals("Cortes e Penteados")).findFirst().orElse(catServicos);
        CategoriaFinanceira catTratamentos = categorias.stream().filter(c -> c.getNome().equals("Tratamentos Capilares")).findFirst().orElse(catServicos);
        CategoriaFinanceira catVendas = categorias.stream().filter(c -> c.getNome().equals("Venda de Produtos")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catAluguel = categorias.stream().filter(c -> c.getNome().equals("Aluguel e Condomínio")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catProdutos = categorias.stream().filter(c -> c.getNome().equals("Produtos e Insumos")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catSalarios = categorias.stream().filter(c -> c.getNome().equals("Salários")).findFirst().orElse(categorias.get(0));
        CategoriaFinanceira catEnergia = categorias.stream().filter(c -> c.getNome().equals("Energia Elétrica")).findFirst().orElse(categorias.get(0));

        int contador = 0;
        String[] formasPagamento = {"PIX", "DINHEIRO", "CARTAO_CREDITO", "CARTAO_DEBITO", "TRANSFERENCIA"};

        // === RECEITAS (25 lançamentos nos últimos 3 meses) ===
        for (int i = 0; i < 25; i++) {
            LancamentoFinanceiro lf = new LancamentoFinanceiro();
            lf.setOrganizacao(org);
            lf.setTipo(LancamentoFinanceiro.TipoLancamento.RECEITA);

            LocalDate dtLanc = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(1, 90));
            lf.setDtLancamento(dtLanc);
            lf.setDtCompetencia(dtLanc.withDayOfMonth(1));

            ContaBancaria contaDest;
            String forma = formasPagamento[ThreadLocalRandom.current().nextInt(formasPagamento.length)];
            if (forma.equals("PIX")) {
                contaDest = contaPix;
            } else if (forma.equals("DINHEIRO")) {
                contaDest = contaCaixa;
            } else {
                contaDest = contaPrincipal;
            }
            lf.setContaBancaria(contaDest);
            lf.setFormaPagamento(forma);

            if (i < 10) {
                lf.setCategoriaFinanceira(catCortes);
                lf.setDescricao("Corte e Escova - Atendimento #" + (1000 + i));
                lf.setValor(BigDecimal.valueOf(80 + ThreadLocalRandom.current().nextInt(120)));
            } else if (i < 17) {
                lf.setCategoriaFinanceira(catTratamentos);
                lf.setDescricao("Tratamento Capilar - Atendimento #" + (2000 + i));
                lf.setValor(BigDecimal.valueOf(120 + ThreadLocalRandom.current().nextInt(200)));
            } else if (i < 22) {
                lf.setCategoriaFinanceira(catVendas);
                lf.setDescricao("Venda de Produtos - Pedido #" + (3000 + i));
                lf.setValor(BigDecimal.valueOf(50 + ThreadLocalRandom.current().nextInt(300)));
            } else {
                lf.setCategoriaFinanceira(catServicos);
                lf.setDescricao("Serviço Diverso - Atendimento #" + (4000 + i));
                lf.setValor(BigDecimal.valueOf(100 + ThreadLocalRandom.current().nextInt(400)));
            }

            lf.setCentroCusto(centroOp);
            lf.setStatus(i < 22 ? LancamentoFinanceiro.StatusLancamento.EFETIVADO : LancamentoFinanceiro.StatusLancamento.PENDENTE);
            lf.setDtCriacao(LocalDateTime.now());
            lancamentoFinanceiroRepository.save(lf);
            contador++;
        }

        // === DESPESAS (12 lançamentos) ===
        Object[][] despesasData = {
                {"Aluguel do Salão - Janeiro", 4500.00, catAluguel, centroAdmin, "TRANSFERENCIA", 30},
                {"Aluguel do Salão - Fevereiro", 4500.00, catAluguel, centroAdmin, "TRANSFERENCIA", 60},
                {"Compra de Produtos Capilares", 2800.00, catProdutos, centroOp, "CARTAO_CREDITO", 25},
                {"Compra de Esmaltes e Acessórios", 1200.00, catProdutos, centroOp, "PIX", 18},
                {"Conta de Energia - Janeiro", 890.00, catEnergia, centroOp, "PIX", 35},
                {"Conta de Energia - Fevereiro", 920.00, catEnergia, centroOp, "PIX", 5},
                {"Salário Cabeleireira Ana", 3500.00, catSalarios, centroOp, "TRANSFERENCIA", 5},
                {"Salário Manicure Carla", 2800.00, catSalarios, centroOp, "TRANSFERENCIA", 5},
                {"Salário Barbeiro João", 3200.00, catSalarios, centroOp, "TRANSFERENCIA", 5},
                {"Material de Limpeza", 450.00, catProdutos, centroOp, "DINHEIRO", 12},
                {"Manutenção Ar Condicionado", 850.00, catProdutos, centroOp, "PIX", 20},
                {"Assinatura Software Gestão", 199.90, catProdutos, centroAdmin, "CARTAO_CREDITO", 2}
        };

        for (Object[] data : despesasData) {
            LancamentoFinanceiro lf = new LancamentoFinanceiro();
            lf.setOrganizacao(org);
            lf.setTipo(LancamentoFinanceiro.TipoLancamento.DESPESA);
            lf.setDescricao((String) data[0]);
            lf.setValor(BigDecimal.valueOf((double) data[1]));
            lf.setCategoriaFinanceira((CategoriaFinanceira) data[2]);
            lf.setCentroCusto((CentroCusto) data[3]);
            lf.setFormaPagamento((String) data[4]);
            lf.setDtLancamento(LocalDate.now().minusDays((int) data[5]));
            lf.setDtCompetencia(lf.getDtLancamento().withDayOfMonth(1));
            lf.setContaBancaria(contaPrincipal);
            lf.setStatus(LancamentoFinanceiro.StatusLancamento.EFETIVADO);
            lf.setDtCriacao(LocalDateTime.now());
            lancamentoFinanceiroRepository.save(lf);
            contador++;
        }

        // === TRANSFERÊNCIAS (3 lançamentos) ===
        Object[][] transferencias = {
                {"Transferência para Poupança", 5000.00, contaPrincipal, contaPoupanca, 15},
                {"Retirada de Caixa para Conta", 2000.00, contaCaixa, contaPrincipal, 7},
                {"Transferência PIX para Conta Principal", 3500.00, contaPix, contaPrincipal, 3}
        };

        for (Object[] data : transferencias) {
            LancamentoFinanceiro lf = new LancamentoFinanceiro();
            lf.setOrganizacao(org);
            lf.setTipo(LancamentoFinanceiro.TipoLancamento.TRANSFERENCIA);
            lf.setDescricao((String) data[0]);
            lf.setValor(BigDecimal.valueOf((double) data[1]));
            lf.setContaBancaria((ContaBancaria) data[2]);
            lf.setContaBancariaDestino((ContaBancaria) data[3]);
            lf.setDtLancamento(LocalDate.now().minusDays((int) data[4]));
            lf.setDtCompetencia(lf.getDtLancamento().withDayOfMonth(1));
            lf.setStatus(LancamentoFinanceiro.StatusLancamento.EFETIVADO);
            lf.setFormaPagamento("TRANSFERENCIA");
            lf.setDtCriacao(LocalDateTime.now());
            lancamentoFinanceiroRepository.save(lf);
            contador++;
        }

        System.out.println("✅ " + contador + " lançamentos financeiros criados!");
    }

    /**
     * Cria compras de produtos com diversos status, cobranças e pagamentos.
     */
    private void criarCompras(Organizacao org, List<Cliente> clientes, List<Produto> produtos) {
        System.out.println("🛒 Criando compras de produtos...");

        if (produtos == null || produtos.isEmpty()) {
            System.out.println("⚠️ Nenhum produto disponível para criar compras.");
            return;
        }

        Compra.StatusCompra[] statusDistribuicao = {
                Compra.StatusCompra.CARRINHO, Compra.StatusCompra.CARRINHO,
                Compra.StatusCompra.AGUARDANDO_PAGAMENTO, Compra.StatusCompra.AGUARDANDO_PAGAMENTO,
                Compra.StatusCompra.PAGO, Compra.StatusCompra.PAGO, Compra.StatusCompra.PAGO, Compra.StatusCompra.PAGO,
                Compra.StatusCompra.PROCESSANDO, Compra.StatusCompra.PROCESSANDO,
                Compra.StatusCompra.PRONTO,
                Compra.StatusCompra.ENTREGUE, Compra.StatusCompra.ENTREGUE,
                Compra.StatusCompra.CANCELADA, Compra.StatusCompra.CANCELADA
        };

        Compra.TipoCompra[] tiposCompra = {
                Compra.TipoCompra.BALCAO, Compra.TipoCompra.BALCAO, Compra.TipoCompra.ONLINE,
                Compra.TipoCompra.BALCAO, Compra.TipoCompra.ONLINE, Compra.TipoCompra.BALCAO,
                Compra.TipoCompra.TELEFONE, Compra.TipoCompra.BALCAO, Compra.TipoCompra.ONLINE,
                Compra.TipoCompra.BALCAO, Compra.TipoCompra.BALCAO, Compra.TipoCompra.ONLINE,
                Compra.TipoCompra.BALCAO, Compra.TipoCompra.TELEFONE, Compra.TipoCompra.BALCAO
        };

        Pagamento.FormaPagamento[] formasPag = Pagamento.FormaPagamento.values();

        for (int i = 0; i < statusDistribuicao.length; i++) {
            Cliente cliente = clientes.get(ThreadLocalRandom.current().nextInt(clientes.size()));
            Compra.StatusCompra status = statusDistribuicao[i];

            Compra compra = new Compra();
            compra.setOrganizacao(org);
            compra.setCliente(cliente);
            compra.setStatusCompra(status);
            compra.setTipoCompra(tiposCompra[i]);
            compra.setNumeroPedido("PED-" + String.format("%06d", 1000 + i));
            compra.setDtCriacao(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 60)));

            if (status == Compra.StatusCompra.ENTREGUE) {
                compra.setDtFinalizacao(compra.getDtCriacao().plusDays(ThreadLocalRandom.current().nextInt(1, 5)));
            }

            // Adicionar itens (1-3 produtos aleatórios)
            int numItens = 1 + ThreadLocalRandom.current().nextInt(3);
            List<CompraProduto> itens = new ArrayList<>();
            BigDecimal valorTotal = BigDecimal.ZERO;
            Set<Long> produtosUsados = new HashSet<>();

            for (int j = 0; j < numItens; j++) {
                Produto produto;
                int tentativas = 0;
                do {
                    produto = produtos.get(ThreadLocalRandom.current().nextInt(produtos.size()));
                    tentativas++;
                } while (produtosUsados.contains(produto.getId()) && tentativas < 10);

                if (produtosUsados.contains(produto.getId())) continue;
                produtosUsados.add(produto.getId());

                int quantidade = 1 + ThreadLocalRandom.current().nextInt(3);
                BigDecimal precoUnit = produto.getPreco();
                BigDecimal subtotal = precoUnit.multiply(BigDecimal.valueOf(quantidade));

                CompraProduto item = new CompraProduto();
                item.setCompra(compra);
                item.setProduto(produto);
                item.setQuantidade(quantidade);
                item.setPrecoUnitario(precoUnit);
                item.setSubtotal(subtotal);
                item.setDescontoItem(BigDecimal.ZERO);
                item.setDtAdicionado(compra.getDtCriacao());

                itens.add(item);
                valorTotal = valorTotal.add(subtotal);
            }

            compra.setItens(itens);
            compra.setValorTotal(valorTotal);
            compra.setValorDesconto(BigDecimal.ZERO);
            compra.setValorFinal(valorTotal);

            Compra compraSalva = compraRepository.save(compra);

            // Criar cobrança para compras que não são CARRINHO
            if (status != Compra.StatusCompra.CARRINHO) {
                Cobranca cob = new Cobranca();
                cob.setOrganizacao(org);
                cob.setCliente(cliente);
                cob.setCompra(compraSalva);
                cob.setValor(valorTotal);
                cob.setTipoCobranca(Cobranca.TipoCobranca.COMPRA);
                cob.setDtVencimento(compraSalva.getDtCriacao().toLocalDate().plusDays(3));

                boolean isPaga = status == Compra.StatusCompra.PAGO || status == Compra.StatusCompra.PROCESSANDO
                        || status == Compra.StatusCompra.PRONTO || status == Compra.StatusCompra.ENTREGUE;

                if (isPaga) {
                    cob.setStatusCobranca(Cobranca.StatusCobranca.PAGO);
                    cob.setValorPago(valorTotal);
                    cob.setValorPendente(BigDecimal.ZERO);
                    cob.setDtPagamentoCompleto(compraSalva.getDtCriacao().plusHours(1));
                } else if (status == Compra.StatusCompra.CANCELADA) {
                    cob.setStatusCobranca(Cobranca.StatusCobranca.CANCELADA);
                    cob.setValorPago(BigDecimal.ZERO);
                    cob.setValorPendente(valorTotal);
                } else {
                    cob.setStatusCobranca(Cobranca.StatusCobranca.PENDENTE);
                    cob.setValorPago(BigDecimal.ZERO);
                    cob.setValorPendente(valorTotal);
                }

                Cobranca cobSalva = cobrancaRepository.save(cob);

                // Criar pagamento para compras pagas
                if (isPaga) {
                    Pagamento pag = new Pagamento();
                    pag.setCobranca(cobSalva);
                    pag.setCliente(cliente);
                    pag.setOrganizacao(org);
                    pag.setValor(valorTotal);
                    pag.setDtPagamento(compraSalva.getDtCriacao().plusHours(1));
                    pag.setFormaPagamento(formasPag[ThreadLocalRandom.current().nextInt(formasPag.length)]);
                    pag.setStatusPagamento(Pagamento.StatusPagamento.CONFIRMADO);
                    pag.setNumeroTransacao("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    pag.setDtCriacao(compraSalva.getDtCriacao());
                    pagamentoRepository.save(pag);
                }
            }
        }

        System.out.println("✅ " + statusDistribuicao.length + " compras criadas com cobranças e pagamentos!");
    }

    /**
     * Cria pagamentos para as cobranças de agendamentos, simulando diversos cenários.
     */
    private void criarPagamentosParaCobrancas(Organizacao org) {
        System.out.println("💳 Criando pagamentos para cobranças de agendamentos...");

        List<Cobranca> cobrancas = cobrancaRepository.findAll().stream()
                .filter(c -> c.getOrganizacao() != null && c.getOrganizacao().getId().equals(org.getId()))
                .filter(c -> c.getTipoCobranca() == Cobranca.TipoCobranca.AGENDAMENTO)
                .collect(Collectors.toList());

        if (cobrancas.isEmpty()) {
            System.out.println("⚠️ Nenhuma cobrança de agendamento encontrada.");
            return;
        }

        int pagos = 0, parciais = 0, vencidos = 0, cancelados = 0;
        Pagamento.FormaPagamento[] formasPag = Pagamento.FormaPagamento.values();

        for (int i = 0; i < cobrancas.size(); i++) {
            Cobranca cob = cobrancas.get(i);

            // Verificar se o agendamento tem status que justifica pagamento
            if (cob.getAgendamento() == null) continue;
            Status statusAgendamento = cob.getAgendamento().getStatus();

            if (statusAgendamento == Status.CONCLUIDO || statusAgendamento == Status.PAGO) {
                // Agendamento concluído → cobrança PAGO + pagamento confirmado
                cob.setStatusCobranca(Cobranca.StatusCobranca.PAGO);
                cob.setValorPago(cob.getValor());
                cob.setValorPendente(BigDecimal.ZERO);
                cob.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.INTEGRAL);
                cob.setDtPagamentoCompleto(cob.getAgendamento().getDtAgendamento().plusHours(2));
                cobrancaRepository.save(cob);

                Pagamento pag = new Pagamento();
                pag.setCobranca(cob);
                pag.setCliente(cob.getCliente());
                pag.setOrganizacao(org);
                pag.setValor(cob.getValor());
                pag.setDtPagamento(cob.getAgendamento().getDtAgendamento().plusHours(2));
                pag.setFormaPagamento(formasPag[ThreadLocalRandom.current().nextInt(formasPag.length)]);
                pag.setStatusPagamento(Pagamento.StatusPagamento.CONFIRMADO);
                pag.setNumeroTransacao("AGD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                pag.setDtCriacao(pag.getDtPagamento());
                pagamentoRepository.save(pag);
                pagos++;

            } else if (statusAgendamento == Status.AGENDADO || statusAgendamento == Status.CONFIRMADO) {
                // 50% pagam sinal, 50% ficam pendentes
                if (i % 2 == 0 && cob.getValor().compareTo(BigDecimal.valueOf(50)) > 0) {
                    // Pagar sinal (30%)
                    BigDecimal valorSinal = cob.getValor().multiply(BigDecimal.valueOf(0.30)).setScale(2, BigDecimal.ROUND_HALF_UP);
                    cob.setStatusCobranca(Cobranca.StatusCobranca.PARCIALMENTE_PAGO);
                    cob.setValorPago(valorSinal);
                    cob.setValorPendente(cob.getValor().subtract(valorSinal));
                    cob.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.SINAL);
                    cob.setPercentualSinal(BigDecimal.valueOf(30));
                    cobrancaRepository.save(cob);

                    Pagamento pagSinal = new Pagamento();
                    pagSinal.setCobranca(cob);
                    pagSinal.setCliente(cob.getCliente());
                    pagSinal.setOrganizacao(org);
                    pagSinal.setValor(valorSinal);
                    pagSinal.setDtPagamento(cob.getDtVencimento().atTime(10, 0).minusDays(1));
                    pagSinal.setFormaPagamento(Pagamento.FormaPagamento.PIX);
                    pagSinal.setStatusPagamento(Pagamento.StatusPagamento.CONFIRMADO);
                    pagSinal.setNumeroTransacao("SIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    pagSinal.setObservacoes("Pagamento de sinal - 30% do valor total");
                    pagSinal.setDtCriacao(pagSinal.getDtPagamento());
                    pagamentoRepository.save(pagSinal);
                    parciais++;
                }
                // Demais ficam PENDENTE (já é o default)

            } else if (statusAgendamento == Status.CANCELADO) {
                cob.setStatusCobranca(Cobranca.StatusCobranca.CANCELADA);
                cob.setValorPago(BigDecimal.ZERO);
                cob.setValorPendente(cob.getValor());
                cobrancaRepository.save(cob);
                cancelados++;

            } else if (statusAgendamento == Status.NAO_COMPARECEU) {
                // Cobrança vencida - cliente não pagou e não compareceu
                cob.setStatusCobranca(Cobranca.StatusCobranca.VENCIDA);
                cob.setValorPago(BigDecimal.ZERO);
                cob.setValorPendente(cob.getValor());
                cob.setObservacoes("Cliente não compareceu ao agendamento");
                cobrancaRepository.save(cob);
                vencidos++;

            } else if (statusAgendamento == Status.EM_ANDAMENTO) {
                // Pagamento pendente ou parcial
                cob.setSubtipoCobrancaAgendamento(Cobranca.SubtipoCobrancaAgendamento.INTEGRAL);
                cobrancaRepository.save(cob);
            }
        }

        System.out.println("✅ Pagamentos de agendamentos processados: " + pagos + " pagos, "
                + parciais + " parciais, " + vencidos + " vencidos, " + cancelados + " cancelados");
    }

    /**
     * Cria configurações de notificação da organização.
     */
    private void criarConfigNotificacoesOrganizacao(Organizacao org) {
        System.out.println("🔔 Criando configurações de notificação...");

        Object[][] configs = {
                {TipoNotificacao.CONFIRMACAO, 24, true,
                        "Olá {{cliente_nome}}! Seu agendamento no {{org_nome}} está confirmado para {{data_agendamento}} às {{hora_agendamento}}. Confirma sua presença? Responda SIM ou NÃO."},
                {TipoNotificacao.CONFIRMACAO, 48, true,
                        "Olá {{cliente_nome}}! Lembrando que você tem um agendamento no {{org_nome}} em 2 dias ({{data_agendamento}} às {{hora_agendamento}}). Confirma? Responda SIM, NÃO ou REAGENDAR."},
                {TipoNotificacao.LEMBRETE, 1, true,
                        "{{cliente_nome}}, seu horário no {{org_nome}} é daqui a 1 hora! Te esperamos às {{hora_agendamento}}. Endereço: {{org_endereco}}."},
                {TipoNotificacao.LEMBRETE, 3, true,
                        "Olá {{cliente_nome}}! Lembrando que hoje você tem um agendamento no {{org_nome}} às {{hora_agendamento}}. Nos vemos em breve!"}
        };

        for (Object[] data : configs) {
            org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao config =
                    new org.exemplo.bellory.model.entity.notificacao.ConfigNotificacao();
            config.setOrganizacao(org);
            config.setTipo((TipoNotificacao) data[0]);
            config.setHorasAntes((Integer) data[1]);
            config.setAtivo((Boolean) data[2]);
            config.setMensagemTemplate((String) data[3]);
            config.setDtCriacao(LocalDateTime.now().minusDays(60));
            notificacaoConfigRepository.save(config);
        }

        System.out.println("✅ 4 configurações de notificação criadas!");
    }

    /**
     * Cria questionários completos com perguntas e opções de resposta.
     */
    private void criarQuestionarios(Organizacao org) {
        System.out.println("📝 Criando questionários...");

        // === QUESTIONÁRIO 1: Pesquisa de Satisfação ===
        Questionario q1 = Questionario.builder()
                .organizacao(org)
                .titulo("Pesquisa de Satisfação")
                .descricao("Ajude-nos a melhorar! Avalie sua experiência conosco.")
                .tipo(TipoQuestionario.PESQUISA_SATISFACAO)
                .ativo(true)
                .obrigatorio(false)
                .anonimo(false)
                .corTema("#4CAF50")
                .build();

        Pergunta p1_1 = Pergunta.builder()
                .texto("Como você avalia nosso atendimento?")
                .descricao("Avalie de 1 a 5 estrelas")
                .tipo(TipoPergunta.AVALIACAO_ESTRELAS)
                .obrigatoria(true)
                .ordem(1)
                .escalaMin(1)
                .escalaMax(5)
                .build();
        q1.addPergunta(p1_1);

        Pergunta p1_2 = Pergunta.builder()
                .texto("O que mais gostou no serviço?")
                .descricao("Selecione todas as opções que se aplicam")
                .tipo(TipoPergunta.SELECAO_MULTIPLA)
                .obrigatoria(false)
                .ordem(2)
                .build();
        adicionarOpcoes(p1_2, "Qualidade do serviço", "Atendimento dos profissionais", "Ambiente do salão", "Preço justo", "Pontualidade");
        q1.addPergunta(p1_2);

        Pergunta p1_3 = Pergunta.builder()
                .texto("Você recomendaria nosso salão para amigos e familiares?")
                .tipo(TipoPergunta.SIM_NAO)
                .obrigatoria(true)
                .ordem(3)
                .build();
        q1.addPergunta(p1_3);

        Pergunta p1_4 = Pergunta.builder()
                .texto("Em uma escala de 0 a 10, qual a probabilidade de você nos recomendar?")
                .descricao("NPS - Net Promoter Score")
                .tipo(TipoPergunta.ESCALA)
                .obrigatoria(true)
                .ordem(4)
                .escalaMin(0)
                .escalaMax(10)
                .labelMin("Nada provável")
                .labelMax("Muito provável")
                .build();
        q1.addPergunta(p1_4);

        Pergunta p1_5 = Pergunta.builder()
                .texto("Deixe sua sugestão ou comentário")
                .descricao("Sua opinião é muito importante para nós")
                .tipo(TipoPergunta.TEXTO_LONGO)
                .obrigatoria(false)
                .ordem(5)
                .maxCaracteres(1000)
                .build();
        q1.addPergunta(p1_5);

        questionarioRepository.save(q1);

        // === QUESTIONÁRIO 2: Feedback de Atendimento ===
        Questionario q2 = Questionario.builder()
                .organizacao(org)
                .titulo("Feedback de Atendimento")
                .descricao("Conte-nos como foi seu atendimento hoje.")
                .tipo(TipoQuestionario.FEEDBACK_ATENDIMENTO)
                .ativo(true)
                .obrigatorio(false)
                .anonimo(false)
                .corTema("#2196F3")
                .build();

        Pergunta p2_1 = Pergunta.builder()
                .texto("Qual serviço você realizou?")
                .tipo(TipoPergunta.SELECAO_UNICA)
                .obrigatoria(true)
                .ordem(1)
                .build();
        adicionarOpcoes(p2_1, "Corte", "Coloração", "Tratamento Capilar", "Manicure/Pedicure", "Barba", "Estética Facial", "Massagem", "Outro");
        q2.addPergunta(p2_1);

        Pergunta p2_2 = Pergunta.builder()
                .texto("Como avalia o profissional que te atendeu?")
                .descricao("Avalie a qualidade técnica e o atendimento")
                .tipo(TipoPergunta.AVALIACAO_ESTRELAS)
                .obrigatoria(true)
                .ordem(2)
                .escalaMin(1)
                .escalaMax(5)
                .build();
        q2.addPergunta(p2_2);

        Pergunta p2_3 = Pergunta.builder()
                .texto("O tempo de espera foi adequado?")
                .tipo(TipoPergunta.SIM_NAO)
                .obrigatoria(false)
                .ordem(3)
                .build();
        q2.addPergunta(p2_3);

        Pergunta p2_4 = Pergunta.builder()
                .texto("O resultado ficou como esperava?")
                .tipo(TipoPergunta.SELECAO_UNICA)
                .obrigatoria(true)
                .ordem(4)
                .build();
        adicionarOpcoes(p2_4, "Superou minhas expectativas", "Ficou como eu esperava", "Ficou abaixo do esperado");
        q2.addPergunta(p2_4);

        Pergunta p2_5 = Pergunta.builder()
                .texto("Comentários adicionais sobre o atendimento")
                .tipo(TipoPergunta.TEXTO_LONGO)
                .obrigatoria(false)
                .ordem(5)
                .maxCaracteres(500)
                .build();
        q2.addPergunta(p2_5);

        questionarioRepository.save(q2);

        // === QUESTIONÁRIO 3: Avaliação de Desempenho ===
        Questionario q3 = Questionario.builder()
                .organizacao(org)
                .titulo("Avaliação de Desempenho do Colaborador")
                .descricao("Avaliação periódica de desempenho dos colaboradores.")
                .tipo(TipoQuestionario.AVALIACAO_DESEMPENHO)
                .ativo(true)
                .obrigatorio(true)
                .anonimo(false)
                .corTema("#FF9800")
                .build();

        Pergunta p3_1 = Pergunta.builder()
                .texto("Pontualidade do colaborador")
                .descricao("Avalie a pontualidade e cumprimento de horários")
                .tipo(TipoPergunta.ESCALA)
                .obrigatoria(true)
                .ordem(1)
                .escalaMin(1)
                .escalaMax(5)
                .labelMin("Muito ruim")
                .labelMax("Excelente")
                .build();
        q3.addPergunta(p3_1);

        Pergunta p3_2 = Pergunta.builder()
                .texto("Qualidade técnica do trabalho")
                .descricao("Habilidade técnica nos serviços realizados")
                .tipo(TipoPergunta.ESCALA)
                .obrigatoria(true)
                .ordem(2)
                .escalaMin(1)
                .escalaMax(5)
                .labelMin("Muito ruim")
                .labelMax("Excelente")
                .build();
        q3.addPergunta(p3_2);

        Pergunta p3_3 = Pergunta.builder()
                .texto("Relacionamento com clientes")
                .descricao("Capacidade de atender bem e se comunicar com clientes")
                .tipo(TipoPergunta.ESCALA)
                .obrigatoria(true)
                .ordem(3)
                .escalaMin(1)
                .escalaMax(5)
                .labelMin("Muito ruim")
                .labelMax("Excelente")
                .build();
        q3.addPergunta(p3_3);

        Pergunta p3_4 = Pergunta.builder()
                .texto("Trabalho em equipe")
                .descricao("Colaboração com colegas e contribuição ao ambiente")
                .tipo(TipoPergunta.ESCALA)
                .obrigatoria(true)
                .ordem(4)
                .escalaMin(1)
                .escalaMax(5)
                .labelMin("Muito ruim")
                .labelMax("Excelente")
                .build();
        q3.addPergunta(p3_4);

        Pergunta p3_5 = Pergunta.builder()
                .texto("Áreas de melhoria e observações")
                .descricao("Descreva pontos que o colaborador pode melhorar")
                .tipo(TipoPergunta.TEXTO_LONGO)
                .obrigatoria(false)
                .ordem(5)
                .maxCaracteres(1000)
                .build();
        q3.addPergunta(p3_5);

        questionarioRepository.save(q3);

        System.out.println("✅ 3 questionários criados com perguntas e opções!");
    }

    /**
     * Método auxiliar para adicionar opções de resposta a uma pergunta.
     */
    private void adicionarOpcoes(Pergunta pergunta, String... textos) {
        for (int i = 0; i < textos.length; i++) {
            OpcaoResposta opcao = OpcaoResposta.builder()
                    .texto(textos[i])
                    .valor(String.valueOf(i + 1))
                    .ordem(i + 1)
                    .build();
            pergunta.addOpcao(opcao);
        }
    }
}
