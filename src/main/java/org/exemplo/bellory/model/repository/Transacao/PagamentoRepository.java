package org.exemplo.bellory.model.repository.Transacao;

import org.exemplo.bellory.model.entity.agendamento.Agendamento;
import org.exemplo.bellory.model.entity.pagamento.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByClienteIdOrderByDtCriacaoDesc(Long clienteId);

    List<Pagamento> findByCobrancaId(Long cobrancaId);

    @Query("SELECT p FROM Pagamento p WHERE p.cobranca.id = :cobrancaId AND p.statusPagamento = 'CONFIRMADO'")
    List<Pagamento> findPagamentosConfirmadosByCobranca(@Param("cobrancaId") Long cobrancaId);
}
