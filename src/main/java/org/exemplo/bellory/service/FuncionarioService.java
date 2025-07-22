package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.FuncionarioAgendamento;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuncionarioService {


    private final FuncionarioRepository funcionarioRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository, OrganizacaoRepository organizacaoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.organizacaoRepository = organizacaoRepository;
    }


    public List<Funcionario> getListAllFuncionarios() {

        return this.funcionarioRepository.findAll();
    }

    public List<FuncionarioAgendamento> getListAllFuncionariosAgendamento() {

        return this.funcionarioRepository.findAllProjectedBy();
    }

    public Funcionario postNewFuncionario(Funcionario funcionario) {
//        Organizacao org = organizacaoRepository.findAllById(orgId);


        return null;
    }
}
