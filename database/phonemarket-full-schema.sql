




SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS phone_market;
CREATE DATABASE phone_market
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE phone_market;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- ============================================================
CREATE TABLE `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE game (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    current_round INT NOT NULL DEFAULT 1,
    max_round INT NOT NULL DEFAULT 10,
    player_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_game_status (status),
    CONSTRAINT chk_game_status
        CHECK (status IN ('WAITING', 'RUNNING', 'FINISHED', 'ABORTED')),
    CONSTRAINT chk_game_current_round CHECK (current_round >= 1),
    CONSTRAINT chk_game_max_round CHECK (max_round >= 1),
    CONSTRAINT chk_game_player_count CHECK (player_count BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE game_player (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seat_no INT NOT NULL,

    cash DECIMAL(15,2) NOT NULL DEFAULT 1000000.00,
    debt DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    debt_limit DECIMAL(15,2) NOT NULL DEFAULT 1000000.00,
    total_sales DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    cumulative_sales_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00
        COMMENT 'Cumulative public sales profit: consumer revenue minus production cost',
    total_settlement_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00
        COMMENT 'Cumulative settlement profit used for ranking',

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_game_player_user (game_id, user_id),
    UNIQUE KEY uk_game_player_seat (game_id, seat_no),
    KEY idx_game_player_user (user_id),
    KEY idx_game_player_status (game_id, status),

    CONSTRAINT fk_game_player_game
        FOREIGN KEY (game_id) REFERENCES game (id),
    CONSTRAINT fk_game_player_user
        FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT chk_game_player_seat CHECK (seat_no BETWEEN 1 AND 4),
    CONSTRAINT chk_game_player_cash CHECK (cash >= 0),
    CONSTRAINT chk_game_player_debt CHECK (debt >= 0),
    CONSTRAINT chk_game_player_debt_limit CHECK (debt_limit >= 0),
    CONSTRAINT chk_game_player_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE game_round (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    round_no INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COLLECTING',
    expected_player_count INT NOT NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    economy_factor DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_game_round_no (game_id, round_no),
    KEY idx_game_round_status (game_id, status),

    CONSTRAINT fk_game_round_game
        FOREIGN KEY (game_id) REFERENCES game (id),
    CONSTRAINT chk_game_round_no CHECK (round_no >= 1),
    CONSTRAINT chk_game_round_status
        CHECK (status IN ('COLLECTING', 'PROCESSING', 'FINISHED')),
    CONSTRAINT chk_game_round_expected CHECK (expected_player_count BETWEEN 1 AND 4),
    CONSTRAINT chk_game_round_submitted CHECK (
        submitted_count >= 0 AND submitted_count <= expected_player_count
    ),
    CONSTRAINT chk_game_round_economy CHECK (
        economy_factor = 0
        OR economy_factor BETWEEN 0.6500 AND 1.1500
        OR economy_factor BETWEEN -0.9500 AND -0.0500
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE phone_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NULL,
    game_player_id BIGINT NULL,
    model_name VARCHAR(50) NOT NULL,
    model_type VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    model_code VARCHAR(30) NULL,

    screen_level INT NOT NULL,
    processor_level INT NOT NULL,
    body_level INT NOT NULL,
    battery_level INT NOT NULL,
    storage_level INT NOT NULL,
    camera_level INT NOT NULL,

    total_grade INT GENERATED ALWAYS AS (
        screen_level
        + processor_level
        + body_level
        + battery_level
        + storage_level
        + camera_level
    ) STORED,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_phone_model_round_player (round_id, game_player_id),
    UNIQUE KEY uk_phone_model_code (model_code),
    KEY idx_phone_model_player (game_player_id),
    KEY idx_phone_model_round (round_id),

    CONSTRAINT fk_phone_model_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_phone_model_player
        FOREIGN KEY (game_player_id) REFERENCES game_player (id),

    CONSTRAINT chk_phone_model_type CHECK (model_type IN ('SYSTEM', 'PLAYER')),
    CONSTRAINT chk_phone_model_screen CHECK (screen_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_processor CHECK (processor_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_body CHECK (body_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_battery CHECK (battery_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_storage CHECK (storage_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_camera CHECK (camera_level BETWEEN 1 AND 3),
    CONSTRAINT chk_phone_model_owner CHECK (
        (model_type = 'SYSTEM' AND round_id IS NULL AND game_player_id IS NULL AND model_code IS NOT NULL)
        OR
        (model_type = 'PLAYER' AND round_id IS NOT NULL AND game_player_id IS NOT NULL AND model_code IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE consumer_segment_rule (
    segment_code VARCHAR(30) NOT NULL,
    group_type VARCHAR(20) NOT NULL,
    gender VARCHAR(10) NOT NULL,

    base_population INT NOT NULL,
    initial_budget INT NOT NULL,
    budget_growth INT NOT NULL,
    economy_sensitivity DECIMAL(6,4) NOT NULL,

    screen_preference DECIMAL(4,2) NOT NULL,
    processor_preference DECIMAL(4,2) NOT NULL,
    body_preference DECIMAL(4,2) NOT NULL,
    battery_preference DECIMAL(4,2) NOT NULL,
    storage_preference DECIMAL(4,2) NOT NULL,
    camera_preference DECIMAL(4,2) NOT NULL,

    initial_screen_level INT NOT NULL,
    initial_processor_level INT NOT NULL,
    initial_body_level INT NOT NULL,
    initial_battery_level INT NOT NULL,
    initial_storage_level INT NOT NULL,
    initial_camera_level INT NOT NULL,
    initial_used_rounds INT NOT NULL,

    PRIMARY KEY (segment_code),
    KEY idx_segment_rule_group (group_type, gender),

    CONSTRAINT chk_segment_rule_group
        CHECK (group_type IN ('BUSINESS', 'WORKER', 'STUDENT')),
    CONSTRAINT chk_segment_rule_gender CHECK (gender IN ('FEMALE', 'MALE')),
    CONSTRAINT chk_segment_rule_population CHECK (base_population > 0),
    CONSTRAINT chk_segment_rule_budget CHECK (initial_budget >= 0 AND budget_growth >= 0),
    CONSTRAINT chk_segment_rule_sensitivity CHECK (economy_sensitivity BETWEEN 0 AND 1),
    CONSTRAINT chk_segment_rule_used_rounds CHECK (initial_used_rounds >= 0),
    CONSTRAINT chk_segment_rule_screen CHECK (initial_screen_level BETWEEN 1 AND 3),
    CONSTRAINT chk_segment_rule_processor CHECK (initial_processor_level BETWEEN 1 AND 3),
    CONSTRAINT chk_segment_rule_body CHECK (initial_body_level BETWEEN 1 AND 3),
    CONSTRAINT chk_segment_rule_battery CHECK (initial_battery_level BETWEEN 1 AND 3),
    CONSTRAINT chk_segment_rule_storage CHECK (initial_storage_level BETWEEN 1 AND 3),
    CONSTRAINT chk_segment_rule_camera CHECK (initial_camera_level BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_segment_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    segment_code VARCHAR(30) NOT NULL,
    population INT NOT NULL,
    average_budget INT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_round_segment_state (round_id, segment_code),
    KEY idx_round_segment_code (segment_code),

    CONSTRAINT fk_round_segment_state_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_round_segment_state_rule
        FOREIGN KEY (segment_code) REFERENCES consumer_segment_rule (segment_code),
    CONSTRAINT chk_round_segment_population CHECK (population >= 0),
    CONSTRAINT chk_round_segment_budget CHECK (average_budget >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_consumer_cohort (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    segment_code VARCHAR(30) NOT NULL,
    phone_model_id BIGINT NOT NULL,
    population INT NOT NULL,
    used_rounds INT NOT NULL,

    PRIMARY KEY (id),
    KEY idx_cohort_round_segment (round_id, segment_code),
    KEY idx_cohort_phone_model (phone_model_id),

    CONSTRAINT fk_cohort_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_cohort_segment
        FOREIGN KEY (segment_code) REFERENCES consumer_segment_rule (segment_code),
    CONSTRAINT fk_cohort_phone_model
        FOREIGN KEY (phone_model_id) REFERENCES phone_model (id),
    CONSTRAINT chk_cohort_population CHECK (population > 0),
    CONSTRAINT chk_cohort_used_rounds CHECK (used_rounds >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_component_market (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    component_type VARCHAR(20) NOT NULL,
    component_level INT NOT NULL,
    base_price INT NOT NULL,
    supply_quantity INT NOT NULL,
    demand_quantity INT NOT NULL DEFAULT 0,
    premium_change DECIMAL(8,4) NULL,
    premium_factor DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
    actual_unit_price INT NOT NULL,
    next_supply_quantity INT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_round_component_market (round_id, component_type, component_level),
    KEY idx_component_market_round (round_id),

    CONSTRAINT fk_component_market_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT chk_component_market_type CHECK (
        component_type IN ('SCREEN', 'PROCESSOR', 'BODY', 'BATTERY', 'STORAGE', 'CAMERA')
    ),
    CONSTRAINT chk_component_market_level CHECK (component_level BETWEEN 1 AND 3),
    CONSTRAINT chk_component_market_base_price CHECK (base_price > 0),
    CONSTRAINT chk_component_market_supply CHECK (supply_quantity >= 0),
    CONSTRAINT chk_component_market_demand CHECK (demand_quantity >= 0),
    CONSTRAINT chk_component_market_factor CHECK (premium_factor >= 1.0000),
    CONSTRAINT chk_component_market_actual_price CHECK (actual_unit_price > 0),
    CONSTRAINT chk_component_market_next_supply CHECK (
        next_supply_quantity IS NULL OR next_supply_quantity >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_star (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    target_segment_code VARCHAR(30) NOT NULL,
    boost DECIMAL(6,4) NOT NULL,
    winner_game_player_id BIGINT NULL,
    winning_bid DECIMAL(15,2) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_round_star_round (round_id),
    KEY idx_round_star_winner (winner_game_player_id),
    KEY idx_round_star_segment (target_segment_code),

    CONSTRAINT fk_round_star_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_round_star_segment
        FOREIGN KEY (target_segment_code) REFERENCES consumer_segment_rule (segment_code),
    CONSTRAINT fk_round_star_winner
        FOREIGN KEY (winner_game_player_id) REFERENCES game_player (id),
    CONSTRAINT chk_round_star_boost CHECK (boost BETWEEN 0.0000 AND 0.5000),
    CONSTRAINT chk_round_star_bid CHECK (winning_bid IS NULL OR winning_bid >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    game_player_id BIGINT NOT NULL,
    phone_model_id BIGINT NOT NULL,
    production_quantity INT NOT NULL,
    sale_price DECIMAL(15,2) NOT NULL,
    film_ad TINYINT(1) NOT NULL DEFAULT 0,
    online_ad TINYINT(1) NOT NULL DEFAULT 0,
    magazine_ad TINYINT(1) NOT NULL DEFAULT 0,
    star_bid DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_round_action_round_player (round_id, game_player_id),
    KEY idx_round_action_player (game_player_id),
    KEY idx_round_action_phone (phone_model_id),

    CONSTRAINT fk_round_action_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_round_action_player
        FOREIGN KEY (game_player_id) REFERENCES game_player (id),
    CONSTRAINT fk_round_action_phone
        FOREIGN KEY (phone_model_id) REFERENCES phone_model (id),
    CONSTRAINT chk_round_action_production CHECK (production_quantity >= 0),
    CONSTRAINT chk_round_action_sale_price CHECK (sale_price > 0),
    CONSTRAINT chk_round_action_star_bid CHECK (star_bid >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
CREATE TABLE round_player_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id BIGINT NOT NULL,
    game_player_id BIGINT NOT NULL,
    phone_model_id BIGINT NOT NULL,

    production_quantity INT NOT NULL,
    consumer_sales_quantity INT NOT NULL,
    unsold_quantity INT NOT NULL,
    sale_price DECIMAL(15,2) NOT NULL,

    component_unit_cost DECIMAL(15,2) NOT NULL,
    component_cost DECIMAL(15,2) NOT NULL,
    assembly_unit_cost DECIMAL(15,2) NOT NULL,
    assembly_cost DECIMAL(15,2) NOT NULL,
    production_cost DECIMAL(15,2) NOT NULL,

    film_ad TINYINT(1) NOT NULL DEFAULT 0,
    online_ad TINYINT(1) NOT NULL DEFAULT 0,
    magazine_ad TINYINT(1) NOT NULL DEFAULT 0,
    film_advertising_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    online_advertising_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    magazine_advertising_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    advertising_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    star_bid DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    won_star TINYINT(1) NOT NULL DEFAULT 0,
    star_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    consumer_sales_revenue DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    liquidation_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    liquidation_revenue DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_revenue DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    sales_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    beginning_cash DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    beginning_debt DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    beginning_available_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    new_normal_loan DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    normal_loan_principal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    normal_loan_interest DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payday_principal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payday_interest DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_repayment_due DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    actual_repayment DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ending_cash DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ending_debt DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ending_available_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,

    round_cash_result DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    round_settlement_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ending_cumulative_sales_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ending_total_settlement_profit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_round_player_result (round_id, game_player_id),
    KEY idx_round_player_result_player (game_player_id),
    KEY idx_round_player_result_phone (phone_model_id),

    CONSTRAINT fk_round_player_result_round
        FOREIGN KEY (round_id) REFERENCES game_round (id),
    CONSTRAINT fk_round_player_result_player
        FOREIGN KEY (game_player_id) REFERENCES game_player (id),
    CONSTRAINT fk_round_player_result_phone
        FOREIGN KEY (phone_model_id) REFERENCES phone_model (id),

    CONSTRAINT chk_result_production CHECK (production_quantity >= 0),
    CONSTRAINT chk_result_sales CHECK (consumer_sales_quantity >= 0),
    CONSTRAINT chk_result_unsold CHECK (unsold_quantity >= 0),
    CONSTRAINT chk_result_quantity CHECK (
        consumer_sales_quantity + unsold_quantity = production_quantity
    ),
    CONSTRAINT chk_result_sale_price CHECK (sale_price > 0),
    CONSTRAINT chk_result_nonnegative_money CHECK (
        component_unit_cost >= 0
        AND component_cost >= 0
        AND assembly_unit_cost >= 0
        AND assembly_cost >= 0
        AND production_cost >= 0
        AND advertising_cost >= 0
        AND star_bid >= 0
        AND star_cost >= 0
        AND consumer_sales_revenue >= 0
        AND liquidation_unit_price >= 0
        AND liquidation_revenue >= 0
        AND total_revenue >= 0
        AND beginning_cash >= 0
        AND beginning_debt >= 0
        AND beginning_available_credit >= 0
        AND new_normal_loan >= 0
        AND normal_loan_principal >= 0
        AND normal_loan_interest >= 0
        AND payday_principal >= 0
        AND payday_interest >= 0
        AND total_repayment_due >= 0
        AND actual_repayment >= 0
        AND ending_cash >= 0
        AND ending_debt >= 0
        AND ending_available_credit >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ============================================================
INSERT INTO consumer_segment_rule (
    segment_code, group_type, gender,
    base_population, initial_budget, budget_growth, economy_sensitivity,
    screen_preference, processor_preference, body_preference,
    battery_preference, storage_preference, camera_preference,
    initial_screen_level, initial_processor_level, initial_body_level,
    initial_battery_level, initial_storage_level, initial_camera_level,
    initial_used_rounds
) VALUES
(
    'BUSINESS_FEMALE', 'BUSINESS', 'FEMALE',
    1000, 8000, 800, 1.0000,
    1.50, 1.00, 2.00, 1.00, 1.00, 1.50,
    3, 2, 2, 2, 3, 3,
    0
),
(
    'BUSINESS_MALE', 'BUSINESS', 'MALE',
    1000, 8000, 800, 1.0000,
    1.00, 1.50, 2.00, 1.50, 1.00, 1.00,
    3, 2, 2, 2, 3, 3,
    0
),
(
    'WORKER_FEMALE', 'WORKER', 'FEMALE',
    2000, 4500, 400, 0.5000,
    2.00, 1.00, 1.50, 1.00, 1.00, 1.50,
    1, 2, 1, 2, 2, 2,
    2
),
(
    'WORKER_MALE', 'WORKER', 'MALE',
    2000, 4500, 400, 0.5000,
    1.00, 1.50, 1.00, 2.00, 1.50, 1.00,
    1, 2, 1, 2, 2, 2,
    2
),
(
    'STUDENT_FEMALE', 'STUDENT', 'FEMALE',
    2000, 2000, 200, 0.2500,
    1.50, 1.00, 1.00, 1.00, 1.50, 2.00,
    1, 1, 1, 1, 1, 1,
    4
),
(
    'STUDENT_MALE', 'STUDENT', 'MALE',
    2000, 2000, 200, 0.2500,
    1.00, 2.00, 1.50, 1.00, 1.50, 1.00,
    1, 1, 1, 1, 1, 1,
    4
);

-- ============================================================
-- ============================================================
INSERT INTO phone_model (
    round_id, game_player_id, model_name, model_type, model_code,
    screen_level, processor_level, body_level,
    battery_level, storage_level, camera_level
) VALUES
(
    NULL, NULL, CONVERT(0xE9AB98E7ABAFE6898BE69CBAEFBC88E5889DE5A78BEFBC89 USING utf8mb4), 'SYSTEM', 'INITIAL_BUSINESS',
    3, 2, 2, 2, 3, 3
),
(
    NULL, NULL, CONVERT(0xE699AEE9809AE6898BE69CBAEFBC88E5889DE5A78BEFBC89 USING utf8mb4), 'SYSTEM', 'INITIAL_WORKER',
    1, 2, 1, 2, 2, 2
),
(
    NULL, NULL, CONVERT(0xE4BA8CE6898BE6898BE69CBAEFBC88E5889DE5A78BEFBC89 USING utf8mb4), 'SYSTEM', 'INITIAL_STUDENT',
    1, 1, 1, 1, 1, 1
);

-- ============================================================
-- ============================================================
SELECT 'phone_market database created successfully' AS result;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'phone_market'
ORDER BY TABLE_NAME;

SELECT segment_code, budget_growth, economy_sensitivity
FROM consumer_segment_rule
ORDER BY segment_code;

SELECT id, model_code, model_name, HEX(model_name) AS model_name_utf8_hex, total_grade
FROM phone_model
WHERE model_type = 'SYSTEM'
ORDER BY id;
