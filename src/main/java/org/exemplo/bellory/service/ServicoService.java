package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.repository.servico.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }


    public List<Servico> getListAllServicos() {

        return this.servicoRepository.findAll();
    }

    public List<ServicoAgendamento> getListAgendamentoServicos() {

        return this.servicoRepository.findAllProjectedBy();
    }
}
