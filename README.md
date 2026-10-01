# shiftlogger-api

REST-API for registrering av arbeidstimer, bygget med sikkerhet som utgangspunkt og i drift 24/7 på egen server.

## Høydepunkter

- **Stateless JWT-autentisering** (HS256) med 24 timers levetid.
- **BCrypt** for passordhashing
- **Streng tilgangskontroll:** Brukeren hentes alltid fra det verifiserte tokenet, aldri fra request-body, og alle databaseoperasjoner filtreres på bruker-ID. En bruker kan ikke lese eller endre andres data.
- **Forsvar i reverse proxy:** nginx håndterer TLS og rate limiting, med strengere grenser på innlogging og registrering for å bremse brute force.
- **Minimal eksponering:** Både API-et og databasen lytter kun på localhost. Secrets ligger utenfor repoet.
- **Håndskrevet SQL** med `JdbcTemplate` og parametriserte spørringer, uten ORM

## Arkitektur

```
Internett → nginx (TLS, rate limiting) → Spring Boot (localhost) → PostgreSQL (Docker, localhost)
```

## Endepunkter

| Metode | Sti | Beskrivelse | Auth |
|--------|-----|-------------|------|
| POST | `/auth/register` | Opprett bruker | Nei |
| POST | `/auth/login` | Logg inn, returnerer JWT | Nei |
| GET | `/entries?from=&to=` | Hent egne timeregistreringer | JWT |
| POST | `/entries` | Opprett registrering | JWT |
| PUT | `/entries/{id}` | Oppdater egen registrering | JWT |
| DELETE | `/entries/{id}` | Slett egen registrering | JWT |
| GET | `/health`, `/ready` | Helsesjekk | Nei |

## Teknologier

Java 21 · Spring Boot 3 · Spring Security · PostgreSQL 16 · Docker · nginx · Let's Encrypt · JUnit · Testcontainers

## Kjøre lokalt

Krever Java 21, Maven og Docker.

```bash
# Start Postgres
docker compose up -d

# Sett miljøvariabler
export DB_URL=jdbc:postgresql://localhost:5432/timetracker
export DB_USER=...
export DB_PASS=...
export JWT_SECRET=...   # minst 32 byte

# Kjør tester (inkludert integrasjonstester mot ekte Postgres)
mvn test

# Start appen
mvn spring-boot:run
```

## Tester

- **Enhetstester** for tokenhåndtering (inkludert avvisning av manipulerte og utløpte tokens) og forretningsregler
- **Integrasjonstester** med Testcontainers: ekte HTTP-kall mot ekte Postgres, fra registrering og innlogging til CRUD, og verifisering av at brukere ikke kan endre hverandres data

## TODO


- [ ] Databasemigrasjoner med Flyway
- [ ] Refresh tokens og token-revokering
- [ ] Rollebasert autorisasjon
- [ ] Web-klient