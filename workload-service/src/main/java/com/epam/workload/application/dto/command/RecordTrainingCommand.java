package com.epam.workload.application.dto.command;

import com.epam.workload.domain.model.valueobject.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record RecordTrainingCommand(
        @NotBlank(message = "Trainer username is required")
        String trainerUsername,
        @NotBlank(message = "Trainer first name is required")
        String trainerFirstName,
        @NotBlank(message = "Trainer last name is required")
        String trainerLastName,
        @NotNull(message = "Trainer status (isActive) must be provided")
        Boolean isActive,
        @NotNull(message = "Training date is required")
        LocalDate trainingDate,
        @Positive(message = "Training duration must be greater than 0 minutes")
        int trainingDurationMinutes,
        @NotNull(message = "Action type (ADD or DELETE) is required")
        ActionType actionType
) {
}
