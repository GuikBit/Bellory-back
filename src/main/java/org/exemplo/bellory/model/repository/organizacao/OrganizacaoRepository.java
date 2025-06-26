package org.exemplo.bellory.model.repository.organizacao;

import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    Optional<Organizacao> findByNome(String nome);
}