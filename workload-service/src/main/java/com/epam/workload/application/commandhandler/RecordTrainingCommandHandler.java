package com.epam.workload.application.commandhandler;

import com.epam.workload.application.dto.command.RecordTrainingCommand;
import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.model.valueobject.TrainingMonth;
import com.epam.workload.domain.port.in.command.RecordTrainingCommandPort;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecordTrainingCommandHandler implements RecordTrainingCommandPort {
    private final LoadWorkloadPort loadPort;
    private final SaveWorkloadPort savePort;

    @Override
    @Transactional
    public void handle(RecordTrainingCommand c) {
        var id = new TrainerId(c.trainerUsername());
        var ym = YearMonth.from(c.trainingDate());
        var month = new TrainingMonth(ym);

        log.info("Processing training command: actionType={}, trainer={}, date={}, minutes={}",
                c.actionType(), c.trainerUsername(), c.trainingDate(), c.trainingDurationMinutes());

        int currentTotal = loadPort.loadMonthlyMinutes(id.value(), ym.getYear(), ym.getMonthValue())
                .orElse(0);

        log.debug("Current workload for trainer={} in {}: {} minutes",
                c.trainerUsername(), ym, currentTotal);

        var agg = new TrainerWorkload(id, c.trainerFirstName(), c.trainerLastName(), c.isActive());
        if (currentTotal > 0) {
            agg.record(month, currentTotal);
        }

        switch (c.actionType()) {
            case ADD -> {
                agg.record(month, c.trainingDurationMinutes());
                log.info("Added {} minutes for trainer={} in {}",
                        c.trainingDurationMinutes(), c.trainerUsername(), ym);
            }
            case DELETE -> {
                agg.delete(month, c.trainingDurationMinutes());
                log.info("Deleted {} minutes for trainer={} in {}",
                        c.trainingDurationMinutes(), c.trainerUsername(), ym);
            }
            default -> log.warn("Unsupported actionType={} for trainer={}", c.actionType(), c.trainerUsername());
        }

        var newTotal = agg.getMinutesByMonth().getOrDefault(ym, 0);
        log.debug("New workload total for trainer={} in {}: {} minutes",
                c.trainerUsername(), ym, newTotal);

        if (newTotal <= 0) {
            savePort.deleteMonth(id.value(), ym.getYear(), ym.getMonthValue());
            log.info("Deleted workload record for trainer={} in {}", c.trainerUsername(), ym);
        } else {
            savePort.upsertMonth(id.value(), ym.getYear(), ym.getMonthValue(),
                    c.trainerFirstName(), c.trainerLastName(), c.isActive(), newTotal);
            log.info("Upserted workload record for trainer={} in {} with totalMinutes={}",
                    c.trainerUsername(), ym, newTotal);
        }

        log.info("Finished processing training command for trainer={} in {}", c.trainerUsername(), ym);
    }
}
