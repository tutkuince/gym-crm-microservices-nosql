package com.epam.workload.domain.port.in.query;

import com.epam.workload.application.dto.query.TrainerWorkloadResponseDto;

public interface GetWorkloadQueryPort {
    TrainerWorkloadResponseDto getByUsername(String username);
    int getMonthlyMinutes(String username, int year, int month);
}
