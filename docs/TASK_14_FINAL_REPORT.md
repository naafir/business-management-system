# Task 14 Final Report: Production Hardening Review & Targeted Fixes

## 1. Issues Investigated
- **PostgreSQL vs H2 Test Verification:** Investigated the concurrency tests (Payment, Inventory, Customer Balance, Deadlock). Found they are currently running against an in-memory H2 database configured with PostgreSQL dialect compatibility via `application-test.yml`.
- **Inventory RBAC NullPointerException:** Investigated the log `Cannot invoke "CustomUserDetails.getId()" because "userDetails" is null`. Found it occurred in `RbacSecurityIntegrationTest.java` where `@WithMockUser` created a generic Spring Security `User` instead of the expected `CustomUserDetails`, resulting in a null cast injection in `InventoryController.adjustStock`.
- **Lombok `@Builder.Default` Warning:** Inspected `ExpenseCategory.java` where `createdAt` and `updatedAt` were initialized to `Instant.now()` without the `@Builder.Default` annotation, causing Lombok to ignore the initialization during builder construction.
- **Deprecated Security API:** Examined `LoginRateLimitFilter.java` relying on the deprecated `Bandwidth.classic` API from the Bucket4j library.
- **Secret/Credential Audit:** Scanned the codebase for hardcoded secrets, API keys, and passwords.
- **Git Hygiene:** Reviewed `.gitignore` file.
- **Docker Production Review:** Evaluated `docker-compose.yml` and `Dockerfile`.
- **Observability Review:** Checked logging and MDC implementations from Task 13.

## 2. Issues Actually Found
- **H2 Pretending to be PostgreSQL:** The integration tests do not prove true PostgreSQL locking (like `SELECT ... FOR UPDATE SKIP LOCKED` or PostgreSQL-specific deadlock detection nuances) because they execute against H2. 
- **Test Context Defect:** The NPE in the inventory adjustment endpoint was strictly a test setup deficiency. The production application utilizes the correct `JwtTokenProvider` which injects `CustomUserDetails`.
- **Lombok Builder Issue:** `ExpenseCategory.java` was missing `@Builder.Default`.
- **Deprecated Bucket4j Method:** `Bandwidth.classic` is deprecated in Bucket4j 8.7.0.

## 3. Fixes Made
- **Inventory RBAC NPE Fixed:** Replaced `@WithMockUser` with `SecurityMockMvcRequestPostProcessors.user(...)` and an actual mock `CustomUserDetails` instance in `RbacSecurityIntegrationTest`.
- **Lombok `@Builder.Default` Fixed:** Added `@Builder.Default` to `createdAt` and `updatedAt` in `ExpenseCategory.java`.
- **Bucket4j API Updated:** Upgraded the rate-limiting filter to use `Bandwidth.builder().capacity(...).refillIntervally(...)`.

## 4. Files Changed
- `backend/src/main/java/com/antigravity/billing/entity/ExpenseCategory.java`
- `backend/src/main/java/com/antigravity/billing/security/LoginRateLimitFilter.java`
- `backend/src/test/java/com/antigravity/billing/security/RbacSecurityIntegrationTest.java`

## 5. PostgreSQL vs H2 Test Coverage
**Limitation Documented:** The concurrency integration tests use H2. Introducing Testcontainers for PostgreSQL requires substantial infrastructural changes (adding Docker/Testcontainers dependencies, modifying Spring Boot test contexts, altering CI/CD pipeline dependencies). Following the directive to "report the limitation instead of pretending H2 proves PostgreSQL behavior," I have left the H2 tests intact and formally note this limitation. True PostgreSQL locking semantics are not currently validated by the automated test suite.

## 6. Flyway V1–V10 Verification Result
- Verified successfully using native environment property substitution in Maven. 
- All migrations V1 through V10 are structurally sound. The `flyway:info` command confirms the database connection works perfectly utilizing the intended production environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`). No migration files were altered.

## 7. Secret Audit Result
- **Clean.** No production passwords, API keys, or JWT secrets are hardcoded in Java or SQL files. `.env.example` safely uses placeholders. `application-dev.yml` uses local dummy defaults (`postgres`).

## 8. Docker/Deployment Review Result
- **Clean.** `docker-compose.yml` uses Alpine images, properly maps volumes for PostgreSQL persistence and file uploads, and routes environment variables correctly. 
- **Limitation Documented:** The backend rate-limiting relies on an in-memory Caffeine cache, making it per-instance rather than global across a multi-node cluster.

## 9. Observability Review Result
- **Clean.** `X-Request-ID` is correctly generated and propagated. `GlobalExceptionHandler` masks stack traces. No passwords or tokens are printed in the console.

## 10. Tests Executed and Results
- `mvn clean test` executed: **158 tests passed** (including the repaired RBAC test).
- `npm run build` executed: **Frontend compiled successfully**.
- `git diff --check`: No whitespace or syntax errors found.

## 11. Remaining Known Limitations
1. Integration tests use H2, not PostgreSQL (Testcontainers required for true validation).
2. Rate-limiting is local to the JVM (Caffeine) rather than distributed (Redis).
3. File storage requires a shared persistent volume or an Amazon S3 provider to scale horizontally.

## 12. Explicit Statement
**No commits, pushes, database resets, or destructive SQL commands were performed during this hardening review.**
