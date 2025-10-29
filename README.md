# JavaFX Demo Project

A simple JavaFX desktop application demo built with Maven.

## Prerequisites

Before running this project, ensure you have the following installed:

1. **Java Development Kit (JDK) 17 or higher**
   - Check your version: `java -version`
   - Download if needed: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)

2. **Apache Maven 3.6.0 or higher**
   - Check your version: `mvn -version`
   - Download if needed: [Maven Downloads](https://maven.apache.org/download.cgi)

## Project Structure

```
javafx-demo/
├── pom.xml                                    # Maven project configuration
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── javafx/
│                   └── demo/
│                       └── HelloJavaFX.java   # Main JavaFX application
└── target/                                    # Compiled classes (generated)
```

## Setup Instructions

1. **Clone or download this project** to your local machine

2. **Navigate to the project directory** in your terminal:
   ```bash
   cd javafx-demo
   ```

3. **Verify your Java and Maven installations**:
   ```bash
   java -version
   mvn -version
   ```

## Running the Application

### Option 1: Using Maven JavaFX Plugin (Recommended)

Simply run:
```bash
mvn javafx:run
```

This will:
- Automatically download dependencies
- Compile the Java source files
- Launch the JavaFX application

### Option 2: Using Maven Exec Plugin

For a fresh build and run:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.javafx.demo.HelloJavaFX"
```

### Option 3: Rebuild and Run

To clean the previous build and run the application:
```bash
mvn clean javafx:run
```

## Expected Output

When you run the application, a desktop window should appear with:
- **Window title**: "JavaFX Demo in Cursor"
- **Label**: "Hello, JavaFX from Cursor!"
- **Button**: "Click me!" (clicking it updates the label text)

## Project Configuration Details

### Maven Configuration (`pom.xml`)

- **Java Version**: 17
- **JavaFX Version**: 20.0.1
- **Main Class**: `com.javafx.demo.HelloJavaFX`

### JavaFX Dependencies

This project uses:
- `javafx-controls` - UI controls (Button, Label, etc.)
- `javafx-fxml` - FXML support for scene graph definition

### Plugins

- **maven-compiler-plugin**: Compiles Java source code
- **javafx-maven-plugin**: Provides the `javafx:run` goal

## Troubleshooting

### Issue: "Module not found" errors

**Solution**: Ensure you're using Java 17 or higher and that the JavaFX dependencies are properly downloaded:
```bash
mvn clean install
```

### Issue: Application doesn't start

**Solution**: Check that your Java version is compatible:
```bash
java -version  # Should show 17 or higher
```

### Issue: GUI window doesn't appear

**Solution**: On some systems, the window might open behind other windows. Check your taskbar or mission control.

### Issue: Build failures on first run

**Solution**: Maven might be downloading dependencies. Wait for the download to complete. If it persists:
```bash
mvn clean
mvn compile
mvn javafx:run
```

## Development in Cursor IDE

### Opening the Project

1. Open Cursor IDE
2. Select "File" → "Open Folder"
3. Navigate to and select the `javafx-demo` directory

### Running the Application

You can run the application from within Cursor by:

1. Opening the integrated terminal (`Ctrl + ~` or `Cmd + ~`)
2. Running: `mvn javafx:run`

### Java Extension Recommended

For better Java development experience in Cursor, consider installing:
- **Extension Pack for Java** (by Microsoft)
- **Language Support for Java** (by Red Hat)

## Building the Project

To compile the project without running it:
```bash
mvn compile
```

This will create the compiled `.class` files in the `target/classes` directory.

## Cleaning the Project

To remove compiled files and start fresh:
```bash
mvn clean
```

## Contributing

When contributing to this project:

1. Make sure your changes compile successfully
2. Test by running `mvn javafx:run`
3. Ensure the application window opens and functions correctly
4. Commit your changes with clear, descriptive messages

## Additional Resources

- [JavaFX Documentation](https://openjfx.io/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [JavaFX Tutorials](https://openjfx.io/openjfx-docs/)

## License

This is a demo project for educational purposes.

