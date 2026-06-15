package com.openisle.repository;

import com.openisle.model.TreeholeInterventionAction;
import com.openisle.model.TreeholeInterventionRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreeholeInterventionRecordRepository
  extends JpaRepository<TreeholeInterventionRecord, Long> {
  List<TreeholeInterventionRecord> findByInterventionCase_IdOrderByCreatedAtAsc(Long caseId);

  long countByPost_IdAndAction(Long postId, TreeholeInterventionAction action);

  void deleteByPost_Id(Long postId);
}
