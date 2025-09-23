package com.epam.workload.infrastructure.adapter.out.persistence.doc;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("trainer_workloads")
@CompoundIndexes({
        @CompoundIndex(name = "idx_fullname", def = "{'firstName': 1, 'lastName': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrainerWorkloadDoc {
    @Id
    private String username;     // _id = username
    private String firstName;
    private String lastName;
    private Boolean active;

    private List<YearWorkDoc> years = new ArrayList<>();
    private Instant updatedAt;

    @Builder
    public TrainerWorkloadDoc(String username, String firstName, String lastName, Boolean active) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }
}
