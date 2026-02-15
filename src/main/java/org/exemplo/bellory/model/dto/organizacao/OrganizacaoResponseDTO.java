package org.exemplo.bellory.model.dto.organizacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.organizacao.AcessoAdm;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.entity.organizacao.Responsavel;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.plano.PlanoBellory;
import org.exemplo.bellory.model.entity.plano.PlanoLimites;
import org.exemplo.bellory.model.entity.plano.PlanoLimitesBellory;
import org.exemplo.bellory.model.entity.tema.Tema;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizacaoResponseDTO {
    private Long id;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;
    private String emailPrincipal;
    private String telefone1;
    private String telefone2;
    private String whatsapp;
    private String slug;

    // ✅ DTOs para Embeddables
    private Responsavel responsavel;
    private Integer acessoAdm;
    private RedesSociais redesSociais;
    private Tema tema;

    // Relacionamentos
    private Long planoId;
    private String planoNome;
    private PlanoBellory plano;
    private ConfigSistema configSistema;
    private Endereco enderecoPrincipal;
    private PlanoLimitesBellory limitesPersonalizados;

    // Imagens
    private String logoUrl;
    private String bannerUrl;

    // Controle
    private Boolean ativo;
    private LocalDateTime dtCadastro;
    private LocalDateTime dtAtualizacao;

}
