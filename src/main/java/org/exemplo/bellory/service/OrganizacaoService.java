package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.instancia.InstanceCreateDTO;
import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.UpdateOrganizacaoDTO;
import org.exemplo.bellory.model.entity.config.*;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.funcionario.Cargo;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.mapper.OrganizacaoMapper;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoBelloryRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.util.CNPJUtil;
import org.exemplo.bellory.util.SlugUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrganizacaoService {


    OrganizacaoRepository organizacaoRepository;
    OrganizacaoMapper organizacaoMapper;
    PasswordEncoder passwordEncoder;
    PlanoRepository planoRepository;
    PlanoBelloryRepository planoBelloryRepository;
    AdminRepository adminRepository;
    FuncionarioRepository funcionarioRepository;
    CargoRepository cargoRepository;
    private EmailService emailService;
    private FileStorageService fileStorageService;
    private InstanceService instanceService;
    private static final int MAX_TENTATIVAS_SLUG = 10;

    @Value("${app.url}")
    private String appUrl;

    public OrganizacaoService(OrganizacaoRepository organizacaoRepository, OrganizacaoMapper organizacaoMapper, PasswordEncoder passwordEncoder, PlanoRepository planoRepository, AdminRepository adminRepository, EmailService emailService, PlanoBelloryRepository planoBelloryRepository, FileStorageService fileStorageService, FuncionarioRepository funcionarioRepository, CargoRepository cargoRepository, InstanceService instanceService) {
        this.organizacaoRepository = organizacaoRepository;
        this.organizacaoMapper = organizacaoMapper;
        this.passwordEncoder = passwordEncoder;
        this.planoRepository = planoRepository;
        this.adminRepository = adminRepository;
        this.emailService = emailService;
        this.planoBelloryRepository = planoBelloryRepository;
        this.fileStorageService = fileStorageService;
        this.funcionarioRepository = funcionarioRepository;
        this.cargoRepository = cargoRepository;
        this.instanceService = instanceService;

    }

    public Organizacao getOrganizacaoPadrao() {
        return null;
    }
    public boolean existsByCnpj(String cnpj) {
        return organizacaoRepository.existsByCnpj(cnpj);
    }

    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsername(username) || funcionarioRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email) || funcionarioRepository.existsByEmail(email);
    }

    public boolean existsBySlug(String slug) {
        return organizacaoRepository.existsBySlug(slug);
    }


    /**
     * Cria uma nova organização
     */
    public OrganizacaoResponseDTO create(CreateOrganizacaoDTO createDTO) {

        // Normaliza e valida CNPJ
        String cnpjLimpo = CNPJUtil.removerFormatacao(createDTO.getCnpj());

        if (!CNPJUtil.validarCNPJ(cnpjLimpo)) {
            throw new IllegalArgumentException("CNPJ inválido");
        }

        // Valida se já existe organização com o mesmo CNPJ
        if (organizacaoRepository.existsByCnpj(cnpjLimpo)) {
            throw new IllegalArgumentException("Já existe uma organização cadastrada com este CNPJ");
        }

        // Valida dados do administrador antes de criar a organização
        if (createDTO.getAcessoAdm() != null) {

            // Valida username
            if (createDTO.getAcessoAdm().getLogin() == null ||
                    createDTO.getAcessoAdm().getLogin().isBlank()) {
                throw new IllegalArgumentException("Username do administrador é obrigatório");
            }

            if (adminRepository.existsByUsername(createDTO.getAcessoAdm().getLogin()) ||
                    funcionarioRepository.existsByUsername(createDTO.getAcessoAdm().getLogin())) {
                throw new IllegalArgumentException("Username '" + createDTO.getAcessoAdm().getLogin() +
                        "' já está em uso");
            }

            // Valida email
            if (createDTO.getEmail() == null || createDTO.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email do administrador é obrigatório");
            }

            if (adminRepository.existsByEmail(createDTO.getEmail()) ||
                    funcionarioRepository.existsByEmail(createDTO.getEmail())) {
                throw new IllegalArgumentException("Email '" + createDTO.getEmail() +
                        "' já está em uso");
            }

            // Valida senha
            if (createDTO.getAcessoAdm().getSenha() == null ||
                    createDTO.getAcessoAdm().getSenha().length() < 6) {
                throw new IllegalArgumentException("Senha do administrador deve ter no mínimo 6 caracteres");
            }
        } else {
            throw new IllegalArgumentException("Dados de acesso do administrador são obrigatórios");
        }

        // Converte DTO para Entity
        Organizacao organizacao = organizacaoMapper.toEntity(createDTO);
        organizacao.setCnpj(cnpjLimpo); // Salva CNPJ sem formatação

        String slug = gerarSlugUnico(createDTO.getNomeFantasia());
        organizacao.setSlug(slug);

        // Busca e atribui plano
        PlanoBellory planos = planoBelloryRepository.findByCodigo(createDTO.getPlano().getId()).get();
        if (!planos.isAtivo() ) {
            throw new IllegalStateException("Nenhum plano disponível no sistema");
        }
        organizacao.setPlano(planos);

        ConfigAgendamento configAgendamento = new ConfigAgendamento();
        ConfigServico configServico = new ConfigServico();
        ConfigColaborador configColaborador= new ConfigColaborador();
        ConfigNotificacao configNotificacao = new ConfigNotificacao();

        ConfigCliente configCliente = new ConfigCliente();

        ConfigSistema configSistema = new ConfigSistema();
        configSistema.setOrganizacao(organizacao);
        configSistema.setConfigAgendamento(configAgendamento);
        configSistema.setConfigServico(configServico);
        configSistema.setConfigColaborador(configColaborador);
        configSistema.setConfigNotificacao(configNotificacao);
        configSistema.setConfigCliente(configCliente);

        organizacao.setConfigSistema(configSistema);

        // Salva a organização
        Organizacao savedOrganizacao = organizacaoRepository.save(organizacao);

        // Cria o cargo "Administrador" na organização
        Cargo cargoAdmin = new Cargo();
        cargoAdmin.setOrganizacao(savedOrganizacao);
        cargoAdmin.setNome("Administrador");
        cargoAdmin.setDescricao("Administrador da organização");
        cargoAdmin.setAtivo(true);
        cargoAdmin.setDataCriacao(LocalDateTime.now());
        cargoRepository.save(cargoAdmin);

        // Cria o funcionário dono da organização com ROLE_ADMIN
        Funcionario funcionario = new Funcionario();
        funcionario.setOrganizacao(savedOrganizacao);
        funcionario.setEmail(createDTO.getEmail());
        funcionario.setNomeCompleto(createDTO.getRazaoSocial());
        funcionario.setUsername(createDTO.getAcessoAdm().getLogin());
        funcionario.setPassword(passwordEncoder.encode(createDTO.getAcessoAdm().getSenha()));
        funcionario.setRole("ROLE_ADMIN");
        funcionario.setCargo(cargoAdmin);
        funcionario.setDataCriacao(LocalDateTime.now());
        funcionario.setDataContratacao(LocalDateTime.now());
        funcionario.setSituacao("Ativo");
        funcionario.setVisivelExterno(false);
        funcionario.setPrimeiroAcesso(true);
        funcionarioRepository.save(funcionario);

        // Cria o admin master de suporte (credenciais fixas por organização)
        Admin adminSuporte = new Admin();
        adminSuporte.setOrganizacao(savedOrganizacao);
        adminSuporte.setEmail("suporte@bellory.com.br");
        adminSuporte.setNomeCompleto("Suporte Bellory");
        adminSuporte.setUsername("bellory_suporte");
        adminSuporte.setPassword(passwordEncoder.encode("B3ll0ry@Sup2026!"));
        adminSuporte.setDtCriacao(LocalDateTime.now());
        adminRepository.save(adminSuporte);

        InstanceCreateDTO instance = new InstanceCreateDTO();
        instance.setInstanceName(savedOrganizacao.getSlug());
        instance.setInstanceNumber(savedOrganizacao.getTelefone1().replaceAll("\\D", ""));
        instance.setWebhookUrl("https://auto.bellory.com.br/webhook/whatsapp");
        instanceService.createInstance(instance, true, savedOrganizacao.getId());

        enviarEmailBoasVindas(savedOrganizacao, funcionario);

        return organizacaoMapper.toResponseDTO(savedOrganizacao);
    }

    /**
     * Lista todas as organizações ativas
     * Valida o token JWT antes de retornar os dados
     */
//    @Transactional(readOnly = true)
//    public List<OrganizacaoResponseDTO> findAll() {
//
//
//        // Valida o token JWT
////        Long organizacaoId = getOrganizacaoIdFromContext();
//
//
//        List<Organizacao> organizacoes = organizacaoRepository.findAllByAtivoTrue();
//
//        return organizacaoMapper.toResponseDTOList(organizacoes);
//    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponseDTO> findAll() {
        // Use o novo método com FETCH
        List<Organizacao> organizacoes = organizacaoRepository.findAllByAtivoTrueWithDetails();
        return organizacaoMapper.toResponseDTOList(organizacoes);
    }

    /**
     * Busca uma organização por ID
     * Valida o token JWT e verifica permissão de acesso
     */
    @Transactional(readOnly = true)
    public OrganizacaoResponseDTO findById(long id) {

        // Valida o token JWT
        Long organizacaoIdFromToken = getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada com ID: " + id));

        if (!organizacao.getId().equals(organizacaoIdFromToken)) {
            throw new IllegalArgumentException("Você não tem permissão para acessar esta organização");
        }

        return organizacaoMapper.toResponseDTO(organizacao);
    }

    /**
     * Atualiza uma organização
     * Valida o token JWT e atualiza apenas as propriedades fornecidas
     */
    public OrganizacaoResponseDTO update(long id, UpdateOrganizacaoDTO updateDTO) {


        // Valida o token JWT
        Long organizacaoIdFromToken = getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada com ID: " + id));

        // Verifica se o usuário tem permissão para atualizar esta organização
        if (!organizacao.getId().equals(organizacaoIdFromToken)) {
            throw new IllegalArgumentException("Você não tem permissão para atualizar esta organização");
        }

        // Valida se o CNPJ está sendo alterado e se já existe outro com o mesmo CNPJ
        if (updateDTO.getCnpj() != null && !updateDTO.getCnpj().equals(organizacao.getCnpj())) {
            if (organizacaoRepository.existsByCnpjAndIdNot(updateDTO.getCnpj(), id)) {
                throw new IllegalArgumentException("Já existe uma organização cadastrada com este CNPJ");
            }
        }

        // Criptografa a senha se estiver sendo alterada
        if (updateDTO.getAcessoAdm() != null && updateDTO.getAcessoAdm().getSenha() != null) {
            var acessoAdm = updateDTO.getAcessoAdm();
            acessoAdm.setSenha(passwordEncoder.encode(acessoAdm.getSenha()));
            updateDTO.setAcessoAdm(acessoAdm);
        }

        // Atualiza apenas os campos fornecidos
//        organizacaoMapper.updateEntityFromDTO(updateDTO, organizacao);

        // Salva as alterações
        Organizacao updatedOrganizacao = organizacaoRepository.save(organizacao);

        return organizacaoMapper.toResponseDTO(updatedOrganizacao);
    }

    /**
     * Remove (desativa) uma organização
     * Soft delete - apenas marca como inativa
     */
    public void delete(long id) {

        // Valida o token JWT
        Long organizacaoIdFromToken = getOrganizacaoIdFromContext();


        Organizacao organizacao = organizacaoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada com ID: " + id));

        // Verifica se o usuário tem permissão para desativar esta organização
        if (!organizacao.getId().equals(organizacaoIdFromToken)) {
            throw new IllegalArgumentException("Você não tem permissão para desativar esta organização");
        }

        // Soft delete - apenas marca como inativa
        organizacao.setAtivo(false);
        organizacaoRepository.save(organizacao);

    }

    /**
     * Busca organização por CNPJ
     */
    @Transactional(readOnly = true)
    public OrganizacaoResponseDTO findByCnpj(String cnpj) {

        // Valida o token JWT
        getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findByCnpjAndAtivoTrue(cnpj)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada com CNPJ: " + cnpj));

        return organizacaoMapper.toResponseDTO(organizacao);
    }

    private String gerarSlugUnico(String nomeFantasia) {
        String slug;
        int tentativas = 0;

        do {
            slug = SlugUtil.gerarSlug(nomeFantasia);
            tentativas++;

            if (tentativas > MAX_TENTATIVAS_SLUG) {
                throw new IllegalStateException(
                        "Não foi possível gerar um slug único após " + MAX_TENTATIVAS_SLUG + " tentativas"
                );
            }

        } while (organizacaoRepository.existsBySlug(slug));

        return slug;
    }

    private void enviarEmailBoasVindas(Organizacao organizacao, Funcionario funcionario) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nomeOrganizacao", organizacao.getNomeFantasia());
            variables.put("razaoSocial", organizacao.getRazaoSocial());
            variables.put("cnpj", CNPJUtil.formatarCNPJ(organizacao.getCnpj()));
            variables.put("slug", organizacao.getSlug());
            variables.put("email", organizacao.getEmailPrincipal());
            variables.put("username", funcionario.getUsername());
            variables.put("emailAdmin", funcionario.getEmail());
            variables.put("urlSistema", appUrl);

            emailService.enviarEmailComTemplate(
                    List.of(organizacao.getEmailPrincipal()),
                    EmailTemplate.BEM_VINDO_ORGANIZACAO,
                    variables
            );


        } catch (Exception e) {
            // Não falha a criação da organização se o e-mail não for enviado
//            log.error("Erro ao enviar e-mail de boas-vindas para: {}", organizacao.getEmail(), e);
        }
    }

    // ==================== LOGO ====================

    @Transactional
    public Map<String, String> uploadLogo(MultipartFile file) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        // Deletar logo antiga se existir
        if (organizacao.getLogoUrl() != null) {
            String oldRelativePath = fileStorageService.getRelativePathFromUrl(organizacao.getLogoUrl());
            fileStorageService.deleteFile(oldRelativePath, organizacaoId);
        }

        String relativePath = fileStorageService.storeFile(file, organizacaoId, organizacaoId, FileStorageService.TipoUpload.LOGO_ORGANIZACAO);
        String fullUrl = fileStorageService.getFileUrl(relativePath);

        organizacao.setLogoUrl(fullUrl);
        organizacaoRepository.save(organizacao);

        Map<String, String> response = new HashMap<>();
        response.put("filename", relativePath.substring(relativePath.lastIndexOf("/") + 1));
        response.put("url", fullUrl);
        response.put("relativePath", relativePath);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, String> getLogo() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        if (organizacao.getLogoUrl() == null) {
            throw new IllegalArgumentException("Organização não possui logo cadastrada.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("url", organizacao.getLogoUrl());
        response.put("filename", organizacao.getLogoUrl().substring(organizacao.getLogoUrl().lastIndexOf("/") + 1));
        return response;
    }

    @Transactional
    public void deleteLogo() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        if (organizacao.getLogoUrl() == null) {
            throw new IllegalArgumentException("Organização não possui logo para remover.");
        }

        String relativePath = fileStorageService.getRelativePathFromUrl(organizacao.getLogoUrl());
        fileStorageService.deleteFile(relativePath, organizacaoId);

        organizacao.setLogoUrl(null);
        organizacaoRepository.save(organizacao);
    }

    // ==================== BANNER ====================

    @Transactional
    public Map<String, String> uploadBanner(MultipartFile file) {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        // Deletar banner antigo se existir
        if (organizacao.getBannerUrl() != null) {
            String oldRelativePath = fileStorageService.getRelativePathFromUrl(organizacao.getBannerUrl());
            fileStorageService.deleteFile(oldRelativePath, organizacaoId);
        }

        String relativePath = fileStorageService.storeFile(file, organizacaoId, organizacaoId, FileStorageService.TipoUpload.BANNER_ORGANIZACAO);
        String fullUrl = fileStorageService.getFileUrl(relativePath);

        organizacao.setBannerUrl(fullUrl);
        organizacaoRepository.save(organizacao);

        Map<String, String> response = new HashMap<>();
        response.put("filename", relativePath.substring(relativePath.lastIndexOf("/") + 1));
        response.put("url", fullUrl);
        response.put("relativePath", relativePath);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, String> getBanner() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        if (organizacao.getBannerUrl() == null) {
            throw new IllegalArgumentException("Organização não possui banner cadastrado.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("url", organizacao.getBannerUrl());
        response.put("filename", organizacao.getBannerUrl().substring(organizacao.getBannerUrl().lastIndexOf("/") + 1));
        return response;
    }

    @Transactional
    public void deleteBanner() {
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada."));

        if (organizacao.getBannerUrl() == null) {
            throw new IllegalArgumentException("Organização não possui banner para remover.");
        }

        String relativePath = fileStorageService.getRelativePathFromUrl(organizacao.getBannerUrl());
        fileStorageService.deleteFile(relativePath, organizacaoId);

        organizacao.setBannerUrl(null);
        organizacaoRepository.save(organizacao);
    }

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }


    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }
}
