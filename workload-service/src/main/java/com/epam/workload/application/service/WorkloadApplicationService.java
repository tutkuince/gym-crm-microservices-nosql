package com.epam.workload.application.service;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.model.valueobject.TrainingMonth;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class WorkloadApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorkloadApplicationService.class);

    private final LoadWorkloadPort loadPort;
    private final SaveWorkloadPort savePort;

    @Transactional
    public void apply(String trainerUsername,
                      String trainerFirstName,
                      String trainerLastName,
                      boolean isActive,
                      LocalDate trainingDate,
                      int minutes,
                      String actionType,
                      String txId) {

        YearMonth ym = YearMonth.from(trainingDate);
        int year = ym.getYear();
        int month = ym.getMonthValue();

        int current = loadPort.loadMonthlyMinutes(trainerUsername, year, month).orElse(0);

        if ("ADD".equalsIgnoreCase(actionType)) {
            int updated = current + Math.max(0, minutes);
            savePort.upsertMonth(trainerUsername, year, month,
                    trainerFirstName, trainerLastName, isActive, updated);

            log.info("[txId={}] workload ADD: user={}, {}-{}, +{} => total={}",
                    txId, trainerUsername, year, month, minutes, updated);

        } else if ("DELETE".equalsIgnoreCase(actionType)) {
            int updated = Math.max(0, current - Math.max(0, minutes));
            if (updated == 0) {
                // if it 0, delete the record
                savePort.deleteMonth(trainerUsername, year, month);
                log.info("[txId={}] workload DELETE: user={}, {}-{}, -{} => total=0 (deleted)",
                        txId, trainerUsername, year, month, minutes);
            } else {
                savePort.upsertMonth(trainerUsername, year, month,
                        trainerFirstName, trainerLastName, isActive, updated);
                log.info("[txId={}] workload DELETE: user={}, {}-{}, -{} => total={}",
                        txId, trainerUsername, year, month, minutes, updated);
            }
        } else {
            throw new IllegalArgumentException("Unsupported actionType: " + actionType);
        }
    }
}
