package shiftlogger.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for JwtService. No Spring context needed since the
 * secret is injected through the constructor.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-definitely-longer-than-32-bytes";
    private static final String OTHER_SECRET = "a-completely-different-secret-also-longer-than-32-bytes";

    @Test
    void constructor_secretShorterThan32Bytes_throws() {
        assertThatThrownBy(() -> new JwtService("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_nullSecret_throws() {
        assertThatThrownBy(() -> new JwtService(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createToken_thenVerify_returnsSameClaims() {
        JwtService jwt = new JwtService(SECRET);
        String userId = UUID.randomUUID().toString();

        String token = jwt.createToken(userId, "ulrik");
        JwtClaims claims = jwt.verify(token);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.username()).isEqualTo("ulrik");
    }

    @Test
    void verify_tokenSignedWithOtherKey_throws() {
        JwtService signer = new JwtService(OTHER_SECRET);
        JwtService verifier = new JwtService(SECRET);

        String token = signer.createToken(UUID.randomUUID().toString(), "ulrik");

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_tamperedSignature_throws() {
        JwtService jwt = new JwtService(SECRET);
        String token = jwt.createToken(UUID.randomUUID().toString(), "ulrik");

        // Flip the last character of the signature segment.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwt.verify(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_tamperedPayload_throws() {
        JwtService jwt = new JwtService(SECRET);
        String token = jwt.createToken(UUID.randomUUID().toString(), "ulrik");

        // header.payload.signature — replace payload with a different valid-looking one
        String[] parts = token.split("\\.");
        String otherPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"someone-else\",\"username\":\"admin\"}".getBytes());
        String tampered = parts[0] + "." + otherPayload + "." + parts[2];

        assertThatThrownBy(() -> jwt.verify(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_garbage_throws() {
        JwtService jwt = new JwtService(SECRET);

        assertThatThrownBy(() -> jwt.verify("not.a.jwt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwt.verify(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}