package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.compra.Compra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends User {

    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;

    // --- CAMPO DE ROLE COMO STRING ---
    private String role;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    private List<Compra> listaCompra = new ArrayList<>();
}
