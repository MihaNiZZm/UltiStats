ALTER TABLE match_teams ADD COLUMN team_name VARCHAR(255);

UPDATE match_teams
SET team_name = (
    SELECT teams.name FROM teams WHERE teams.id = match_teams.team_id
);

ALTER TABLE match_teams ALTER COLUMN team_name SET NOT NULL;

ALTER TABLE match_participants ADD COLUMN first_name VARCHAR(255);
ALTER TABLE match_participants ADD COLUMN last_name VARCHAR(255);

UPDATE match_participants
SET first_name = (
        SELECT players.first_name FROM players
        WHERE players.id = match_participants.participant_id
    ),
    last_name = (
        SELECT players.last_name FROM players
        WHERE players.id = match_participants.participant_id
    )
WHERE kind = 'PLAYER';

ALTER TABLE match_participants
    ADD CONSTRAINT chk_match_participant_display_snapshot
    CHECK (
        (kind = 'PLAYER' AND first_name IS NOT NULL AND last_name IS NOT NULL)
        OR
        (kind = 'UNKNOWN' AND first_name IS NULL AND last_name IS NULL)
    );
