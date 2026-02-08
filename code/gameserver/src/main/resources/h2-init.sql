-- =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
-- DD Poker - H2 Database Schema Init
-- Adapted from tools/db/create_tables.sql for H2 compatibility
-- Safe to run on every connection (uses IF NOT EXISTS on all statements)
-- =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

CREATE TABLE IF NOT EXISTS wan_profile (
    wpr_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wpr_name VARCHAR(32) NOT NULL,
    wpr_license_key VARCHAR(55) NOT NULL,
    wpr_email VARCHAR(255) NOT NULL,
    wpr_password VARCHAR(255) NOT NULL,
    wpr_is_activated BOOLEAN NOT NULL,
    wpr_is_retired BOOLEAN NOT NULL,
    wpr_create_date DATETIME NOT NULL,
    wpr_modify_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS wpr_name ON wan_profile(wpr_name);
CREATE INDEX IF NOT EXISTS wpr_email ON wan_profile(wpr_email);

CREATE TABLE IF NOT EXISTS wan_game (
    wgm_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wgm_license_key VARCHAR(55) NOT NULL,
    wgm_url VARCHAR(64) NOT NULL,
    wgm_host_player VARCHAR(64) NOT NULL,
    wgm_start_date DATETIME NULL,
    wgm_end_date DATETIME NULL,
    wgm_create_date DATETIME NOT NULL,
    wgm_modify_date DATETIME NOT NULL,
    wgm_mode TINYINT NOT NULL,
    wgm_tournament_data TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS wgm_license_key ON wan_game(wgm_license_key, wgm_url);
CREATE INDEX IF NOT EXISTS wgm_host_player ON wan_game(wgm_host_player);
CREATE INDEX IF NOT EXISTS wgm_modify_date ON wan_game(wgm_modify_date);
CREATE INDEX IF NOT EXISTS wgm_end_date ON wan_game(wgm_end_date);
CREATE INDEX IF NOT EXISTS wgm_create_date_mode ON wan_game(wgm_create_date, wgm_mode);
CREATE INDEX IF NOT EXISTS wgm_end_date_mode ON wan_game(wgm_end_date, wgm_mode);
CREATE INDEX IF NOT EXISTS wgm_mode ON wan_game(wgm_mode);

CREATE TABLE IF NOT EXISTS wan_history (
    whi_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    whi_game_id INT NOT NULL,
    whi_tournament_name VARCHAR(255) NOT NULL,
    whi_num_players INT NOT NULL,
    whi_is_ended BOOL NOT NULL,
    whi_profile_id INT NOT NULL,
    whi_player_name VARCHAR(32) NOT NULL,
    whi_player_type TINYINT NOT NULL,
    whi_finish_place SMALLINT NOT NULL,
    whi_prize DECIMAL NOT NULL,
    whi_buy_in DECIMAL NOT NULL,
    whi_total_rebuy DECIMAL NOT NULL,
    whi_total_add_on DECIMAL NOT NULL,
    whi_rank_1 DECIMAL(10,3) NOT NULL,
    whi_disco DECIMAL(10,0) NOT NULL,
    whi_end_date DATETIME NOT NULL,
    FOREIGN KEY (whi_game_id) REFERENCES wan_game(wgm_id),
    FOREIGN KEY (whi_profile_id) REFERENCES wan_profile(wpr_id)
);
CREATE INDEX IF NOT EXISTS whi_end_date ON wan_history(whi_end_date);
CREATE INDEX IF NOT EXISTS whi_player_type ON wan_history(whi_player_type);
CREATE INDEX IF NOT EXISTS whi_is_ended ON wan_history(whi_is_ended);

CREATE TABLE IF NOT EXISTS registration (
    reg_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reg_license_key VARCHAR(55) NOT NULL,
    reg_product_version VARCHAR(32) NOT NULL,
    reg_ip_address VARCHAR(16) NOT NULL,
    reg_host_name VARCHAR(255) NULL,
    reg_host_name_modified VARCHAR(255) NULL,
    reg_port SMALLINT NULL,
    reg_server_time DATETIME NOT NULL,
    reg_java_version VARCHAR(32) NULL,
    reg_os VARCHAR(32) NULL,
    reg_type TINYINT NOT NULL,
    reg_is_duplicate BOOL NOT NULL,
    reg_is_ban_attempt BOOL NOT NULL,
    reg_name VARCHAR(100) NULL,
    reg_email VARCHAR(255) NULL,
    reg_address VARCHAR(255) NULL,
    reg_city VARCHAR(50) NULL,
    reg_state VARCHAR(50) NULL,
    reg_postal VARCHAR(50) NULL,
    reg_country VARCHAR(120) NULL
);
CREATE INDEX IF NOT EXISTS reg_address ON registration(reg_address);
CREATE INDEX IF NOT EXISTS reg_email ON registration(reg_email);
CREATE INDEX IF NOT EXISTS reg_host_name_modified ON registration(reg_host_name_modified);
CREATE INDEX IF NOT EXISTS reg_ip_address ON registration(reg_ip_address);
CREATE INDEX IF NOT EXISTS reg_is_duplicate ON registration(reg_is_duplicate);
CREATE INDEX IF NOT EXISTS reg_name ON registration(reg_name);
CREATE INDEX IF NOT EXISTS reg_license_key ON registration(reg_license_key);
CREATE INDEX IF NOT EXISTS reg_type ON registration(reg_type);

CREATE TABLE IF NOT EXISTS banned_key (
    ban_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ban_key VARCHAR(255) NOT NULL,
    ban_until DATE NOT NULL,
    ban_comment VARCHAR(128) NULL,
    ban_create_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ban_key ON banned_key(ban_key);

CREATE TABLE IF NOT EXISTS upgraded_key (
    upg_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    upg_license_key VARCHAR(55) NOT NULL,
    upg_count SMALLINT NOT NULL,
    upg_create_date DATETIME NOT NULL,
    upg_modify_date DATETIME NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS upg_license_key ON upgraded_key(upg_license_key);
