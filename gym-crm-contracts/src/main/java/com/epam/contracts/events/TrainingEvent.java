package com.epam.contracts.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TrainingEvent(
        @NotNull
        UUID eventId,
        @NotBlank
        String transactionId,
        @NotNull
        OffsetDateTime occurredAt,
        @NotBlank
        String trainerUsername,
        @NotBlank
        String trainerFirstName,
        @NotBlank
        String trainerLastName,
        @NotNull
        Boolean isActive,
        @NotNull
        @Pattern(regexp = "ADD|DELETE")
        String actionType,
        @NotNull
        LocalDate trainingDate,
        @Positive
        int trainingDurationMinutes,
        @NotNull
        Integer schemaVersion
) {
}
