package com.epam.gymcrm.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic trainerTrainingTopic(@Value("${app.topics.training}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
