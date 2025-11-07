# Database Testing & Viewing Guide

## 🗄️ How to Access the Database

### Command Line (MySQL Client)

```bash
# Connect to the database
export PATH="/usr/local/mysql/bin:$PATH"
mysql -u factory -pstrongpassword -h 127.0.0.1 factory
```

This opens an interactive MySQL prompt where you can run SQL queries.

### Or Run Single Commands

```bash
# Show all tables
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SHOW TABLES;"

# View table structure
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "DESCRIBE products;"

# View all data in a table
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM roles;"
```

---

## 📊 Useful Queries to Test

### View All Tables
```sql
SHOW TABLES;
```
**Expected output:**
- alerts
- logs
- products
- roles
- users

### View Roles
```sql
SELECT * FROM roles;
```
**Expected output:**
- ADMIN (id: 1)
- SECURITY (id: 2)
- STAFF (id: 3)

### View Users
```sql
SELECT id, username, role_id FROM users;
```

### View Table Structure
```sql
DESCRIBE products;
DESCRIBE logs;
DESCRIBE alerts;
```

### Count Records
```sql
SELECT COUNT(*) as total_products FROM products;
SELECT COUNT(*) as total_logs FROM logs;
SELECT COUNT(*) as total_alerts FROM alerts;
```

---

## 🧪 Test by Inserting Sample Data

### 1. Insert a Test Product
```sql
INSERT INTO products (name, description, quantity, location)
VALUES ('Widget A', 'Sample widget for testing', 100, 'Warehouse A');
```

### 2. View the Product
```sql
SELECT * FROM products;
```

### 3. Create a Log Entry (Check-Out)
```sql
-- First, get the product ID and user ID
SELECT id FROM products WHERE name = 'Widget A';
SELECT id FROM users WHERE username = 'admin';

-- Then create log (replace 1 and 1 with actual IDs)
INSERT INTO logs (product_id, user_id, action_type, quantity, notes)
VALUES (1, 1, 'CHECK_OUT', 20, 'Checked out for delivery');
```

### 4. View Logs
```sql
SELECT * FROM logs;
```

### 5. Create an Alert
```sql
-- Get product and log IDs first
INSERT INTO alerts (product_id, log_id, alert_type, message)
VALUES (1, 1, 'OVERDUE', 'Product checked out for more than 2 hours');
```

### 6. View Alerts
```sql
SELECT * FROM alerts;
SELECT * FROM alerts WHERE status = 'UNRESOLVED';
```

---

## 🎨 GUI Tools (Optional)

### MySQL Workbench (Recommended)
```bash
# Install via Homebrew (if you have it)
brew install --cask mysqlworkbench
```

**Connection Settings:**
- Host: `127.0.0.1`
- Port: `3306`
- Username: `factory`
- Password: `strongpassword`
- Database: `factory`

### Other GUI Options:
- **TablePlus** (macOS) - https://tableplus.com
- **DBeaver** (Free, cross-platform) - https://dbeaver.io
- **Sequel Pro** (macOS, free) - https://www.sequelpro.com

---

## 🔍 Quick Test Script

Run this to see all your data at once:

```bash
export PATH="/usr/local/mysql/bin:$PATH"

echo "=== TABLES ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SHOW TABLES;"

echo -e "\n=== ROLES ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM roles;"

echo -e "\n=== USERS ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT id, username FROM users;"

echo -e "\n=== PRODUCTS ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM products;"

echo -e "\n=== LOGS ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM logs;"

echo -e "\n=== ALERTS ==="
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM alerts;"
```

---

## 💡 Tips

1. **View data in a readable format:**
   ```sql
   mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM products\G"
   ```
   (The `\G` makes it vertical format)

2. **Export data to a file:**
   ```bash
   mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT * FROM products;" > products.csv
   ```

3. **Clear all test data:**
   ```sql
   DELETE FROM alerts;
   DELETE FROM logs;
   DELETE FROM products;
   ```

4. **View recent logs:**
   ```sql
   SELECT * FROM logs ORDER BY timestamp DESC LIMIT 10;
   ```
