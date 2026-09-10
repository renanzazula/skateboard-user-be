---
name: add-me-endpoint
description: >-
  Add a new self-service operation (endpoint + use case) to skateboard-user-be.
  Walks the full hexagonal vertical slice in the right order — api/openapi.yaml,
  Keycloak FUNC_USER_* permission, domain model, Flyway migration, in-port,
  application service, out-port + persistence adapter, UserController,
  UserFacadeService, and tests — using this repo's exact conventions. Use
  whenever asked to add/change a `/me` endpoint, a new field on the user
  profile, or a new user-domain capability.
---

# Add a self-service `/me` operation

This service is **self-service only**: every operation is scoped to the caller's
own JWT subject, there is **no `{userId}` path param anywhere**, and admin
user-management is explicitly out of scope (README). Keep it that way unless the
user explicitly says they're building the admin surface.

Package roots: hand-written code is `com.skateboard.user.*`; generated OpenAPI
code is `com.skateboard.infrastructure.web.api` (interfaces) and
`com.skateboard.application.dto` (DTOs). Both appear side by side — expected.

## Order of work

Do these in order; each step compiles against the previous one.

### 1. `api/openapi.yaml` — the API is spec-first

The spec is the **source of truth**, compiled at build time into the `MeApi`
interface + DTOs. **Edit the YAML, never generated code under
`target/generated-sources/`.**

- Add the path under `paths:` (e.g. `/me/<thing>`). Pick the HTTP verb the way
  existing ops do: `GET` read, `PATCH` partial profile update, `POST` for
  actions/sub-resources (`/me/username`, `/me/deactivate`, `/me/problem-reports`).
- Give it an `operationId` (camelCase, becomes the `MeApi` method name),
  `tags: [me]`, and `security: - bearerAuth: []`.
- Add `x-required-permissions: [FUNC_USER_<AREA>_<ACTION>]` — one authority.
  Reuse an existing one if it fits (`FUNC_USER_SELF_READ`,
  `FUNC_USER_SELF_UPDATE`, `FUNC_USER_ACCOUNT_DELETE`,
  `FUNC_USER_PASSWORD_CHANGE`, `FUNC_USER_ACCOUNT_DEACTIVATE`,
  `FUNC_USER_PROBLEM_REPORT_CREATE`); only mint a new one for a genuinely new
  capability.
- Declare request/response schemas under `components.schemas`. Match existing
  style: `UserResponse` for anything returning the profile, a dedicated
  `*Request` / `*Response` otherwise. Always wire `'401'` (and `'400'` when
  there's a body) to `ErrorResponse`.
- Response codes follow the existing set: `200` returns `UserResponse`, `201`
  returns a created sub-resource, `204` for actions with no body.

### 2. Keycloak permission — only if you minted a new `FUNC_USER_*`

**Cross-repo, needs human confirmation.** The authority is enforced by
`@PreAuthorize` but only works if the realm grants it. Edit
`../skateboard-infrastructure/.docker/keycloak/realm-export.json`:

1. Add the role to the top-level `roles.realm` array (with a `description`,
   same shape as the other `FUNC_*` entries).
2. Add its name to **both** the `ADMIN` and `STANDARD` composites'
   `composites.realm` arrays (the `STANDARD` role's `description` text also
   enumerates the `FUNC_USER_*` grants — update it). **Not `GUEST`.**

Realm roles flow into the JWT `authorities` claim via the `authorities`
realm-role mapper, which `SecurityConfig` reads with an empty prefix — so no
code change is needed once the composite is set. Flag to the user that the
realm-export change lives in another repo and needs to be applied to running
Keycloak instances.

### 3. Domain model — `com.skateboard.user.domain.model`

- **Existing profile field/behavior**: add a behavior method to `UserProfile`
  (e.g. alongside `updateDisplayName`, `deactivate`, `markDeleted`). It must set
  `this.updatedAt = Instant.now();`. Don't add public setters — `UserProfile` is
  a rich model with a private constructor.
- **New field**: add it to the constructor, `provision(...)`, `reconstitute(...)`,
  a getter, and the behavior method that mutates it. Then do step 4.
- **New aggregate** (like `ProblemReport`): plain class, private constructor,
  static factories `create(...)` (new, `UUID.randomUUID()` + `Instant.now()`)
  and `reconstitute(...)` (rehydrate from persistence), final fields, getters
  only. Tie it to the user via `UUID userId` = `UserProfile.getId()` (the local
  row id, **not** the keycloak id).

### 4. Flyway migration — only if the schema changes

Flyway owns the schema; Hibernate is `ddl-auto: validate`, so a JPA change
without a migration **fails at startup**. Add
`src/main/resources/db/migration/V{next}__<desc>.sql` (currently through `V4`).
New tables reference `user_profile (id)` and use `TIMESTAMPTZ`, `UUID` PKs,
`ON DELETE CASCADE` + an index on `user_id` (see `V3__problem_reports.sql`).

### 5. Inbound port — `application/port/in/<Name>UseCase.java`

```java
public interface <Name>UseCase {
    <ReturnType> execute(Input input);          // or execute(UUID keycloakUserId) if no other args
    record Input(UUID keycloakUserId, ...) {}    // nested record, only when there are args beyond the id
}
```

Return the **domain type** (`UserProfile` / the new aggregate), never a DTO.
One use case per interface.

### 6. Application service — `application/service/<Name>Service.java`

`@Service`, `implements <Name>UseCase`, constructor injection. Pattern:

```java
UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);
profile.<behavior>(...);
return userRepositoryPort.save(profile);
```

- **Always fetch-or-provision through `GetCurrentUserUseCase`**, not
  `UserRepositoryPort` directly — every `/me` op must work for any authenticated
  caller regardless of call order (it lazily provisions the row + backfills
  tenant). Exception: a pure delete/idempotency path may go straight to the repo
  (see `DeleteCurrentUserService`).
- **Anything touching Keycloak** (username, enable/disable, delete, password)
  goes through `IdentityProviderPort` — add a method there and implement it in
  `KeycloakIdentityAdapter` (**the only class allowed to import
  `org.keycloak.*`**). Save the local row *first*, then call the identity
  provider (see `DeactivateCurrentUserService`).
- Passwords are never stored/logged/returned here.

### 7. Outbound persistence — only if there's a new aggregate/repo

Mirror `ProblemReport*`:
- `application/port/out/<Name>RepositoryPort.java` — domain-typed interface.
- `adapter/out/persistence/<Name>JpaEntity.java` — `@Entity @Table(name="...")`,
  plain getters/setters, `String` columns for enums.
- `adapter/out/persistence/Spring<Name>Repository.java` — `extends JpaRepository`.
- `adapter/out/persistence/<Name>PersistenceAdapter.java` — `@Component
  implements <Name>RepositoryPort`, maps domain↔entity, returns
  `<Name>.reconstitute(...)` from the saved entity.

For a new `UserProfile` field, instead just add the column to
`UserProfileJpaEntity` + its `toDomain`/`toEntity` in `UserPersistenceAdapter`.

### 8. `adapter/in/rest/UserController.java`

- Add a `private static final String <PERM> = "hasAuthority('FUNC_USER_...')";`
  constant if new.
- Implement the generated `MeApi` method: `@Override`, `@PreAuthorize(<PERM>)`,
  one line delegating to `userFacadeService`, passing `currentUserId()` (and
  `currentUsernameHint()` only for the provisioning read path).
- Return `ResponseEntity.ok(...)` / `.status(HttpStatus.CREATED).body(...)` /
  `.noContent().build()` to match the spec's response code.
- **Never** add a userId parameter.

### 9. `adapter/in/rest/UserFacadeService.java`

- Add the use case to the constructor (all deps are constructor-injected).
- Add a method: unwrap the DTO into the use case `Input`, call it, map the
  returned domain object back to a DTO via `toUserResponse(...)` /
  `toProblemReportResponse(...)` (add a new mapper if it's a new response type).
- Enum fields: convert by `.name()` / `valueOf(dto.getX().getValue())` across the
  boundary — domain enums and generated DTO enums are distinct types.
- `toUserResponse` **must** keep computing `profilePictureUrl` via
  `profileImageStoragePort.presignGetUrl(profile.getProfilePictureObjectKey())`
  — the stored object is private; the URL is always a fresh 60-min presigned GET,
  never the stored value. No caching of `/me` — deliberate.

### 10. Tests

- **Service test** (`src/test/java/.../application/service/<Name>ServiceTest.java`):
  plain JUnit 5 + `MockitoAnnotations.openMocks(this)` in `@BeforeEach`, `@Mock`
  the ports, AssertJ. Cover: happy path, the lazy-provision path (missing profile
  still works), idempotency/coordination with `IdentityProviderPort` where
  relevant. See `UpdateCurrentUserServiceTest`, `DeleteCurrentUserServiceTest`.
- **Facade test** (`UserFacadeServiceTest`): add a case asserting DTO↔domain
  mapping (both directions for enums).
- Keep the `new UserFacadeService(...)` constructor call in
  `UserFacadeServiceTest.setUp()` in sync with the new dependency.

### 11. Build

```bash
./mvnw clean package          # regenerates MeApi/DTOs from the YAML, compiles, runs tests
```

Use `./mvnw` / `mvnw.cmd` — there is no global `mvn`. If the generated `MeApi`
method signature isn't what you expected, fix the YAML, not the Java.

## Quick checklist

- [ ] `api/openapi.yaml`: path, operationId, `x-required-permissions`, schemas, 401/400
- [ ] New `FUNC_USER_*`? → realm-export.json role + ADMIN & STANDARD composites (cross-repo, tell the user)
- [ ] Domain: behavior method (sets `updatedAt`) / new field in ctor+provision+reconstitute / new aggregate
- [ ] Schema change? → new `V{n}__*.sql`
- [ ] `*UseCase` in-port (+ `Input` record), returns domain type
- [ ] `*Service` — fetch-or-provision via `GetCurrentUserUseCase`; Keycloak only via `IdentityProviderPort`
- [ ] New persistence? → out-port + entity + Spring repo + adapter (`reconstitute`)
- [ ] `UserController`: `@Override` + `@PreAuthorize` + delegate, no userId param
- [ ] `UserFacadeService`: constructor dep + DTO mapping; presigned URL untouched
- [ ] Service test + facade mapping test; fix `UserFacadeServiceTest` constructor
- [ ] `./mvnw clean package` green
