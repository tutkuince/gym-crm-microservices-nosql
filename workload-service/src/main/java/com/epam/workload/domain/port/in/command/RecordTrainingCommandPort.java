package com.epam.workload.domain.port.in.command;

import com.epam.workload.application.dto.command.RecordTrainingCommand;

public interface RecordTrainingCommandPort {
    void handle(RecordTrainingCommand command);
}
