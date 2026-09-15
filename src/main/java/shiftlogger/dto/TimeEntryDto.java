package shiftlogger.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TimeEntryDto(
        UUID id,
        UUID userId,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        OffsetDateTime createdAt,
        OffsetDateTime lastEdit
) {}
