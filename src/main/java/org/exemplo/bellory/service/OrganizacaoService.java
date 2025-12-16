package org.exemplo.bellory.service;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.UpdateOrganizacaoDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.mapper.OrganizacaoMapper;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.organizacao.PlanoRepository;
import org.exemplo.bellory.model.repository.users.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizacaoService {


    OrganizacaoRepository organizacaoRepository;
    OrganizacaoMapper organizacaoMapper;
    PasswordEncoder passwordEncoder;
    PlanoRepository planoRepository;
    AdminRepository adminRepository;


    public OrganizacaoService(OrganizacaoRepository organizacaoRepository, OrganizacaoMapper organizacaoMapper, PasswordEncoder passwordEncoder, PlanoRepository planoRepository, AdminRepository adminRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.organizacaoMapper = organizacaoMapper;
        this.passwordEncoder = passwordEncoder;
        this.planoRepository = planoRepository;
        this.adminRepository = adminRepository;
    }

    public Organizacao getOrganizacaoPadrao() {
        return null;
    }
    public boolean existsByCnpj(String cnpj) {
        return organizacaoRepository.existsByCnpj(cnpj);
    }
    /**
     * Cria uma nova organização
     */
    public OrganizacaoResponseDTO create(CreateOrganizacaoDTO createDTO) {


        // Valida se já existe organização com o mesmo CNPJ
        if (organizacaoRepository.existsByCnpj(createDTO.getCnpj())) {
            throw new IllegalArgumentException("Já existe uma organização cadastrada com este CNPJ");
        }

        // Converte DTO para Entity
        Organizacao organizacao = organizacaoMapper.toEntity(createDTO);

        List<Plano> plano = planoRepository.findAll();

        organizacao.setPlano(plano.get(0));

        // Salva a organização
        Organizacao savedOrganizacao = organizacaoRepository.save(organizacao);

        // Criptografa a senha do administrador
        if (createDTO.getAcessoAdm() != null && createDTO.getAcessoAdm().getSenha() != null) {

            Admin admin = new Admin();

            admin.setOrganizacao(savedOrganizacao);
            admin.setEmail(createDTO.getEmail());
            admin.setNomeCompleto(createDTO.getRazaoSocial());
            admin.setUsername(createDTO.getAcessoAdm().getLogin());
            admin.setPassword(passwordEncoder.encode(createDTO.getAcessoAdm().getSenha()));

            Admin saveAdmin = adminRepository.save(admin);

//            organizacao.setAcessoAdm(saveAdmin.getId());
        }

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
