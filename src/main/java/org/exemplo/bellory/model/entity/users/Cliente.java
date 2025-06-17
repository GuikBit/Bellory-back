package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tb_clientes")
@Getter
@Setter
public class Cliente extends User {

    // Exemplo de campos específicos para Cliente
    private String nomeCompleto;

    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;
}
