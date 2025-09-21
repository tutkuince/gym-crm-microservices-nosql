package com.epam.gymcrm.api.controller;

import com.epam.gymcrm.api.payload.request.AddTrainingRequest;
import com.epam.gymcrm.domain.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/trainings")
@Tag(name = "Training", description = "API for managing training sessions")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Operation(
            summary = "Create a training session",
            description = "Creates a new training session for a trainee with a specific trainer, date, and duration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Training successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @PostMapping
    public ResponseEntity<Void> createTraining(@RequestBody @Valid AddTrainingRequest request) {
        trainingService.addTraining(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Cancel training by trainer and date",
            description = "Cancels a training session for the given trainer at the specified training date and time. " +
                    "If no training is found, the operation is idempotent and still returns 204."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Training cancelled successfully or already not present"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden – trainer can only cancel own trainings"),
            @ApiResponse(responseCode = "404", description = "Training not found (if implemented to throw)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/delete-by")
    public ResponseEntity<Void> cancel(@RequestParam String trainerUsername, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime trainingDate) {
        trainingService.cancelTraining(trainerUsername, trainingDate);
        return ResponseEntity.noContent().build();
    }
}
