package com.epam.workload.infrastructure.adapter.in.rest;

import com.epam.workload.application.dto.command.RecordTrainingCommand;
import com.epam.workload.domain.port.in.command.RecordTrainingCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkloadCommandControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecordTrainingCommandPort commandPort;

    @InjectMocks
    private WorkloadCommandController controller;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void record_shouldReturn200_whenValidCommand() throws Exception {
        doNothing().when(commandPort).handle(any(RecordTrainingCommand.class));

        String json = """
                {
                  "trainerUsername": "ali.veli",
                  "trainerFirstName": "Ali",
                  "trainerLastName": "Veli",
                  "isActive": true,
                  "trainingDate": "2025-09-16",
                  "trainingDurationMinutes": 45,
                  "actionType": "ADD"
                }
                """;

        mockMvc.perform(post("/api/v1/workloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(commandPort).handle(any(RecordTrainingCommand.class));
    }

    @Test
    void cancel_shouldReturn200_whenValidCommand() throws Exception {
        doNothing().when(commandPort).handle(any(RecordTrainingCommand.class));

        String json = """
                {
                  "trainerUsername": "ali.veli",
                  "trainerFirstName": "Ali",
                  "trainerLastName": "Veli",
                  "isActive": true,
                  "trainingDate": "2025-09-16",
                  "trainingDurationMinutes": 30,
                  "actionType": "DELETE"
                }
                """;

        mockMvc.perform(delete("/api/v1/workloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(commandPort).handle(any(RecordTrainingCommand.class));
    }

    @Test
    void record_shouldReturn400_whenInvalidPayload() throws Exception {
        // missing mandatory fields -> validation fail
        String invalidJson = """
                {
                  "trainerUsername": "",
                  "trainerFirstName": "",
                  "trainerLastName": "",
                  "isActive": null,
                  "trainingDate": null,
                  "trainingDurationMinutes": 0,
                  "actionType": null
                }
                """;

        mockMvc.perform(post("/api/v1/workloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}