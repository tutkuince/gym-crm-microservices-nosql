package com.epam.workload.infrastructure.adapter.out.persistence.mapper;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.MonthWorkDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.TrainerWorkloadDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.YearWorkDoc;

import java.time.YearMonth;
import java.util.*;

public class TrainerWorkloadMapper {

    private TrainerWorkloadMapper() {
    }

    public static TrainerWorkloadDoc toTrainerWorkloadDoc(TrainerWorkload trainerWorkload) {
        if (Objects.isNull(trainerWorkload)) {
            return null;
        }

        TrainerWorkloadDoc doc = new TrainerWorkloadDoc(
                trainerWorkload.getId().value(),
                trainerWorkload.getFirstName(),
                trainerWorkload.getLastName(),
                trainerWorkload.isActive()
        );

        Map<Integer, Map<Integer, Integer>> byYear = new TreeMap<>();
        for (Map.Entry<YearMonth, Integer> e : trainerWorkload.getMinutesByMonth().entrySet()) {
            int y = e.getKey().getYear();
            int m = e.getKey().getMonthValue();
            int min = e.getValue();
            byYear.computeIfAbsent(y, k -> new TreeMap<>()).merge(m, min, Integer::sum);
        }

        List<YearWorkDoc> years = new ArrayList<>();
        byYear.forEach((year, monthsMap) -> {
            var ydoc = new YearWorkDoc(year);
            monthsMap.forEach((month, minutes) -> ydoc.getMonths().add(new MonthWorkDoc(month, minutes)));
            years.add(ydoc);
        });

        doc.setYears(years);
        return doc;
    }

    public static TrainerWorkload toDomain(TrainerWorkloadDoc doc) {
        if (doc == null) return null;

        var agg = new TrainerWorkload(
                new TrainerId(doc.getUsername()),
                doc.getFirstName(),
                doc.getLastName(),
                doc.isActive()
        );

        List<YearWorkDoc> years = doc.getYears();
        if (Objects.nonNull(years)) {
            for (YearWorkDoc y : years) {
                if (Objects.isNull(y) || Objects.isNull(y.getMonths()))
                    continue;

                int year = y.getYear();

                for (MonthWorkDoc m : y.getMonths()) {
                    if (m == null) continue;
                    YearMonth ym = YearMonth.of(year, m.getMonth());
                    agg.getMinutesByMonth().merge(ym, m.getTotalMinutes(), Integer::sum);
                }
            }
        }
        return agg;
    }
}
