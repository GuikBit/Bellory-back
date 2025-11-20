package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    Optional<Organizacao> findByNomeFantasia(String nome);

    // ✅ ADICIONE ESTE MÉTODO
    @Query("SELECT DISTINCT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.plano " +
            "LEFT JOIN FETCH o.enderecoPrincipal " +
            "LEFT JOIN FETCH o.configSistema " +
            "LEFT JOIN FETCH o.limitesPersonalizados " +
            "WHERE o.ativo = true")
    List<Organizacao> findAllByAtivoTrueWithDetails();

    @Query("SELECT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.plano " +
            "LEFT JOIN FETCH o.enderecoPrincipal " +
            "LEFT JOIN FETCH o.configSistema " +
            "LEFT JOIN FETCH o.limitesPersonalizados " +
            "LEFT JOIN FETCH o.dadosFaturamento " +
            "WHERE o.id = :id AND o.ativo = true")
    Optional<Organizacao> findByIdWithDetails(@Param("id") Long id);

    /**
     * Busca uma organização pelo CNPJ
     */
    Optional<Organizacao> findByCnpj(String cnpj);

    /**
     * Busca uma organização pelo CNPJ e que esteja ativa
     */
    Optional<Organizacao> findByCnpjAndAtivoTrue(String cnpj);

    /**
     * Busca uma organização pelo ID e que esteja ativa
     */
    Optional<Organizacao> findByIdAndAtivoTrue(long id);

    /**
     * Lista todas as organizações ativas
     */
    List<Organizacao> findAllByAtivoTrue();

    /**
     * Verifica se existe uma organização com o CNPJ informado
     */
    boolean existsByCnpj(String cnpj);

    /**
     * Verifica se existe uma organização com o CNPJ informado, excluindo o ID fornecido
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organizacao o " +
            "WHERE o.cnpj = :cnpj AND o.id != :id")
    boolean existsByCnpjAndIdNot(@Param("cnpj") String cnpj, @Param("id") long id);

    /**
     * Busca organizações pelo tenant ID
     */
    @Query("SELECT o FROM Organizacao o WHERE o.configSistema.tenantId = :tenantId AND o.ativo = true")
    List<Organizacao> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Busca uma organização específica pelo ID e tenant ID
     */
    @Query("SELECT o FROM Organizacao o WHERE o.id = :id AND o.configSistema.tenantId = :tenantId AND o.ativo = true")
    Optional<Organizacao> findByIdAndTenantId(@Param("id") String id, @Param("tenantId") String tenantId);

}
