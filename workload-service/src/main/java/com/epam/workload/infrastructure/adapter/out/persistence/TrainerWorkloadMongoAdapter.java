package com.epam.workload.infrastructure.adapter.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import com.epam.workload.domain.port.out.persistence.SaveWorkloadPort;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.MonthWorkDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.TrainerWorkloadDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.mapper.TrainerWorkloadMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TrainerWorkloadMongoAdapter implements LoadWorkloadPort, SaveWorkloadPort {

    private final MongoTemplate mongo;

    public TrainerWorkloadMongoAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<TrainerWorkload> loadByUsername(String username) {
        var doc = mongo.findById(username, TrainerWorkloadDoc.class);
        return Optional.ofNullable(doc).map(TrainerWorkloadMapper::toDomain);
    }

    @Override
    public Optional<Integer> loadMonthlyMinutes(String username, int year, int month) {
        var doc = mongo.findById(username, TrainerWorkloadDoc.class);
        if (doc == null || doc.getYears() == null) return Optional.empty();

        return doc.getYears().stream()
                .filter(y -> y.getYear() == year && y.getMonths() != null)
                .flatMap(y -> y.getMonths().stream())
                .filter(m -> m.getMonth() == month)
                .map(MonthWorkDoc::getTotalMinutes)
                .findFirst();
    }

    public void save(TrainerWorkload aggregate) {
        TrainerWorkloadDoc doc = TrainerWorkloadMapper.toTrainerWorkloadDoc(aggregate);
        Objects.requireNonNull(doc).setUpdatedAt(Instant.now());
        mongo.save(doc);
    }

    @Override
    public void upsertMonth(String username, int year, int month,
                            String firstName, String lastName, boolean active,
                            int totalMinutes) {

        Query byUser = Query.query(Criteria.where("_id").is(username));

        // If document does not exist, set basic fields on insert
        Update base = new Update()
                .setOnInsert("_id", username)
                .setOnInsert("firstName", firstName)
                .setOnInsert("lastName", lastName)
                .setOnInsert("active", active);

        // Add the year entry if it does not exist (idempotent)
        Update addYear = base.addToSet("years")
                .each(List.of(Map.of("year", year)));
        mongo.upsert(byUser, addYear, TrainerWorkloadDoc.class);

        // Add the month entry inside the given year if it does not exist (idempotent)
        Update addMonth = new Update()
                .addToSet("years.$[y].months").each(
                        List.of(Map.of("month", month, "totalMinutes", 0))
                )
                .filterArray(Criteria.where("y.year").is(year));
        mongo.updateFirst(byUser, addMonth, "trainer_workloads");

        // Finally, set the totalMinutes value and update the timestamp
        Update setTotal = new Update()
                .set("years.$[y].months.$[m].totalMinutes", totalMinutes)
                .set("updatedAt", Instant.now())
                .filterArray(Criteria.where("y.year").is(year))
                .filterArray(Criteria.where("m.month").is(month));
        mongo.updateFirst(byUser, setTotal, "trainer_workloads");
    }


    @Override
    public void deleteMonth(String username, int year, int month) {
        Query byUser = Query.query(Criteria.where("_id").is(username));

        // 1) Remove the month object from the matching year's months array (idempotent if not present)
        Update pullMonth = new Update()
                .pull("years.$[y].months",
                        Query.query(Criteria.where("month").is(month)).getQueryObject())
                .currentDate("updatedAt") // keep audit timestamp fresh
                .filterArray(Criteria.where("y.year").is(year));
        mongo.updateFirst(byUser, pullMonth, "trainer_workloads");

        // 2) If the year has no months left, remove the entire year entry
        //    Also handle the rare case where "months" might be absent (exists:false).
        Update pullEmptyYear = new Update()
                .pull("years", Query.query(new Criteria().andOperator(
                        Criteria.where("year").is(year),
                        new Criteria().orOperator(
                                Criteria.where("months").size(0),
                                Criteria.where("months").exists(false)
                        )
                )).getQueryObject())
                .currentDate("updatedAt");
        mongo.updateFirst(byUser, pullEmptyYear, "trainer_workloads");
    }

}
