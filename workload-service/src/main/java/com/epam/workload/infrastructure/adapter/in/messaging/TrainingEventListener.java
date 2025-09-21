package com.epam.workload.infrastructure.adapter.in.messaging;

import com.epam.contracts.events.TrainingEvent;
import com.epam.contracts.headers.Headers;
import com.epam.workload.application.service.WorkloadApplicationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrainingEventListener {

    private static final Logger log = LoggerFactory.getLogger(TrainingEventListener.class);

    private final WorkloadApplicationService service;
    private final ProcessedEventStore processed;

    @KafkaListener(topics = "${app.topics.training}")
    public void onMessage(TrainingEvent event,
                          @Header(Headers.TX_ID) String txId) {

        if (processed.isProcessed(event.eventId())) {
            log.info("[txId={}] duplicate event skipped: eventId={}", txId, event.eventId());
            return;
        }

        try {
            service.apply(
                    event.trainerUsername(),
                    event.trainerFirstName(),
                    event.trainerLastName(),
                    event.isActive(),
                    event.trainingDate(),
                    event.trainingDurationMinutes(),
                    event.actionType(),
                    txId
            );

            log.info("[txId={}] CONSUMED {}: eventId={}, key={}, date={}, minutes={}",
                    txId, event.actionType(), event.eventId(),
                    event.trainerUsername(), event.trainingDate(), event.trainingDurationMinutes());

        } catch (Exception e) {
            log.error("[txId={}] processing failed: eventId={}, reason={}",
                    txId, event.eventId(), e.toString());
            throw e;
        }
    }
}
