package com.epam.gymcrm.infrastructure.messaging;

import com.epam.contracts.events.TrainingEvent;
import com.epam.contracts.headers.Headers;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrainingEventPublisher {
    private final KafkaTemplate<String, TrainingEvent> kafkaTemplate;

    @Value("${app.topics.training}")
    private String topic;

    public TrainingEventPublisher(KafkaTemplate<String, TrainingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TrainingEvent event) {
        var headers = new RecordHeaders()
                .add(Headers.TX_ID, event.transactionId().getBytes())
                .add(Headers.SCHEMA_VERSION, String.valueOf(event.schemaVersion()).getBytes());

        ProducerRecord<String, TrainingEvent> record = new ProducerRecord<>(topic, null, event.trainerUsername(), event, headers);

        kafkaTemplate.send(record);
    }
}
