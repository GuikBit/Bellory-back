package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends User {

    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;

    private Boolean isCadastroIncompleto;

    @Column(length = 14)
    private String cpf;

    // --- CAMPO DE ROLE COMO STRING ---
    private String role;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

}
