package com.epam.workload.infrastructure.adapter.out.persistence.doc;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YearWorkDoc {
    private int year;
    private List<MonthWorkDoc> months = new ArrayList<>();

    @Builder
    public YearWorkDoc(int year) {
        this.year = year;
    }
}
