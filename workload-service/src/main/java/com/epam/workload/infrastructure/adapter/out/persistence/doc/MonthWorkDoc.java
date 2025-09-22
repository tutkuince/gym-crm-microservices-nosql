package com.epam.workload.infrastructure.adapter.out.persistence.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthWorkDoc {
    private int month;        // 1..12
    private int totalMinutes; // number type
}
