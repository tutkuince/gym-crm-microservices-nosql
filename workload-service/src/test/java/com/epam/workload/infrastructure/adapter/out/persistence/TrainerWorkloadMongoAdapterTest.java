package com.epam.workload.infrastructure.adapter.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.MonthWorkDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.TrainerWorkloadDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.doc.YearWorkDoc;
import com.epam.workload.infrastructure.adapter.out.persistence.mapper.TrainerWorkloadMapper;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadMongoAdapterTest {

    @Mock
    MongoTemplate mongo;

    TrainerWorkloadMongoAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new TrainerWorkloadMongoAdapter(mongo);
    }

    @Test
    void loadByUsername_callsMapperToDomain_andReturnsAggregate() {
        var doc = new TrainerWorkloadDoc("tutku", "Tutku", "Ince", true);
        var y2025 = new YearWorkDoc(2025);
        y2025.getMonths().add(new MonthWorkDoc(9, 180));
        doc.setYears(List.of(y2025));
        when(mongo.findById("tutku", TrainerWorkloadDoc.class)).thenReturn(doc);

        try (MockedStatic<TrainerWorkloadMapper> mocked = mockStatic(TrainerWorkloadMapper.class, CALLS_REAL_METHODS)) {

            // when
            Optional<TrainerWorkload> res = adapter.loadByUsername("tutku");

            // then
            assertThat(res).isPresent();
            var agg = res.get();
            assertThat(agg.getId().value()).isEqualTo("tutku");
            assertThat(agg.getMinutesByMonth().get(YearMonth.of(2025, 9))).isEqualTo(180);

            mocked.verify(() -> TrainerWorkloadMapper.toDomain(doc), times(1));
        }
    }

    @Test
    void loadByUsername_returnsEmpty_whenNotFound() {
        when(mongo.findById("x", TrainerWorkloadDoc.class)).thenReturn(null);
        assertThat(adapter.loadByUsername("x")).isEmpty();
    }

    @Test
    void loadMonthlyMinutes_happyPath_andEmptyCases() {
        var doc = new TrainerWorkloadDoc("tutku", "T", "I", true);
        var y = new YearWorkDoc(2025);
        y.getMonths().add(new MonthWorkDoc(8, 120));
        y.getMonths().add(new MonthWorkDoc(9, 200));
        doc.setYears(List.of(y));
        when(mongo.findById("tutku", TrainerWorkloadDoc.class)).thenReturn(doc);

        assertThat(adapter.loadMonthlyMinutes("tutku", 2025, 9)).contains(200);
        assertThat(adapter.loadMonthlyMinutes("tutku", 2025, 7)).isEmpty();

        when(mongo.findById("a", TrainerWorkloadDoc.class)).thenReturn(null);
        assertThat(adapter.loadMonthlyMinutes("a", 2025, 9)).isEmpty();

        var docNoYears = new TrainerWorkloadDoc("b", "A", "B", true);
        docNoYears.setYears(null);
        when(mongo.findById("b", TrainerWorkloadDoc.class)).thenReturn(docNoYears);
        assertThat(adapter.loadMonthlyMinutes("b", 2025, 9)).isEmpty();
    }

    @Test
    void save_callsMapperToDoc_setsUpdatedAt_andSaves() {
        var agg = new TrainerWorkload(new TrainerId("tutku"), "Tutku", "Ince", true);
        agg.getMinutesByMonth().put(YearMonth.of(2025, 8), 100);
        agg.getMinutesByMonth().put(YearMonth.of(2025, 9), 200);

        ArgumentCaptor<TrainerWorkloadDoc> docCap = ArgumentCaptor.forClass(TrainerWorkloadDoc.class);

        try (MockedStatic<TrainerWorkloadMapper> mocked = mockStatic(TrainerWorkloadMapper.class, CALLS_REAL_METHODS)) {
            adapter.save(agg);

            verify(mongo).save(docCap.capture());
            var saved = docCap.getValue();

            assertThat(saved.getUsername()).isEqualTo("tutku");
            assertThat(saved.getYears()).hasSize(1);
            var y = saved.getYears().getFirst();
            assertThat(y.getYear()).isEqualTo(2025);
            assertThat(y.getMonths())
                    .extracting(MonthWorkDoc::getMonth, MonthWorkDoc::getTotalMinutes)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(8, 100),
                            org.assertj.core.groups.Tuple.tuple(9, 200)
                    );

            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isBeforeOrEqualTo(Instant.now());

            mocked.verify(() -> TrainerWorkloadMapper.toTrainerWorkloadDoc(agg), times(1));
        }
    }

    @Test
    void upsertMonth_executesThreeMongoOps_andArrayFiltersAreCorrect() {
        // given
        String username = "tutku";
        int year = 2025;
        int month = 9;
        int minutes = 240;

        ArgumentCaptor<Query> qCap = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> uCap = ArgumentCaptor.forClass(Update.class);

        // when
        adapter.upsertMonth(username, year, month, "T", "I", true, minutes);

        // then
        // 1) verify yearly upsert
        verify(mongo).upsert(any(Query.class), any(Update.class), eq(TrainerWorkloadDoc.class));

        // 2) verify two updateFirst calls (add month + set totalMinutes)
        verify(mongo, times(2)).updateFirst(qCap.capture(), uCap.capture(), eq("trainer_workloads"));

        var updates = uCap.getAllValues();
        assertThat(updates).hasSize(2);

        // ---- (2.a) $addToSet validation
        Update u1 = updates.get(0);
        Document d1 = u1.getUpdateObject();

        // $addToSet should exist
        assertThat(d1).containsKey("$addToSet");

        String addToSetStr = String.valueOf(d1.get("$addToSet"));
        assertThat(addToSetStr).contains("years.$[y].months");
        assertThat(addToSetStr).contains("$each");
        assertThat(addToSetStr).contains("month").contains(String.valueOf(month));
        assertThat(addToSetStr)
                .as("Initial placeholder should set totalMinutes=0")
                .contains("totalMinutes")
                .contains("0");

        // at least one array filter must be present (y.year filter)
        assertThat(u1.getArrayFilters())
                .as("Expected at least one array filter for year")
                .isNotNull()
                .hasSizeGreaterThan(0);

        // ---- (2.b) $set validation
        Update u2 = updates.get(1);
        Document d2 = u2.getUpdateObject();

        assertThat(d2).containsKey("$set");
        Document setDoc = (Document) d2.get("$set");

        // totalMinutes should be set to the given value
        assertThat(setDoc.get("years.$[y].months.$[m].totalMinutes"))
                .as("Expected totalMinutes to be set to provided value")
                .isEqualTo(minutes);

        // updatedAt should also be set
        assertThat(setDoc)
                .as("Expected updatedAt field to be set")
                .containsKey("updatedAt");

        // at least two array filters must be present (y.year and m.month)
        assertThat(u2.getArrayFilters())
                .as("Expected at least two array filters for year and month")
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(2);

        // update path must include both $[y] and $[m] placeholders
        String setStr = String.valueOf(d2.get("$set"));
        assertThat(setStr)
                .as("Expected path to include both $[y] and $[m] placeholders")
                .contains("years.$[y].months.$[m].totalMinutes");
    }

    @Test
    void deleteMonth_executesTwoMongoOps_withExpectedPulls() {
        String username = "tutku";
        int year = 2025, month = 9;

        ArgumentCaptor<Query> qCap = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> uCap = ArgumentCaptor.forClass(Update.class);

        adapter.deleteMonth(username, year, month);

        verify(mongo, times(2)).updateFirst(qCap.capture(), uCap.capture(), eq("trainer_workloads"));

        var u1 = uCap.getAllValues().get(0);
        assertThat(getUpdateDocument(u1).toJson())
                .contains("\"$pull\"")
                .contains("years.$[y].months")
                .contains("\"updatedAt\"");
        assertThat(u1.getArrayFilters()).anySatisfy(f -> assertThat(f.asDocument().toJson()).contains("\"y.year\": " + year));

        var u2 = uCap.getAllValues().get(1);
        assertThat(getUpdateDocument(u2).toJson())
                .contains("\"$pull\"")
                .contains("\"years\"")
                .contains("\"updatedAt\"");
    }

    // --- helpers ---
    private static Document getUpdateDocument(Update update) {
        return update.getUpdateObject();
    }

    private static boolean hasFilter(String jsonLike, String key, int value) {
        if (jsonLike == null) return false;
        // ör: {"y.year": 2025} veya {'y.year': 2025}
        String quotedKey = "\""+key+"\"|'"+key+"'";
        String pattern = "(?:\""+key+"\"|'"+key+"')\\s*:\\s*"+value+"\\b";
        return jsonLike.contains(key) && jsonLike.replaceAll("\\s+", " ").matches(".*" + pattern + ".*");
    }
}