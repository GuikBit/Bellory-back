package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_funcionarios")
@Getter
@Setter
public class Funcionario extends User {

    // Exemplo de um campo específico para Funcionário
    private String cargo;
}
