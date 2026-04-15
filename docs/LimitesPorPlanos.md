Plano: Limites de Plano via ThreadLocal

Contexto

Os limites dos planos (PlanoLimitesBellory) estao definidos no banco mas nunca sao verificados nas operacoes de criacao. Precisamos carregar os limites no TenantContext (ThreadLocal) e
validar automaticamente quando o cliente tenta criar recursos (clientes, funcionarios, servicos, agendamentos).

Cadeia de dados: Organizacao.plano → PlanoBellory.limites → PlanoLimitesBellory (com override opcional via Organizacao.limitesPersonalizados)

Convencao existente: null nos campos Integer = ilimitado.

 ---
Etapa 1: Adicionar PlanoLimitesBellory ao TenantContext

Arquivo: src/main/java/org/exemplo/bellory/context/TenantContext.java

- Adicionar novo ThreadLocal<PlanoLimitesBellory> currentLimites
- Adicionar setCurrentLimites(), getCurrentLimites()
- Atualizar clear() para fazer currentLimites.remove()

 ---
Etapa 2: Carregar limites no JwtAuthFilter

Arquivo: src/main/java/org/exemplo/bellory/config/JwtAuthFilter.java

- Injetar OrganizacaoRepository no construtor
- Criar metodo carregarLimitesPlano(Long organizacaoId) similar ao carregarConfigSistema()
- Logica:
  a. Buscar Organizacao pelo ID
  b. Se organizacao.getLimitesPersonalizados() != null → usar esse (override)
  c. Senao → usar organizacao.getPlano().getLimites()
  d. TenantContext.setCurrentLimites(limites)
- Chamar nos dois pontos onde carregarConfigSistema() ja e chamado (API Key e JWT auth)

Repositorio necessario: Verificar se OrganizacaoRepository ja tem query que faz fetch join do plano + limites. Se nao, criar:
@Query("SELECT o FROM Organizacao o LEFT JOIN FETCH o.limitesPersonalizados LEFT JOIN FETCH o.plano p LEFT JOIN FETCH p.limites WHERE o.id = :id")
Optional<Organizacao> findByIdWithLimites(@Param("id") Long id);

 ---
Etapa 3: Criar LimiteService

Novo arquivo: src/main/java/org/exemplo/bellory/service/LimiteService.java

Servico centralizado para validacao de limites:

@Service
public class LimiteService {

     private final ClienteRepository clienteRepository;
     private final FuncionarioRepository funcionarioRepository;
     private final ServicoRepository servicoRepository;
     private final AgendamentoRepository agendamentoRepository;

     // Metodo generico privado
     private void validarLimite(Long usoAtual, Integer limite, String recurso) {
         if (limite != null && usoAtual >= limite) {
             throw new LimitePlanoExcedidoException(recurso, limite);
         }
     }

     public void validarLimiteClientes(Long organizacaoId) {
         PlanoLimitesBellory limites = TenantContext.getCurrentLimites();
         if (limites == null || limites.getMaxClientes() == null) return;
         Long count = clienteRepository.countByOrganizacao_Id(organizacaoId);
         validarLimite(count, limites.getMaxClientes(), "clientes");
     }

     public void validarLimiteFuncionarios(Long organizacaoId) { ... }
     public void validarLimiteServicos(Long organizacaoId) { ... }
     public void validarLimiteAgendamentosMes(Long organizacaoId) { ... }

     // Feature flags
     public void validarPermissaoWhatsapp() { ... }
     public void validarPermissaoSite() { ... }
     public void validarPermissaoAgendamentoOnline() { ... }
}

 ---
Etapa 4: Criar Exception customizada

Novo arquivo: src/main/java/org/exemplo/bellory/exception/LimitePlanoExcedidoException.java

public class LimitePlanoExcedidoException extends RuntimeException {
private final String recurso;
private final Integer limite;
// Mensagem: "Limite do plano atingido para {recurso}. Maximo permitido: {limite}"
}

Arquivo: src/main/java/org/exemplo/bellory/config/GlobalExceptionHandler.java
- Adicionar handler para LimitePlanoExcedidoException → retornar HTTP 403 ou 429

 ---
Etapa 5: Adicionar count queries nos repositorios que faltam

- FuncionarioRepository — adicionar: Long countByOrganizacao_IdAndIsDeletadoFalse(Long organizacaoId);
- ServicoRepository — adicionar: Long countByOrganizacao_Id(Long organizacaoId); (ou equivalente com ativo=true)
- AgendamentoRepository — ja tem queries de count, mas adicionar se necessario para contagem mensal: Long countByOrganizacaoIdAndDtAgendamentoBetween(Long orgId, LocalDateTime inicio,
  LocalDateTime fim);

 ---
Etapa 6: Integrar validacao nos Services existentes

Injetar LimiteService e chamar antes de criar o recurso:

┌────────────────────┬─────────────────────┬───────────────────────────────────────────────────┐
│      Service       │       Metodo        │                     Validacao                     │
├────────────────────┼─────────────────────┼───────────────────────────────────────────────────┤
│ ClienteService     │ createCliente()     │ limiteService.validarLimiteClientes(orgId)        │
├────────────────────┼─────────────────────┼───────────────────────────────────────────────────┤
│ FuncionarioService │ createFuncionario() │ limiteService.validarLimiteFuncionarios(orgId)    │
├────────────────────┼─────────────────────┼───────────────────────────────────────────────────┤
│ ServicoService     │ createServico()     │ limiteService.validarLimiteServicos(orgId)        │
├────────────────────┼─────────────────────┼───────────────────────────────────────────────────┤
│ AgendamentoService │ createAgendamento() │ limiteService.validarLimiteAgendamentosMes(orgId) │
└────────────────────┴─────────────────────┴───────────────────────────────────────────────────┘

 ---
Arquivos a modificar/criar

┌───────────┬───────────────────────────────────────────────────────────────────┐
│   Acao    │                              Arquivo                              │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ context/TenantContext.java                                        │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ config/JwtAuthFilter.java                                         │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Criar     │ service/LimiteService.java                                        │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Criar     │ exception/LimitePlanoExcedidoException.java                       │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ config/GlobalExceptionHandler.java                                │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ repository/funcionario/FuncionarioRepository.java                 │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ repository/servico/ServicoRepository.java                         │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ repository/agendamento/AgendamentoRepository.java (se necessario) │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ repository/organizacao/OrganizacaoRepository.java                 │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ service/ClienteService.java                                       │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ service/FuncionarioService.java                                   │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ service/ServicoService.java                                       │
├───────────┼───────────────────────────────────────────────────────────────────┤
│ Modificar │ service/AgendamentoService.java                                   │
└───────────┴───────────────────────────────────────────────────────────────────┘

Verificacao

1. Compilar o projeto: mvn compile
2. Testar criacao de cliente quando limite esta atingido → deve retornar erro 403/429
3. Testar criacao quando limite e null (ilimitado) → deve funcionar normalmente
4. Testar com limitesPersonalizados na organizacao → deve usar o override
5. Verificar que TenantContext.clear() limpa os limites (sem memory leak)