package com.epam.workload.infrastructure.adapter.in.rest;

import com.epam.workload.application.dto.query.MonthlySummaryDto;
import com.epam.workload.application.dto.query.TrainerWorkloadResponseDto;
import com.epam.workload.domain.port.in.query.GetWorkloadQueryPort;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WorkloadQueryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetWorkloadQueryPort queryPort;

    @InjectMocks
    private WorkloadQueryController controller;

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
    void get_shouldReturnTrainerWorkloadResponse() throws Exception {
        String username = "trainer1";
        var dto = new TrainerWorkloadResponseDto(
                username,
                "Ada",
                "Lovelace",
                true,
                List.of(new MonthlySummaryDto(2025, 9, 60))
        );

        when(queryPort.getByUsername(username)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/workloads/{username}", username)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.trainerUsername").value("trainer1"))
                .andExpect(jsonPath("$.trainerFirstName").value("Ada"))
                .andExpect(jsonPath("$.trainerLastName").value("Lovelace"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.months[0].year").value(2025))
                .andExpect(jsonPath("$.months[0].month").value(9))
                .andExpect(jsonPath("$.months[0].totalMinutes").value(60));

        verify(queryPort).getByUsername(username);
    }

    @Test
    void getMonthly_shouldReturnIntegerMinutes() throws Exception {
        String username = "trainer2";
        int year = 2025, month = 8, minutes = 90;

        when(queryPort.getMonthlyMinutes(username, year, month)).thenReturn(minutes);

        mockMvc.perform(get("/api/v1/workloads/{username}/months/{year}/{month}", username, year, month)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("90"));

        verify(queryPort).getMonthlyMinutes(username, year, month);
    }

    @Test
    void getMonthly_shouldReturnZero_whenPortReturnsZero() throws Exception {
        when(queryPort.getMonthlyMinutes(anyString(), anyInt(), anyInt())).thenReturn(0);

        mockMvc.perform(get("/api/v1/workloads/{username}/months/{year}/{month}",
                        "someone", 2024, 12))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}