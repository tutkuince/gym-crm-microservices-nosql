package com.epam.workload.domain.port.out.persistence;

public interface SaveWorkloadPort {
    void upsertMonth(String username, int year, int month,
                     String firstName, String lastName, boolean active,
                     int totalMinutes);
    void deleteMonth(String username, int year, int month);
}
