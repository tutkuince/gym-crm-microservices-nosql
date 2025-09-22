package com.epam.workload.infrastructure.adapter.out.persistence;

import java.time.YearMonth;

public interface UpsertWorkloadPort {
    void upsertMinutes(
            String username,
            String firstName,
            String lastName,
            boolean active,
            YearMonth ym,
            int deltaMinutes
    );
}
