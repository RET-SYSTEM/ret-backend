package me.cbhud.ret.repository;

import me.cbhud.ret.entity.SpendingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpendingReportRepository extends JpaRepository<SpendingReport, Long> {

    List<SpendingReport> findAllByOrderByCreatedAtDesc();
}
