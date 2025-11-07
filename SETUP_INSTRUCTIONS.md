# Setup Instructions for Factory Inventory Application

## Current Status
✅ Java 23 installed  
❌ Maven not installed  
❌ MySQL not configured  

## Step 1: Install Maven

### Option A: Using Homebrew (if you install it)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install maven
```

### Option B: Manual Installation
1. Download Maven from: https://maven.apache.org/download.cgi
2. Extract to a directory (e.g., `/usr/local/apache-maven`)
3. Add to PATH in `~/.zshrc`:
   ```bash
   export PATH="/usr/local/apache-maven/bin:$PATH"
   ```
4. Reload: `source ~/.zshrc`
5. Verify: `mvn -version`

## Step 2: Setup MySQL Database

### Option A: Install MySQL via Homebrew
```bash
brew install mysql
brew services start mysql
```

### Option B: Download MySQL Installer
Download from: https://dev.mysql.com/downloads/mysql/

### Create Database and User
Once MySQL is running, execute:
```bash
mysql -u root -p <<'SQL'
CREATE DATABASE IF NOT EXISTS factory;
CREATE USER IF NOT EXISTS 'factory'@'localhost' IDENTIFIED BY 'strongpassword';
CREATE USER IF NOT EXISTS 'factory'@'127.0.0.1' IDENTIFIED BY 'strongpassword';
GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'localhost';
GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
```

## Step 3: Run the Application

Once Maven and MySQL are set up:

```bash
cd "/Users/azaria/Desktop/GitHub projects/IB-IA-project"
mvn javafx:run
```

## Default Login Credentials
- Username: `admin`
- Password: `admin123`

The application will automatically:
- Create database tables on first run
- Create the admin user if missing

## Troubleshooting

### If Maven command not found:
- Verify PATH includes Maven bin directory
- Check `~/.zshrc` for export statement

### If MySQL connection fails:
- Verify MySQL is running: `mysqladmin -u root -p ping`
- Check database exists: `mysql -u root -p -e "SHOW DATABASES;"`
- Verify user permissions: `mysql -u root -p -e "SHOW GRANTS FOR 'factory'@'localhost';"`

### If application fails to start:
- Check Java version: `java -version` (needs 17+)
- Clean and rebuild: `mvn clean compile`
- Check error logs in terminal output
