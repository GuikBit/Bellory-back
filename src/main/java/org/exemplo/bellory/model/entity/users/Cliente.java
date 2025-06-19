package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends User {


    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;
}
