package org.exemplo.bellory.model.entity.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_admins")
@Getter
@Setter
public class Admin extends User {
    // A classe Admin pode ter campos específicos no futuro,
    // mas por agora, ela serve para diferenciar o tipo de utilizador.
}
