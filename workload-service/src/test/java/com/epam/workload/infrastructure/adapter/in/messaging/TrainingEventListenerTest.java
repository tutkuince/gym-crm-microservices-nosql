package com.epam.workload.infrastructure.adapter.in.messaging;

import com.epam.contracts.events.TrainingEvent;
import com.epam.workload.application.service.WorkloadApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingEventListenerTest {

    @Mock
    private WorkloadApplicationService service;
    @Mock
    private ProcessedEventStore processed;

    @InjectMocks
    private TrainingEventListener listener;

    @Test
    void onMessage_shouldCallService_whenEventNotProcessed() {
        // given
        UUID eventId = UUID.randomUUID();
        String txId = "tx-123";
        LocalDate date = LocalDate.of(2025, 9, 10);

        TrainingEvent event = mock(TrainingEvent.class);
        when(event.eventId()).thenReturn(eventId);
        when(event.trainerUsername()).thenReturn("trainer1");
        when(event.trainerFirstName()).thenReturn("Jane");
        when(event.trainerLastName()).thenReturn("Doe");
        when(event.isActive()).thenReturn(true);
        when(event.trainingDate()).thenReturn(date);
        when(event.trainingDurationMinutes()).thenReturn(45);
        when(event.actionType()).thenReturn("ADD");

        when(processed.isProcessed(eventId)).thenReturn(false);

        // when
        listener.onMessage(event, txId);

        // then
        verify(service).apply(
                "trainer1", "Jane", "Doe", true, date, 45, "ADD", txId
        );
        verifyNoMoreInteractions(service);
    }

    @Test
    void onMessage_shouldSkip_whenDuplicateEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        String txId = "tx-dup";

        TrainingEvent event = mock(TrainingEvent.class);
        when(event.eventId()).thenReturn(eventId);

        when(processed.isProcessed(eventId)).thenReturn(true);

        // when
        listener.onMessage(event, txId);

        // then
        verifyNoInteractions(service);
    }

    @Test
    void onMessage_shouldRethrow_whenServiceThrows() {
        // given
        UUID eventId = UUID.randomUUID();
        String txId = "tx-fail";
        LocalDate date = LocalDate.of(2025, 9, 10);

        TrainingEvent event = mock(TrainingEvent.class);
        when(event.eventId()).thenReturn(eventId);
        when(event.trainerUsername()).thenReturn("trainer1");
        when(event.trainerFirstName()).thenReturn("Jane");
        when(event.trainerLastName()).thenReturn("Doe");
        when(event.isActive()).thenReturn(true);
        when(event.trainingDate()).thenReturn(date);
        when(event.trainingDurationMinutes()).thenReturn(30);
        when(event.actionType()).thenReturn("DELETE");

        when(processed.isProcessed(eventId)).thenReturn(false);
        doThrow(new IllegalArgumentException("boom"))
                .when(service).apply(anyString(), anyString(), anyString(), anyBoolean(),
                        any(), anyInt(), anyString(), anyString());

        // then
        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(event, txId));

        verify(service).apply(
                "trainer1", "Jane", "Doe", true, date, 30, "DELETE", txId
        );
    }
}