package com.epam.workload.infrastructure.adapter.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.model.valueobject.TrainingMonth;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkloadJpaAdapter implements LoadWorkloadPort, SaveWorkloadPort {

    private final WorkloadRepository workloadRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TrainerWorkload> loadByUsername(String username) {
        log.info("Loading workload aggregate for trainerUsername={}", username);

        var rows = workloadRepository.findAllByUsername(username);
        if (rows.isEmpty()) {
            log.warn("No workload records found for trainer={}", username);
            return Optional.empty();
        }

        var first = rows.getFirst();
        var agg = new TrainerWorkload(new TrainerId(username),
                first.getFirstName(), first.getLastName(), first.isActive());

        rows.forEach(r -> {
            var ym = YearMonth.of(r.getWorkYear(), r.getWorkMonth());
            agg.record(new TrainingMonth(ym), r.getTotalMinutes());
            log.debug("Loaded row for trainer={}, year={}, month={}, minutes={}",
                    username, r.getWorkYear(), r.getWorkMonth(), r.getTotalMinutes());
        });

        log.info("Finished loading workload aggregate for trainer={}, totalMonths={}", username, rows.size());
        return Optional.of(agg);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> loadMonthlyMinutes(String username, int year, int month) {
        log.info("Loading monthly workload for trainer={}, year={}, month={}", username, year, month);

        var result = workloadRepository.findByUsernameAndWorkYearAndWorkMonth(username, year, month)
                .map(entity -> {
                    log.debug("Found monthly workload entity: trainer={}, year={}, month={}, minutes={}",
                            username, year, month, entity.getTotalMinutes());
                    return entity.getTotalMinutes();
                });

        if (result.isEmpty()) {
            log.warn("No monthly workload found for trainer={}, year={}, month={}", username, year, month);
        }

        return result;
    }

    @Override
    @Transactional
    public void upsertMonth(String username, int year, int month,
                            String firstName, String lastName, boolean active,
                            int totalMinutes) {

        log.info("Upserting workload record: trainer={}, year={}, month={}, minutes={}",
                username, year, month, totalMinutes);

        var existing = workloadRepository.findByUsernameAndWorkYearAndWorkMonth(username, year, month);
        if (existing.isPresent()) {
            var e = existing.get();
            e.setFirstName(firstName);
            e.setLastName(lastName);
            e.setActive(active);
            e.setTotalMinutes(totalMinutes);
            log.debug("Updated existing workload entity for trainer={}, year={}, month={}", username, year, month);
        } else {
            var e = new TrainerMonthlyWorkloadEntity(username, year, month, firstName, lastName, active, totalMinutes);
            workloadRepository.save(e);
            log.debug("Inserted new workload entity for trainer={}, year={}, month={}", username, year, month);
        }
    }

    @Transactional
    public void deleteMonth(String username, int year, int month) {
        log.info("Deleting workload record for trainer={}, year={}, month={}", username, year, month);
        workloadRepository.deleteByUsernameAndWorkYearAndWorkMonth(username, year, month);
    }
}
