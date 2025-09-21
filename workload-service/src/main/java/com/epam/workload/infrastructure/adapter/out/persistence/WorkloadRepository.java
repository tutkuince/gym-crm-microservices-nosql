package com.epam.workload.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkloadRepository extends JpaRepository<TrainerMonthlyWorkloadEntity, Long> {

    List<TrainerMonthlyWorkloadEntity> findAllByUsername(String username);

    Optional<TrainerMonthlyWorkloadEntity> findByUsernameAndWorkYearAndWorkMonth(String username, int workYear, int workMonth);

    void deleteByUsernameAndWorkYearAndWorkMonth(String username, int workYear, int workMonth);
}
