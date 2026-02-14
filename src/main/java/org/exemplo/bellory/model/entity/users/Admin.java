package org.exemplo.bellory.model.entity.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin", schema = "app", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_org_username", columnNames = {"organizacao_id", "username"}),
        @UniqueConstraint(name = "uk_admin_org_email", columnNames = {"organizacao_id", "email"})
})
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Admin extends User {

    private String role = "ROLE_SUPERADMIN";

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;
}
