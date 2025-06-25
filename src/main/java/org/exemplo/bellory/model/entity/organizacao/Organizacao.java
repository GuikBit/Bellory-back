package org.exemplo.bellory.model.entity.organizacao;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.pagamento.Cobranca;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String nomeFantasia;
    private String cnpj;
    private String smail;
    private String nomeProprietario;
    private String cpfProprietario;
    private String emailProprietario;

    private Endereco endereco;

    private Cobranca cobranca;



    @CreationTimestamp
    private LocalDateTime dtCriacao;

    @UpdateTimestamp
    private LocalDateTime dtAtualizacao;

    private boolean ativo;
}
