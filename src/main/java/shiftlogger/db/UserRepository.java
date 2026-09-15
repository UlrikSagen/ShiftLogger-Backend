package shiftlogger.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<UserRow> MAPPER = (rs, rowNum) ->
            new UserRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class)
            );

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findByUsername(String username) {
        var rows = jdbc.query(
                "select id, username, password_hash, created_at from users where username = ?",
                MAPPER,
                username
        );
        return rows.stream().findFirst();
    }

    public UserRow insert(UUID id, String username, String passwordHash) {
        jdbc.update(
                "insert into users (id, username, password_hash) values (?, ?, ?)",
                id, username, passwordHash
        );
        return findByUsername(username).orElseThrow();
    }
    
    public record UserRow(UUID id, String username, String passwordHash, OffsetDateTime createdAt) {}

}
