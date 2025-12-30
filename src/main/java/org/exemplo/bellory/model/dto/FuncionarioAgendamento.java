package org.exemplo.bellory.model.dto;

import org.exemplo.bellory.model.entity.funcionario.Cargo;

public interface FuncionarioAgendamento {
    Long getId();
    String getNomeCompleto();
    Cargo getCargo();
}
