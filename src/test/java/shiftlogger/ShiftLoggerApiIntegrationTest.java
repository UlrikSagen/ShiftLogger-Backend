package shiftlogger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack test: real Spring context, real HTTP, real Postgres in Docker.
 * schema.sql runs on startup via spring.sql.init.mode=always.
 *
 * Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ShiftLoggerApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    TestRestTemplate rest;

    private static final String PASSWORD = "correct-horse-battery";

    // ---------- helpers ----------

    private static String uniqueUsername() {
        return "u_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> register(String username, String password) {
        return rest.postForEntity("/auth/register",
                Map.of("username", username, "password", password), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> login(String username, String password) {
        return rest.postForEntity("/auth/login",
                Map.of("username", username, "password", password), Map.class);
    }

    private String registerAndLogin() {
        String username = uniqueUsername();
        register(username, PASSWORD);
        return (String) login(username, PASSWORD).getBody().get("token");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> createEntry(String token, LocalDate date, String start, String end) {
        Map<String, String> body = Map.of("date", date.toString(), "start", start, "end", end);
        return rest.exchange("/entries", HttpMethod.POST, new HttpEntity<>(body, bearer(token)), Map.class);
    }

    private List<Map<String, Object>> listEntries(String token) {
        return rest.exchange("/entries", HttpMethod.GET, new HttpEntity<>(bearer(token)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }

    // ---------- health ----------

    @Test
    void health_isPublic() {
        assertThat(rest.getForEntity("/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ready_reportsDbOk() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> res = rest.getForEntity("/ready", Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("db")).isEqualTo("ok");
    }

    // ---------- auth ----------

    @Test
    void register_newUser_returns201WithIdAndUsername() {
        String username = uniqueUsername();

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> res = register(username, PASSWORD);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().get("username")).isEqualTo(username);
        assertThat(res.getBody().get("userId")).isNotNull();
    }

    @Test
    void register_duplicateUsername_returns409() {
        String username = uniqueUsername();
        register(username, PASSWORD);

        assertThat(register(username, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_shortPassword_returns400() {
        assertThat(register(uniqueUsername(), "short").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_blankUsername_returns400() {
        assertThat(register("   ", PASSWORD).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_correctCredentials_returnsToken() {
        String username = uniqueUsername();
        register(username, PASSWORD);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> res = login(username, PASSWORD);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) res.getBody().get("token")).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        String username = uniqueUsername();
        register(username, PASSWORD);

        assertThat(login(username, "wrong-password").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unknownUser_returns401() {
        assertThat(login("does-not-exist", PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- authentication on protected routes ----------

    @Test
    void entries_withoutToken_returns401() {
        ResponseEntity<String> res = rest.getForEntity("/entries", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void entries_withInvalidToken_returns401() {
        ResponseEntity<String> res = rest.exchange("/entries", HttpMethod.GET,
                new HttpEntity<>(bearer("not-a-real-token")), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- entries CRUD ----------

    @Test
    void createEntry_thenList_containsIt() {
        String token = registerAndLogin();
        LocalDate date = LocalDate.now().minusDays(1);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created = createEntry(token, date, "08:00", "16:00");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("id")).isNotNull();
        assertThat(created.getBody().get("date")).isEqualTo(date.toString());

        List<Map<String, Object>> entries = listEntries(token);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("id")).isEqualTo(created.getBody().get("id"));
    }

    @Test
    void createEntry_startAfterEnd_returns400() {
        String token = registerAndLogin();

        assertThat(createEntry(token, LocalDate.now(), "16:00", "08:00").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createEntry_futureDate_returns400() {
        String token = registerAndLogin();

        assertThat(createEntry(token, LocalDate.now().plusDays(1), "08:00", "16:00").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateEntry_changesTimes() {
        String token = registerAndLogin();
        LocalDate date = LocalDate.now();
        String id = (String) createEntry(token, date, "08:00", "16:00").getBody().get("id");

        Map<String, String> body = Map.of("date", date.toString(), "start", "09:00", "end", "17:30");
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> res = rest.exchange("/entries/" + id, HttpMethod.PUT,
                new HttpEntity<>(body, bearer(token)), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("start")).isEqualTo("09:00:00");
        assertThat(res.getBody().get("end")).isEqualTo("17:30:00");
    }

    @Test
    void updateEntry_unknownId_returns404() {
        // NOTE: this test is RED until the repository's IllegalStateException
        // is mapped to 404 (currently it surfaces as 500).
        String token = registerAndLogin();
        Map<String, String> body = Map.of("date", LocalDate.now().toString(), "start", "08:00", "end", "16:00");

        ResponseEntity<String> res = rest.exchange("/entries/" + UUID.randomUUID(), HttpMethod.PUT,
                new HttpEntity<>(body, bearer(token)), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteEntry_removesIt() {
        String token = registerAndLogin();
        String id = (String) createEntry(token, LocalDate.now(), "08:00", "16:00").getBody().get("id");

        ResponseEntity<Void> res = rest.exchange("/entries/" + id, HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listEntries(token)).isEmpty();
    }

    @Test
    void deleteEntry_unknownId_returns404() {
        String token = registerAndLogin();

        ResponseEntity<String> res = rest.exchange("/entries/" + UUID.randomUUID(), HttpMethod.DELETE,
                new HttpEntity<>(bearer(token)), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getEntries_withRange_filtersByDate() {
        String token = registerAndLogin();
        LocalDate today = LocalDate.now();
        createEntry(token, today.minusDays(10), "08:00", "16:00");
        createEntry(token, today.minusDays(2), "08:00", "16:00");
        createEntry(token, today, "08:00", "16:00");

        List<Map<String, Object>> inRange = rest.exchange(
                "/entries?from=" + today.minusDays(3) + "&to=" + today.minusDays(1),
                HttpMethod.GET, new HttpEntity<>(bearer(token)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();

        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).get("date")).isEqualTo(today.minusDays(2).toString());
    }

    @Test
    void getEntries_fromAfterTo_returns400() {
        String token = registerAndLogin();
        LocalDate today = LocalDate.now();

        ResponseEntity<String> res = rest.exchange(
                "/entries?from=" + today + "&to=" + today.minusDays(1),
                HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- ownership (IDOR) ----------

    @Test
    void userCannotSeeOtherUsersEntries() {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        createEntry(tokenA, LocalDate.now(), "08:00", "16:00");

        assertThat(listEntries(tokenB)).isEmpty();
    }

    @Test
    void userCannotDeleteOtherUsersEntry() {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        String id = (String) createEntry(tokenA, LocalDate.now(), "08:00", "16:00").getBody().get("id");

        ResponseEntity<String> res = rest.exchange("/entries/" + id, HttpMethod.DELETE,
                new HttpEntity<>(bearer(tokenB)), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listEntries(tokenA)).hasSize(1);
    }

    @Test
    void userCannotUpdateOtherUsersEntry() {
        // NOTE: RED for the same reason as updateEntry_unknownId_returns404
        // (repo throws IllegalStateException -> 500 until mapped to 404).
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        LocalDate date = LocalDate.now();
        String id = (String) createEntry(tokenA, date, "08:00", "16:00").getBody().get("id");

        Map<String, String> body = Map.of("date", date.toString(), "start", "01:00", "end", "02:00");
        ResponseEntity<String> res = rest.exchange("/entries/" + id, HttpMethod.PUT,
                new HttpEntity<>(body, bearer(tokenB)), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listEntries(tokenA).get(0).get("start")).isEqualTo("08:00:00");
    }
}