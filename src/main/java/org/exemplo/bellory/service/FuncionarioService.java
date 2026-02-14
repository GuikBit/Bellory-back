package org.exemplo.bellory.service;

import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.funcionario.Cargo;
import org.exemplo.bellory.model.entity.users.RoleEnum;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.exemplo.bellory.util.HorarioValidator;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final CargoRepository cargoRepository;
    private final ServicoRepository servicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final JornadaTrabalhoService jornadaTrabalhoService;
    private final HorarioValidator horarioValidator;

    public FuncionarioService(
            FuncionarioRepository funcionarioRepository,
            OrganizacaoRepository organizacaoRepository,
            CargoRepository cargoRepository,
            ServicoRepository servicoRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService,
            JornadaTrabalhoService jornadaTrabalhoService,
            HorarioValidator horarioValidator) {
        this.funcionarioRepository = funcionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.cargoRepository = cargoRepository;
        this.servicoRepository = servicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.jornadaTrabalhoService = jornadaTrabalhoService;
        this.horarioValidator = horarioValidator;
    }

    @Transactional
    public Funcionario postNewFuncionario(FuncionarioCreateDTO dto) {
        // === VALIDAÇÃO DE SEGURANÇA ===
        Long organizacaoId = getOrganizacaoIdFromContext();
        if (!organizacaoId.equals(dto.getIdOrganizacao())) {
            throw new SecurityException("Acesso negado: Você não tem permissão para criar funcionário nesta organização");
        }

        // === VALIDAÇÕES OBRIGATÓRIAS ===
        validarCamposObrigatorios(dto);

        // === VALIDAÇÃO DE PLANO DE HORÁRIOS ===
        if (dto.getPlanoHorarios() != null && !dto.getPlanoHorarios().isEmpty()) {
            String erroHorario = horarioValidator.validarPlanoHorarios(dto.getPlanoHorarios());
            if (erroHorario != null) {
                throw new IllegalArgumentException("Erro no plano de horários: " + erroHorario);
            }
        }

        // === VALIDAÇÃO DE UNICIDADE ===
        validarUnicidade(dto);

        // === BUSCA DE ENTIDADES RELACIONADAS ===
        Organizacao org = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        Cargo cargo = cargoRepository.findById(dto.getCargo().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cargo '" + dto.getCargo().getNome() + "' não encontrado"));

        // === CRIAÇÃO DO FUNCIONÁRIO ===
        Funcionario novoFuncionario = criarFuncionario(dto, org, cargo);

        // === PROCESSAR FOTO DE PERFIL (BASE64) ===
        if (dto.getFotoPerfil() != null && dto.getFotoPerfil().startsWith("data:image/")) {
            try {
                // Salva funcionário primeiro para ter o ID
                Funcionario funcionarioSalvo = funcionarioRepository.save(novoFuncionario);

                String relativePath = fileStorageService.storeProfilePictureFromBase64(
                        dto.getFotoPerfil(),
                        funcionarioSalvo.getId(),
                        organizacaoId
                );

                String fullUrl = fileStorageService.getFileUrl(relativePath);
                funcionarioSalvo.setFotoPerfil(fullUrl);

                novoFuncionario = funcionarioSalvo;
            } catch (Exception e) {
                System.err.println("❌ Erro ao salvar foto de perfil: " + e.getMessage());
                // Continua sem a foto
            }
        }

        // === SALVAR FUNCIONÁRIO ===
        Funcionario funcionarioFinal = funcionarioRepository.save(novoFuncionario);

        // === PROCESSAR PLANO DE HORÁRIOS ===
        if (dto.getPlanoHorarios() != null && !dto.getPlanoHorarios().isEmpty()) {
            for (JornadaDiaDTO jornadaDTO : dto.getPlanoHorarios()) {
                if (jornadaDTO.getAtivo() && jornadaDTO.getHorarios() != null && !jornadaDTO.getHorarios().isEmpty()) {
                    try {
                        jornadaTrabalhoService.criarOuAtualizarJornada(funcionarioFinal.getId(), jornadaDTO);
                    } catch (Exception e) {
                        System.err.println("⚠️ Erro ao salvar jornada do dia " + jornadaDTO.getDiaSemana() + ": " + e.getMessage());
                    }
                }
            }
        }

        return funcionarioFinal;
    }
    @Transactional
    public Funcionario updateFuncionario(Long id, FuncionarioUpdateDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        Cargo cargo = cargoRepository.findById(funcionario.getCargo().getId()).orElseThrow(()-> new IllegalArgumentException("Cargo nao encontrado."));
        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        // Atualiza apenas os campos fornecidos
        if (dto.getNomeCompleto() != null && !dto.getNomeCompleto().trim().isEmpty()) {
            funcionario.setNomeCompleto(dto.getNomeCompleto());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            funcionario.setEmail(dto.getEmail());
        }
        if (dto.getTelefone() != null) {
            funcionario.setTelefone(dto.getTelefone());
        }
        if (dto.getDataNasc() != null) {
            funcionario.setDataNasc(dto.getDataNasc());
        }
        if (dto.getCargo() != null && !dto.getCargo().trim().isEmpty()) {
            funcionario.setCargo(cargo);
        }
        if (dto.getSalario() != null) {
            funcionario.setSalario(dto.getSalario());
        }

        funcionario.setComissao(dto.isComissao());
        funcionario.setDataUpdate(LocalDateTime.now());

        return funcionarioRepository.save(funcionario);
    }

    @Transactional
    public void deleteFuncionario(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado para exclusão."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    @Transactional
    public FuncionarioDTO getFuncionarioById(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        // ATUALIZADO: Converte JornadaDia para JornadaDiaDTO (novo modelo)
        List<JornadaDiaDTO> jornadasDTO = funcionario.getJornadasDia().stream()
                .map(jornadaDia -> {
                    // Converte os horários do dia
                    List<HorarioTrabalhoDTO> horariosDTO = jornadaDia.getHorarios().stream()
                            .map(h -> new HorarioTrabalhoDTO(h.getId(), h.getHoraInicio(), h.getHoraFim()))
                            .collect(Collectors.toList());

                    return new JornadaDiaDTO(
                            jornadaDia.getId(),
                            jornadaDia.getDiaSemana().getDescricao(),
                            jornadaDia.getAtivo(),
                            horariosDTO
                    );
                })
                .collect(Collectors.toList());

        List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
                .filter(bloqueio ->
                        // Filtro para incluir SOMENTE os bloqueios cujo TipoBloqueio seja DIFERENTE de "AGENDAMENTO"
                        !bloqueio.getTipoBloqueio().name().equals("AGENDAMENTO")
                )
                .map(bloqueio -> new BloqueioAgendaDTO(
                        bloqueio.getInicioBloqueio(),
                        bloqueio.getFimBloqueio(),
                        bloqueio.getDescricao(),
                        bloqueio.getTipoBloqueio().name()
                ))
                .collect(Collectors.toList());
        List<Servico> servicos = funcionario.getServicos();

        return new FuncionarioDTO(funcionario, bloqueiosDTO, jornadasDTO, servicos);
    }

    @Transactional
    public List<FuncionarioDTO> getListAllFuncionarios() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        List<Funcionario> funcionarios = funcionarioRepository.findAllByOrganizacao_Id(organizacaoId);

        return funcionarios.stream()
                .map(funcionario -> {
                    // ATUALIZADO: Converte JornadaDia para JornadaDiaDTO (novo modelo)
                    List<JornadaDiaDTO> jornadasDTO = funcionario.getJornadasDia().stream()
                            .map(jornadaDia -> {
                                // Converte os horários do dia
                                List<HorarioTrabalhoDTO> horariosDTO = jornadaDia.getHorarios().stream()
                                        .map(h -> new HorarioTrabalhoDTO(h.getId(), h.getHoraInicio(), h.getHoraFim()))
                                        .collect(Collectors.toList());

                                return new JornadaDiaDTO(
                                        jornadaDia.getId(),
                                        jornadaDia.getDiaSemana().getDescricao(),
                                        jornadaDia.getAtivo(),
                                        horariosDTO
                                );
                            })
                            .collect(Collectors.toList());

//                    List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
//                            .map(bloqueio -> new BloqueioAgendaDTO(
//                                    bloqueio.getInicioBloqueio(),
//                                    bloqueio.getFimBloqueio(),
//                                    bloqueio.getDescricao(),
//                                    bloqueio.getTipoBloqueio().name()
//                            ))
//                            .collect(Collectors.toList());

                    List<Servico> servicos = funcionario.getServicos();

                    return new FuncionarioDTO(funcionario, null, jornadasDTO, servicos);
                })
                .collect(Collectors.toList());
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return funcionarioRepository.findAllProjectedByOrganizacao_Id(organizacaoId);
    }

    public boolean existeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username não pode ser nulo ou vazio.");
        }

        Long organizacaoId = getOrganizacaoIdFromContext();

        return funcionarioRepository.findByUsernameAndOrganizacao_Id(username.trim(), organizacaoId).isPresent();
    }

    @Transactional
    public Map<String, String> uploadFotoPerfil(Long id, MultipartFile file) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Verificar se funcionário existe e pertence à organização
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        // Deletar foto antiga se existir
        if (funcionario.getFotoPerfil() != null) {
            String oldRelativePath = fileStorageService.getRelativePathFromUrl(funcionario.getFotoPerfil());
            fileStorageService.deleteFile(oldRelativePath, organizacaoId);
        }

        // Salvar nova foto e obter path relativo
        String relativePath = fileStorageService.storeProfilePicture(file, id, organizacaoId);

        // Construir URL completa
        String fullUrl = fileStorageService.getFileUrl(relativePath);

        // Salvar URL completa no banco
        funcionario.setFotoPerfil(fullUrl);
        funcionario.setDataUpdate(LocalDateTime.now());
        funcionarioRepository.save(funcionario);

        Map<String, String> response = new HashMap<>();
        response.put("filename", relativePath.substring(relativePath.lastIndexOf("/") + 1));
        response.put("url", fullUrl); // URL completa para acessar a imagem
        response.put("relativePath", relativePath);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> downloadFotoPerfil(Long id) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        if (funcionario.getFotoPerfil() == null) {
            throw new IllegalArgumentException("Funcionário não possui foto de perfil.");
        }

        // Agora o banco já tem a URL completa
        // Esse endpoint pode redirecionar ou retornar a URL
        Map<String, Object> response = new HashMap<>();
        response.put("url", funcionario.getFotoPerfil()); // URL completa
        response.put("filename", funcionario.getFotoPerfil().substring(funcionario.getFotoPerfil().lastIndexOf("/") + 1));

        return response;
    }

    @Transactional
    public void deleteFotoPerfil(Long id) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário com ID " + id + " não encontrado."));

        // Validar organização
        validarOrganizacao(funcionario.getOrganizacao().getId());

        if (funcionario.getFotoPerfil() == null) {
            throw new IllegalArgumentException("Funcionário não possui foto de perfil para remover.");
        }

        // Extrair path relativo da URL completa
        String relativePath = fileStorageService.getRelativePathFromUrl(funcionario.getFotoPerfil());

        // Deletar arquivo físico
        fileStorageService.deleteFile(relativePath, organizacaoId);

        // Atualizar registro no banco
        funcionario.setFotoPerfil(null);
        funcionario.setDataUpdate(LocalDateTime.now());
        funcionarioRepository.save(funcionario);
    }

    private Funcionario criarFuncionario(FuncionarioCreateDTO dto, Organizacao org, Cargo cargo) {
        Funcionario funcionario = new Funcionario();
        funcionario.setOrganizacao(org);
        funcionario.setUsername(dto.getUsername());
        funcionario.setNomeCompleto(dto.getNomeCompleto());
        funcionario.setEmail(dto.getEmail());
        funcionario.setPassword(passwordEncoder.encode(dto.getPassword()));
        funcionario.setCargo(cargo);
        funcionario.setNivel(dto.getNivel());
        funcionario.setVisivelExterno(dto.isVisibleExterno());
        funcionario.setAtivo(true);
        funcionario.setRole(RoleEnum.FUNCIONARIO.getDescricao());
        funcionario.setDataContratacao(LocalDateTime.now());
        funcionario.setDataCriacao(LocalDateTime.now());
        funcionario.setPrimeiroAcesso(true);

        // Campos opcionais
        if (dto.getTelefone() != null && !dto.getTelefone().trim().isEmpty()) {
            funcionario.setTelefone(dto.getTelefone());
        }

        if (dto.getCpf() != null && !dto.getCpf().trim().isEmpty()) {
            String cpfLimpo = dto.getCpf().replaceAll("[^0-9]", "");
            funcionario.setCpf(cpfLimpo);
        }

        // Foto de perfil (URL ou será processada depois se for base64)
        if (dto.getFotoPerfil() != null && !dto.getFotoPerfil().startsWith("data:image/")) {
            funcionario.setFotoPerfil(dto.getFotoPerfil());
        }

        // Serviços
        if (dto.getServicosId() != null && !dto.getServicosId().isEmpty()) {
            List<Servico> servicos = servicoRepository.findAllById(dto.getServicosId());

            if (servicos.size() != dto.getServicosId().size()) {
                throw new IllegalArgumentException("Um ou mais serviços informados não foram encontrados");
            }

            funcionario.setServicos(servicos);
        }

        return funcionario;
    }

    private void validarCamposObrigatorios(FuncionarioCreateDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("O login é obrigatório");
        }
        if (dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome completo é obrigatório");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("A senha é obrigatória");
        }
        if (dto.getCargo() == null || dto.getCargo().getId() == null) {
            throw new IllegalArgumentException("O cargo é obrigatório");
        }
        if (dto.getNivel() == null) {
            throw new IllegalArgumentException("O nível é obrigatório");
        }
        if (dto.getRole() == null || dto.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("O perfil de acesso é obrigatório");
        }

        // Validação de e-mail básica
        if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("E-mail inválido");
        }
    }

    private void validarUnicidade(FuncionarioCreateDTO dto) {
        Long organizacaoId = getOrganizacaoIdFromContext();

        funcionarioRepository.findByUsernameAndOrganizacao_Id(dto.getUsername(), organizacaoId).ifPresent(f -> {
            throw new IllegalArgumentException("O login '" + dto.getUsername() + "' já está em uso");
        });

        if (dto.getCpf() != null && !dto.getCpf().trim().isEmpty()) {
            String cpfLimpo = dto.getCpf().replaceAll("[^0-9]", "");

            if (cpfLimpo.length() != 11) {
                throw new IllegalArgumentException("CPF inválido. Deve conter 11 dígitos");
            }

            funcionarioRepository.findByCpfAndOrganizacao_Id(cpfLimpo, organizacaoId).ifPresent(f -> {
                throw new IllegalArgumentException("O CPF '" + dto.getCpf() + "' já está cadastrado");
            });
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
