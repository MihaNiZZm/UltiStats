ALTER TABLE match_players RENAME TO match_participants;
ALTER TABLE match_participants RENAME COLUMN player_id TO participant_id;
ALTER TABLE match_participants DROP CONSTRAINT match_players_player_id_fkey;

ALTER TABLE match_participants ADD COLUMN player_id UUID REFERENCES players(id);
ALTER TABLE match_participants ADD COLUMN kind VARCHAR(16);
ALTER TABLE match_participants ADD COLUMN unknown_slot INTEGER;

UPDATE match_participants
SET player_id = participant_id,
    kind = 'PLAYER';

ALTER TABLE match_participants ALTER COLUMN kind SET NOT NULL;
ALTER TABLE match_participants
    ADD CONSTRAINT uq_match_participants_player UNIQUE (match_id, player_id);
ALTER TABLE match_participants
    ADD CONSTRAINT uq_match_participants_unknown_slot UNIQUE (match_id, team_id, unknown_slot);
ALTER TABLE match_participants
    ADD CONSTRAINT chk_match_participants_shape CHECK (
        (kind = 'PLAYER' AND player_id IS NOT NULL AND unknown_slot IS NULL)
        OR
        (kind = 'UNKNOWN' AND player_id IS NULL AND unknown_slot IN (1, 2) AND number IS NULL)
    );

ALTER TABLE events RENAME COLUMN from_player_id TO from_participant_id;
ALTER TABLE events RENAME COLUMN to_player_id TO to_participant_id;

INSERT INTO match_participants (
    match_id,
    participant_id,
    team_id,
    player_id,
    kind,
    unknown_slot,
    number
)
SELECT
    match_teams.match_id,
    md5(match_teams.match_id::text || ':' || match_teams.team_id::text || ':' || slots.slot::text)::uuid,
    match_teams.team_id,
    NULL,
    'UNKNOWN',
    slots.slot,
    NULL
FROM match_teams
CROSS JOIN generate_series(1, 2) AS slots(slot);
