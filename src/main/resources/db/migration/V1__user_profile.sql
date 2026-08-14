CREATE TABLE user_profile (
    id                          UUID         PRIMARY KEY,
    keycloak_user_id            UUID         NOT NULL UNIQUE,
    username                    VARCHAR(100),
    display_name                VARCHAR(200),
    profile_picture_url         TEXT,
    profile_picture_object_key  TEXT,
    account_status              VARCHAR(20)  NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL
);
