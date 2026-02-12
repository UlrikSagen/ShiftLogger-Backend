package timetracker.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import timetracker.db.TimeEntryRepository;
import timetracker.dto.TimeEntryDto;
import timetracker.security.UserPrincipal;
import timetracker.service.TimeEntryService;

@RestController
@RequestMapping("/entries")
public class TimeEntryController {

    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) {
        this.service = service;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryDto create(@RequestBody CreateTimeEntryRequest req, @AuthenticationPrincipal UserPrincipal me) {
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            TimeEntryRepository.TimeEntryRow saved = service.create(me.userId(), req.date(), req.start(), req.end());

            return new TimeEntryDto(
                    saved.id(),
                    saved.userId(),
                    saved.date(),
                    saved.start(),
                    saved.end(),
                    saved.createdAt(),
                    saved.lastEdit()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    @PutMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public TimeEntryDto update(@RequestBody UpdateTimeEntryRequest req, @AuthenticationPrincipal UserPrincipal me) {
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            TimeEntryRepository.TimeEntryRow saved = service.update(me.userId(),req.id(), req.date(), req.start(), req.end());

            return new TimeEntryDto(
                    saved.id(),
                    saved.userId(),
                    saved.date(),
                    saved.start(),
                    saved.end(),
                    saved.createdAt(),
                    saved.lastEdit()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal me){
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try{
            boolean deleted = service.delete(id, me.userId());
            if(!deleted){
               throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Kunne ikke slette TimeEntry med id: " + id);
            }
        } catch(IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/getrange")
    @ResponseStatus(HttpStatus.OK)
    public List<TimeEntryDto> getRange(@RequestParam LocalDate from, @RequestParam LocalDate to, @AuthenticationPrincipal UserPrincipal me){
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try{
            return service.getByUserIdAndRange(me.userId(), from, to);
        } catch(IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/getbyuser")
    @ResponseStatus(HttpStatus.OK)
    public List<TimeEntryDto> getByUser(@AuthenticationPrincipal UserPrincipal me){
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try{
            return service.getByUserId(me.userId());
        } catch(IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public record CreateTimeEntryRequest(
        LocalDate date,
        LocalTime start,
        LocalTime end) 
    {}
    public record UpdateTimeEntryRequest(
        UUID id,
        LocalDate date,
        LocalTime start,
        LocalTime end)
    {}
}
