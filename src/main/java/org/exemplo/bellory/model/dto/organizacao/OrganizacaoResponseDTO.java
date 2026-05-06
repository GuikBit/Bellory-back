package org.exemplo.bellory.model.dto.organizacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.entity.organizacao.Responsavel;

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
    private String publicoAlvo;
    private String segmento;

    private Responsavel responsavel;
    private Integer acessoAdm;
    private RedesSociais redesSociais;
    private String tema;

    private ConfigSistema configSistema;
    private Endereco enderecoPrincipal;

    private String logoUrl;
    private String bannerUrl;

    private Boolean ativo;
    private LocalDateTime dtCadastro;
    private LocalDateTime dtAtualizacao;
}
