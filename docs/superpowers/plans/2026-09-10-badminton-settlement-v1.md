# Badminton Settlement V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a small Spring Boot web app for a badminton club that lets OWNER/ADMIN manage members and sessions, split session expenses equally, generate direct settlements, display member QR codes on demand, track payments, and preserve session history.

**Architecture:** A single Spring Boot application serves both REST APIs and a static HTML/CSS/JavaScript frontend. PostgreSQL is the source of truth for users, members, sessions, expenses, settlement transfers, and audit records; Supabase Storage holds QR images. Settlement logic is isolated in a pure service and tested independently before persistence/payment tracking is layered on top.

**Tech Stack:** Java 17, Spring Boot 4.1.x, Spring Web, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Thymeleaf not required, static HTML/CSS/JS, Supabase PostgreSQL/Storage, Maven, JUnit 5, Testcontainers optional for later integration tests.

**Spec:** `docs/superpowers/specs/2026-09-10-badminton-settlement-v1-design.md`

## Global Constraints

- Only OWNER and ADMIN are authenticated users.
- Public members require no login.
- Public session URLs must use strong opaque random tokens.
- All selected session participants share all expenses equally regardless of play duration.
- Each expense records the member who paid/provided it.
- Money calculations use `BigDecimal`; database money columns use `NUMERIC(12,2)`.
- Public "I've paid" is trust-based and undoable for 5 minutes.
- OWNER/ADMIN may reverse payment state at any time.
- Financial edits after settlement creation require explicit recalculation confirmation.
- Financial edits and settlement replacement must be transactional.
- Session history is session-by-session only in V1.
- QR data is fetched only when a specific payee is clicked.
- Render Free + Supabase Free is the target deployment.
- No React/Angular, payment gateway, weighted attendance, analytics dashboard, notifications, or member accounts in V1.

---

## File Structure

```text
badminton-settlement/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── render.yaml
├── src/main/java/com/fit/badminton/
│   ├── BadmintonSettlementApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── StorageProperties.java
│   │   └── BootstrapAdminProperties.java
│   ├── auth/
│   │   ├── UserAccount.java
│   │   ├── UserRole.java
│   │   ├── UserAccountRepository.java
│   │   ├── UserAccountService.java
│   │   └── AuthController.java
│   ├── member/
│   │   ├── Member.java
│   │   ├── MemberRepository.java
│   │   ├── MemberService.java
│   │   ├── MemberController.java
│   │   └── dto/
│   ├── session/
│   │   ├── BadmintonSession.java
│   │   ├── SessionStatus.java
│   │   ├── SessionParticipant.java
│   │   ├── BadmintonSessionRepository.java
│   │   ├── SessionParticipantRepository.java
│   │   ├── SessionService.java
│   │   ├── SessionController.java
│   │   └── dto/
│   ├── expense/
│   │   ├── Expense.java
│   │   ├── ExpenseType.java
│   │   ├── ExpenseRepository.java
│   │   ├── ExpenseService.java
│   │   └── dto/
│   ├── settlement/
│   │   ├── SettlementCalculator.java
│   │   ├── SettlementResult.java
│   │   ├── MemberBalance.java
│   │   ├── SettlementTransfer.java
│   │   ├── TransferStatus.java
│   │   ├── SettlementTransferRepository.java
│   │   └── SettlementService.java
│   ├── payment/
│   │   ├── PaymentAudit.java
│   │   ├── PaymentAuditAction.java
│   │   ├── ChangedByType.java
│   │   ├── PaymentAuditRepository.java
│   │   └── PaymentService.java
│   ├── publicview/
│   │   ├── PublicSessionController.java
│   │   └── dto/
│   ├── storage/
│   │   ├── QrStorageService.java
│   │   └── SupabaseQrStorageService.java
│   └── common/
│       ├── ApiExceptionHandler.java
│       └── NotFoundException.java
├── src/main/resources/
│   ├── application.properties
│   ├── db/migration/
│   │   └── V1__initial_schema.sql
│   └── static/
│       ├── index.html
│       ├── admin.html
│       ├── session.html
│       ├── css/app.css
│       └── js/
│           ├── admin.js
│           └── public-session.js
└── src/test/java/com/fit/badminton/
    ├── settlement/SettlementCalculatorTest.java
    ├── payment/PaymentServiceTest.java
    ├── session/SessionServiceTest.java
    └── publicview/PublicSessionControllerTest.java
```

---

### Task 1: Scaffold the application and prove database migrations run

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/fit/badminton/BadmintonSettlementApplication.java`
- Create: `src/main/resources/application.properties`
- Create: `src/main/resources/db/migration/V1__initial_schema.sql`
- Test: `src/test/java/com/fit/badminton/BadmintonSettlementApplicationTest.java`

**Interfaces:**
- Produces: runnable Spring Boot application and schema tables used by all later tasks.
- Consumes: none.

- [ ] **Step 1: Create the Maven project with required dependencies**

Use Java 17 and include:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

- [ ] **Step 2: Create the main class**

```java
package com.fit.badminton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BadmintonSettlementApplication {
    public static void main(String[] args) {
        SpringApplication.run(BadmintonSettlementApplication.class, args);
    }
}
```

- [ ] **Step 3: Configure environment-driven PostgreSQL settings**

```properties
spring.application.name=badminton-settlement

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

spring.flyway.enabled=true

server.port=${PORT:8080}

management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
```

- [ ] **Step 4: Write the initial Flyway schema**

Create these tables with foreign keys and timestamps:

```sql
CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    nickname VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    qr_storage_path VARCHAR(500),
    payment_note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE badminton_session (
    id BIGSERIAL PRIMARY KEY,
    public_token VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    session_date DATE NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL,
    created_by_user_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE session_participant (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(id),
    CONSTRAINT uq_session_participant UNIQUE(session_id, member_id)
);

CREATE TABLE expense (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    paid_by_member_id BIGINT NOT NULL REFERENCES member(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE settlement_transfer (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
    payer_member_id BIGINT NOT NULL REFERENCES member(id),
    payee_member_id BIGINT NOT NULL REFERENCES member(id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL,
    paid_at TIMESTAMPTZ,
    undo_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_audit (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL REFERENCES settlement_transfer(id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    changed_by_type VARCHAR(30) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 5: Write a context-load test**

```java
@SpringBootTest
class BadmintonSettlementApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: Run tests and migration startup locally**

Run:

```powershell
mvn clean test
mvn spring-boot:run
```

Expected:
- tests pass
- Flyway applies V1 migration
- `/actuator/health` returns `UP`

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "chore: scaffold badminton settlement app"
```

---

### Task 2: Implement authenticated OWNER/ADMIN accounts

**Files:**
- Create: `auth/UserRole.java`
- Create: `auth/UserAccount.java`
- Create: `auth/UserAccountRepository.java`
- Create: `auth/UserAccountService.java`
- Create: `auth/AuthController.java`
- Create: `config/SecurityConfig.java`
- Test: `auth/AuthControllerTest.java`

**Interfaces:**
- Produces: session-based login and role checks.
- Produces: `UserAccountService.loadUserByUsername(String)`.
- Consumes: `user_account` table.

- [ ] **Step 1: Write failing authentication tests**

Test:
- anonymous `/api/admin/**` returns 401
- valid OWNER credentials create an authenticated session
- `/api/auth/me` returns role
- disabled user cannot log in

- [ ] **Step 2: Run the tests and verify failure**

```powershell
mvn -Dtest=AuthControllerTest test
```

Expected: FAIL because auth components do not exist.

- [ ] **Step 3: Add role and entity**

```java
public enum UserRole {
    OWNER, ADMIN
}
```

Entity fields mirror `user_account`.

- [ ] **Step 4: Implement `UserDetailsService`**

Use BCrypt:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Map OWNER/ADMIN to Spring authorities:

```java
new SimpleGrantedAuthority("ROLE_" + account.getRole().name())
```

- [ ] **Step 5: Configure Spring Security**

Use session authentication, CSRF enabled for authenticated write operations, and rules:

```java
.requestMatchers("/api/public/**", "/", "/session.html", "/css/**", "/js/**").permitAll()
.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "ADMIN")
.anyRequest().authenticated()
```

Return JSON 401/403 rather than HTML login redirects for API calls.

- [ ] **Step 6: Implement auth endpoints**

```http
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

Login request:

```json
{"username":"fit","password":"..."}
```

- [ ] **Step 7: Run auth tests**

```powershell
mvn -Dtest=AuthControllerTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main src/test
git commit -m "feat: add owner and admin authentication"
```

---

### Task 3: Implement member management

**Files:**
- Create: `member/Member.java`
- Create: `member/MemberRepository.java`
- Create: `member/MemberService.java`
- Create: `member/MemberController.java`
- Create: `member/dto/MemberRequest.java`
- Create: `member/dto/MemberResponse.java`
- Test: `member/MemberServiceTest.java`
- Test: `member/MemberControllerTest.java`

**Interfaces:**
- Produces: CRUD-ish member management with deactivate-not-delete semantics.
- Produces: `MemberService.getRequired(long id)`.
- Consumes: authenticated OWNER/ADMIN.

- [ ] **Step 1: Write failing member tests**

Cover:
- create member
- update nickname/payment note
- deactivate member
- list active members
- historical/inactive member remains retrievable by id

- [ ] **Step 2: Verify failure**

```powershell
mvn -Dtest=MemberServiceTest,MemberControllerTest test
```

- [ ] **Step 3: Implement entity and repository**

Expose methods:

```java
List<Member> findAllByOrderByNameAsc();
List<Member> findByActiveTrueOrderByNameAsc();
```

- [ ] **Step 4: Implement service**

Key methods:

```java
MemberResponse create(MemberRequest request)
MemberResponse update(long id, MemberRequest request)
MemberResponse setActive(long id, boolean active)
Member getRequired(long id)
List<MemberResponse> list(boolean activeOnly)
```

- [ ] **Step 5: Implement admin endpoints**

```http
GET  /api/admin/members?activeOnly=true
POST /api/admin/members
GET  /api/admin/members/{id}
PUT  /api/admin/members/{id}
```

- [ ] **Step 6: Run tests**

Expected all member tests PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/fit/badminton/member src/test/java/com/fit/badminton/member
git commit -m "feat: add member management"
```

---

### Task 4: Implement session, participant, and expense editing

**Files:**
- Create: `session/SessionStatus.java`
- Create: `session/BadmintonSession.java`
- Create: `session/SessionParticipant.java`
- Create repositories
- Create: `expense/ExpenseType.java`
- Create: `expense/Expense.java`
- Create repository/service
- Create DTOs
- Create: `session/SessionService.java`
- Create: `session/SessionController.java`
- Test: `session/SessionServiceTest.java`

**Interfaces:**
- Produces: DRAFT session editing.
- Produces: `SessionService.getAggregate(long sessionId)` returning session + participants + expenses.
- Consumes: `MemberService.getRequired`.

- [ ] **Step 1: Write failing tests**

Cover:
- create DRAFT session with random public token
- add participants
- reject duplicate participants
- add multiple expenses
- reject expense payer who is not an existing member
- update non-financial fields

- [ ] **Step 2: Verify tests fail**

```powershell
mvn -Dtest=SessionServiceTest test
```

- [ ] **Step 3: Implement enums**

```java
public enum SessionStatus {
    DRAFT, OPEN, COMPLETED
}

public enum ExpenseType {
    COURT, SHUTTLE, OTHER
}
```

- [ ] **Step 4: Generate opaque public token**

Use:

```java
UUID.randomUUID().toString()
```

Store token separately from numeric ID.

- [ ] **Step 5: Implement create/update DTO**

Example:

```java
public record SessionUpsertRequest(
    String title,
    LocalDate sessionDate,
    String notes,
    List<Long> participantIds,
    List<ExpenseInput> expenses
) {}
```

```java
public record ExpenseInput(
    ExpenseType type,
    String description,
    BigDecimal amount,
    long paidByMemberId
) {}
```

- [ ] **Step 6: Implement DRAFT editing transactionally**

Use:

```java
@Transactional
public SessionDetailResponse updateSession(...)
```

Replace participant/expense collections only after request validation succeeds.

- [ ] **Step 7: Run tests**

Expected PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/fit/badminton/session src/main/java/com/fit/badminton/expense src/test
git commit -m "feat: add session and expense management"
```

---

### Task 5: Implement the settlement calculator with TDD

**Files:**
- Create: `settlement/MemberBalance.java`
- Create: `settlement/SettlementInstruction.java`
- Create: `settlement/SettlementResult.java`
- Create: `settlement/SettlementCalculator.java`
- Test: `settlement/SettlementCalculatorTest.java`

**Interfaces:**
- Produces:

```java
SettlementResult calculate(
    List<Long> participantIds,
    List<ExpenseShareInput> expenses
)
```

- `ExpenseShareInput(long paidByMemberId, BigDecimal amount)`
- `SettlementResult(BigDecimal totalCost, Map<Long,BigDecimal> shares, List<MemberBalance> balances, List<SettlementInstruction> transfers)`

- [ ] **Step 1: Write failing equal-split test**

Example:

```java
@Test
void splitsSimpleSessionEqually() {
    var result = calculator.calculate(
        List.of(1L, 2L, 3L),
        List.of(new ExpenseShareInput(1L, new BigDecimal("90.00")))
    );

    assertThat(result.totalCost()).isEqualByComparingTo("90.00");
    assertThat(result.shares().get(1L)).isEqualByComparingTo("30.00");
    assertThat(result.shares().get(2L)).isEqualByComparingTo("30.00");
    assertThat(result.shares().get(3L)).isEqualByComparingTo("30.00");
}
```

- [ ] **Step 2: Run and verify RED**

```powershell
mvn -Dtest=SettlementCalculatorTest test
```

Expected failure because calculator does not exist.

- [ ] **Step 3: Implement equal cent allocation**

Convert total to cents:

```java
long totalCents = total.movePointRight(2).longValueExact();
long base = totalCents / participantCount;
long remainder = totalCents % participantCount;
```

Sort participant IDs ascending. First `remainder` members receive one extra cent of share.

- [ ] **Step 4: Add failing test for multiple creditors**

Use:

```text
Participants: 1,2,3,4,5,6
Expense: RM120 paid by 1
Expense: RM60 paid by 2
```

Expected:
- member 1 balance +90
- member 2 balance +30
- members 3–6 balance -30

- [ ] **Step 5: Implement net balance calculation**

```java
balance = advanced.subtract(share)
```

- [ ] **Step 6: Add failing test for deterministic transfers**

Expected exact transfer order when IDs are stable.

- [ ] **Step 7: Implement debtor/creditor matching**

Pseudo-code:

```java
while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
    BigDecimal amount = min(debtorRemaining.abs(), creditorRemaining);
    transfers.add(new SettlementInstruction(debtorId, creditorId, amount));
    ...
}
```

Sort both lists by member ID before matching.

- [ ] **Step 8: Add awkward-cent test**

For RM100 / 6:
- four shares are 16.67
- two shares are 16.66
- shares sum exactly to 100.00
- balances sum to zero

- [ ] **Step 9: Add zero-cost and invalid-input tests**

Reject:
- no participants
- negative expense
- payer not in participant list

Allow:
- zero total cost -> zero transfers

- [ ] **Step 10: Run full calculator test suite**

```powershell
mvn -Dtest=SettlementCalculatorTest test
```

Expected: PASS.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/fit/badminton/settlement src/test/java/com/fit/badminton/settlement
git commit -m "feat: add deterministic settlement calculator"
```

---

### Task 6: Persist settlement previews and transfers

**Files:**
- Create: `settlement/SettlementTransfer.java`
- Create: `settlement/TransferStatus.java`
- Create repository
- Create: `settlement/SettlementService.java`
- Modify: `session/SessionController.java`
- Test: `settlement/SettlementServiceTest.java`

**Interfaces:**
- Produces:

```java
SettlementPreview preview(long sessionId)
SettlementPreview recalculateAndPersist(long sessionId)
```

- Consumes `SettlementCalculator`, session participants, expenses.

- [ ] **Step 1: Write failing persistence tests**

Cover:
- preview does not persist transfers
- persist replaces previous generated PENDING transfers
- exact transfer amounts match calculator output

- [ ] **Step 2: Verify failure**

```powershell
mvn -Dtest=SettlementServiceTest test
```

- [ ] **Step 3: Implement `TransferStatus`**

```java
public enum TransferStatus {
    PENDING, PAID
}
```

- [ ] **Step 4: Implement preview**

Map current session aggregate to calculator inputs.

- [ ] **Step 5: Implement transactional persistence**

```java
@Transactional
public SettlementPreview recalculateAndPersist(long sessionId)
```

Delete/replace existing generated transfers only after calculation succeeds.

- [ ] **Step 6: Add endpoint**

```http
POST /api/admin/sessions/{id}/calculate
```

Return the preview/persisted settlement response.

- [ ] **Step 7: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: persist session settlements"
```

---

### Task 7: Implement financial-edit recalculation rules

**Files:**
- Modify: `session/SessionService.java`
- Modify: `settlement/SettlementService.java`
- Create: `payment/PaymentAudit.java`
- Create enums/repository
- Test: `session/SessionFinancialEditTest.java`

**Interfaces:**
- Produces: explicit `recalculate=true` requirement for financial edits on sessions with transfers.
- Consumes: settlement/payment repositories.

- [ ] **Step 1: Write failing tests**

Cover:
- title/notes update does not reset paid transfer
- participant change without confirmation is rejected
- expense amount change without confirmation is rejected
- confirmed financial edit recalculates
- prior affected paid status becomes PENDING
- audit contains `RECALCULATION_RESET`

- [ ] **Step 2: Verify RED**

```powershell
mvn -Dtest=SessionFinancialEditTest test
```

- [ ] **Step 3: Add audit enums**

```java
public enum PaymentAuditAction {
    MARK_PAID, UNDO, ADMIN_RESET, RECALCULATION_RESET
}

public enum ChangedByType {
    PUBLIC_MEMBER, OWNER, ADMIN, SYSTEM
}
```

- [ ] **Step 4: Implement update request flag**

```java
boolean recalculateSettlement
```

Only required when a financial difference is detected and transfers already exist.

- [ ] **Step 5: Detect financial differences**

Compare:
- participant IDs
- expense IDs/types/amounts/payers

Do not classify title/notes-only changes as financial.

- [ ] **Step 6: Make edit + recalculation one transaction**

Use one `@Transactional` service boundary.

- [ ] **Step 7: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: protect settlement consistency on session edits"
```

---

### Task 8: Implement public session view API

**Files:**
- Create: `publicview/PublicSessionController.java`
- Create public DTOs
- Modify repositories with token lookup
- Test: `publicview/PublicSessionControllerTest.java`

**Interfaces:**
- Produces:

```http
GET /api/public/sessions/{token}
```

- Returns public-safe session data only.

- [ ] **Step 1: Write failing public API tests**

Cover:
- anonymous access works
- invalid token returns 404
- numeric database ID alone cannot access public session
- response includes expenses/transfers but no QR storage path or admin user details

- [ ] **Step 2: Verify failure**

- [ ] **Step 3: Implement token repository lookup**

```java
Optional<BadmintonSession> findByPublicToken(String publicToken);
```

- [ ] **Step 4: Implement public DTO**

Include:
- title/date/notes/status
- total cost
- participant count
- shares
- expense summaries
- transfer id, payer display name, payee display name, amount, status, paidAt, undoUntil

Exclude:
- password hashes
- internal account data
- QR storage paths
- unrelated members

- [ ] **Step 5: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: add public session API"
```

---

### Task 9: Implement payment tracking and 5-minute undo

**Files:**
- Create/modify: `payment/PaymentService.java`
- Modify: `PublicSessionController.java`
- Add admin payment controller method
- Test: `payment/PaymentServiceTest.java`

**Interfaces:**
- Produces:

```java
SettlementTransfer markPaid(String token, long transferId)
SettlementTransfer undoPublic(String token, long transferId, Instant now)
SettlementTransfer markUnpaidAdmin(long transferId, UserRole actorRole)
```

- [ ] **Step 1: Write failing payment tests**

Cover:
- PENDING -> PAID
- paidAt set
- undoUntil = paidAt + 5 minutes
- duplicate paid call is idempotent or returns conflict consistently
- undo at 4:59 succeeds
- undo at 5:01 fails
- OWNER/ADMIN reversal always succeeds
- audit written for each change
- transfer from another token/session cannot be modified

- [ ] **Step 2: Verify RED**

```powershell
mvn -Dtest=PaymentServiceTest test
```

- [ ] **Step 3: Implement public endpoints**

```http
POST /api/public/sessions/{token}/transfers/{transferId}/paid
POST /api/public/sessions/{token}/transfers/{transferId}/undo
```

- [ ] **Step 4: Implement admin reversal**

```http
POST /api/admin/transfers/{id}/mark-unpaid
```

Resolve authenticated role and audit as OWNER/ADMIN.

- [ ] **Step 5: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: add payment tracking and undo"
```

---

### Task 10: Implement QR storage and on-demand QR reveal

**Files:**
- Create: `storage/QrStorageService.java`
- Create: `storage/SupabaseQrStorageService.java`
- Create: `config/StorageProperties.java`
- Modify: member service/controller
- Modify: `PublicSessionController.java`
- Test: `storage/QrStorageServiceTest.java`
- Test: public QR access tests

**Interfaces:**
- Produces:

```java
String upload(long memberId, byte[] bytes, String contentType)
QrPayload loadForPublicSession(String token, long payeeMemberId)
```

- [ ] **Step 1: Write failing authorization tests**

Cover:
- valid payee in session can reveal QR
- unrelated member cannot
- member with no QR returns 404
- initial public session response contains no QR URL/path

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Define storage configuration**

Environment variables:

```text
SUPABASE_URL
SUPABASE_SERVICE_KEY
SUPABASE_QR_BUCKET
```

Never commit actual values.

- [ ] **Step 4: Implement member QR upload endpoint**

Use multipart upload:

```http
POST /api/admin/members/{id}/qr
Content-Type: multipart/form-data
```

Accept image MIME types only and enforce a small size limit, e.g. 2 MB.

- [ ] **Step 5: Implement public QR endpoint**

```http
GET /api/public/sessions/{token}/payees/{memberId}/qr
```

Validate the member is actually a payee on a transfer in that session before fetching image data/signed URL.

- [ ] **Step 6: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: add on-demand member QR access"
```

---

### Task 11: Implement session lifecycle and history

**Files:**
- Modify: `SessionService.java`
- Modify: `SessionController.java`
- Add history DTO
- Test: `session/SessionLifecycleTest.java`

**Interfaces:**
- Produces:
  - open
  - complete
  - reopen
  - list history

- [ ] **Step 1: Write failing lifecycle tests**

Cover:
- DRAFT -> OPEN
- OPEN -> COMPLETED
- COMPLETED -> OPEN via reopen
- complete allowed even if manual choice after all transfers paid
- historical records remain queryable
- inactive/renamed members do not break existing session references

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Implement endpoints**

```http
POST /api/admin/sessions/{id}/open
POST /api/admin/sessions/{id}/complete
POST /api/admin/sessions/{id}/reopen
GET  /api/admin/sessions
```

- [ ] **Step 4: Add history ordering**

Order by `sessionDate DESC, id DESC`.

- [ ] **Step 5: Run tests and commit**

```bash
git add src/main src/test
git commit -m "feat: add session lifecycle and history"
```

---

### Task 12: Build the admin frontend

**Files:**
- Create: `static/admin.html`
- Create: `static/js/admin.js`
- Create/modify: `static/css/app.css`

**Interfaces:**
- Consumes admin/member/session APIs.
- Produces browser UI for the two privileged users.

- [ ] **Step 1: Add admin login form**

Fields:
- username
- password

Submit to `/api/auth/login`.

- [ ] **Step 2: Add member management UI**

Show:
- name
- nickname
- active
- QR status
- add/edit
- activate/deactivate

- [ ] **Step 3: Add session editor**

Fields:
- title
- date
- notes
- participant checklist
- repeatable expense rows
- type/description/amount/paid by

- [ ] **Step 4: Add settlement preview**

Display:
- total cost
- shares
- advanced amounts
- net balances
- transfers

Require confirmation before opening.

- [ ] **Step 5: Add financial-edit warning modal**

When server returns a "recalculation required" response:
- show explicit warning
- retry with `recalculateSettlement: true` only after confirmation

- [ ] **Step 6: Add payment management/history**

Show pending/paid and admin reversal controls.

- [ ] **Step 7: Manual browser verification**

Verify:
- owner login
- member CRUD
- session creation
- preview
- open
- payment reversal
- history

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/static
git commit -m "feat: add admin web interface"
```

---

### Task 13: Build the public payment page

**Files:**
- Create: `static/session.html`
- Create: `static/js/public-session.js`
- Modify: `static/css/app.css`

**Interfaces:**
- Consumes public session, QR, paid, undo APIs.

- [ ] **Step 1: Render session summary from token**

Read token from query/path and call:

```http
GET /api/public/sessions/{token}
```

- [ ] **Step 2: Render expense breakdown**

Show type, description, amount, and payer/provider display name.

- [ ] **Step 3: Render transfers**

Each row includes:
- payer
- payee
- amount
- status
- `Show QR`
- `I've paid` when pending

- [ ] **Step 4: Implement on-demand QR reveal**

Do not fetch QR until click.

- [ ] **Step 5: Implement paid action**

After success:
- change row to PAID
- display undo countdown using `undoUntil`

- [ ] **Step 6: Implement short undo**

Hide/disable public undo after expiry and refresh authoritative state from backend.

- [ ] **Step 7: Manual mobile-width test**

Verify at ~390px width:
- no horizontal scrolling
- QR fits viewport
- buttons remain tappable
- transfer rows are readable

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/static
git commit -m "feat: add public settlement page"
```

---

### Task 14: Add production error handling and deployment files

**Files:**
- Create: `common/ApiExceptionHandler.java`
- Create: `common/NotFoundException.java`
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `render.yaml`
- Modify: `application.properties`
- Test: exception handler tests

**Interfaces:**
- Produces deployable Render artifact and consistent JSON errors.

- [ ] **Step 1: Write failing error response tests**

Expected format:

```json
{
  "message": "Transfer not found",
  "timestamp": "..."
}
```

No stack trace or SQL details.

- [ ] **Step 2: Implement exception handler**

Handle:
- validation errors -> 400
- unauthorized -> 401
- forbidden -> 403
- not found -> 404
- conflict -> 409
- unexpected -> 500 generic message

- [ ] **Step 3: Add Dockerfile**

Use multi-stage build:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

- [ ] **Step 4: Add Render configuration**

```yaml
services:
  - type: web
    name: badminton-settlement
    runtime: docker
    plan: free
    healthCheckPath: /actuator/health
```

- [ ] **Step 5: Run full verification**

```powershell
mvn clean test
mvn clean package
java -jar .\target\badminton-settlement-*.jar
```

Expected:
- all tests pass
- package succeeds
- health endpoint reports `UP`

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "chore: prepare badminton app for deployment"
```

---

### Task 15: Configure Supabase and deploy

**Files:**
- No committed secrets.
- Modify deployment environment only.

**Interfaces:**
- Produces public deployed app.

- [ ] **Step 1: Create Supabase project**

Collect:
- PostgreSQL connection URL
- DB username/password
- project URL
- service key
- storage bucket name

- [ ] **Step 2: Create private QR bucket**

Bucket name:

```text
member-qr
```

Keep it non-public; access goes through backend-controlled API.

- [ ] **Step 3: Configure local environment**

Set:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SUPABASE_URL
SUPABASE_SERVICE_KEY
SUPABASE_QR_BUCKET
```

- [ ] **Step 4: Run app against Supabase**

```powershell
mvn spring-boot:run
```

Verify Flyway migration succeeds and create/read/update flows persist.

- [ ] **Step 5: Push to personal GitHub repo**

```bash
git push -u origin main
```

- [ ] **Step 6: Create Render service**

Set all environment variables in Render dashboard.

Health check:

```text
/actuator/health
```

- [ ] **Step 7: Verify deployed behavior**

Check:
- health
- OWNER login
- member creation
- QR upload
- session creation
- settlement generation
- public token page
- QR reveal
- mark paid
- undo
- admin reversal
- completion/history

- [ ] **Step 8: Record the live URL in README**

Add:
- live URL
- tech stack
- architecture summary
- trust-based payment disclaimer
- deployment notes

- [ ] **Step 9: Commit README**

```bash
git add README.md
git commit -m "docs: add deployment and usage guide"
git push
```

---

## Plan Self-Review

### Spec coverage

Covered:
- OWNER/ADMIN auth
- member records
- optional QR per member
- session creation/editing
- participants
- multiple expenses and payer attribution
- equal-share rule
- BigDecimal/cent-safe settlement
- deterministic direct transfers
- public opaque token
- QR-on-click
- public mark-paid
- 5-minute undo
- admin reversal
- audit trail
- financial edit recalculation
- lifecycle/history
- free-tier deployment
- future analytics-compatible schema

### Placeholder scan

No TBD/TODO/“implement later” placeholders remain in the implementation requirements.

### Type consistency

The plan consistently uses:
- `BigDecimal` for money
- `long`/`Long` for member/session IDs
- `String publicToken`
- `UserRole {OWNER, ADMIN}`
- `SessionStatus {DRAFT, OPEN, COMPLETED}`
- `ExpenseType {COURT, SHUTTLE, OTHER}`
- `TransferStatus {PENDING, PAID}`
- 5-minute `undoUntil`
