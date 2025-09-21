package com.epam.workload.domain.model.valueobject;

import java.util.Locale;
import java.util.Objects;

public record TrainerId(String username) {
    public TrainerId {
        if (Objects.isNull(username) || username.isBlank()) {
            throw new IllegalStateException("Username required");
        }
        username = username.toLowerCase(Locale.ROOT);
    }

    public String value() {
        return username;
    }
}
