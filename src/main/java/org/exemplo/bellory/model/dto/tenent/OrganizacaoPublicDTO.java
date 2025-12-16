package org.exemplo.bellory.model.dto.tenent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizacaoPublicDTO {

    private Long id;
    private String nome;
    private String nomeFantasia;
    private String slug;
    private String descricao;
    private String logo;
    private String banner;
    private String telefone;
    private String whatsapp;
    private String email;

    // Endereço
    private EnderecoPublicDTO endereco;
}
