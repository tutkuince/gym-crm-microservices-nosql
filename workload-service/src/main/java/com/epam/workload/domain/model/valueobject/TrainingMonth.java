package com.epam.workload.domain.model.valueobject;

import java.time.YearMonth;
import java.util.Objects;

public record TrainingMonth(YearMonth value) {
    public TrainingMonth {
        if (Objects.isNull(value)) {
            throw new IllegalStateException("YearMonth required");
        }
    }

    public int year() {
        return value.getYear();
    }

    public int month() {
        return value.getMonthValue();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
