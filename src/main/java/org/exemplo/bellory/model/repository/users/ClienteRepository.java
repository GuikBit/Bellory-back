package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsername(String username);

    Optional<Cliente> findByEmail(String email);

    Long countByAtivo(boolean ativo);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.dtCriacao BETWEEN :inicio AND :fim")
    Long countByDataCriacaoBetween(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(DISTINCT a.cliente.id) FROM Agendamento a GROUP BY a.cliente.id HAVING COUNT(a.id) > 1")
    Long countClientesRecorrentes();

    @Query("SELECT c FROM Cliente c WHERE c.id IN (" +
            "SELECT a.cliente.id FROM Agendamento a " +
            "GROUP BY a.cliente.id " +
            "ORDER BY COUNT(a.id) DESC, SUM(COALESCE(a.cobranca.valor, 0)) DESC)")
    List<Cliente> findTopClientes();

    @Query("SELECT COUNT(c) FROM Cliente c WHERE DAY(c.dataNascimento) = DAY(CURRENT_DATE) AND MONTH(c.dataNascimento) = MONTH(CURRENT_DATE)")
    Long countAniversariantesHoje();

    // CORREÇÃO: Alterar para usar LocalDate e ajustar a query
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.dataNascimento BETWEEN :inicioSemana AND :fimSemana")
    Long countAniversariantesEstaSemana(@Param("inicioSemana") LocalDate inicioSemana, @Param("fimSemana") LocalDate fimSemana);

    // Método para buscar clientes por termo (alternativa ao Specification)
    @Query("SELECT c FROM Cliente c WHERE " +
            "LOWER(c.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "c.telefone LIKE CONCAT('%', :termo, '%')")
    List<Cliente> findByTermoBusca(@Param("termo") String termo);

    // Buscar clientes aniversariantes por mês
    @Query("SELECT c FROM Cliente c WHERE MONTH(c.dataNascimento) = :mes AND c.ativo = true")
    List<Cliente> findAniversariantesByMes(@Param("mes") int mes);

    // Buscar clientes aniversariantes por mês e ano
    @Query("SELECT c FROM Cliente c WHERE MONTH(c.dataNascimento) = :mes AND YEAR(c.dataNascimento) = :ano AND c.ativo = true")
    List<Cliente> findAniversariantesByMesAndAno(@Param("mes") int mes, @Param("ano") int ano);

    List<Cliente> findAll(Specification<Cliente> spec, Sort sort);
}
