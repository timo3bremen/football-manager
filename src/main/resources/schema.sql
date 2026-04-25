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
