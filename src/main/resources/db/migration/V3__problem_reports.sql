CREATE TABLE problem_reports (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES user_profile (id) ON DELETE CASCADE,
    category     VARCHAR(30)  NOT NULL,
    message      TEXT         NOT NULL,
    app_version  VARCHAR(50),
    platform     VARCHAR(20),
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_problem_reports_user_id ON problem_reports (user_id);
