package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.BloqueioAgendaDTO;
import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.dto.FuncionarioDTO; // Importar o DTO
import org.exemplo.bellory.model.dto.JornadaTrabalhoDTO;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.JornadaTrabalho;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors; // Importar

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, OrganizacaoRepository organizacaoRepository, PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Funcionario postNewFuncionario(FuncionarioDTO funcionario) {
        // --- VALIDAÇÕES ESSENCIAIS ---
        if (funcionario.getIdOrganizacao() == null ) {
            throw new IllegalArgumentException("A organização é obrigatória.");
        }
        if (funcionario.getUsername() == null || funcionario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome de usuário (login) é obrigatório.");
        }
        if (funcionario.getNomeCompleto() == null || funcionario.getNomeCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome completo é obrigatório.");
        }
        if (funcionario.getEmail() == null || funcionario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        if (funcionario.getPassword() == null || funcionario.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        if (funcionario.getCargo() == null || funcionario.getCargo().trim().isEmpty()){
            throw new IllegalArgumentException("O cargo é obrigatório.");
        }

        // --- VERIFICAÇÃO DE UNICIDADE ---
        funcionarioRepository.findByUsername(funcionario.getUsername()).ifPresent(f -> {
            throw new IllegalArgumentException("O nome de usuário '" + funcionario.getUsername() + "' já está em uso.");
        });

        // --- LÓGICA DE NEGÓCIO ---
        // Garante que a senha seja criptografada antes de salvar
        funcionario.setPassword(passwordEncoder.encode(funcionario.getPassword()));

        // Garante que o funcionário seja salvo como ativo por padrão
        funcionario.setAtivo(true);

        Funcionario f = new Funcionario();
        f.setUsername(funcionario.getUsername());
        f.setEmail(funcionario.getEmail());
        f.setPassword(funcionario.getPassword());
        f.setCargo(funcionario.getCargo());
        f.setRole("ROLE_FUNCIONARIO");

        organizacaoRepository.findById(funcionario.getIdOrganizacao()).ifPresent(org -> {f.setOrganizacao(org);});

        return funcionarioRepository.save(f);

    }

    // O método agora retorna uma lista de DTOs
    public List<FuncionarioDTO> getListAllFuncionarios() {
        List<Funcionario> funcionarios = this.funcionarioRepository.findAll();

        // Lógica de mapeamento completa
        return funcionarios.stream()
                .map(funcionario -> {
                    // Converte o Set<JornadaTrabalho> em List<JornadaTrabalhoDTO>
                    List<JornadaTrabalhoDTO> jornadaDTO = funcionario.getJornadaDeTrabalho().stream()
                            .map(jornada -> new JornadaTrabalhoDTO(
                                    jornada.getDiaSemana().name(),
                                    jornada.getHoraInicio(),
                                    jornada.getHoraFim(),
                                    jornada.getAtivo()
                            ))
                            .collect(Collectors.toList());

                    // Converte o Set<BloqueioAgenda> em List<BloqueioAgendaDTO>
                    List<BloqueioAgendaDTO> bloqueiosDTO = funcionario.getBloqueiosAgenda().stream()
                            .map(bloqueio -> new BloqueioAgendaDTO(
                                    bloqueio.getInicioBloqueio(),
                                    bloqueio.getFimBloqueio(),
                                    bloqueio.getDescricao(),
                                    bloqueio.getTipoBloqueio().name()
                            ))
                            .collect(Collectors.toList());

                    // Cria o DTO final do funcionário com as listas de DTOs
                    return new FuncionarioDTO( funcionario, bloqueiosDTO, jornadaDTO
                    );
                })
                .collect(Collectors.toList());
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {
        return this.funcionarioRepository.findAllProjectedBy();
    }

}
