package com.openisle.repository;

import com.openisle.model.TreeholeInterventionCase;
import com.openisle.model.TreeholeInterventionStatus;
import com.openisle.model.TreeholeRiskLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreeholeInterventionCaseRepository
  extends JpaRepository<TreeholeInterventionCase, Long> {
  Optional<TreeholeInterventionCase> findByPost_Id(Long postId);

  boolean existsByPost_Id(Long postId);

  long countByStatus(TreeholeInterventionStatus status);

  void deleteByPost_Id(Long postId);

  @Query(
    "SELECT c FROM TreeholeInterventionCase c JOIN c.post p " +
    "WHERE (:riskLevel IS NULL OR p.treeholeRiskLevel = :riskLevel) " +
    "AND (:status IS NULL OR c.status = :status) " +
    "ORDER BY c.updatedAt DESC, c.createdAt DESC"
  )
  List<TreeholeInterventionCase> findRiskCases(
    @Param("riskLevel") TreeholeRiskLevel riskLevel,
    @Param("status") TreeholeInterventionStatus status,
    Pageable pageable
  );
}
