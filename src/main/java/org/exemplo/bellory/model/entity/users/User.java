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
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class User{ // Tornada abstrata, pois um User sempre será um tipo específico

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CORREÇÃO PRINCIPAL ---
    // Substituído int por um relacionamento real para garantir a integridade dos dados.
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
//
//    // Métodos da interface UserDetails (sem alterações)
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return this.roles;
//    }
//
//    @Override
//    public String getPassword() {
//        return this.password;
//    }
//
//    @Override
//    public String getUsername() {
//        return this.username;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return this.ativo;
//    }
}
