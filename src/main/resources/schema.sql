-- Migrationsscript für neue Player-Felder
-- Dieses Script wird von Hibernate ausgeführt

ALTER TABLE players ADD COLUMN IF NOT EXISTS age INT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS salary BIGINT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS market_value BIGINT DEFAULT 0;
ALTER TABLE players ADD COLUMN IF NOT EXISTS contract_end_date BIGINT DEFAULT 0;
