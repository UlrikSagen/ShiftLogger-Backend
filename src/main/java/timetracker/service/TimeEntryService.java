package timetracker.service;

import org.springframework.stereotype.Service;
import timetracker.db.TimeEntryRepository;

import timetracker.dto.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class TimeEntryService {

    private final TimeEntryRepository repo;

    public TimeEntryService(TimeEntryRepository repo) {
        this.repo = repo;
    }

    public TimeEntryRepository.TimeEntryRow create(UUID userId, LocalDate date, LocalTime start, LocalTime end) {
        
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("date/start/end må være satt");
        }
        if(date.isAfter(LocalDate.now())){
            throw new IllegalArgumentException("dato må være før eller lik dagens dato");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start må være før end");
        }
        return repo.insert(UUID.randomUUID(), userId, date, start, end);
    }

    public TimeEntryRepository.TimeEntryRow update(UUID userId, UUID id, LocalDate date, LocalTime start, LocalTime end){
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("date/start/end må være satt");
        }
        if(date.isAfter(LocalDate.now())){
            throw new IllegalArgumentException("dato må være før eller lik dagens dato");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start må være før end");
        }
        return repo.update(id, userId, date, start, end);
    }

    public boolean delete(UUID id, UUID userId){
        return repo.delete(id, userId);
    }

    public List<TimeEntryDto> getByUserIdAndRange(UUID userId, LocalDate from, LocalDate to){
        if (from.isAfter(to)){
            throw new IllegalArgumentException("From-dato må være før eller lik to-dato");
        }
        return repo.findByUserIdAndRange(userId, from, to);
    }

    public List<TimeEntryDto> getByUserId(UUID userId){
        return repo.findByUserId(userId);
    }
}
