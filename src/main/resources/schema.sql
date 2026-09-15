CREATE TABLE IF NOT EXISTS users (
    id            uuid PRIMARY KEY,
    username      text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS time_entries (
    id         uuid PRIMARY KEY,
    user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date date NOT NULL,
    start_time time NOT NULL,
    end_time   time NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_edit  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT time_entries_start_before_end CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_time_entries_user_date
    ON time_entries (user_id, entry_date);