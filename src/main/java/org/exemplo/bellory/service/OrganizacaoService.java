package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.UpdateOrganizacaoDTO;
import org.exemplo.bellory.model.entity.email.EmailTemplate;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.mapper.OrganizacaoMapper;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.exemplo.bellory.util.CNPJUtil;
import org.exemplo.bellory.util.SlugUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    AdminRepository adminRepository;
    private EmailService emailService;
    private static final int MAX_TENTATIVAS_SLUG = 10;

    @Value("${app.url}")
    private String appUrl;

    public OrganizacaoService(OrganizacaoRepository organizacaoRepository, OrganizacaoMapper organizacaoMapper, PasswordEncoder passwordEncoder, PlanoRepository planoRepository, AdminRepository adminRepository, EmailService emailService) {
        this.organizacaoRepository = organizacaoRepository;
        this.organizacaoMapper = organizacaoMapper;
        this.passwordEncoder = passwordEncoder;
        this.planoRepository = planoRepository;
        this.adminRepository = adminRepository;
        this.emailService = emailService;
    }

    public Organizacao getOrganizacaoPadrao() {
        return null;
    }
    public boolean existsByCnpj(String cnpj) {
        return organizacaoRepository.existsByCnpj(cnpj);
    }

    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
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

            if (adminRepository.existsByUsername(createDTO.getAcessoAdm().getLogin())) {
                throw new IllegalArgumentException("Username '" + createDTO.getAcessoAdm().getLogin() +
                        "' já está em uso por outro administrador");
            }

            // Valida email
            if (createDTO.getEmail() == null || createDTO.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email do administrador é obrigatório");
            }

            if (adminRepository.existsByEmail(createDTO.getEmail())) {
                throw new IllegalArgumentException("Email '" + createDTO.getEmail() +
                        "' já está em uso por outro administrador");
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
        List<Plano> planos = planoRepository.findAll();
        if (planos.isEmpty()) {
            throw new IllegalStateException("Nenhum plano disponível no sistema");
        }
        organizacao.setPlano(planos.get(0));

        // Salva a organização
        Organizacao savedOrganizacao = organizacaoRepository.save(organizacao);

        // Cria o administrador
        Admin admin = new Admin();
        admin.setOrganizacao(savedOrganizacao);
        admin.setEmail(createDTO.getEmail());
        admin.setNomeCompleto(createDTO.getRazaoSocial());
        admin.setUsername(createDTO.getAcessoAdm().getLogin());
        admin.setPassword(passwordEncoder.encode(createDTO.getAcessoAdm().getSenha()));
        admin.setDtCriacao(LocalDateTime.now());

        adminRepository.save(admin);

        enviarEmailBoasVindas(savedOrganizacao, admin);

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

    private void enviarEmailBoasVindas(Organizacao organizacao, Admin admin) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nomeOrganizacao", organizacao.getNomeFantasia());
            variables.put("razaoSocial", organizacao.getRazaoSocial());
            variables.put("cnpj", CNPJUtil.formatarCNPJ(organizacao.getCnpj()));
            variables.put("slug", organizacao.getSlug());
            variables.put("email", organizacao.getEmailPrincipal());
            variables.put("username", admin.getUsername());
            variables.put("emailAdmin", admin.getEmail());
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
