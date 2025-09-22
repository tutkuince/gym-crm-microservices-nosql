package com.epam.workload.infrastructure.adapter.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.TrainerWorkloadDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.mapper.TrainerWorkloadMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TrainerWorkloadMongoAdapter implements UpsertWorkloadPort {

    private final MongoTemplate mongo;

    public TrainerWorkloadMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<TrainerWorkload> loadByUsername(String username) {
        var doc = mongo.findById(username, TrainerWorkloadDoc.class);
        return Optional.ofNullable(doc).map(TrainerWorkloadMapper::toDomain);
    }

    public void save(TrainerWorkload aggregate) {
        TrainerWorkloadDoc doc = TrainerWorkloadMapper.toTrainerWorkloadDoc(aggregate);
        Objects.requireNonNull(doc).setUpdatedAt(Instant.now());
        mongo.save(doc);
    }

    @Override
    public void upsertMinutes(String username,
                              String firstName,
                              String lastName,
                              boolean active,
                              YearMonth ym,
                              int deltaMinutes) {

        // _id = username
        Query byUser = Query.query(Criteria.where("_id").is(username));

        Update base = new Update()
                .setOnInsert("_id", username)
                .setOnInsert("firstName", firstName)
                .setOnInsert("lastName", lastName)
                .setOnInsert("active", active);

        // 2) (idempotent: addToSet)
        Update addYear = base.addToSet("years").each(List.of(Map.of(
                "year", ym.getYear(),
                "months", List.of()
        )));
        mongo.upsert(byUser, addYear, TrainerWorkloadDoc.class);

        // 3) O yıl içinde ilgili ay yoksa ekle
        Update addMonth = new Update()
                .addToSet("years.$[y].months").each(
                        List.of(Map.of("month", ym.getMonthValue(), "totalMinutes", 0))
                )
                .filterArray(Criteria.where("y.year").is(ym.getYear()));
        mongo.updateFirst(byUser, addMonth, "trainer_workloads");

        // 4) Dakikayı artır + updatedAt
        Update inc = new Update()
                .inc("years.$[y].months.$[m].totalMinutes", deltaMinutes)
                .set("updatedAt", Instant.now())
                .filterArray(Criteria.where("y.year").is(ym.getYear()))
                .filterArray(Criteria.where("m.month").is(ym.getMonthValue()));
        mongo.updateFirst(byUser, inc, "trainer_workloads");
    }
}
