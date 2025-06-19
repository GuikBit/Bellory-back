package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioService {


    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }


    public List<Funcionario> getListAllFuncionarios() {

        return this.funcionarioRepository.findAll();
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {

        return this.funcionarioRepository.findAllProjectedBy();
    }
}
