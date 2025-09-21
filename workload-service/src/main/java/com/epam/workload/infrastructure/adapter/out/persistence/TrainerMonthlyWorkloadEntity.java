package com.epam.workload.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Table(name = "trainer_monthly_workload", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_year_month",
        columnNames = {"username", "year", "month"}
))
public class TrainerMonthlyWorkloadEntity {

    public TrainerMonthlyWorkloadEntity() {
    }

    public TrainerMonthlyWorkloadEntity(String username, int workYear, int workMonth, String firstName, String lastName, boolean active, int totalMinutes) {
        this.username = username;
        this.workYear = workYear;
        this.workMonth = workMonth;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
        this.totalMinutes = totalMinutes;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;
    @Column(name = "work_year", nullable = false)
    private int workYear;
    @Column(name = "work_month", nullable = false)
    private int workMonth;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private int totalMinutes;

}
