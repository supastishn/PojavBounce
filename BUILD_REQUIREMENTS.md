# Build Requirements

## Java Version
This project requires **Java 21** or newer to build successfully.

### Setting up Java 21
If you encounter build errors related to Java version, ensure you're using Java 21:

```bash
# Set JAVA_HOME to Java 21
export JAVA_HOME=/path/to/java-21
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
java -version  # Should show version 21.x.x
```

### Common Error
If you see this error:
```
Dependency requires at least JVM runtime version 21. This build uses a Java 17 JVM.
```

You need to update your JAVA_HOME and PATH to point to Java 21.

## Building the Project
```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Run linting
./gradlew detekt
```

## Troubleshooting
- Ensure all Gradle daemons are stopped: `./gradlew --stop`
- If build cache issues occur, try: `./gradlew clean --refresh-dependencies`