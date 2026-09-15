# Badminton Settlement Application

Small Spring Boot web app for splitting badminton court/shuttle costs among club members.

https://courtbalance.onrender.com/

## V1 features

- OWNER/ADMIN login
- member and DuitNow QR management
- session participants and multiple expenses
- exact equal-share settlement using `BigDecimal`
- deterministic direct debtor-to-creditor transfers
- public shareable session link
- trust-based "I've paid" + 5-minute undo
- admin payment reversal API
- DRAFT / OPEN / COMPLETED session history
- PostgreSQL persistence and Supabase Storage QR images

## Local requirements

- Java 17+
- Maven 3.9+
- PostgreSQL, or a Supabase PostgreSQL connection

Run:

```bash
mvn clean test
mvn spring-boot:run
```

Open `http://localhost:8080/admin.html`.

## Supabase setup

1. Create a Supabase project.
2. Create a private Storage bucket named `member-qr`.
3. Use the PostgreSQL connection details for `DB_*`.
4. Put the Supabase project URL and service-role key in environment variables only. Never commit them.

## Public session link

After an OWNER/ADMIN opens a session, share:

```text
https://<host>/session.html?token=<public-token>
```

The public page can view the settlement, reveal a specific payee QR, mark a transfer paid, and undo that mark for 5 minutes. This is intentionally trust-based; it is not proof of a bank payment.

## Render

The included `Dockerfile` and `render.yaml` are ready for a Render web service. Add the environment variables in Render and use `/actuator/health` as the health check.

## Money rules

All participants selected for a session split all session expenses equally. Remainder cents are assigned deterministically by ascending member ID so the total always reconciles exactly.

## Security notes

- Authenticated admin APIs use Spring Security sessions.
- Admin API CSRF uses the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header.
- Public session identifiers are opaque UUID tokens rather than sequential IDs.
- QR objects remain in a private Supabase bucket and are fetched through the backend only for valid session payees.
- Do not expose the Supabase service-role key to browser JavaScript.
