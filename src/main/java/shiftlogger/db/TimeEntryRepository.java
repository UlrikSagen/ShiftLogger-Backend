package shiftlogger.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import shiftlogger.dto.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public class TimeEntryRepository {

    private final JdbcTemplate jdbc;
    
    private static final RowMapper<TimeEntryDto> ROW_MAPPER =
        (rs, rowNum) -> new TimeEntryDto(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("entry_date", LocalDate.class),
            rs.getObject("start_time", LocalTime.class),
            rs.getObject("end_time", LocalTime.class),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("last_edit", OffsetDateTime.class)
        );

    public TimeEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public TimeEntryRow insert(UUID id, UUID userId, LocalDate date, LocalTime start, LocalTime end) {
        jdbc.update("INSERT INTO time_entries (id, user_id, entry_date, start_time, end_time) VALUES (?, ?, ?, ?, ?)", id, userId, date, start, end);

        OffsetDateTime createdAt = jdbc.queryForObject("SELECT created_at FROM time_entries WHERE id = ?",
        OffsetDateTime.class,id);
        OffsetDateTime lastEdited = jdbc.queryForObject("SELECT last_edit FROM time_entries WHERE id = ?",
        OffsetDateTime.class, id);

        return new TimeEntryRow(id, userId, date, start, end, lastEdited, createdAt);
    }

    public boolean delete(UUID id, UUID userId){
        int deleted = jdbc.update("DELETE FROM time_entries WHERE id = ? AND user_id = ?", id, userId);
        return deleted > 0;
    }

    public TimeEntryRow update(UUID id, UUID userId, LocalDate date, LocalTime start, LocalTime end){
        int updated = jdbc.update("UPDATE time_entries SET entry_date = ?, start_time = ?, end_time = ?, last_edit = now() WHERE id = ? AND user_id = ?",
            date, start, end, id, userId);
        if (updated == 0){
            throw new IllegalStateException("Fant ingen time_entry med id " + id);
        }
        OffsetDateTime createdAt = jdbc.queryForObject("SELECT created_at FROM time_entries WHERE id = ?",
         OffsetDateTime.class, id);
        OffsetDateTime lastEdited = jdbc.queryForObject("SELECT last_edit FROM time_entries WHERE id = ?",
         OffsetDateTime.class, id);

        return new TimeEntryRow(id, userId, date, start, end, lastEdited, createdAt);
    }

    public List<TimeEntryDto> findByUserId(UUID userId){
        return jdbc.query("SELECT id, user_id, entry_date, start_time, end_time, created_at, last_edit FROM time_entries WHERE user_id = ?",
            ROW_MAPPER,
            userId
            );     
    }

    public List<TimeEntryDto> findByUserIdAndRange(UUID userId, LocalDate from, LocalDate to){
        return jdbc.query("SELECT id, user_id, entry_date, start_time, end_time, created_at, last_edit FROM time_entries WHERE user_id = ? AND entry_date >= ? AND entry_date <= ?",
            ROW_MAPPER,
            userId, from, to
            ); 
    }

    public record TimeEntryRow(
            UUID id,
            UUID userId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            OffsetDateTime createdAt,
            OffsetDateTime lastEdit
    ) {}

}
