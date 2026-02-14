package org.exemplo.bellory.service;


import org.exemplo.bellory.model.dto.auth.*;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.users.Admin;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.entity.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class UserInfoService {

    @Transactional(readOnly = true)
    public UserInfoDTO buildUserInfo(User user) {
        // Construir informações básicas
        UserInfoDTO.UserInfoDTOBuilder userInfoBuilder = UserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nomeCompleto(user.getNomeCompleto())
                .email(user.getEmail())
//                .telefone(user.getTelefone())
//                .cpf(user.getCpf())
                .roles(user.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toList()))
                .active(user.isEnabled());

        // Adicionar ID da organização se disponível
        if (user.getOrganizacao() != null) {
            userInfoBuilder.idOrganizacao(user.getOrganizacao().getId());
        }

        // Verificar o tipo concreto do usuário usando instanceof
        if (user instanceof Cliente cliente) {
            userInfoBuilder
                    .userType("CLIENTE")
                    .dataCriacao(cliente.getDtCriacao())
                    .clienteInfo(buildClienteInfo(cliente));
            return userInfoBuilder.build();
        }

        if (user instanceof Funcionario funcionario) {
            userInfoBuilder
                    .userType("FUNCIONARIO")
                    .dataCriacao(funcionario.getDataCriacao())
                    .isPrimeiroAcesso(funcionario.isPrimeiroAcesso())
                    .funcionarioInfo(buildFuncionarioInfo(funcionario));
            return userInfoBuilder.build();
        }

        if (user instanceof Admin admin) {
            userInfoBuilder
                    .userType("ADMIN")
                    .dataCriacao(admin.getDtCriacao())
                    .adminInfo(buildAdminInfo(admin));
            return userInfoBuilder.build();
        }

        // Fallback para dados básicos
        return userInfoBuilder
                .userType("USER")
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    private ClienteInfoDTO buildClienteInfo(Cliente cliente) {
        return ClienteInfoDTO.builder()
                .dataNascimento(cliente.getDataNascimento())
                .role(cliente.getRole())
                // Estatísticas podem ser calculadas aqui ou em outro serviço
                .totalAgendamentos(0L) // TODO: Implementar cálculo
                .totalCompras(0L) // TODO: Implementar cálculo
                .valorTotalGasto(BigDecimal.ZERO) // TODO: Implementar cálculo
                .agendamentosPendentes(0L) // TODO: Implementar cálculo
                .cobrancasPendentes(0L) // TODO: Implementar cálculo
                .build();
    }

    private FuncionarioInfoDTO buildFuncionarioInfo(Funcionario funcionario) {
        return FuncionarioInfoDTO.builder()
                .foto(funcionario.getFotoPerfil())
                .dataNasc(funcionario.getDataNasc())
                .dataContratacao(funcionario.getDataContratacao())
                .sexo(funcionario.getSexo())
                .nivel(funcionario.getNivel())
                .apelido(funcionario.getApelido())
                .situacao(funcionario.getSituacao())
                .cargo(funcionario.getCargo().getNome())
                .salario(funcionario.getSalario())
                .isComissao(funcionario.isComissao())
                .comissao(funcionario.getComissao())
                .jornadaSemanal(funcionario.getJornadaSemanal())
                .isVisivelExterno(funcionario.isVisivelExterno())
                .role(funcionario.getRole())
//                .endereco(buildEndereco(funcionario))
//                .documentos(buildDocumentos(funcionario))
//                .dadosBancarios(buildDadosBancarios(funcionario))
                .build();
    }

    private AdminInfoDTO buildAdminInfo(Admin admin) {
        return AdminInfoDTO.builder()
                .nomeCompleto(admin.getNomeCompleto())
                .email(admin.getEmail())
                .dtCriacao(admin.getDtCriacao())
                .role(admin.getRole())
                .build();
    }

    private EnderecoDTO buildEndereco(Funcionario funcionario) {
        return EnderecoDTO.builder()
                .cep(funcionario.getCep())
                .logradouro(funcionario.getLogradouro())
                .numero(funcionario.getNumero())
                .complemento(funcionario.getComplemento())
                .bairro(funcionario.getBairro())
                .cidade(funcionario.getCidade())
                .uf(funcionario.getUf())
                .build();
    }

    private DocumentosDTO buildDocumentos(Funcionario funcionario) {
        return DocumentosDTO.builder()
                .rg(funcionario.getRg())
                .rgOrgEmissor(funcionario.getRgOrgEmissor())
                .tituloEleitor(funcionario.getTituloEleitor())
                .certMilitar(funcionario.getCertMilitar())
                .cnh(funcionario.getCnh())
                .categHabilitacao(funcionario.getCategHabilitacao())
                .ctps(funcionario.getCtps())
                .ctpsSerie(funcionario.getCtpsSerie())
                .pisPasep(funcionario.getPisPasep())
                .build();
    }

    private DadosBancariosDTO buildDadosBancarios(Funcionario funcionario) {
        return DadosBancariosDTO.builder()
                .banco(funcionario.getBanco())
                .agencia(funcionario.getAgencia())
                .conta(funcionario.getConta())
                .operacao(funcionario.getOperacao())
                .build();
    }

    public UserInfoDTO buildUserInfoWithStatistics(User user) {
        UserInfoDTO userInfo = buildUserInfo(user);

        // Se for cliente, calcular estatísticas detalhadas
        if (user instanceof Cliente cliente && userInfo.getClienteInfo() != null) {
            ClienteInfoDTO clienteInfoWithStats = userInfo.getClienteInfo();
            clienteInfoWithStats.setTotalAgendamentos(calculateTotalAgendamentos(cliente.getId()));
            clienteInfoWithStats.setTotalCompras(calculateTotalCompras(cliente.getId()));
            clienteInfoWithStats.setValorTotalGasto(calculateValorTotalGasto(cliente.getId()));
            clienteInfoWithStats.setUltimoAgendamento(findUltimoAgendamento(cliente.getId()));
            clienteInfoWithStats.setUltimaCompra(findUltimaCompra(cliente.getId()));
            clienteInfoWithStats.setAgendamentosPendentes(countAgendamentosPendentes(cliente.getId()));
            clienteInfoWithStats.setCobrancasPendentes(countCobrancasPendentes(cliente.getId()));
        }

        return userInfo;
    }

    // Métodos para calcular estatísticas - implementar conforme suas necessidades
    private Long calculateTotalAgendamentos(Long clienteId) {
        // TODO: Implementar query para contar agendamentos do cliente
        return 0L;
    }

    private Long calculateTotalCompras(Long clienteId) {
        // TODO: Implementar query para contar compras do cliente
        return 0L;
    }

    private BigDecimal calculateValorTotalGasto(Long clienteId) {
        // TODO: Implementar query para somar valor total gasto pelo cliente
        return BigDecimal.ZERO;
    }

    private LocalDateTime findUltimoAgendamento(Long clienteId) {
        // TODO: Implementar query para encontrar último agendamento
        return null;
    }

    private LocalDateTime findUltimaCompra(Long clienteId) {
        // TODO: Implementar query para encontrar última compra
        return null;
    }

    private Long countAgendamentosPendentes(Long clienteId) {
        // TODO: Implementar query para contar agendamentos pendentes
        return 0L;
    }

    private Long countCobrancasPendentes(Long clienteId) {
        // TODO: Implementar query para contar cobranças pendentes
        return 0L;
    }
}
