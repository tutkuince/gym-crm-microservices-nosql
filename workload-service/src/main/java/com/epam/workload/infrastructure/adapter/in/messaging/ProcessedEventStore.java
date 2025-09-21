package com.epam.workload.infrastructure.adapter.in.messaging;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProcessedEventStore {
    private final Set<UUID> processed = ConcurrentHashMap.newKeySet();

    public boolean isProcessed(UUID eventId) {
        return !processed.add(eventId);
    }
}
