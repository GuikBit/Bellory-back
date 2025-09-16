package org.exemplo.bellory.model.repository.tenant;

import org.exemplo.bellory.model.entity.tenant.Page;
import org.exemplo.bellory.model.entity.tenant.PageComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade PageComponent.
 * Fornece métodos para buscar e manipular componentes de página.
 */
@Repository
public interface PageComponentRepository extends JpaRepository<PageComponent, Long> {

    /**
     * Busca todos os componentes de uma página ordenados por orderIndex.
     * @param page A página
     * @return Lista de componentes ordenados
     */
    List<PageComponent> findByPageOrderByOrderIndexAsc(Page page);

    /**
     * Busca todos os componentes ativos de uma página ordenados por orderIndex.
     * @param page A página
     * @return Lista de componentes ativos ordenados
     */
    List<PageComponent> findByPageAndActiveTrueOrderByOrderIndexAsc(Page page);

    /**
     * Busca componentes por página e tipo.
     * @param page A página
     * @param type O tipo do componente
     * @return Lista de componentes do tipo especificado
     */
    List<PageComponent> findByPageAndType(Page page, String type);

    /**
     * Busca componentes ativos por página e tipo.
     * @param page A página
     * @param type O tipo do componente
     * @return Lista de componentes ativos do tipo especificado
     */
    List<PageComponent> findByPageAndTypeAndActiveTrue(Page page, String type);

    /**
     * Busca componentes por ID da página ordenados por orderIndex.
     * @param pageId O ID da página
     * @return Lista de componentes ordenados
     */
    @Query("SELECT pc FROM PageComponent pc WHERE pc.page.id = :pageId AND pc.active = true ORDER BY pc.orderIndex ASC")
    List<PageComponent> findByPageIdOrderByOrderIndexAsc(@Param("pageId") Long pageId);

    /**
     * Busca um componente específico de uma página pelo orderIndex.
     * @param page A página
     * @param orderIndex O índice de ordem
     * @return Optional com o componente encontrado
     */
    Optional<PageComponent> findByPageAndOrderIndex(Page page, Integer orderIndex);

    /**
     * Conta o número de componentes de uma página.
     * @param page A página
     * @return Número de componentes
     */
    long countByPage(Page page);

    /**
     * Conta o número de componentes ativos de uma página.
     * @param page A página
     * @return Número de componentes ativos
     */
    long countByPageAndActiveTrue(Page page);

    /**
     * Busca o maior orderIndex de uma página.
     * Útil para adicionar novos componentes no final.
     * @param page A página
     * @return O maior orderIndex ou null se não houver componentes
     */
    @Query("SELECT MAX(pc.orderIndex) FROM PageComponent pc WHERE pc.page = :page")
    Optional<Integer> findMaxOrderIndexByPage(@Param("page") Page page);

    /**
     * Atualiza o orderIndex de componentes após uma posição específica.
     * Útil para reordenação de componentes.
     * @param page A página
     * @param fromIndex A partir de qual índice atualizar
     * @param increment O incremento a aplicar (pode ser negativo)
     */
    @Modifying
    @Query("UPDATE PageComponent pc SET pc.orderIndex = pc.orderIndex + :increment WHERE pc.page = :page AND pc.orderIndex >= :fromIndex")
    void updateOrderIndexFromPosition(@Param("page") Page page, @Param("fromIndex") Integer fromIndex, @Param("increment") Integer increment);

    /**
     * Remove componentes de uma página por tipo.
     * @param page A página
     * @param type O tipo dos componentes a remover
     */
    @Modifying
    @Query("DELETE FROM PageComponent pc WHERE pc.page = :page AND pc.type = :type")
    void deleteByPageAndType(@Param("page") Page page, @Param("type") String type);

    /**
     * Busca componentes por múltiplos tipos.
     * @param page A página
     * @param types Lista de tipos
     * @return Lista de componentes dos tipos especificados
     */
    @Query("SELECT pc FROM PageComponent pc WHERE pc.page = :page AND pc.type IN :types AND pc.active = true ORDER BY pc.orderIndex ASC")
    List<PageComponent> findByPageAndTypeInOrderByOrderIndexAsc(@Param("page") Page page, @Param("types") List<String> types);
}
