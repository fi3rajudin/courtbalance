# Badminton Court & Shuttle Settlement App — V1 Design Spec

Date: 2026-09-10

## Purpose

Build a small, low-maintenance web app for a badminton club of fewer than 10 regular members.

Two privileged users — OWNER and ADMIN/TREASURER/HOST — can create and manage badminton sessions, record court/shuttle/other expenses, record who advanced each expense, calculate an equal split among attendees, generate direct settlement transfers, display a payee's DuitNow QR only after that payee is clicked, and track payment status.

Ordinary members do not need accounts. They use a shareable read-only session link and may mark a specific transfer as paid using a trust-based flow.

## V1 scope

Included:
- OWNER and ADMIN authentication
- Member management
- Optional saved QR for every member
- Create/edit sessions
- Select participants
- Multiple expenses per session
- Expense types: COURT, SHUTTLE, OTHER
- Record who paid/provided each expense
- Equal sharing regardless of play duration
- Direct settlement generation
- Exact cent-safe rounding
- Settlement preview
- Shareable public session link
- QR reveal only after clicking payee
- Public "I've paid"
- 5-minute public undo
- OWNER/ADMIN reversal
- DRAFT / OPEN / COMPLETED lifecycle
- Session-by-session history
- Financial-edit warning + recalculation
- Payment audit trail
- Free-tier deployment target

Deferred from V1:
- Weighted attendance
- Bank/payment verification
- Payment gateway
- Dynamic DuitNow amount QR
- Ordinary-member accounts
- React/Angular/mobile app
- Monthly analytics
- Notifications
- Multi-club tenancy

## Roles

OWNER:
- Full control
- Manage members and QR images
- Create/edit sessions
- Manage expenses and participants
- Recalculate settlements
- Open/complete/reopen sessions
- Reverse payment state
- Manage ADMIN access
- View history

ADMIN:
- Manage members and QR images
- Create/edit sessions
- Manage expenses and participants
- Recalculate settlements
- Open/complete/reopen sessions
- Reverse payment state
- View history
- Cannot remove/demote OWNER

PUBLIC MEMBER:
- No login
- View shared session
- View expenses and settlement
- Reveal specific payee QR
- Mark transfer paid
- Undo within 5 minutes
- Cannot edit session structure

## Architecture

Browser
- Admin UI
- Public session UI

Spring Boot
- Authentication/authorization
- Member service
- Session service
- Expense service
- Settlement calculator
- Payment tracking
- Public session API

PostgreSQL
- Users
- Members
- Sessions
- Participants
- Expenses
- Transfers
- Audit

Supabase Storage
- Member QR images

Deployment:
GitHub -> Render Free Web Service -> Supabase Free PostgreSQL + Storage

## Core data model

UserAccount
- id
- username
- password_hash
- role: OWNER | ADMIN
- active
- created_at
- updated_at

Member
- id
- name
- nickname
- active
- qr_storage_path
- payment_note
- created_at
- updated_at

BadmintonSession
- id
- public_token
- title
- session_date
- notes
- status: DRAFT | OPEN | COMPLETED
- created_by_user_id
- created_at
- updated_at
- completed_at

SessionParticipant
- id
- session_id
- member_id

Expense
- id
- session_id
- type: COURT | SHUTTLE | OTHER
- description
- amount
- paid_by_member_id
- created_at
- updated_at

SettlementTransfer
- id
- session_id
- payer_member_id
- payee_member_id
- amount
- status: PENDING | PAID
- paid_at
- undo_until
- created_at
- updated_at

PaymentAudit
- id
- transfer_id
- action: MARK_PAID | UNDO | ADMIN_RESET | RECALCULATION_RESET
- changed_by_type: PUBLIC_MEMBER | OWNER | ADMIN | SYSTEM
- changed_at

## Settlement rules

All selected participants share the session total equally.

totalSessionCost = sum(all expenses)

fairShare = totalSessionCost / participantCount

For each participant:

netBalance = totalAdvancedByMember - fairShare

Interpretation:
- positive -> should receive money
- negative -> should pay money
- zero -> already settled

Example:

6 players
Court RM120 -> Admin
Shuttle RM60 -> Fit

Total RM180
Fair share RM30 each

Balances:
Admin +RM90
Fit +RM30
Ali -RM30
John -RM30
Sarah -RM30
Bob -RM30

Possible direct settlement:
Ali -> Admin RM30
John -> Admin RM30
Sarah -> Admin RM30
Bob -> Fit RM30

## Money and rounding

Java uses BigDecimal.
PostgreSQL uses NUMERIC/DECIMAL.
Never use double/float for money.

Example:
RM100 = 10000 cents
10000 / 6 = 1666 cents remainder 4

4 participants pay RM16.67
2 participants pay RM16.66
Total remains exactly RM100.00

Remainder allocation and transfer generation must be deterministic.

## Direct transfer generation

1. Calculate exact member balances.
2. Build ordered creditor list.
3. Build ordered debtor list.
4. Match debtors to creditors.
5. Transfer min(amount owed, amount receivable).
6. Continue until all balances are zero.

Use stable ordering so unchanged input produces unchanged settlement output.

## Session lifecycle

DRAFT
- Fully editable
- Settlement preview allowed
- Public settlement not final

OPEN
- Public link active
- Payment tracking active
- OWNER/ADMIN may still edit
- Financial edits require warning + recalculation

COMPLETED
- Retained in history
- Public page remains viewable
- OWNER/ADMIN may reopen/correct
- Completion is explicit, not automatic

## Editing after payments start

Non-financial edits:
- title
- notes
- descriptive fields

These preserve payment status.

Financial edits:
- add/remove participant
- add/remove expense
- change amount
- change expense payer

Show warning before applying:

"This change affects the settlement. Current payment records may no longer be valid. Recalculate settlement and reset affected payments?"

If confirmed:
1. Save edit.
2. Recalculate settlement.
3. Replace/update affected transfers.
4. Reset affected statuses to PENDING.
5. Write audit entries.

All of this must be transactional.

## Payment tracking

Public transfer row:

Ali -> Fit
RM30.00
[I've paid]

After click:
- status = PAID
- paid_at = now
- undo_until = now + 5 minutes

During window:
Paid
[Undo] with remaining time

After 5 minutes:
- public undo no longer allowed
- OWNER/ADMIN can still reverse

Because public users are unauthenticated, audit records use PUBLIC_MEMBER instead of pretending to know the actual person who clicked.

## QR handling

Every member may optionally have a saved DuitNow QR.

QRs are not included in the initial public session response.

Public flow:
Ali -> Fit RM30
[Show Fit's QR]

Only after click:
Pay Fit
RM30.00
[QR image]

Database stores the QR object path/reference only.
Actual image stored in Supabase Storage.

## Admin screens

Dashboard:
- Open sessions
- Unsettled payments
- Recent sessions
- Create session

Members:
- Add/edit member
- Activate/deactivate
- Upload/replace QR
- Payment note

Create/Edit Session:
- title
- date
- notes
- participants
- expense rows with type, description, amount, paid/provided by

Settlement Preview:
- total cost
- participant count
- share
- amount advanced
- net balance
- generated transfers

Payment Management:
- transfer states
- admin reversal
- audit trail
- recalculation controls

History:
- session-by-session only in V1

## Public session page

Accessible via opaque random token.

Shows:
- title/date
- total session cost
- participant count
- average/equal share
- expense breakdown
- transfer instructions
- payee QR reveal
- paid state
- short undo

No unrelated member QR browsing.

## API design

Authentication:
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me

Admin members:
GET  /api/admin/members
POST /api/admin/members
GET  /api/admin/members/{id}
PUT  /api/admin/members/{id}

Admin sessions:
GET  /api/admin/sessions
POST /api/admin/sessions
GET  /api/admin/sessions/{id}
PUT  /api/admin/sessions/{id}

Expenses:
POST   /api/admin/sessions/{id}/expenses
PUT    /api/admin/sessions/{id}/expenses/{expenseId}
DELETE /api/admin/sessions/{id}/expenses/{expenseId}

Settlement/session lifecycle:
POST /api/admin/sessions/{id}/calculate
POST /api/admin/sessions/{id}/open
POST /api/admin/sessions/{id}/complete
POST /api/admin/sessions/{id}/reopen

Admin payment:
POST /api/admin/transfers/{id}/mark-unpaid

Public:
GET  /api/public/sessions/{token}
GET  /api/public/sessions/{token}/payees/{memberId}/qr
POST /api/public/sessions/{token}/transfers/{transferId}/paid
POST /api/public/sessions/{token}/transfers/{transferId}/undo

Public endpoints must verify the referenced transfer/payee belongs to the supplied session token.

## Authentication and security

Use Spring Security session-based authentication for V1.

Rules:
/api/admin/** -> OWNER or ADMIN
/api/auth/** -> auth flow
/api/public/** -> no login

Use strong opaque random public tokens.
Do not expose sequential IDs as public session identifiers.
Passwords stored only as secure hashes.
Secrets/database credentials supplied through environment variables and never committed.

## Error handling and consistency

Financial updates are transactional.

On any failure:
ROLLBACK
Keep previous valid settlement intact.

Public endpoints handle:
- already paid
- expired undo
- wrong session token
- duplicate clicks
- restricted session state

Do not expose stack traces or DB details to users.

## Testing strategy

Settlement engine tests:
- equal split
- one member fronts all
- multiple members front costs
- several creditors/debtors
- awkward-cent division
- deterministic remainder allocation
- deterministic transfer generation
- zero-cost session
- participant changes
- expense changes
- all balances sum to zero
- transfer totals reconcile exactly

Payment tests:
- mark paid
- duplicate paid click
- undo within 5 minutes
- expired undo rejected
- admin reversal
- audit record

Session tests:
- non-financial edit preserves statuses
- financial edit recalculates
- affected statuses reset
- recalculation transactional
- history remains intact

Security tests:
- public endpoint anonymous access
- admin endpoint rejects anonymous
- token isolation
- QR access limited to valid payee/session

## Deployment

Render Free Web Service
- Spring Boot
- static HTML/CSS/JS
- health check /actuator/health
- server.port=${PORT:8080}

Supabase Free
- PostgreSQL
- QR storage

Expected constraints:
- Render free cold starts after inactivity
- Supabase free-tier inactivity/limits may change
- free availability cannot be guaranteed forever
- this club's data volume is tiny

## Future V2 possibilities

- Monthly spending totals
- Court vs shuttle trends
- Average cost per session/player
- Member contribution summaries
- Shuttle usage analytics
- Notifications
- PWA/mobile improvements
- Member accounts
- Payment-provider integration
- Multi-club support

## Recommended implementation order

1. Project scaffold/config
2. Supabase/PostgreSQL connection
3. Entities + migrations
4. Member management
5. Session + participant management
6. Expense management
7. Settlement calculator + tests
8. Settlement persistence
9. Public session page
10. QR reveal
11. Payment tracking + undo
12. OWNER/ADMIN auth + reversal
13. Lifecycle/history
14. Deployment hardening
15. GitHub + Render + Supabase deployment
