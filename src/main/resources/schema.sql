CREATE TABLE IF NOT EXISTS roles (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role_id INT NOT NULL,
  FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS products (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  quantity INT NOT NULL DEFAULT 0,
  location VARCHAR(100),
  unit VARCHAR(16) NOT NULL DEFAULT 'pcs',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS logs (
  id INT PRIMARY KEY AUTO_INCREMENT,
  product_id INT NOT NULL,
  user_id INT NOT NULL,
  action_type VARCHAR(50) NOT NULL,
  quantity INT NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notes TEXT,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_product_id (product_id),
  INDEX idx_user_id (user_id),
  INDEX idx_timestamp (timestamp)
);

CREATE TABLE IF NOT EXISTS alerts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  product_id INT NOT NULL,
  log_id INT,
  alert_type VARCHAR(50) NOT NULL,
  message TEXT NOT NULL,
  status ENUM('UNRESOLVED', 'RESOLVED') NOT NULL DEFAULT 'UNRESOLVED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP NULL,
  resolved_by INT NULL,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (log_id) REFERENCES logs(id) ON DELETE SET NULL,
  FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);

-- Ensure newer columns exist when upgrading an existing DB
-- Backfill 'unit' column on existing databases without using IF NOT EXISTS (compat with older MySQL)
SET @unit_col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'unit'
);
SET @alter_stmt := IF(@unit_col_exists = 0,
  'ALTER TABLE products ADD COLUMN unit VARCHAR(16) NOT NULL DEFAULT ''pcs''',
  'DO 0'
);
PREPARE alter_unit FROM @alter_stmt;
EXECUTE alter_unit;
DEALLOCATE PREPARE alter_unit;

CREATE TABLE IF NOT EXISTS settings (
  `key` VARCHAR(64) PRIMARY KEY,
  `value` VARCHAR(255) NOT NULL
);

-- New: locations table
CREATE TABLE IF NOT EXISTS locations (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE
);

-- New: product_stock table
CREATE TABLE IF NOT EXISTS product_stock (
  product_id INT NOT NULL,
  location_id INT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, location_id),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE
);

-- Idempotent upgrade: ensure logs.action_type is VARCHAR(50) (from older ENUM)
SET @col_type := (
  SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'logs' AND COLUMN_NAME = 'action_type'
);
SET @needs_alter := IF(@col_type LIKE 'enum(%', 1, 0);
SET @alter_logs_type := IF(@needs_alter = 1,
  'ALTER TABLE logs MODIFY COLUMN action_type VARCHAR(50) NOT NULL',
  'DO 0'
);
PREPARE alter_logs_type_stmt FROM @alter_logs_type;
EXECUTE alter_logs_type_stmt;
DEALLOCATE PREPARE alter_logs_type_stmt;

-- Add from_location_id if missing
SET @from_loc_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'logs' AND COLUMN_NAME = 'from_location_id'
);
SET @alter_add_from := IF(@from_loc_exists = 0,
  'ALTER TABLE logs ADD COLUMN from_location_id INT NULL',
  'DO 0'
);
PREPARE alter_add_from_stmt FROM @alter_add_from;
EXECUTE alter_add_from_stmt;
DEALLOCATE PREPARE alter_add_from_stmt;

-- Add to_location_id if missing
SET @to_loc_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'logs' AND COLUMN_NAME = 'to_location_id'
);
SET @alter_add_to := IF(@to_loc_exists = 0,
  'ALTER TABLE logs ADD COLUMN to_location_id INT NULL',
  'DO 0'
);
PREPARE alter_add_to_stmt FROM @alter_add_to;
EXECUTE alter_add_to_stmt;
DEALLOCATE PREPARE alter_add_to_stmt;

-- Add foreign keys for new columns if not exists (best-effort, may fail if already exist)
SET @fk_from_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'logs'
    AND CONSTRAINT_NAME = 'fk_logs_from_location'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @alter_fk_from := IF(@fk_from_exists = 0,
  'ALTER TABLE logs ADD CONSTRAINT fk_logs_from_location FOREIGN KEY (from_location_id) REFERENCES locations(id) ON DELETE SET NULL',
  'DO 0'
);
PREPARE alter_fk_from_stmt FROM @alter_fk_from;
EXECUTE alter_fk_from_stmt;
DEALLOCATE PREPARE alter_fk_from_stmt;

SET @fk_to_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'logs'
    AND CONSTRAINT_NAME = 'fk_logs_to_location'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @alter_fk_to := IF(@fk_to_exists = 0,
  'ALTER TABLE logs ADD CONSTRAINT fk_logs_to_location FOREIGN KEY (to_location_id) REFERENCES locations(id) ON DELETE SET NULL',
  'DO 0'
);
PREPARE alter_fk_to_stmt FROM @alter_fk_to;
EXECUTE alter_fk_to_stmt;
DEALLOCATE PREPARE alter_fk_to_stmt;

-- Seed default locations idempotently
INSERT IGNORE INTO locations(name) VALUES ('Warehouse A'), ('Warehouse B'), ('Storage C');

