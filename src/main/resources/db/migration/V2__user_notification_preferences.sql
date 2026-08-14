CREATE TABLE user_notification_preferences (
    user_id              UUID         PRIMARY KEY REFERENCES user_profile (id) ON DELETE CASCADE,
    push_enabled         BOOLEAN      NOT NULL DEFAULT true,
    new_podcast_enabled  BOOLEAN      NOT NULL DEFAULT true,
    updated_at           TIMESTAMPTZ  NOT NULL
);
