package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// DTO para receber os dados de criação de um novo funcionário.
@Getter
@Setter
public class FuncionarioCreateDTO {
    private Long idOrganizacao;
    private String username;
    private String nomeCompleto;
    private String email;
    private String password;
    private String cargo;
    private Integer nivel;
    private String role;
    private boolean isVisibleExterno;

    private String telefone;
    private String cpf;
    private List<Long> servicosId;
    private boolean isResumido;
}
