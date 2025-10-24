package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;

// DTO para receber os dados de criação de um novo funcionário.
@Getter
@Setter
public class FuncionarioCreateDTO {
    private Long idOrganizacao;
    private String username;
    private String nomeCompleto;
    private String email;
    private String password;
    private String telefone;
    private String cargo;
    private Integer nivel;
    private String role;
    private boolean isVisibleExterno;
}
