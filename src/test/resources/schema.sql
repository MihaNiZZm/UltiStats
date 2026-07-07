DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS teams;

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255),
    photo_url VARCHAR(1024)
);

CREATE TABLE players (
    id UUID PRIMARY KEY,
    team_id UUID,
    number INTEGER,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    photo_url VARCHAR(1024),
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
);

CREATE INDEX idx_players_team_id ON players(team_id);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    team_ids UUID ARRAY NOT NULL,
    events JSON NOT NULL DEFAULT '[]',
    team_scores JSON NOT NULL DEFAULT '[]',
    disk_holder_id UUID,
    planned_start_timestamp TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_matches_started_at ON matches(started_at);
