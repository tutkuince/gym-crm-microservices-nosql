package com.epam.workload.infrastructure.adapter.in.rest;

import com.epam.workload.application.dto.query.TrainerWorkloadResponseDto;
import com.epam.workload.domain.port.in.query.GetWorkloadQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/workloads", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WorkloadQueryController {

    private final GetWorkloadQueryPort queryPort;

    @GetMapping("/{username}")
    public ResponseEntity<TrainerWorkloadResponseDto> get(@PathVariable String username) {
        return ResponseEntity.ok(queryPort.getByUsername(username));
    }

    @GetMapping("/{username}/months/{year}/{month}")
    public ResponseEntity<Integer> getMonthly(@PathVariable String username,
                                              @PathVariable int year,
                                              @PathVariable int month) {
        return ResponseEntity.ok(queryPort.getMonthlyMinutes(username, year, month));
    }
}
