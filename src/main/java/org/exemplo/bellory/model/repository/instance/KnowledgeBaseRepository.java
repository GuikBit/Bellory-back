package org.exemplo.bellory.model.repository.instance;


import org.exemplo.bellory.model.entity.instancia.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByInstanceId(Long instanceId);

    void deleteByInstanceId(Long instanceId);
}
