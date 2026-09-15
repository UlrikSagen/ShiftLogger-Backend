package shiftlogger.security;

public record JwtClaims(
        String userId,
        String username
) {}
