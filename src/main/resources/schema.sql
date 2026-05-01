-- Migrationsscript für neue Player-Felder
-- Dieses Script wird von Hibernate ausgeführt

ALTER TABLE players ADD COLUMN IF NOT EXISTS age INT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS salary BIGINT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS market_value BIGINT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS contract_end_date BIGINT DEFAULT 0;

-- Sponsors Tabelle
CREATE TABLE IF NOT EXISTS sponsors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    appearance_payout INT DEFAULT 0,
    win_payout INT DEFAULT 0,
    survive_payout INT DEFAULT 0,
    title_payout INT DEFAULT 0,
    UNIQUE(team_id)
);

-- Player Performances Tabelle für Spielerleistungen in Matches
CREATE TABLE IF NOT EXISTS player_performances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    rating DOUBLE DEFAULT 3.0,
    goals INT DEFAULT 0,
    assists INT DEFAULT 0,
    yellow_cards INT DEFAULT 0,
    red_cards INT DEFAULT 0,
    minutes_played INT DEFAULT 90,
    INDEX idx_player_performances_player (player_id),
    INDEX idx_player_performances_match (match_id)
);

-- Contract Negotiations Tabelle für Verhandlungshistorie
CREATE TABLE IF NOT EXISTS contract_negotiations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    attempt_count INT DEFAULT 0,
    season INT NOT NULL,
    failed BOOLEAN DEFAULT FALSE,
    INDEX idx_contract_negotiations_player_season (player_id, season)
);

-- Free Agents Tabelle für vertragslose Spieler
CREATE TABLE IF NOT EXISTS free_agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL UNIQUE,
    matchday_created INT,
    available_since TIMESTAMP NOT NULL,
    decision_deadline TIMESTAMP NOT NULL,
    best_offer_team_id BIGINT,
    best_offer_salary BIGINT,
    best_offer_contract_length INT,
    status VARCHAR(50) DEFAULT 'available',
    INDEX idx_free_agents_status (status),
    INDEX idx_free_agents_deadline (decision_deadline)
);

-- Free Agent Offers Tabelle für Angebote an freie Spieler
CREATE TABLE IF NOT EXISTS free_agent_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    salary BIGINT NOT NULL,
    contract_length INT NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    INDEX idx_free_agent_offers_player (player_id),
    INDEX idx_free_agent_offers_team (team_id),
    INDEX idx_free_agent_offers_status (status)
);

-- Add isFreeAgent column to players table
ALTER TABLE players ADD COLUMN IF NOT EXISTS is_free_agent BOOLEAN DEFAULT FALSE;
