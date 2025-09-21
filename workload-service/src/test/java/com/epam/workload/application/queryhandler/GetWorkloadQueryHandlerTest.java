package com.epam.workload.application.queryhandler;

import com.epam.workload.application.dto.query.TrainerWorkloadResponseDto;
import com.epam.workload.domain.model.entity.TrainerWorkload;
import com.epam.workload.domain.model.valueobject.TrainerId;
import com.epam.workload.domain.port.out.persistence.LoadWorkloadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetWorkloadQueryHandlerTest {

    @Mock
    LoadWorkloadPort loadPort;

    @InjectMocks
    GetWorkloadQueryHandler handler;

    @Test
    void getByUsername_shouldMapFields_andSortMonthsAscending() {
        String username = "trainer.alex";
        TrainerWorkload workload = mock(TrainerWorkload.class);
        TrainerId id = mock(TrainerId.class);

        Map<YearMonth, Integer> minutes = new HashMap<>();
        minutes.put(YearMonth.of(2025, 9), 60);
        minutes.put(YearMonth.of(2024, 12), 30);
        minutes.put(YearMonth.of(2025, 1), 45);

        when(loadPort.loadByUsername(username)).thenReturn(Optional.of(workload));
        when(workload.getId()).thenReturn(id);
        when(id.value()).thenReturn(username);
        when(workload.getFirstName()).thenReturn("Alex");
        when(workload.getLastName()).thenReturn("Turner");
        when(workload.isActive()).thenReturn(true);
        when(workload.getMinutesByMonth()).thenReturn(minutes);

        TrainerWorkloadResponseDto dto = handler.getByUsername(username);

        assertThat(dto.trainerUsername()).isEqualTo(username);
        assertThat(dto.trainerFirstName()).isEqualTo("Alex");
        assertThat(dto.trainerLastName()).isEqualTo("Turner");
        assertThat(dto.active()).isTrue();

        assertThat(dto.months())
                .extracting(m -> List.of(m.year(), m.month(), m.totalMinutes()))
                .containsExactly(
                        List.of(2024, 12, 30),
                        List.of(2025, 1, 45),
                        List.of(2025, 9, 60)
                );

        verify(loadPort, times(1)).loadByUsername(username);
        verifyNoMoreInteractions(loadPort);
    }

    @Test
    void getByUsername_whenNotFound_shouldThrowNoSuchElementException() {
        String username = "ghost";
        when(loadPort.loadByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.getByUsername(username))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(username);

        verify(loadPort).loadByUsername(username);
        verifyNoMoreInteractions(loadPort);
    }

    @Test
    void getMonthlyMinutes_shouldReturnValue_whenRepositoryHasData() {
        String username = "trainer.jane";
        int year = 2025, month = 9, expected = 90;
        when(loadPort.loadMonthlyMinutes(username, year, month)).thenReturn(Optional.of(expected));

        int minutes = handler.getMonthlyMinutes(username, year, month);

        assertThat(minutes).isEqualTo(expected);
        verify(loadPort).loadMonthlyMinutes(username, year, month);
        verifyNoMoreInteractions(loadPort);
    }

    @Test
    void getMonthlyMinutes_shouldReturnZero_whenRepositoryEmpty() {
        String username = "trainer.jane";
        int year = 2025, month = 8;
        when(loadPort.loadMonthlyMinutes(username, year, month)).thenReturn(Optional.empty());

        int minutes = handler.getMonthlyMinutes(username, year, month);

        assertThat(minutes).isZero();
        verify(loadPort).loadMonthlyMinutes(username, year, month);
        verifyNoMoreInteractions(loadPort);
    }
}