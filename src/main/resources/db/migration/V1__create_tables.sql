-- Таблица команд
CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255),
    photo_url VARCHAR(1024)
);

-- Таблица игроков
CREATE TABLE players (
    id UUID PRIMARY KEY,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    number INTEGER,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    photo_url VARCHAR(1024)
);

-- Индекс для быстрого поиска игроков по команде
CREATE INDEX idx_players_team_id ON players(team_id);

-- Таблица матчей
CREATE TABLE matches (
    id UUID PRIMARY KEY,
    team_ids UUID[] NOT NULL,
    events JSONB NOT NULL DEFAULT '[]',
    team_scores JSONB NOT NULL DEFAULT '[]',
    disk_holder_id UUID,
    planned_start_timestamp TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE
);

-- Индекс для поиска матчей по командам (GIN для массива)
CREATE INDEX idx_matches_team_ids ON matches USING GIN(team_ids);

-- Индекс для поиска матчей по дате начала
CREATE INDEX idx_matches_started_at ON matches(started_at);
