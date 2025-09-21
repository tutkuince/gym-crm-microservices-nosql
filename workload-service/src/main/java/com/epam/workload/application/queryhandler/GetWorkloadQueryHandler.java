package com.epam.workload.application.queryhandler;

import com.epam.workload.application.dto.query.MonthlySummaryDto;
import com.epam.workload.application.dto.query.TrainerWorkloadResponseDto;
import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.port.in.query.GetWorkloadQueryPort;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RequiredArgsConstructor
@Service
public class GetWorkloadQueryHandler implements GetWorkloadQueryPort {

    private final LoadWorkloadPort loadPort;

    @Override
    public TrainerWorkloadResponseDto getByUsername(String username) {
        log.info("Fetching workload for trainerUsername={}", username);

        TrainerWorkload trainerWorkload = loadPort.loadByUsername(username).orElseThrow(() -> {
            log.warn("Trainer workload not found for username={}", username);
            return new NoSuchElementException("trainer not found: " + username);
        });

        List<MonthlySummaryDto> months = new ArrayList<>();
        trainerWorkload.getMinutesByMonth().forEach((yearMonth, total) -> {
            months.add(new MonthlySummaryDto(yearMonth.getYear(), yearMonth.getMonthValue(), total));
            log.debug("Workload month entry: year={}, month={}, minutes={}",
                    yearMonth.getYear(), yearMonth.getMonthValue(), total);
        });

        months.sort(Comparator.comparingInt(MonthlySummaryDto::year)
                .thenComparing(MonthlySummaryDto::month));

        log.info("Returning workload summary for trainer={}, totalMonths={}",
                username, months.size());

        return new TrainerWorkloadResponseDto(
                trainerWorkload.getId().value(),
                trainerWorkload.getFirstName(),
                trainerWorkload.getLastName(),
                trainerWorkload.isActive(),
                months
        );
    }

    @Override
    public int getMonthlyMinutes(String username, int year, int month) {
        log.info("Fetching monthly workload for trainer={}, year={}, month={}", username, year, month);

        int minutes = loadPort.loadMonthlyMinutes(username, year, month).orElse(0);

        log.debug("Trainer={}, year={}, month={}, totalMinutes={}", username, year, month, minutes);

        return minutes;
    }
}
