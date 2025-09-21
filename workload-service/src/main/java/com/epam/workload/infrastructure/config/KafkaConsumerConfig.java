package com.epam.workload.infrastructure.config;

import com.epam.contracts.events.TrainingEvent;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, TrainingEvent> template,
            @Value("${app.topics.training-dlq}") String dlqTopic) {
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(dlqTopic, record.partition()));
    }

    @Bean
    DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
        var backoff = new FixedBackOff(1000L, 3L); // 3 retry
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(
                jakarta.validation.ValidationException.class,
                IllegalArgumentException.class
        );
        return handler;
    }

}
