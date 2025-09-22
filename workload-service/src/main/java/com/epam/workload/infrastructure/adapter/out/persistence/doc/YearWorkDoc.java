package com.epam.workload.infrastructure.adapter.out.persistence.doc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YearWorkDoc {
    private int year;
    private List<MonthWorkDoc> months = new ArrayList<>();
}
