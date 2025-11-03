# Database Guide

This document describes the database used by the project: current status, schema, configuration, local setup, and troubleshooting. Keep this file updated as the schema evolves.

## Overview
- Engine: MySQL (local development)
- JDBC Driver: `mysql-connector-j`
- Auth: BCrypt password hashing
- Access pattern: lightweight JDBC via small DAO classes (no ORM)
- Migration: simple boot-time execution of `schema.sql` (no Flyway/Liquibase yet)

## Versions and Dependencies
- Java: 17
- MySQL Server: 8.4+ (Homebrew currently installed 9.5.0)
- Maven deps (see `pom.xml`):
  - `com.mysql:mysql-connector-j` (runtime)
  - `org.mindrot:jbcrypt`

## Connection Configuration
Configuration lives in `src/main/resources/db.properties` and is loaded by `com.javafx.demo.db.Database`.

```properties
# Example local settings
db.url=jdbc:mysql://127.0.0.1:3306/factory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=factory
db.password=strongpassword
```

Notes:
- Prefer `127.0.0.1` over `localhost` to force TCP (avoids socket issues).
- Do not commit real production credentials. For prod, load from environment or an external secret store later.

## Startup Lifecycle (app boot)
- `Database.migrateIfNeeded()` executes `src/main/resources/schema.sql` if present.
- `AuthService.seedAdminIfMissing()` ensures an `ADMIN` role and a default admin user exist:
  - username: `admin`
  - password: `admin123` (BCrypt hashed in code)
- App then shows the login screen.

Relevant classes:
- `com.javafx.demo.db.Database`
- `com.javafx.demo.dao.UserDao`
- `com.javafx.demo.security.AuthService`

## Schema
Defined in `src/main/resources/schema.sql` and applied on startup.

```sql
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
```

### Logical model
- `roles (id, name)`
- `users (id, username, password_hash, role_id)` → FK to `roles.id`

ER (simplified):
```
roles 1 ────< users
```

### Seeding
On boot, the app inserts role `ADMIN` if missing and creates the `admin` user if missing. This is done in code by `UserDao.ensureAdminSeeded(...)`.

## Local Setup
### Homebrew (recommended)
```bash
brew install mysql
brew services start mysql
mysql -u root <<'SQL'
CREATE DATABASE IF NOT EXISTS factory;
CREATE USER IF NOT EXISTS 'factory'@'localhost' IDENTIFIED BY 'strongpassword';
CREATE USER IF NOT EXISTS 'factory'@'127.0.0.1' IDENTIFIED BY 'strongpassword';
GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'localhost';
GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
```

Update `db.properties`, then run the app:
```bash
mvn javafx:run
```

### Docker (alternative)
```bash
docker run --name factory-mysql -e MYSQL_ROOT_PASSWORD=changeme \
  -e MYSQL_DATABASE=factory -p 3306:3306 -d mysql:8.4
# optional app user
docker exec -it factory-mysql mysql -uroot -pchangeme -e \
 "CREATE USER IF NOT EXISTS 'factory'@'%' IDENTIFIED BY 'strongpassword';
  GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'%'; FLUSH PRIVILEGES;"
```

## Working With the DB
CLI:
```bash
mysql -h 127.0.0.1 -P 3306 -u factory -p factory
SHOW TABLES;
SELECT * FROM roles;
SELECT id, username FROM users;
```

GUI: `brew install --cask mysqlworkbench` and connect to `127.0.0.1:3306`.

## Data Access Pattern
- DAO classes encapsulate SQL and mappings; see `com.javafx.demo.dao.UserDao`.
- Use try-with-resources, prepared statements, and explicit mappings to records in `com.javafx.demo.model`.
- No connection pool yet; `Database.getConnection()` uses `DriverManager`. We can add HikariCP later if needed.

## Migrations Policy (current)
- Single `schema.sql` executed on startup; suitable for early development.
- When schema grows, adopt Flyway or Liquibase. For now:
  - Edit `schema.sql` for new tables.
  - For existing tables, include forward-only `ALTER TABLE` statements.
  - Document each change in this file under “Change Log”.

## Change Log
- 2025-10-30: Initial schema with `roles`, `users`. Boot-time migration and admin seeding.

## Security Notes
- Passwords are stored as BCrypt hashes (`jbcrypt`).
- Avoid using MySQL `root` for the application; use a least-privilege user (e.g., `factory`).
- Don’t commit real credentials; rotate passwords on shared machines.

## Troubleshooting
- Communications link failure
  - Ensure MySQL is running: `brew services list`
  - Use TCP host `127.0.0.1`; verify: `mysql -h 127.0.0.1 -u factory -p factory -e "SELECT 1;"`
- Access denied
  - Confirm user grants: `SHOW GRANTS FOR 'factory'@'localhost';`
- Schema not created
  - App runs `schema.sql` on boot; otherwise run manually: `SOURCE <repo>/src/main/resources/schema.sql;`

## Next Steps
- Introduce a migration tool (Flyway) as tables expand.
- Add application-level connection pooling.
- Extend schema for inventory, logs, and alerts per `factory_inventory_prd.md`.


