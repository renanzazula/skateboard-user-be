# skateboard-user-be

## What this service is

Owns the **user/account domain** for the Skateboard platform: user profile, profile
picture, account status, self-deactivation/deletion, and problem reports. It does
**not** own identity, credentials, or authentication — that's Keycloak. It also does
not own notification preferences (moved out to `skateboard-notification-be`, see
migration `V4__drop_user_notification_preferences.sql` and commit `93d351a`).

Place in the system (confirmed by `.docs/README-skateboard-user-be-mobile-self-service.md`
and code):

```
skateboard-fe -> skateboard-ui-backend (BFF) -> skateboard-user-be -> Postgres
                                                                    -> Keycloak (Admin API)
                                                                    -> S3-compatible storage (profile pictures)
```

`GET /api/me` on the FE/BFF side is served by this service's `GET /me`. Currently
scoped to **self-service only** — no admin user-management endpoints exist yet
(they're explicitly out of scope per the README, to be added later as a separate
set of use cases).

## Tech stack

- Java 21, Spring Boot 3.4.4 (Maven, `spring-boot-starter-parent`)
- Spring Web, Spring Data JPA, Spring Security (OAuth2 resource server, JWT)
- PostgreSQL + Flyway (schema-owned migrations, see below)
- Keycloak Admin Client 26.0.7 (`org.keycloak:keycloak-admin-client`) — used only in
  `adapter/out/keycloak`
- AWS SDK v2 `s3` module — used only in `adapter/out/storage`, talks to an
  S3-compatible bucket (MinIO locally, Railway/Tigris in prod)
- springdoc-openapi (Swagger UI) + **openapi-generator-maven-plugin**, generating
  server interfaces/DTOs from `api/openapi.yaml` at build time (see below)
- Tests: JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers (Postgres) + H2
- New Relic Java agent wired in for deploy (`newrelic-config/`, `railpack.json`) — not
  used in local dev

## Build / run / test

Maven wrapper only — use `./mvnw` (or `mvnw.cmd` on native Windows shells), don't
assume a global `mvn`.

```bash
./mvnw clean package          # build (runs openapi-generator + compiles + tests)
./mvnw clean package -DskipTests
./mvnw test                   # unit + slice tests only
./mvnw spring-boot:run        # run locally
```

Local run requires:
- Postgres reachable at `localhost:5432/skateboard` (override via
  `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`) — default schema is
  `skateboard-user` (application.yml) / `skateboard_user` (application-railway.yml,
  underscore — note the mismatch between profiles)
- Keycloak reachable at `http://localhost:8180`, realm **`skateboard-podcast`**
  (yes — that's the one shared platform realm for *every* backend service, not just
  podcast; see `../skateboard-infrastructure/.docker/keycloak/realm-export.json`).
  JWKS is fetched from `{issuer-uri}/protocol/openid-connect/certs` at request time
  (lazy), but a Keycloak Admin client-credentials login happens for admin operations.
- A confidential Keycloak client `skateboard-user-be` with the `manage-users`
  realm-management role (client id matches the required JWT `aud` claim) — separate
  from the public client used for end-user login
- MinIO (or another S3-compatible endpoint) at `localhost:9000` for profile picture
  storage, bucket `skateboard-user`
- **Port 8082** for this service. Sibling local ports: 8080 podcast-be, 8081
  Expo/Metro, 8090 ui-backend, 8180 Keycloak.

The `api/openapi.yaml` spec is the source of truth for the HTTP API — it is compiled
at build time into `com.skateboard.infrastructure.web.api` (interfaces, e.g. `MeApi`)
and `com.skateboard.application.dto` (DTOs). **Edit the YAML, not generated code**;
there is no separate upstream spec. Generated sources land under
`target/generated-sources/`.

## Architecture

Hexagonal, but note the actual package root is `com.skateboard.user` (not just
`com.skateboard`) — generated OpenAPI code intentionally lives under the shorter
`com.skateboard.infrastructure.web.api` / `com.skateboard.application.dto` packages
to match `skateboard-podcast-be`'s layout, so don't be surprised to see both
`com.skateboard.user.*` and `com.skateboard.*` imports side by side.

```
com.skateboard.user
├── domain
│   ├── model            UserProfile, ProblemReport, AccountStatus (enum), ...
│   └── exception         UserNotFoundException
├── application
│   ├── port
│   │   ├── in            *UseCase interfaces (GetCurrentUserUseCase, ...)
│   │   └── out           *Port interfaces (UserRepositoryPort, IdentityProviderPort,
│   │                     ProfileImageStoragePort, ProblemReportRepositoryPort)
│   └── service            One *Service per use case, implements the in-port
├── adapter
│   ├── in/rest            UserController (implements generated MeApi) + UserFacadeService
│   │                      (maps DTO <-> domain, orchestrates use cases)
│   └── out
│       ├── persistence    Spring Data repos + JPA entities + *PersistenceAdapter
│       ├── keycloak        KeycloakIdentityAdapter (the only class touching the
│       │                  Keycloak Admin API), KeycloakAdminConfig
│       └── storage        S3ProfileImageStorageAdapter, S3StorageConfig
└── infrastructure
    ├── security           SecurityConfig, AudienceValidator
    └── web                CorrelationIdFilter, GlobalExceptionHandler
```

Every inbound port has exactly one implementing service (one use case per class).
Domain model (`UserProfile`) is a plain class with private constructor + named
static factories (`provision(...)` for lazy first-time creation, `reconstitute(...)`
for rehydrating from persistence) and behavior methods (`updateDisplayName`,
`deactivate`, `markDeleted`, ...) — not an anemic bean.

## Key conventions / gotchas (evidenced in code)

- **Self-service only, always scoped to the caller's own JWT subject.** Every
  `UserController` method reads the user id from
  `SecurityContextHolder...getAuthentication().getName()` (the JWT `sub`) — there is
  no `{userId}` path param anywhere, by design (README: "PATCH /me", not
  "PATCH /users/{userId}").
- **Every endpoint has two layers of auth**: JWT authentication (audience-checked,
  see `AudienceValidator`) *plus* a `@PreAuthorize("hasAuthority('FUNC_USER_*')")`
  check per operation. These `FUNC_USER_*` authorities are declared as
  `x-required-permissions` in `api/openapi.yaml` and granted to ADMIN and STANDARD
  realm roles (not GUEST) in the shared Keycloak realm-export — same convention as
  `skateboard-podcast-be`.
- **Lazy profile provisioning**: `GetCurrentUserService` creates a `UserProfile` row
  on first `GET /me` if none exists yet (via `UserProfile.provision`), using the
  JWT's `preferred_username` claim as a display-name fallback hint, and calls
  `IdentityProviderPort.ensureTenantAssigned` at that point. There's no
  registration/signup endpoint in this service.
- **No real multi-tenancy yet** — every user is backfilled onto one
  `default-tenant-id` (`app.tenancy.default-tenant-id`) unless a `tenant_id`
  Keycloak user attribute is already set (self-registration / Google login can set
  it). `KeycloakIdentityAdapter.ensureTenantAssigned` never overwrites an existing
  value.
- **Profile pictures are private objects, never public URLs.** `profilePictureUrl`
  in API responses is always a freshly-generated presigned GET URL
  (`ProfileImageStoragePort.presignGetUrl`, 60 min expiry), computed at read time
  from the stored object key — never the stored/cached URL. Reason given in code
  comments: Railway's Tigris-backed bucket doesn't honor per-object canned ACLs the
  way real AWS S3 does.
- **Flyway owns the schema; Hibernate only validates** (`ddl-auto: validate`). Never
  hand-edit the schema via JPA — add a new `V{n}__*.sql` migration under
  `src/main/resources/db/migration`.
- **Delete is anonymize-in-place, not a row delete.** `UserProfile.markDeleted()`
  sets status to `DELETED` and nulls displayName/profile picture fields but keeps
  the row (id, keycloak_user_id, timestamps). Keycloak identity deletion is a
  separate, coordinated step (`DeleteCurrentUserService`), not implied by the DB
  change alone.
- **No caching of `/me`** (see comment in `UserFacadeService`) — deliberately
  different from `skateboard-podcast-be`'s podcast feed, since this data is
  single-row/per-caller and mutated by the same user who reads it.
- Password changes and account disable/delete/username-change all go through
  `IdentityProviderPort` -> `KeycloakIdentityAdapter`, the *only* class in the
  codebase allowed to touch the Keycloak Admin API. Passwords are never stored or
  hashed here.
- `CorrelationIdFilter` + the logging pattern in `application.yml` put a
  correlation id (`%X{correlationId}`) in every log line — check for an incoming
  correlation header convention shared with the BFF before assuming this service
  originates ids.
- `.docs/` contains planning READMEs for **multiple repos**, not just this one —
  `README-skateboard-user-be-mobile-self-service.md` is the one that actually
  describes this service; `README-settings-ui-redesign.md` and
  `README-skateboard-fe-settings.md` are about `skateboard-fe`, and
  `README-youtube-data-api-sync.md` is about `skateboard-podcast-be`. Treat those
  three as cross-repo context, not documentation of this codebase.
- `application.yml` (default) uses schema name `skateboard-user` (hyphen);
  `application-railway.yml` (prod profile) uses `skateboard_user` (underscore) —
  this looks like it could be an inconsistency rather than intentional; verify
  which schema actually exists in each environment before relying on either.

## Uncertain / needs human confirmation

- Whether the `skateboard-user` vs `skateboard_user` schema-name mismatch between
  `application.yml` and `application-railway.yml` is intentional.
- Admin user-management (list users, change role, disable another user, etc.) is
  explicitly out of scope in the README and has no code yet — confirm before adding it.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
