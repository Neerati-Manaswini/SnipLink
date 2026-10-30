# SnipLink

A URL shortener with a Spring Boot backend and a clean browser dashboard. Paste a long link, optionally choose your own alias and an expiry date, and watch the click count grow.

## Features
- Short codes generated with `SecureRandom` (base-62, 7 characters), or a custom alias
- Optional expiry (1 to 365 days); expired links show a clear message instead of redirecting
- Click tracking with an atomic database increment, plus last-clicked time
- Input validation: only `http`/`https` links, no `javascript:` or `data:` URLs, no links back to the service itself
- Each browser sees only the links it created; deleting a link needs a secret token returned at creation
- Copy-to-clipboard, live totals, light and dark mode
- File-based H2 database: links survive restarts

## How it works
```
POST /api/links          create a short link
GET  /api/links?codes=   list the links whose codes you pass (your own)
GET  /api/links/{code}   stats for one link
DELETE /api/links/{code} delete (requires the X-Delete-Token header)
GET  /{code}             302 redirect to the original URL and count the click
```
Example:
```bash
curl -X POST http://localhost:8080/api/links \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/a/long/page","alias":"demo","expiresInDays":7}'
```
Errors come back as `{"error": "..."}` with status 400 (bad input), 403 (wrong delete token), 404 (unknown code) or 409 (alias taken).

### Design notes
- **302, not 301:** browsers cache permanent redirects, which would hide repeat clicks from the counter.
- **Atomic counter:** `UPDATE ... SET click_count = click_count + 1` runs in the database, so simultaneous clicks are not lost.
- **Collisions:** codes are checked against the database, and a unique constraint is the safety net if two requests race.
- **No accounts:** to keep the project simple, "ownership" is a delete token stored in your browser. Adding Spring Security with real users is the natural next step.

## Run it
Requirements: JDK 17 or newer. Use Maven, or open the folder in Eclipse / IntelliJ / VS Code.

```bash
mvn spring-boot:run
```
Open http://localhost:8080

In Eclipse: **File > Import > Maven > Existing Maven Projects**, choose this folder, then run `SnipLinkApplication.java` as a Java Application.

## Tests
```bash
mvn test
```
- `UrlRulesTest`, `CodeGeneratorTest`: unit tests for validation and code generation
- `LinkApiIntegrationTest`: MockMvc tests for create, redirect, click counting, duplicate alias, invalid URL and delete permissions

## Deploying
Set `app.base-url` in `application.properties` (for example `https://sniplink.onrender.com`) so short links use your public address.

## Tech stack
Java 17, Spring Boot 3, Spring Data JPA, Bean Validation, H2, JUnit 5, MockMvc, HTML, CSS, vanilla JavaScript.

## Ideas to extend
- Spring Security login so links belong to real accounts
- Per-day click charts and referrer tracking with a `ClickEvent` table
- QR code for each short link
- Switch to MySQL or PostgreSQL and add a Dockerfile
