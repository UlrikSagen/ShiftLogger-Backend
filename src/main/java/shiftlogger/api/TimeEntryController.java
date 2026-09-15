    package shiftlogger.api;

    import java.time.LocalDate;
    import java.time.LocalTime;
    import java.util.List;
    import java.util.UUID;

    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.server.ResponseStatusException;

    import shiftlogger.db.TimeEntryRepository;
    import shiftlogger.dto.TimeEntryDto;
    import shiftlogger.security.UserPrincipal;
    import shiftlogger.service.TimeEntryService;

    import org.springframework.security.core.annotation.AuthenticationPrincipal;

    @RestController
    @RequestMapping("/entries")
    public class TimeEntryController {

        private final TimeEntryService service;

        public TimeEntryController(TimeEntryService service) {
            this.service = service;
        }

        @PostMapping()
        @ResponseStatus(HttpStatus.CREATED)
        public TimeEntryDto create(@RequestBody TimeEntryRequest req, @AuthenticationPrincipal UserPrincipal me) {
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
        @PutMapping("/{id}")
        public TimeEntryDto update(@PathVariable UUID id, @RequestBody TimeEntryRequest req, @AuthenticationPrincipal UserPrincipal me) {
            if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            try {
                TimeEntryRepository.TimeEntryRow saved = service.update(me.userId(),id, req.date(), req.start(), req.end());

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
        @DeleteMapping("/{id}")
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

        @GetMapping()
        public List<TimeEntryDto> getRange(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @AuthenticationPrincipal UserPrincipal me){
            if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            try{
                if (from != null && to != null){
                    return service.getByUserIdAndRange(me.userId(), from, to);
                }
                else{
                    return service.getByUserId(me.userId());
                }
            } catch(IllegalArgumentException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        }

        public record TimeEntryRequest(
            LocalDate date,
            LocalTime start,
            LocalTime end) 
        {}
    }
