-- ============================================================
-- PhoneMarket existing database upgrade
-- Run this file ONCE on an existing database.
-- A newly created database should use phonemarket-full-schema.sql instead.
-- ============================================================

USE phone_market;

ALTER TABLE game
    ADD COLUMN started_at DATETIME NULL
        COMMENT 'Time when the owner started the game'
        AFTER created_at,
    ADD COLUMN last_activity_at DATETIME NULL
        COMMENT 'Reset after every successful round submission'
        AFTER started_at,
    ADD COLUMN finished_at DATETIME NULL
        AFTER last_activity_at,
    ADD COLUMN finished_reason VARCHAR(40) NULL
        COMMENT 'NORMAL, INACTIVITY_TIMEOUT or OWNER_ABORTED'
        AFTER finished_at,
    ADD INDEX idx_game_status_activity (status, last_activity_at);

-- Give old running games an initial activity time so they do not contain NULL.
UPDATE game
SET started_at = COALESCE(started_at, created_at),
    last_activity_at = COALESCE(last_activity_at, started_at, created_at)
WHERE status = 'RUNNING';

-- Mark historical terminal games with a completion time where possible.
UPDATE game
SET finished_at = COALESCE(finished_at, created_at),
    finished_reason = COALESCE(
        finished_reason,
        CASE
            WHEN status = 'ABORTED' THEN 'OWNER_ABORTED'
            WHEN status = 'FINISHED' THEN 'NORMAL'
            ELSE NULL
        END
    )
WHERE status IN ('FINISHED', 'ABORTED');
