package com.epam.workload.infrastructure.adapter.out.persistence;

import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadJpaAdapterTest {

    @Mock
    private WorkloadRepository repository;

    @InjectMocks
    private WorkloadJpaAdapter adapter;

    @Test
    void loadByUsername_whenNoRows_shouldReturnEmpty() {
        String username = "ghost";
        when(repository.findAllByUsername(username)).thenReturn(List.of());

        var result = adapter.loadByUsername(username);

        assertThat(result).isEmpty();
        verify(repository).findAllByUsername(username);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void loadByUsername_whenRowsExist_shouldBuildAggregateAndMergeMonths() {
        // given
        String username = "trainer.alex";

        List<TrainerMonthlyWorkloadEntity> rows = new ArrayList<>();
        rows.add(new TrainerMonthlyWorkloadEntity(username, 2025, 9, "Alex", "Turner", true, 60));
        rows.add(new TrainerMonthlyWorkloadEntity(username, 2024, 12, "Alex", "Turner", true, 30));
        rows.add(new TrainerMonthlyWorkloadEntity(username, 2025, 1, "Alex", "Turner", true, 45));
        rows.add(new TrainerMonthlyWorkloadEntity(username, 2025, 9, "Alex", "Turner", true, 15));

        when(repository.findAllByUsername(username)).thenReturn(rows);

        // when
        var result = adapter.loadByUsername(username);

        // then
        assertThat(result).isPresent();
        TrainerWorkload agg = result.get();

        assertThat(agg.getId()).isNotNull();
        assertThat(agg.getId()).extracting(TrainerId::value).isEqualTo(username);
        assertThat(agg.getFirstName()).isEqualTo("Alex");
        assertThat(agg.getLastName()).isEqualTo("Turner");
        assertThat(agg.isActive()).isTrue();

        assertThat(agg.getMinutesByMonth())
                .containsEntry(YearMonth.of(2024, 12), 30)
                .containsEntry(YearMonth.of(2025, 1), 45)
                .containsEntry(YearMonth.of(2025, 9), 75);

        verify(repository).findAllByUsername(username);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void loadMonthlyMinutes_whenFound_shouldReturnOptionalWithMinutes() {
        // given
        String username = "trainer.jane";
        int year = 2025, month = 8;
        var entity = new TrainerMonthlyWorkloadEntity(username, year, month, "Jane", "Doe", true, 90);

        when(repository.findByUsernameAndWorkYearAndWorkMonth(username, year, month))
                .thenReturn(Optional.of(entity));

        // when
        var result = adapter.loadMonthlyMinutes(username, year, month);

        // then
        assertThat(result).contains(90);
        verify(repository).findByUsernameAndWorkYearAndWorkMonth(username, year, month);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void loadMonthlyMinutes_whenNotFound_shouldReturnEmpty() {
        String username = "trainer.jane";
        int year = 2025, month = 7;

        when(repository.findByUsernameAndWorkYearAndWorkMonth(username, year, month))
                .thenReturn(Optional.empty());

        var result = adapter.loadMonthlyMinutes(username, year, month);

        assertThat(result).isEmpty();
        verify(repository).findByUsernameAndWorkYearAndWorkMonth(username, year, month);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void upsertMonth_whenExisting_shouldMutateEntityFields_andNotCallSave() {
        String username = "trainer.max";
        int year = 2025, month = 9;

        var existing = new TrainerMonthlyWorkloadEntity(username, year, month, "Old", "Name", false, 10);

        when(repository.findByUsernameAndWorkYearAndWorkMonth(username, year, month))
                .thenReturn(Optional.of(existing));

        adapter.upsertMonth(username, year, month, "New", "Surname", true, 120);

        assertThat(existing.getFirstName()).isEqualTo("New");
        assertThat(existing.getLastName()).isEqualTo("Surname");
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getTotalMinutes()).isEqualTo(120);

        verify(repository).findByUsernameAndWorkYearAndWorkMonth(username, year, month);
        verify(repository, never()).save(any(TrainerMonthlyWorkloadEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void upsertMonth_whenMissing_shouldInsertNewAndCallSave() {
        String username = "trainer.max";
        int year = 2025, month = 10;

        when(repository.findByUsernameAndWorkYearAndWorkMonth(username, year, month))
                .thenReturn(Optional.empty());

        ArgumentCaptor<TrainerMonthlyWorkloadEntity> captor =
                ArgumentCaptor.forClass(TrainerMonthlyWorkloadEntity.class);

        adapter.upsertMonth(username, year, month, "Max", "Payne", true, 200);

        verify(repository).findByUsernameAndWorkYearAndWorkMonth(username, year, month);
        verify(repository).save(captor.capture());
        verifyNoMoreInteractions(repository);

        var saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo(username);
        assertThat(saved.getWorkYear()).isEqualTo(year);
        assertThat(saved.getWorkMonth()).isEqualTo(month);
        assertThat(saved.getFirstName()).isEqualTo("Max");
        assertThat(saved.getLastName()).isEqualTo("Payne");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getTotalMinutes()).isEqualTo(200);
    }

    @Test
    void deleteMonth_shouldDelegateToRepository() {
        String username = "trainer.bob";
        int year = 2024, month = 12;

        adapter.deleteMonth(username, year, month);

        verify(repository).deleteByUsernameAndWorkYearAndWorkMonth(username, year, month);
        verifyNoMoreInteractions(repository);
    }
}