package com.gianmdp03.cuando_llega_pro.domain.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ErrorReportRepository extends JpaRepository<ErrorReportEntity, UUID> {

    Page<ErrorReportEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
