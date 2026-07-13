ALTER TABLE teams
    ALTER COLUMN city TYPE VARCHAR(127),
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE players
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE matches
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE team_players (
    team_id UUID NOT NULL REFERENCES teams(id),
    player_id UUID NOT NULL REFERENCES players(id),
    number INTEGER,
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (team_id, player_id),
    CHECK (number IS NULL OR number >= 0)
);

CREATE UNIQUE INDEX uq_active_team_players_number
    ON team_players(team_id, number)
    WHERE deleted_at IS NULL AND number IS NOT NULL;

CREATE INDEX idx_team_players_player_id
    ON team_players(player_id);

INSERT INTO team_players (team_id, player_id, number)
SELECT team_id, id, number
FROM players
WHERE team_id IS NOT NULL;

CREATE TABLE match_teams (
    match_id UUID NOT NULL REFERENCES matches(id),
    team_id UUID NOT NULL REFERENCES teams(id),
    position INTEGER NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (match_id, team_id),
    UNIQUE (match_id, position),
    CHECK (position IN (1, 2)),
    CHECK (score >= 0)
);

CREATE INDEX idx_match_teams_team_id_match_id
    ON match_teams(team_id, match_id);

INSERT INTO match_teams (match_id, team_id, position, score)
SELECT
    match_row.id,
    match_team.team_id,
    match_team.position,
    COALESCE((
        SELECT (team_score.value ->> 'score')::INTEGER
        FROM jsonb_array_elements(match_row.team_scores) AS team_score(value)
        WHERE team_score.value ->> 'teamId' = match_team.team_id::TEXT
    ), 0)
FROM matches AS match_row
CROSS JOIN LATERAL unnest(match_row.team_ids)
    WITH ORDINALITY AS match_team(team_id, position);

CREATE TABLE match_players (
    match_id UUID NOT NULL,
    player_id UUID NOT NULL REFERENCES players(id),
    team_id UUID NOT NULL,
    number INTEGER,
    PRIMARY KEY (match_id, player_id),
    FOREIGN KEY (match_id, team_id) REFERENCES match_teams(match_id, team_id),
    UNIQUE (match_id, team_id, number),
    CHECK (number IS NULL OR number >= 0)
);

WITH event_participants AS (
    SELECT DISTINCT
        match_row.id AS match_id,
        COALESCE(event_row.value ->> 'fromPlayer', event_row.value ->> 'player')::UUID AS player_id,
        COALESCE(event_row.value ->> 'fromTeam', event_row.value ->> 'team')::UUID AS team_id
    FROM matches AS match_row
    CROSS JOIN LATERAL jsonb_array_elements(match_row.events) AS event_row(value)
    WHERE COALESCE(event_row.value ->> 'fromPlayer', event_row.value ->> 'player') IS NOT NULL

    UNION

    SELECT DISTINCT
        match_row.id,
        (event_row.value ->> 'toPlayer')::UUID,
        (event_row.value ->> 'toTeam')::UUID
    FROM matches AS match_row
    CROSS JOIN LATERAL jsonb_array_elements(match_row.events) AS event_row(value)
    WHERE event_row.value ->> 'toPlayer' IS NOT NULL
)
INSERT INTO match_players (match_id, player_id, team_id, number)
SELECT participant.match_id, participant.player_id, participant.team_id, player.number
FROM event_participants AS participant
JOIN players AS player ON player.id = participant.player_id
JOIN match_teams AS match_team
    ON match_team.match_id = participant.match_id
    AND match_team.team_id = participant.team_id
ON CONFLICT (match_id, player_id) DO NOTHING;

INSERT INTO match_players (match_id, player_id, team_id, number)
SELECT match_team.match_id, team_player.player_id, match_team.team_id, team_player.number
FROM match_teams AS match_team
JOIN team_players AS team_player
    ON team_player.team_id = match_team.team_id
    AND team_player.deleted_at IS NULL
ON CONFLICT (match_id, player_id) DO NOTHING;

CREATE TABLE events (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id),
    sequence_number INTEGER NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    from_player_id UUID,
    to_player_id UUID,
    team_id UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (match_id, sequence_number),
    FOREIGN KEY (match_id, from_player_id) REFERENCES match_players(match_id, player_id),
    FOREIGN KEY (match_id, to_player_id) REFERENCES match_players(match_id, player_id),
    FOREIGN KEY (match_id, team_id) REFERENCES match_teams(match_id, team_id),
    CHECK (sequence_number > 0)
);

INSERT INTO events (
    id,
    match_id,
    sequence_number,
    event_type,
    occurred_at,
    from_player_id,
    to_player_id,
    team_id
)
SELECT
    gen_random_uuid(),
    match_row.id,
    event_row.position::INTEGER,
    event_row.value ->> 'type',
    (event_row.value ->> 'realTimestamp')::TIMESTAMP WITH TIME ZONE,
    COALESCE(event_row.value ->> 'fromPlayer', event_row.value ->> 'player')::UUID,
    (event_row.value ->> 'toPlayer')::UUID,
    CASE
        WHEN event_row.value ->> '_eventClass' = 'TeamEvent'
            THEN (event_row.value ->> 'team')::UUID
        ELSE NULL
    END
FROM matches AS match_row
CROSS JOIN LATERAL jsonb_array_elements(match_row.events)
    WITH ORDINALITY AS event_row(value, position);

DROP INDEX idx_matches_team_ids;

ALTER TABLE players
    DROP COLUMN team_id,
    DROP COLUMN number;

ALTER TABLE matches
    DROP COLUMN team_ids,
    DROP COLUMN events,
    DROP COLUMN team_scores,
    DROP COLUMN disk_holder_id;
