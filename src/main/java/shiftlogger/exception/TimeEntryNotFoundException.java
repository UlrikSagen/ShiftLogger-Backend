package shiftlogger.exception;

import java.util.UUID;

public class TimeEntryNotFoundException extends RuntimeException {
    public TimeEntryNotFoundException(UUID id) {
        super("Could not find time_entry with id: " + id);
    }
}