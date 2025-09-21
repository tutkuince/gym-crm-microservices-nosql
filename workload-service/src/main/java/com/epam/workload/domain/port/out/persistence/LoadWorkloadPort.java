package com.epam.workload.domain.port.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;

import java.util.Optional;

public interface LoadWorkloadPort {
    Optional<TrainerWorkload> loadByUsername(String username);
    Optional<Integer> loadMonthlyMinutes(String username, int year, int month);
}
