package com.epam.workload.application.service;

import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadApplicationServiceTest {

    @Mock
    private LoadWorkloadPort loadPort;
    @Mock
    private SaveWorkloadPort savePort;

    @InjectMocks
    private WorkloadApplicationService service;

    private static final String USER = "trainer1";
    private static final String FIRST = "Jane";
    private static final String LAST = "Doe";
    private static final boolean ACTIVE = true;
    private static final LocalDate DATE = LocalDate.of(2025, 9, 10);
    private static final String TX = "tx-123";

    @Test
    void apply_add_shouldUpsert_whenNoExisting() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                service.apply(USER, FIRST, LAST, ACTIVE, DATE, 30, "ADD", TX)
        );

        verify(savePort).upsertMonth(USER, 2025, 9, FIRST, LAST, ACTIVE, 30);
        verifyNoMoreInteractions(savePort);
    }

    @Test
    void apply_add_shouldAccumulate_whenExisting() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(40));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, 20, "ADD", TX);

        verify(savePort).upsertMonth(USER, 2025, 9, FIRST, LAST, ACTIVE, 60);
        verifyNoMoreInteractions(savePort);
    }

    @Test
    void apply_add_shouldIgnoreNegativeMinutes() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(10));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, -50, "ADD", TX);

        verify(savePort).upsertMonth(USER, 2025, 9, FIRST, LAST, ACTIVE, 10);
        verifyNoMoreInteractions(savePort);
    }

    @Test
    void apply_delete_shouldDecrease_andUpsert_whenNotZero() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(90));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, 30, "DELETE", TX);

        verify(savePort).upsertMonth(USER, 2025, 9, FIRST, LAST, ACTIVE, 60);
        verify(savePort, never()).deleteMonth(anyString(), anyInt(), anyInt());
    }

    @Test
    void apply_delete_shouldDelete_whenReachesZeroExactly() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(45));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, 45, "DELETE", TX);

        verify(savePort).deleteMonth(USER, 2025, 9);
        verify(savePort, never()).upsertMonth(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void apply_delete_shouldClampToZero_andDelete_whenOverSubtract() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(20));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, 100, "DELETE", TX);

        verify(savePort).deleteMonth(USER, 2025, 9);
        verify(savePort, never()).upsertMonth(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void apply_delete_shouldIgnoreNegativeMinutes_andUpsertSameTotal() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(10));

        service.apply(USER, FIRST, LAST, ACTIVE, DATE, -5, "DELETE", TX);

        verify(savePort).upsertMonth(USER, 2025, 9, FIRST, LAST, ACTIVE, 10);
        verify(savePort, never()).deleteMonth(anyString(), anyInt(), anyInt());
    }

    @Test
    void apply_shouldThrow_whenUnsupportedAction() {
        when(loadPort.loadMonthlyMinutes(USER, 2025, 9)).thenReturn(Optional.of(10));

        assertThrows(IllegalArgumentException.class, () ->
                service.apply(USER, FIRST, LAST, ACTIVE, DATE, 15, "UPSERT?", TX)
        );

        verifyNoInteractions(savePort);
    }
}