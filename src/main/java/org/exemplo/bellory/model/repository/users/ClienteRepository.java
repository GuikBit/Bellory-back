package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Usado apenas para autenticação (login/JWT) - não usar para validações de negócio
    Optional<Cliente> findByUsername(String username);

    List<Cliente> findAll(Specification<Cliente> spec, Sort sort);

    // Buscar por organização
    List<Cliente> findAllByOrganizacao_Id(Long organizacaoId);

    // Verificações com organização
    Optional<Cliente> findByUsernameAndOrganizacao_Id(String username, Long organizacaoId);
    Optional<Cliente> findByCpfAndOrganizacao_Id(String cpf, Long organizacaoId);
    Optional<Cliente> findByEmailAndOrganizacao_Id(String email, Long organizacaoId);

    // Top clientes por organização
    List<Cliente> findTopClientesByOrganizacao_Id(Long organizacaoId);

    // Contadores com organização
    Long countByOrganizacao_Id(Long organizacaoId);
    Long countByOrganizacao_IdAndAtivo(Long organizacaoId, Boolean ativo);
    Long countByOrganizacao_IdAndDtCriacaoBetween(Long organizacaoId, LocalDateTime inicio, LocalDateTime fim);

    // Aniversariantes por organização
    Long countAniversariantesHojeByOrganizacao_Id(Long organizacaoId);

    @Query(value = "SELECT COUNT(*) FROM app.cliente c WHERE c.organizacao_id = :organizacaoId " +
            "AND EXTRACT(MONTH FROM c.data_nascimento) = EXTRACT(MONTH FROM CAST(:hoje AS DATE)) " +
            "AND EXTRACT(DAY FROM c.data_nascimento) BETWEEN EXTRACT(DAY FROM CAST(:inicio AS DATE)) AND EXTRACT(DAY FROM CAST(:fim AS DATE))",
            nativeQuery = true)
    Long countAniversariantesEstaSemanaByOrganizacao(@Param("organizacaoId") Long organizacaoId,
                                                     @Param("hoje") LocalDate hoje,
                                                     @Param("inicio") LocalDate inicio,
                                                     @Param("fim") LocalDate fim);
    Long countClientesRecorrentesByOrganizacao_Id(Long organizacaoId);

    Long countByOrganizacao_IdAndAtivoTrue(Long organizacaoId);

//    Long countAniversariantesDoMesByOrganizacao(Organizacao organizacao);

    @Query(value = "SELECT COUNT(*) FROM app.cliente c " +
            "WHERE c.organizacao_id = :organizacaoId " +
            "AND EXTRACT(MONTH FROM c.data_nascimento) = :mes " +
            "AND c.ativo = true", nativeQuery = true)
    Long countAniversariantesDoMesByOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("mes") int mes
    );

    Optional<Cliente> findByTelefoneAndOrganizacao_Id(String telefone, Long organizacaoId);

    // ==================== QUERIES OTIMIZADAS PARA DASHBOARD ====================

    /**
     * Conta aniversariantes de hoje por organização (PostgreSQL)
     */
    @Query(value = "SELECT COUNT(*) FROM app.cliente c " +
            "WHERE c.organizacao_id = :organizacaoId " +
            "AND c.ativo = true " +
            "AND EXTRACT(DAY FROM c.data_nascimento) = EXTRACT(DAY FROM CURRENT_DATE) " +
            "AND EXTRACT(MONTH FROM c.data_nascimento) = EXTRACT(MONTH FROM CURRENT_DATE)",
            nativeQuery = true)
    Long countAniversariantesHojeByOrganizacao(@Param("organizacaoId") Long organizacaoId);

    /**
     * Conta aniversariantes da semana por organização (PostgreSQL)
     */
    @Query(value = "SELECT COUNT(*) FROM app.cliente c " +
            "WHERE c.organizacao_id = :organizacaoId " +
            "AND c.ativo = true " +
            "AND EXTRACT(MONTH FROM c.data_nascimento) = :mes " +
            "AND EXTRACT(DAY FROM c.data_nascimento) BETWEEN :diaInicio AND :diaFim",
            nativeQuery = true)
    Long countAniversariantesEstaSemanaByOrganizacaoOptimized(
            @Param("organizacaoId") Long organizacaoId,
            @Param("mes") int mes,
            @Param("diaInicio") int diaInicio,
            @Param("diaFim") int diaFim
    );

    /**
     * Top clientes por valor gasto (através das cobranças pagas)
     */
    @Query("SELECT c.id, c.nomeCompleto, COALESCE(SUM(cob.valor), 0) as totalGasto, COUNT(DISTINCT a.id) as totalAgendamentos " +
            "FROM Cliente c " +
            "LEFT JOIN Agendamento a ON a.cliente.id = c.id " +
            "LEFT JOIN Cobranca cob ON cob.cliente.id = c.id AND cob.statusCobranca = 'PAGO' " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.ativo = true " +
            "GROUP BY c.id, c.nomeCompleto " +
            "ORDER BY totalGasto DESC")
    List<Object[]> findTopClientesByOrganizacao(
            @Param("organizacaoId") Long organizacaoId
    );

    /**
     * Ticket médio por cliente
     */
    @Query("SELECT COALESCE(AVG(cob.valor), 0) FROM Cobranca cob " +
            "WHERE cob.organizacao.id = :organizacaoId " +
            "AND cob.statusCobranca = 'PAGO'")
    BigDecimal findTicketMedioByOrganizacao(@Param("organizacaoId") Long organizacaoId);

    // ==================== QUERIES PARA RELATÓRIOS ====================

    /**
     * Conta clientes com cadastro incompleto por organização
     */
    @Query("SELECT COUNT(c) FROM Cliente c " +
            "WHERE c.organizacao.id = :organizacaoId " +
            "AND c.isCadastroIncompleto = true")
    Long countCadastrosIncompletos(@Param("organizacaoId") Long organizacaoId);

    /**
     * Conta novos cadastros por data (para gráfico de evolução)
     */
    @Query(value = "SELECT CAST(c.dt_criacao AS DATE), COUNT(c.id) FROM app.cliente c " +
            "WHERE c.organizacao_id = :organizacaoId " +
            "AND c.dt_criacao BETWEEN :inicio AND :fim " +
            "GROUP BY CAST(c.dt_criacao AS DATE) " +
            "ORDER BY CAST(c.dt_criacao AS DATE)", nativeQuery = true)
    List<Object[]> countNovosCadastrosByDataAndOrganizacao(
            @Param("organizacaoId") Long organizacaoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}
