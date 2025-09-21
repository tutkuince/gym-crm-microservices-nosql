package com.epam.workload.domain.model.entity;

import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.model.valueobject.TrainingMonth;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerWorkloadTest {

    @Test
    void constructor_shouldSetFields_whenValid() {
        TrainerId id = new TrainerId("t123");
        TrainerWorkload w = new TrainerWorkload(id, "Ada", "Lovelace", true);

        assertThat(w.getId()).isEqualTo(id);
        assertThat(w.getFirstName()).isEqualTo("Ada");
        assertThat(w.getLastName()).isEqualTo("Lovelace");
        assertThat(w.isActive()).isTrue();
        assertThat(w.getMinutesByMonth()).isEmpty();
    }

    @Test
    void constructor_shouldThrow_whenIdNull() {
        assertThatThrownBy(() -> new TrainerWorkload(null, "Ada", "Lovelace", true))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id");
    }

    @Test
    void constructor_shouldThrow_whenFirstNameBlank() {
        TrainerId id = new TrainerId("t1");
        assertThatThrownBy(() -> new TrainerWorkload(id, "  ", "Last", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("firstName required");
    }

    @Test
    void constructor_shouldThrow_whenLastNameNull() {
        TrainerId id = new TrainerId("t1");
        assertThatThrownBy(() -> new TrainerWorkload(id, "First", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastName required");
    }

    @Test
    void record_shouldAddMinutes_whenValidInput() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        w.record(m, 30);

        assertThat(w.getMinutesByMonth())
                .containsEntry(m.value(), 30);
    }

    @Test
    void record_shouldMergeMinutes_whenSameMonthRepeated() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        w.record(m, 30);
        w.record(m, 15);

        assertThat(w.getMinutesByMonth())
                .containsEntry(m.value(), 45);
    }

    @Test
    void record_shouldThrow_whenMonthNull() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        assertThatThrownBy(() -> w.record(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("month required");
    }

    @Test
    void record_shouldThrow_whenMinutesNotPositive() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        assertThatThrownBy(() -> w.record(m, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minutes must be positive");

        assertThatThrownBy(() -> w.record(m, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minutes must be positive");
    }

    @Test
    void delete_shouldDecreaseMinutes_whenRecordedExists() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        w.record(m, 50);
        w.delete(m, 20);

        assertThat(w.getMinutesByMonth())
                .containsEntry(m.value(), 30);
    }

    @Test
    void delete_shouldRemoveEntry_whenResultIsZeroOrBelow() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        w.record(m, 30);
        w.delete(m, 30);
        assertThat(w.getMinutesByMonth()).doesNotContainKey(m.value());

        w.record(m, 10);
        w.delete(m, 20);
        assertThat(w.getMinutesByMonth()).doesNotContainKey(m.value());
    }

    @Test
    void delete_onNonExistingMonth_shouldLeaveMapUnchanged() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth existing = new TrainingMonth(YearMonth.of(2025, 8));
        TrainingMonth toDelete = new TrainingMonth(YearMonth.of(2025, 9));

        w.record(existing, 40);
        w.delete(toDelete, 15);

        assertThat(w.getMinutesByMonth())
                .containsEntry(existing.value(), 40)
                .doesNotContainKey(toDelete.value());
    }

    @Test
    void delete_shouldThrow_whenMonthNull() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        assertThatThrownBy(() -> w.delete(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("month required");
    }

    @Test
    void delete_shouldThrow_whenMinutesNotPositive() {
        TrainerWorkload w = new TrainerWorkload(new TrainerId("t1"), "First", "Last", true);
        TrainingMonth m = new TrainingMonth(YearMonth.of(2025, 9));

        assertThatThrownBy(() -> w.delete(m, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minutes must be positive");

        assertThatThrownBy(() -> w.delete(m, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minutes must be positive");
    }
}