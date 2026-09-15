CREATE TABLE user_account (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE member (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(120) NOT NULL, nickname VARCHAR(80), active BOOLEAN NOT NULL DEFAULT TRUE,
 qr_storage_path VARCHAR(500), payment_note VARCHAR(255), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE badminton_session (
 id BIGSERIAL PRIMARY KEY, public_token VARCHAR(80) NOT NULL UNIQUE, title VARCHAR(150) NOT NULL, session_date DATE NOT NULL,
 notes TEXT, status VARCHAR(20) NOT NULL, created_by_user_id BIGINT NOT NULL REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), completed_at TIMESTAMPTZ
);
CREATE TABLE session_participant (
 id BIGSERIAL PRIMARY KEY, session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
 member_id BIGINT NOT NULL REFERENCES member(id), member_name_snapshot VARCHAR(120) NOT NULL,
 CONSTRAINT uq_session_participant UNIQUE(session_id, member_id)
);
CREATE TABLE expense (
 id BIGSERIAL PRIMARY KEY, session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
 type VARCHAR(20) NOT NULL, description VARCHAR(255), amount NUMERIC(12,2) NOT NULL CHECK(amount >= 0),
 paid_by_member_id BIGINT NOT NULL REFERENCES member(id), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE settlement_transfer (
 id BIGSERIAL PRIMARY KEY, session_id BIGINT NOT NULL REFERENCES badminton_session(id) ON DELETE CASCADE,
 payer_member_id BIGINT NOT NULL REFERENCES member(id), payee_member_id BIGINT NOT NULL REFERENCES member(id),
 amount NUMERIC(12,2) NOT NULL CHECK(amount > 0), status VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 paid_at TIMESTAMPTZ, undo_until TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE payment_audit (
 id BIGSERIAL PRIMARY KEY, transfer_id BIGINT NOT NULL REFERENCES settlement_transfer(id) ON DELETE CASCADE,
 action VARCHAR(40) NOT NULL, changed_by_type VARCHAR(30) NOT NULL, changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_session_token ON badminton_session(public_token);
CREATE INDEX idx_transfer_session ON settlement_transfer(session_id);
CREATE INDEX idx_expense_session ON expense(session_id);
