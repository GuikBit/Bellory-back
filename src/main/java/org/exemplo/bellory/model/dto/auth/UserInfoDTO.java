package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoDTO {
    private Long id;
    private Long idOrganizacao;
    private String username;
    private String nomeCompleto;
    private String email;
    private String telefone;
    private String cpf;
    private List<String> roles;
    private boolean active;
    private String userType; // "CLIENTE" ou "FUNCIONARIO"
    private boolean isPrimeiroAcesso;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataCriacao;

    // Dados específicos do Cliente
    private ClienteInfoDTO clienteInfo;

    // Dados específicos do Funcionário
    private FuncionarioInfoDTO funcionarioInfo;

    private AdminInfoDTO adminInfo;
}
