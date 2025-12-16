package org.exemplo.bellory.model.dto.organizacao;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.dto.*;
import org.exemplo.bellory.model.dto.tema.TemaDTO;

/**
 * DTO para criação de uma nova Organização
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrganizacaoDTO {
    @NotBlank(message = "CNPJ é obrigatório")
    @Size(min = 14, max = 18, message = "CNPJ deve ter entre 14 e 18 caracteres")
    private String cnpj;

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 255, message = "Razão social deve ter no máximo 255 caracteres")
    private String razaoSocial;

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(max = 255, message = "Nome fantasia deve ter no máximo 255 caracteres")
    private String nomeFantasia;

    @Size(max = 50, message = "Inscrição estadual deve ter no máximo 50 caracteres")
    private String inscricaoEstadual;

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    private String email;

    // ✅ Aceita "telefone" do JSON e mapeia para telefone1
    @NotBlank(message = "Telefone principal é obrigatório")
    @JsonProperty("telefone")
    private String telefone1;

    private String telefone2;

    private String whatsapp;

    @Valid
    @NotNull(message = "Responsável é obrigatório")
    private ResponsavelDTO responsavel;

    @Valid
    @NotNull(message = "Endereço é obrigatório")
    private EnderecoDTO endereco;

    // ✅ Aceita "acesso" do JSON e mapeia para acessoAdm
    @Valid
    @NotNull(message = "Acesso administrativo é obrigatório")
    @JsonProperty("acesso")
    private AcessoAdmDTO acessoAdm;

    @Valid
    @NotNull(message = "Tema é obrigatório")
    private TemaDTO tema;

    @Valid
    @NotNull(message = "Plano é obrigatório")
    private PlanoDTO plano;


}
