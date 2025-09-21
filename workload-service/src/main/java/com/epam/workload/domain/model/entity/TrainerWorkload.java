package com.epam.workload.domain.model.entity;

import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.model.valueobject.TrainingMonth;
import lombok.Getter;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class TrainerWorkload {
    private final TrainerId id;
    private String firstName;
    private String lastName;
    private boolean active;

    private final Map<YearMonth, Integer> minutesByMonth = new HashMap<>();

    public TrainerWorkload(TrainerId id, String firstName, String lastName, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.active = active;
    }

    public void record(TrainingMonth trainingMonth, int minutes) {
        requireMonth(trainingMonth);
        requirePositive(minutes, "minutes");
        minutesByMonth.merge(trainingMonth.value(), minutes, Integer::sum);
    }

    public void delete(TrainingMonth month, int minutes) {
        requireMonth(month);
        requirePositive(minutes, "minutes");
        minutesByMonth.merge(month.value(), -minutes, Integer::sum);
        minutesByMonth.computeIfPresent(month.value(), (k, v) -> v <= 0 ? null : v);
    }

    // Guards
    private static String requireText(String value, String name) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalArgumentException(name + " required");
        }
        return value;
    }

    private static void requireMonth(TrainingMonth trainingMonth) {
        if (Objects.isNull(trainingMonth)) {
            throw new IllegalArgumentException("month required");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
