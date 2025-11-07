# MySQL Installation Guide

## ✅ Maven Status: INSTALLED
Maven 3.9.6 is now installed and ready to use!

## MySQL Installation Options

Since MySQL requires a license agreement, choose one of these options:

### Option 1: Download MySQL DMG Installer (Recommended for macOS)
1. Visit: https://dev.mysql.com/downloads/mysql/
2. Download: **macOS (x86, 64-bit), DMG Archive** (latest 8.0.x version)
3. Open the DMG file and run the installer
4. Follow the installation wizard
5. Set a root password when prompted
6. Start MySQL service from System Preferences or run:
   ```bash
   sudo /usr/local/mysql/support-files/mysql.server start
   ```

### Option 2: Use Docker (If installed)
```bash
docker run --name factory-mysql \
  -e MYSQL_ROOT_PASSWORD=changeme \
  -e MYSQL_DATABASE=factory \
  -p 3306:3306 \
  -d mysql:8.0
```

Then create the user:
```bash
docker exec -it factory-mysql mysql -uroot -pchangeme -e \
  "CREATE USER 'factory'@'%' IDENTIFIED BY 'strongpassword';
   GRANT ALL PRIVILEGES ON factory.* TO 'factory'@'%';
   FLUSH PRIVILEGES;"
```

### Option 3: Install Homebrew first (then use it for MySQL)
If you can run Homebrew installation interactively:
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install mysql
brew services start mysql
```

## After MySQL Installation

Create the database and user:
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

## Verify Installation
```bash
mysql -u factory -pstrongpassword -h 127.0.0.1 factory -e "SELECT 1;"
```

## Then Run the App
```bash
cd "/Users/azaria/Desktop/GitHub projects/IB-IA-project"
export PATH="$HOME/tools/apache-maven-3.9.6/bin:$PATH"
mvn javafx:run
```
