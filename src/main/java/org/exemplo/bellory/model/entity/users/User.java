package org.exemplo.bellory.model.entity.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class User implements UserDetails {

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

    // Método abstrato para que as subclasses forneçam sua role como String
    public abstract String getRole();

    public User(Organizacao organizacao, String username, String nomeCompleto, String password, String email) {
        this.organizacao = organizacao;
        this.username = username;
        this.nomeCompleto = nomeCompleto;
        this.password = password;
        this.email = email;
    }

    // --- MÉTODOS DA INTERFACE UserDetails ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converte a String da role em uma autoridade que o Spring Security entende
        return Collections.singletonList(new SimpleGrantedAuthority(getRole()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return this.ativo;
    }
}
