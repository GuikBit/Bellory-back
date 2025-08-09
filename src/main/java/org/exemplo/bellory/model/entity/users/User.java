package org.exemplo.bellory.model.entity.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe base para todos os tipos de utilizadores do sistema.
 * Utiliza a estratégia de herança JOINED, onde os dados comuns ficam na tabela 'tb_users'
 * e os dados específicos de cada tipo de utilizador ficam em tabelas separadas.
 */
@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name="nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private boolean ativo = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User(Organizacao organizacao, String username, String nomeCompleto, String password, String email) {
        this.organizacao = organizacao;
        this.username = username;
        this.nomeCompleto = nomeCompleto;
        this.password = password;
        this.email = email;
    }
}
