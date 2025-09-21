package com.epam.workload.application.dto.query;

import java.util.List;

public record TrainerWorkloadResponseDto(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        boolean active,
        List<MonthlySummaryDto> months
) {
}
