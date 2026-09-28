package com.portal.procucev.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portal.procucev.model.DailyRfqReportLog;

@Repository
public interface DailyRfqReportLogDao extends JpaRepository<DailyRfqReportLog, Long> {

    Optional<DailyRfqReportLog> findByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

    List<DailyRfqReportLog> findAllByOrderByReportDateDesc();
}
