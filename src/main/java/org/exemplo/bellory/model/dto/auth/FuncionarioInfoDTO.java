package org.exemplo.bellory.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuncionarioInfoDTO {
    private String foto;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dataNasc;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataContratacao;

    private String sexo;
    private Integer nivel;
    private String apelido;
    private String situacao;
    private String cargo;
    private BigDecimal salario;
    private boolean isComissao;
    private String comissao;
    private String jornadaSemanal;
    private boolean isVisivelExterno;
    private String role;

    // Endereço
    private EnderecoDTO endereco;

    // Documentos
    private DocumentosDTO documentos;

    // Dados bancários
    private DadosBancariosDTO dadosBancarios;
}
