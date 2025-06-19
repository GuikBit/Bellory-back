package org.exemplo.bellory.model.entity.funcionario;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.users.User;

@Entity
@Table(name = "funcionario")
@Getter
@Setter
public class Funcionario extends User {

    private String cargo;



}
