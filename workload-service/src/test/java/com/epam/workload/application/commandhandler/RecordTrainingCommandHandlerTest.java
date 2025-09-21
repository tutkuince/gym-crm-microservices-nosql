package com.epam.workload.application.commandhandler;

import com.epam.workload.application.dto.command.RecordTrainingCommand;
import com.epam.workload.domain.model.valueobject.ActionType;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordTrainingCommandHandlerTest {

    @Mock
    LoadWorkloadPort loadPort;

    @Mock
    SaveWorkloadPort savePort;

    RecordTrainingCommandHandler handler;

    private final String trainerUsername = "tutku.trainer";
    private final String firstName = "Tutku";
    private final String lastName = "K.";
    private final boolean isActive = true;
    private final LocalDateTime trainingDate = LocalDateTime.of(2025, 7, 22, 10, 0);

    private RecordTrainingCommand cmd(int minutes, ActionType action) {
        return new RecordTrainingCommand(
                trainerUsername,
                firstName,
                lastName,
                isActive,
                trainingDate.toLocalDate(),
                minutes,
                action
        );
    }

    @BeforeEach
    void setUp() {
        handler = new RecordTrainingCommandHandler(loadPort, savePort);
    }

    @Test
    void add_whenNoExistingTotal_shouldUpsertWithGivenMinutes() {
        // given
        when(loadPort.loadMonthlyMinutes(eq(trainerUsername), eq(2025), eq(7)))
                .thenReturn(Optional.empty());
        var c = cmd(60, ActionType.ADD);

        // when
        handler.handle(c);

        // then
        verify(savePort).upsertMonth(trainerUsername, 2025, 7, firstName, lastName, isActive, 60);
        verify(savePort, never()).deleteMonth(anyString(), anyInt(), anyInt());
    }

    @Test
    void add_whenExistingTotal_shouldAccumulateAndUpsert() {
        // given
        when(loadPort.loadMonthlyMinutes(trainerUsername, 2025, 7))
                .thenReturn(Optional.of(90));
        var c = cmd(30, ActionType.ADD);

        // when
        handler.handle(c);

        // then
        verify(savePort).upsertMonth(trainerUsername, 2025, 7, firstName, lastName, isActive, 120);
        verify(savePort, never()).deleteMonth(anyString(), anyInt(), anyInt());
    }

    @Test
    void delete_whenExistingBecomesZero_shouldDeleteMonth() {
        // given
        when(loadPort.loadMonthlyMinutes(trainerUsername, 2025, 7))
                .thenReturn(Optional.of(30));
        var c = cmd(30, ActionType.DELETE);

        // when
        handler.handle(c);

        // then
        verify(savePort).deleteMonth(trainerUsername, 2025, 7);
        verify(savePort, never()).upsertMonth(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void delete_whenExistingStaysPositive_shouldUpsertWithNewTotal() {
        // given
        when(loadPort.loadMonthlyMinutes(trainerUsername, 2025, 7))
                .thenReturn(Optional.of(60));
        var c = cmd(30, ActionType.DELETE);

        // when
        handler.handle(c);

        // then
        verify(savePort).upsertMonth(trainerUsername, 2025, 7, firstName, lastName, isActive, 30);
        verify(savePort, never()).deleteMonth(anyString(), anyInt(), anyInt());
    }

    @Test
    void usesYearMonthFromCommandTrainingDate() {
        ArgumentCaptor<Integer> yearCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> monthCap = ArgumentCaptor.forClass(Integer.class);

        when(loadPort.loadMonthlyMinutes(anyString(), anyInt(), anyInt())).thenReturn(Optional.empty());
        var c = cmd(10, ActionType.ADD);

        handler.handle(c);

        verify(savePort).upsertMonth(eq(trainerUsername), yearCap.capture(), monthCap.capture(),
                eq(firstName), eq(lastName), eq(isActive), eq(10));

        YearMonth expected = YearMonth.from(trainingDate);
        org.assertj.core.api.Assertions.assertThat(yearCap.getValue()).isEqualTo(expected.getYear());
        org.assertj.core.api.Assertions.assertThat(monthCap.getValue()).isEqualTo(expected.getMonthValue());
    }
}