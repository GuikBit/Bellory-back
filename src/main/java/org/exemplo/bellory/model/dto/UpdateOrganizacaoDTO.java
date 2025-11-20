package org.exemplo.bellory.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.organizacao.AcessoAdm;
import org.exemplo.bellory.model.entity.organizacao.DiaSemanaFuncionamento;
import org.exemplo.bellory.model.entity.organizacao.RedesSociais;
import org.exemplo.bellory.model.entity.organizacao.Responsavel;
import org.exemplo.bellory.model.entity.plano.Plano;
import org.exemplo.bellory.model.entity.plano.PlanoLimites;
import org.exemplo.bellory.model.entity.tema.Tema;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizacaoDTO {

    @Size(min = 14, max = 18, message = "CNPJ deve ter entre 14 e 18 caracteres")
    private String cnpj;

    @Size(max = 255, message = "Razão social deve ter no máximo 255 caracteres")
    private String razaoSocial;

    @Size(max = 255, message = "Nome fantasia deve ter no máximo 255 caracteres")
    private String nomeFantasia;

    @Size(max = 50, message = "Inscrição estadual deve ter no máximo 50 caracteres")
    private String inscricaoEstadual;

    @Email(message = "Email inválido")
    private String email;

    private String telefone1;

    private String telefone2;

    private String whatsapp;

    @Valid
    private Responsavel responsavel;

    @Valid
    private Endereco endereco;

    @Valid
    private AcessoAdm acessoAdm;

    @Valid
    private Tema tema;

    @Valid
    private Plano plano;

    @Valid
    private ConfigSistema configSistema;

    @Valid
    private RedesSociais redesSociais;

    @Valid
    private List<DiaSemanaFuncionamento> horarioFuncionamento;

    private PlanoLimites limitesPersonalizados;

    private Boolean ativo;
}
