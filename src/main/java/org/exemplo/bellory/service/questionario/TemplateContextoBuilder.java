package org.exemplo.bellory.service.questionario;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.funcionario.Funcionario;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.model.entity.users.Cliente;
import org.exemplo.bellory.model.repository.agendamento.AgendamentoRepository;
import org.exemplo.bellory.model.repository.funcionario.FuncionarioRepository;
import org.exemplo.bellory.model.repository.users.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolve as variaveis canonicas de templates de termo no servidor.
 *
 * Usado pelo {@code GET} de questionario quando o front fornece os IDs de
 * cliente / agendamento / funcionario na URL: o servidor carrega as entidades,
 * valida ownership (mesmo tenant, agendamento pertence ao cliente), monta o
 * mapa de variaveis e substitui no {@code Pergunta.textoTermo}.
 */
@Service
public class TemplateContextoBuilder {

    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final FuncionarioRepository funcionarioRepository;

    public TemplateContextoBuilder(ClienteRepository clienteRepository,
                                   AgendamentoRepository agendamentoRepository,
                                   FuncionarioRepository funcionarioRepository) {
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /**
     * Monta o mapa de variaveis com base nos IDs fornecidos.
     *
     * @param organizacaoId  organizacao do questionario sendo resolvido (escopo de tenant)
     * @param clienteId      pode ser null
     * @param agendamentoId  pode ser null
     * @param funcionarioId  pode ser null
     * @return mapa imutavel pronto para substituicao via {@link #renderizar(String, Map)}
     * @throws SecurityException se algum recurso nao pertence ao tenant
     */
    public Map<String, String> construir(Long organizacaoId,
                                         Long clienteId,
                                         Long agendamentoId,
                                         Long funcionarioId) {
        Map<String, String> ctx = new HashMap<>();

        Organizacao org = null;

        Optional<Cliente> clienteOpt = clienteId != null
                ? clienteRepository.findById(clienteId) : Optional.empty();
        clienteOpt.ifPresent(c -> {
            verificarTenant(organizacaoId, c.getOrganizacao() != null ? c.getOrganizacao().getId() : null,
                    "Cliente");
            put(ctx, "nomeCliente", c.getNomeCompleto());
            put(ctx, "cpfCliente", c.getCpf());
            put(ctx, "telefoneCliente", c.getTelefone());
            // Cliente nao tem campo RG no modelo atual — variavel resolvera para vazio.
        });

        Optional<Agendamento> agOpt = agendamentoId != null
                ? agendamentoRepository.findById(agendamentoId) : Optional.empty();
        if (agOpt.isPresent()) {
            Agendamento ag = agOpt.get();
            verificarTenant(organizacaoId, ag.getOrganizacao() != null ? ag.getOrganizacao().getId() : null,
                    "Agendamento");
            // Se o cliente foi informado, tem que pertencer ao agendamento
            if (clienteOpt.isPresent() && ag.getCliente() != null
                    && !ag.getCliente().getId().equals(clienteOpt.get().getId())) {
                throw new SecurityException("Agendamento nao pertence ao cliente informado.");
            }
            if (ag.getDtAgendamento() != null) {
                put(ctx, "dataAtendimento", DATA_FMT.format(ag.getDtAgendamento()));
                put(ctx, "horarioAtendimento", HORA_FMT.format(ag.getDtAgendamento()));
            }
            if (ag.getServicos() != null && !ag.getServicos().isEmpty()) {
                String nomes = ag.getServicos().stream()
                        .map(Servico::getNome)
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(", "));
                if (!nomes.isBlank()) put(ctx, "nomeProcedimento", nomes);
            }
            // Se o agendamento tem 1 unico funcionario e o caller nao passou funcionarioId,
            // usa o do agendamento como conveniencia.
            if (funcionarioId == null && ag.getFuncionarios() != null && ag.getFuncionarios().size() == 1) {
                Funcionario f = ag.getFuncionarios().get(0);
                put(ctx, "nomeProfissional", f.getNomeCompleto());
            }
            org = ag.getOrganizacao();
        }

        if (funcionarioId != null) {
            Funcionario func = funcionarioRepository.findById(funcionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Funcionario nao encontrado."));
            verificarTenant(organizacaoId,
                    func.getOrganizacao() != null ? func.getOrganizacao().getId() : null, "Funcionario");
            put(ctx, "nomeProfissional", func.getNomeCompleto());
        }

        // Resolve dados do estabelecimento via organizacao do agendamento (ou do cliente como fallback).
        if (org == null && clienteOpt.isPresent()) {
            org = clienteOpt.get().getOrganizacao();
        }
        if (org != null) {
            put(ctx, "nomeEstabelecimento", org.getNomeFantasia());
            put(ctx, "cnpjEstabelecimento", org.getCnpj());
            if (org.getEnderecoPrincipal() != null) {
                put(ctx, "enderecoEstabelecimento", formatarEndereco(org.getEnderecoPrincipal()));
            }
        }

        return ctx;
    }

    /**
     * Substitui placeholders {@code {{var}}} no template usando o contexto fornecido.
     * Variaveis ausentes sao substituidas por string vazia.
     */
    public String renderizar(String template, Map<String, String> contexto) {
        if (template == null || template.isEmpty()) return template;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String chave = m.group(1);
            String valor = contexto.getOrDefault(chave, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(valor));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void verificarTenant(Long organizacaoEsperada, Long organizacaoRecurso, String entidade) {
        if (organizacaoEsperada == null) return; // skip quando o caller nao define escopo
        if (organizacaoRecurso == null || !organizacaoRecurso.equals(organizacaoEsperada)) {
            throw new SecurityException(entidade + " nao pertence ao tenant do questionario.");
        }
    }

    private static void put(Map<String, String> ctx, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            ctx.put(chave, valor);
        }
    }

    private static String formatarEndereco(Endereco e) {
        StringBuilder sb = new StringBuilder();
        if (e.getLogradouro() != null) sb.append(e.getLogradouro());
        if (e.getNumero() != null) sb.append(", ").append(e.getNumero());
        if (e.getBairro() != null) sb.append(" - ").append(e.getBairro());
        if (e.getCidade() != null) sb.append(", ").append(e.getCidade());
        if (e.getUf() != null) sb.append("/").append(e.getUf());
        if (e.getCep() != null) sb.append(" - CEP ").append(e.getCep());
        return sb.toString();
    }
}
