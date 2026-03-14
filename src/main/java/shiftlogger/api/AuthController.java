package shiftlogger.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import shiftlogger.db.UserRepository;
import shiftlogger.security.JwtService;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthController(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwt
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody RegisterRequest req) {

        if (req.username() == null || req.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username required");
        }
        if (req.password() == null || req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 8 chars");
        }

        String hash = passwordEncoder.encode(req.password());
        UUID userId = UUID.randomUUID();
        try {
            users.insert(userId, req.username(), hash);
            System.out.println("HEI fra register. alt OK!");
            return new RegisterResponse(userId, req.username());
        } catch (Exception e) {
            System.out.println("hei fra register. feilet." + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "username already exists",
                    e
            );
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {

        var user = users.findByUsername(req.username())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Bad credentials"
                        )
                );

        if (!passwordEncoder.matches(req.password(), user.passwordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bad credentials"
            );
        }

        String token = jwt.createToken(
                user.id().toString(),
                user.username()
        );

        return new LoginResponse(token);
    }

    public record RegisterRequest(String username, String password) {}
    public record RegisterResponse(UUID userId, String username) {}

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token) {}
    
}
