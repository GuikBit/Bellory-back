package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.funcionario.Cargo;

import java.util.List;

// DTO para receber os dados de criação de um novo funcionário.
@Getter
@Setter
public class FuncionarioCreateDTO {
    private Long idOrganizacao;
    private String username;
    private String nomeCompleto;
    private String fotoPerfil;
    private String email;
    private String password;
    private Integer nivel;
    private String role;
    private Cargo cargo;
    private String telefone;
    private String cpf;
    private List<Long> servicosId;
    private List<JornadaDiaDTO> planoHorarios;

    private boolean isResumido;
    private boolean isVisibleExterno;

}
