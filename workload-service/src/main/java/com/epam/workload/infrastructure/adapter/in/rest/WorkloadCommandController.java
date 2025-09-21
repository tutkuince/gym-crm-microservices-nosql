package com.epam.workload.infrastructure.adapter.in.rest;

import com.epam.workload.application.dto.command.RecordTrainingCommand;
import com.epam.workload.domain.port.in.command.RecordTrainingCommandPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/workloads", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WorkloadCommandController {

    private final RecordTrainingCommandPort commandPort;

    @PostMapping
    public ResponseEntity<Void> record(@Valid @RequestBody RecordTrainingCommand command) {
        commandPort.handle(command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> cancel(@Valid @RequestBody RecordTrainingCommand command) {
        commandPort.handle(command);
        return ResponseEntity.ok().build();
    }
}
