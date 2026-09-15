package shiftlogger.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import shiftlogger.db.TimeEntryRepository;
import shiftlogger.db.TimeEntryRepository.TimeEntryRow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, repository is mocked.
 * These test the validation rules in TimeEntryService and nothing else.
 */
class TimeEntryServiceTest {

    private static final LocalTime EIGHT = LocalTime.of(8, 0);
    private static final LocalTime SIXTEEN = LocalTime.of(16, 0);

    private final UUID userId = UUID.randomUUID();
    private final LocalDate today = LocalDate.now();

    private TimeEntryRepository repo;
    private TimeEntryService service;

    @BeforeEach
    void setUp() {
        repo = mock(TimeEntryRepository.class);
        service = new TimeEntryService(repo);
    }

    // ---------- create ----------

    @Test
    void create_validEntry_insertsAndReturnsRow() {
        TimeEntryRow row = new TimeEntryRow(UUID.randomUUID(), userId, today, EIGHT, SIXTEEN,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(repo.insert(any(UUID.class), eq(userId), eq(today), eq(EIGHT), eq(SIXTEEN))).thenReturn(row);

        TimeEntryRow result = service.create(userId, today, EIGHT, SIXTEEN);

        assertThat(result).isEqualTo(row);
        verify(repo).insert(any(UUID.class), eq(userId), eq(today), eq(EIGHT), eq(SIXTEEN));
    }

    @Test
    void create_nullDate_throwsAndDoesNotTouchRepo() {
        assertThatThrownBy(() -> service.create(userId, null, EIGHT, SIXTEEN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_nullStart_throws() {
        assertThatThrownBy(() -> service.create(userId, today, null, SIXTEEN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_nullEnd_throws() {
        assertThatThrownBy(() -> service.create(userId, today, EIGHT, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_startEqualsEnd_throws() {
        assertThatThrownBy(() -> service.create(userId, today, EIGHT, EIGHT))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_startAfterEnd_throws() {
        assertThatThrownBy(() -> service.create(userId, today, SIXTEEN, EIGHT))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_futureDate_throws() {
        assertThatThrownBy(() -> service.create(userId, today.plusDays(1), EIGHT, SIXTEEN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void create_pastDate_isAllowed() {
        LocalDate lastWeek = today.minusDays(7);
        TimeEntryRow row = new TimeEntryRow(UUID.randomUUID(), userId, lastWeek, EIGHT, SIXTEEN,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(repo.insert(any(UUID.class), eq(userId), eq(lastWeek), eq(EIGHT), eq(SIXTEEN))).thenReturn(row);

        assertThat(service.create(userId, lastWeek, EIGHT, SIXTEEN)).isEqualTo(row);
    }

    // ---------- update ----------

    @Test
    void update_validEntry_delegatesToRepo() {
        UUID id = UUID.randomUUID();
        TimeEntryRow row = new TimeEntryRow(id, userId, today, EIGHT, SIXTEEN,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(repo.update(id, userId, today, EIGHT, SIXTEEN)).thenReturn(row);

        assertThat(service.update(userId, id, today, EIGHT, SIXTEEN)).isEqualTo(row);
    }

    @Test
    void update_startAfterEnd_throws() {
        assertThatThrownBy(() -> service.update(userId, UUID.randomUUID(), today, SIXTEEN, EIGHT))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void update_futureDate_throws() {
        assertThatThrownBy(() -> service.update(userId, UUID.randomUUID(), today.plusDays(1), EIGHT, SIXTEEN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    // ---------- delete ----------

    @Test
    void delete_returnsRepoResult() {
        UUID id = UUID.randomUUID();
        when(repo.delete(id, userId)).thenReturn(true);

        assertThat(service.delete(id, userId)).isTrue();
        verify(repo).delete(id, userId);
    }

    // ---------- range query ----------

    @Test
    void getByUserIdAndRange_fromAfterTo_throws() {
        assertThatThrownBy(() -> service.getByUserIdAndRange(userId, today, today.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void getByUserIdAndRange_fromEqualsTo_isAllowed() {
        when(repo.findByUserIdAndRange(userId, today, today)).thenReturn(List.of());

        assertThat(service.getByUserIdAndRange(userId, today, today)).isEmpty();
        verify(repo).findByUserIdAndRange(userId, today, today);
    }

    @Test
    void getByUserId_delegatesToRepo() {
        when(repo.findByUserId(userId)).thenReturn(List.of());

        assertThat(service.getByUserId(userId)).isEmpty();
        verify(repo).findByUserId(userId);
    }
}