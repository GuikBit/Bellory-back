package org.exemplo.bellory.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.servico.Servico;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FuncionarioServicoResponse<T> {
    private List<T> dados;
    private String tipoConsulta; // "POR_SERVICOS" ou "POR_FUNCIONARIOS"


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FuncionarioResumoDTO {
        private Long id;
        private String nomeCompleto;
        private String email;
        private String telefone;
        private String foto;
        private boolean isVisivelExterno;
//        private List<Long> servicoIds;
//        private List<String> servicoNomes;

        public FuncionarioResumoDTO(Funcionario funcionario) {
            this.id = funcionario.getId();
            this.nomeCompleto = funcionario.getNomeCompleto();
            this.email = funcionario.getEmail();
            this.telefone = funcionario.getTelefone();
            this.foto = funcionario.getFoto();
            this.isVisivelExterno = funcionario.isVisivelExterno();

//            if (funcionario.getServicos() != null) {
//                this.servicoIds = funcionario.getServicos().stream()
//                        .map(Servico::getId)
//                        .collect(Collectors.toList());
//                this.servicoNomes = funcionario.getServicos().stream()
//                        .map(Servico::getNome)
//                        .collect(Collectors.toList());
//            }
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServicoResumoDTO {
        private Long id;
        private String nome;
        private String descricao;
        private Integer tempoEstimadoMinutos;
        private java.math.BigDecimal preco;
        private String genero;
        private String categoriaNome;
//        private List<Long> funcionarioIds;
//        private List<String> funcionarioNomes;

        public ServicoResumoDTO(Servico servico) {
            this.id = servico.getId();
            this.nome = servico.getNome();
            this.descricao = servico.getDescricao();
            this.tempoEstimadoMinutos = servico.getTempoEstimadoMinutos();
            this.preco = servico.getPreco();
            this.genero = servico.getGenero();

            if (servico.getCategoria() != null) {
                this.categoriaNome = servico.getCategoria().getLabel();
            }

//            if (servico.getFuncionarios() != null) {
//                this.funcionarioIds = servico.getFuncionarios().stream()
//                        .map(Funcionario::getId)
//                        .collect(Collectors.toList());
//                this.funcionarioNomes = servico.getFuncionarios().stream()
//                        .map(Funcionario::getNomeCompleto)
//                        .collect(Collectors.toList());
//            }
        }
    }
}
