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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TrainerWorkloadMongoAdapter implements UpsertWorkloadPort, LoadWorkloadPort, SaveWorkloadPort {

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

    @Override
    public void upsertMonth(String username, int year, int month,
                            String firstName, String lastName, boolean active,
                            int totalMinutes) {

        Query byUser = Query.query(Criteria.where("_id").is(username));

        // Belge yoksa temel alanlar
        Update base = new Update()
                .setOnInsert("_id", username)
                .setOnInsert("firstName", firstName)
                .setOnInsert("lastName", lastName)
                .setOnInsert("active", active);

        // Yıl yoksa ekle (idempotent)
        Update addYear = base.addToSet("years").each(List.of(Map.of(
                "year", year,
                "months", List.of()
        )));
        mongo.upsert(byUser, addYear, TrainerWorkloadDoc.class);

        // Ay yoksa ekle (idempotent)
        Update addMonth = new Update()
                .addToSet("years.$[y].months").each(
                        List.of(Map.of("month", month, "totalMinutes", 0))
                )
                .filterArray(Criteria.where("y.year").is(year));
        mongo.updateFirst(byUser, addMonth, "trainer_workloads");

        // Toplamı set et + updatedAt
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

        // Ayı months'tan çıkar
        Update pullMonth = new Update()
                .pull("years.$[y].months",
                        Query.query(Criteria.where("month").is(month)).getQueryObject())
                .filterArray(Criteria.where("y.year").is(year));
        mongo.updateFirst(byUser, pullMonth, "trainer_workloads");

        // Ay kalmadıysa yılı da çıkar (opsiyonel ama temiz)
        Update pullEmptyYear = new Update()
                .pull("years",
                        Query.query(Criteria.where("year").is(year)
                                .and("months").size(0)).getQueryObject());
        mongo.updateFirst(byUser, pullEmptyYear, "trainer_workloads");
    }


}
