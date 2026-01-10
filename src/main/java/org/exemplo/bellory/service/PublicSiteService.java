package org.exemplo.bellory.service;


import org.exemplo.bellory.model.dto.tenent.*;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.HorarioTrabalho;
import org.exemplo.bellory.model.entity.funcionario.JornadaDia;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;

import org.exemplo.bellory.model.repository.produtos.ProdutoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsável por agregar todos os dados públicos da organização.
 * Este service é usado para popular a landing page pública do estabelecimento.
 *
 * Não requer autenticação - dados são públicos.
 */
@Service
@Transactional(readOnly = true)
public class PublicSiteService {

    private final OrganizacaoRepository organizacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ServicoRepository servicoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public PublicSiteService(
            OrganizacaoRepository organizacaoRepository,
            FuncionarioRepository funcionarioRepository,
            ServicoRepository servicoRepository,
            CategoriaRepository categoriaRepository,
            ProdutoRepository produtoRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.servicoRepository = servicoRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    /**
     * Busca todos os dados públicos da organização pelo slug.
     *
     * @param slug Identificador único da organização na URL
     * @return PublicSiteResponseDTO com todos os dados ou empty se não encontrada
     */
    public Optional<PublicSiteResponseDTO> getPublicSiteBySlug(String slug) {
        // 1. Buscar organização pelo slug
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);

        if (orgOpt.isEmpty()) {
            return Optional.empty();
        }

        Organizacao org = orgOpt.get();
        Long orgId = org.getId();

        // 2. Buscar dados relacionados
        List<Funcionario> funcionarios = funcionarioRepository
                .findAllByOrganizacao_IdAndAtivoTrueAndIsVisivelExternoTrue(orgId);

        List<Servico> servicos = servicoRepository
                .findAllByOrganizacao_IdAndAtivoTrueOrderByNomeAsc(orgId);

        List<Categoria> categorias = categoriaRepository
                .findByOrganizacao_IdAndAtivoTrue(orgId);

        List<Produto> produtosDestaque = produtoRepository
                .findByOrganizacao_IdAndAtivoTrueAndDestaqueTrue(orgId);

        // 3. Montar resposta
        PublicSiteResponseDTO response = PublicSiteResponseDTO.builder()
                .organizacao(convertOrganizacao(org))
                .siteConfig(buildSiteConfig(org))
                .equipe(convertFuncionarios(funcionarios))
                .servicos(convertServicos(servicos))
                .categorias(convertCategorias(categorias, servicos))
                .produtosDestaque(convertProdutos(produtosDestaque))
                .horariosFuncionamento(buildHorariosFuncionamento(funcionarios))
                .redesSociais(buildRedesSociais(org))
                .seo(buildSeoMetadata(org))
                .features(buildFeatures(org))
                .build();

        return Optional.of(response);
    }

    /**
     * Verifica se um slug existe e está ativo.
     */
    public boolean existsBySlug(String slug) {
        return organizacaoRepository.existsBySlugAndAtivoTrue(slug);
    }

    /**
     * Retorna apenas informações básicas (útil para pré-carregamento).
     */
    public Optional<OrganizacaoPublicDTO> getBasicInfoBySlug(String slug) {
        return organizacaoRepository.findBySlugAndAtivoTrue(slug)
                .map(this::convertOrganizacao);
    }

    // ==================== MÉTODOS DE CONVERSÃO ====================

    private OrganizacaoPublicDTO convertOrganizacao(Organizacao org) {
        OrganizacaoPublicDTO.OrganizacaoPublicDTOBuilder builder = OrganizacaoPublicDTO.builder()
                .id(org.getId())
                .nome(org.getNomeFantasia())
                .nomeFantasia(org.getNomeFantasia())
                .slug(org.getSlug())
                .email(org.getEmailPrincipal());

        // Converter endereço se existir
        if (org.getEnderecoPrincipal() != null) {
            builder.endereco(convertEndereco(org.getEnderecoPrincipal()));
        }

        return builder.build();
    }

    private EnderecoPublicDTO convertEndereco(Endereco endereco) {
        return EnderecoPublicDTO.builder()
                .logradouro(endereco.getLogradouro())
                .numero(endereco.getNumero())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .cidade(endereco.getCidade())
                .uf(endereco.getUf())
                .cep(endereco.getCep())
                .build();
    }

    private SiteConfigDTO buildSiteConfig(Organizacao org) {
        // Configurações padrão - pode ser expandido com entidade ConfigSite
        return SiteConfigDTO.builder()
                .tema(org.getTema())
//                .primaryColor("#6366f1")
//                .secondaryColor("#4f46e5")
//                .showPrices(true)
//                .allowOnlineBooking(true)
//                .showTeam(true)
//                .showProducts(org.getConfigSistema() != null && org.getConfigSistema().isUsaEcommerce())
//                .showReviews(true)
                .build();
    }

    private List<FuncionarioPublicDTO> convertFuncionarios(List<Funcionario> funcionarios) {
        return funcionarios.stream()
                .map(this::convertFuncionario)
                .collect(Collectors.toList());
    }

    private FuncionarioPublicDTO convertFuncionario(Funcionario func) {
        List<Long> servicoIds = new ArrayList<>();
        List<String> servicoNomes = new ArrayList<>();

        if (func.getServicos() != null) {
            for (Servico s : func.getServicos()) {
                servicoIds.add(s.getId());
                servicoNomes.add(s.getNome());
            }
        }

        return FuncionarioPublicDTO.builder()
                .id(func.getId())
                .nome(func.getNomeCompleto())
                .apelido(func.getApelido())
                .foto(func.getFotoPerfil())
                .cargo(func.getCargo().getNome())
                .servicoIds(servicoIds)
                .servicoNomes(servicoNomes)
                .horarios(convertJornadasDia(func.getJornadasDia()))
                .build();
    }

    private List<JornadaDiaPublicDTO> convertJornadasDia(List<JornadaDia> jornadas) {
        if (jornadas == null) return Collections.emptyList();

        return jornadas.stream()
                .filter(j -> j.getAtivo() != null && j.getAtivo())
                .map(this::convertJornadaDia)
                .collect(Collectors.toList());
    }

    private JornadaDiaPublicDTO convertJornadaDia(JornadaDia jornada) {
        return JornadaDiaPublicDTO.builder()
                .diaSemana(jornada.getDiaSemana().name())
                .diaSemanaLabel(getDiaSemanaLabel(jornada.getDiaSemana().name()))
                .ativo(jornada.getAtivo())
                .horarios(convertHorariosTrabalho(jornada.getHorarios()))
                .build();
    }

    private List<HorarioTrabalhoPublicDTO> convertHorariosTrabalho(List<HorarioTrabalho> horarios) {
        if (horarios == null) return Collections.emptyList();

        return horarios.stream()
                .map(h -> HorarioTrabalhoPublicDTO.builder()
                        .inicio(h.getHoraInicio().toString())
                        .fim(h.getHoraFim().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ServicoPublicDTO> convertServicos(List<Servico> servicos) {
        return servicos.stream()
                .map(this::convertServico)
                .collect(Collectors.toList());
    }

    private ServicoPublicDTO convertServico(Servico servico) {
        BigDecimal precoComDesconto = null;
        if (servico.getDesconto() != null) {
            BigDecimal desconto = servico.getPreco()
                    .multiply(servico.getDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = servico.getPreco().subtract(desconto);
        }

        List<Long> funcionarioIds = servico.getFuncionarios() != null
                ? servico.getFuncionarios().stream()
                .filter(Funcionario::isAtivo)
                .filter(Funcionario::isVisivelExterno)
                .map(Funcionario::getId)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return ServicoPublicDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .categoriaId(servico.getCategoria().getId())
                .categoriaNome(servico.getCategoria().getLabel())
                .genero(servico.getGenero())
                .tempoEstimadoMinutos(servico.getTempoEstimadoMinutos())
                .preco(servico.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(servico.getDesconto())
                .imagens(servico.getUrlsImagens())
                .disponivel(servico.isAtivo())
                .funcionarioIds(funcionarioIds)
                .build();
    }

    private List<CategoriaPublicDTO> convertCategorias(List<Categoria> categorias, List<Servico> servicos) {
        // Contar serviços por categoria
        Map<Long, Long> servicosPorCategoria = servicos.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCategoria().getId(),
                        Collectors.counting()
                ));

        return categorias.stream()
                .map(cat -> CategoriaPublicDTO.builder()
                        .id(cat.getId())
                        .label(cat.getLabel())
                        .value(cat.getValue())
                        .quantidadeServicos(servicosPorCategoria.getOrDefault(cat.getId(), 0L).intValue())
                        .build())
                .filter(cat -> cat.getQuantidadeServicos() > 0) // Só retorna categorias com serviços
                .collect(Collectors.toList());
    }

    private List<ProdutoPublicDTO> convertProdutos(List<Produto> produtos) {
        return produtos.stream()
                .map(this::convertProduto)
                .collect(Collectors.toList());
    }

    private ProdutoPublicDTO convertProduto(Produto produto) {
        BigDecimal precoComDesconto = null;
        if (produto.getDescontoPercentual() != null && produto.getDescontoPercentual() > 0) {
            BigDecimal desconto = produto.getPreco()
                    .multiply(BigDecimal.valueOf(produto.getDescontoPercentual()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            precoComDesconto = produto.getPreco().subtract(desconto);
        }

        return ProdutoPublicDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .precoComDesconto(precoComDesconto)
                .descontoPercentual(produto.getDescontoPercentual())
                .imagens(produto.getUrlsImagens())
                .categoria(produto.getCategoria() != null ? produto.getCategoria().getLabel() : null)
                .emEstoque(produto.getQuantidadeEstoque() != null && produto.getQuantidadeEstoque() > 0)
                .avaliacao(produto.getAvaliacao())
                .totalAvaliacoes(produto.getTotalAvaliacoes())
                .build();
    }

    private List<HorarioFuncionamentoDTO> buildHorariosFuncionamento(List<Funcionario> funcionarios) {
        // Agregar horários de todos os funcionários para determinar horário de funcionamento
        Map<String, HorarioFuncionamentoDTO> horariosPorDia = new LinkedHashMap<>();

        // Inicializar todos os dias da semana
        String[] diasSemana = {"SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"};
        for (String dia : diasSemana) {
            horariosPorDia.put(dia, HorarioFuncionamentoDTO.builder()
                    .diaSemana(dia)
                    .diaSemanaLabel(getDiaSemanaLabel(dia))
                    .aberto(false)
                    .build());
        }

        // Processar horários dos funcionários
        for (Funcionario func : funcionarios) {
            if (func.getJornadasDia() == null) continue;

            for (JornadaDia jornada : func.getJornadasDia()) {
                if (jornada.getAtivo() == null || !jornada.getAtivo()) continue;
                if (jornada.getHorarios() == null || jornada.getHorarios().isEmpty()) continue;

                String dia = jornada.getDiaSemana().name();
                HorarioFuncionamentoDTO horarioAtual = horariosPorDia.get(dia);

                // Encontrar horário mais cedo e mais tarde
                for (HorarioTrabalho ht : jornada.getHorarios()) {
                    String inicio = ht.getHoraInicio().toString();
                    String fim = ht.getHoraFim().toString();

                    if (!horarioAtual.getAberto()) {
                        horarioAtual.setAberto(true);
                        horarioAtual.setHoraAbertura(inicio);
                        horarioAtual.setHoraFechamento(fim);
                    } else {
                        // Atualizar se necessário
                        if (inicio.compareTo(horarioAtual.getHoraAbertura()) < 0) {
                            horarioAtual.setHoraAbertura(inicio);
                        }
                        if (fim.compareTo(horarioAtual.getHoraFechamento()) > 0) {
                            horarioAtual.setHoraFechamento(fim);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(horariosPorDia.values());
    }

    private RedesSociaisDTO buildRedesSociais(Organizacao org) {
        // Placeholder - pode ser expandido com entidade ConfigRedesSociais
        return RedesSociaisDTO.builder().build();
    }

    private SeoMetadataDTO buildSeoMetadata(Organizacao org) {
        String nomeDisplay = org.getNomeFantasia() != null ? org.getNomeFantasia() : org.getRazaoSocial();

        return SeoMetadataDTO.builder()
                .title(nomeDisplay + " | Agende seu horário")
                .description("Agende seu horário com " + nomeDisplay + ". Serviços de qualidade com profissionais qualificados.")
                .keywords(nomeDisplay + ", agendamento, beleza, serviços")
                .ogTitle(nomeDisplay)
                .ogDescription("Conheça nossos serviços e agende online!")
                .canonicalUrl("https://app.bellory.com.br/" + org.getSlug())
                .build();
    }

    private FeaturesDTO buildFeatures(Organizacao org) {
        ConfigSistema config = org.getConfigSistema();

        return FeaturesDTO.builder()
                .agendamentoOnline(true)
                .ecommerce(config != null && config.isUsaEcommerce())
                .planosClientes(config != null && config.isUsaPlanosParaClientes())
                .avaliacoes(true)
                .chat(false)
                .notificacoesPush(config != null && config.isDisparaNotificacoesPush())
                .build();
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    private String getDiaSemanaLabel(String dia) {
        return switch (dia) {
            case "SEGUNDA" -> "Segunda-feira";
            case "TERCA" -> "Terça-feira";
            case "QUARTA" -> "Quarta-feira";
            case "QUINTA" -> "Quinta-feira";
            case "SEXTA" -> "Sexta-feira";
            case "SABADO" -> "Sábado";
            case "DOMINGO" -> "Domingo";
            default -> dia;
        };
    }
}
