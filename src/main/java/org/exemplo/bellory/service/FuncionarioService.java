package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.BloqueioAgendaDTO;
import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.dto.FuncionarioDTO; // Importar o DTO
import org.exemplo.bellory.model.dto.JornadaTrabalhoDTO;
import org.exemplo.bellory.model.entity.funcionario.BloqueioAgenda;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.funcionario.JornadaTrabalho;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors; // Importar

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, OrganizacaoRepository organizacaoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
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

    public Funcionario postNewFuncionario(Funcionario funcionario) {
        // Lógica para criar um novo funcionário
        return funcionarioRepository.save(funcionario);
    }
}
