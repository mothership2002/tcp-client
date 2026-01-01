# Contributing to TCP Client Spring Boot Starter

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on the best outcome for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

- **Java 21+** (Java 21 toolchain required)
- **Gradle 8.x** (wrapper included)
- **Git** for version control
- **IDE** (IntelliJ IDEA recommended)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/yourusername/tcp-client-spring-boot-starter.git
   cd tcp-client-spring-boot-starter
   ```

3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/original-owner/tcp-client-spring-boot-starter.git
   ```

### Build the Project

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

**Note**: If you encounter Gradle daemon file locking issues on Windows, use:
```bash
./gradlew --no-daemon build
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests hyun.messageconnecter.serialization.TcpMessageSerializerTest

# Run tests with detailed output
./gradlew test --info

# Run tests in continuous mode
./gradlew test --continuous
```

## Development Workflow

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Write code following our [coding standards](#coding-standards)
   - Add tests for new functionality
   - Update documentation as needed

3. **Test your changes**
   ```bash
   ./gradlew test
   ```

4. **Commit your changes**
   - Follow our [commit message guidelines](#commit-message-guidelines)
   ```bash
   git add .
   git commit -m "[feat] Add new feature description"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Open a Pull Request**
   - Follow our [pull request process](#pull-request-process)

## Coding Standards

This project follows specific coding standards to maintain consistency and quality. For detailed guidelines, see [AGENT_GUIDE.md](.docs/AGENT_GUIDE.md).

### Key Principles

1. **No Lombok** - Use explicit getters/setters and constructors
   ```java
   // Good
   public String getName() {
       return name;
   }

   // Avoid
   @Getter
   private String name;
   ```

2. **Explicit Code Over Magic**
   - Prefer explicit implementations over frameworks or code generation
   - Use standard Java features and patterns

3. **Immutability Where Possible**
   ```java
   // Good - Immutable configuration
   public class TcpClientConfig {
       private final String host;
       private final int port;

       public TcpClientConfig(String host, int port) {
           this.host = host;
           this.port = port;
       }
   }
   ```

4. **Comprehensive JavaDoc**
   - Document all public APIs
   - Include parameter descriptions and return values
   - Provide usage examples for complex APIs
   ```java
   /**
    * Serializes a TCP message object into a byte array according to the fixed-length protocol.
    *
    * @param message the message object to serialize (must be annotated with @TcpMessage)
    * @return byte array representation of the message
    * @throws IllegalArgumentException if message is null or not properly annotated
    */
   public byte[] serialize(Object message) {
       // implementation
   }
   ```

5. **Modern Java Features**
   - Use switch expressions (Java 14+)
   - Use records for DTOs where appropriate
   - Leverage pattern matching
   ```java
   // Good - Switch expression
   return switch (autoCalculate) {
       case MESSAGE_LENGTH -> String.valueOf(totalLength);
       case CHECKSUM_CRC16 -> ChecksumUtil.calculateCRC16(data);
       case CHECKSUM_CRC32 -> ChecksumUtil.calculateCRC32(data);
       case CHECKSUM_XOR -> ChecksumUtil.calculateXOR(data);
       case NONE -> null;
   };
   ```

### Package Structure

```
hyun.messageconnecter/
├── annotation/          # Annotations (@TcpClient, @TcpMessage, @TcpField, etc.)
├── client/tcp/          # TCP client implementation (Netty-based)
├── config/              # AutoConfiguration
├── enums/               # Enums (Align, AutoCalculate, etc.)
├── factory/             # FactoryBeans
├── interceptor/         # Interceptor chain
├── proxy/               # InvocationHandlers
├── registrar/           # Bean registrars
└── util/                # Utilities (ChecksumUtil, etc.)
```

## Testing Guidelines

### Test Structure

1. **Unit Tests** - Test individual components in isolation
   - Location: `src/test/java/hyun/messageconnecter/`
   - Naming: `*Test.java`
   - Example: `TcpMessageSerializerTest.java`

2. **Integration Tests** - Test component interactions
   - Location: `src/test/java/hyun/messageconnecter/integration/`
   - Naming: `*IntegrationTest.java`
   - Example: `AutoCalculateIntegrationTest.java`

3. **E2E Tests** - Test full workflows with mock servers
   - Location: `src/test/java/hyun/messageconnecter/e2e/`
   - Naming: `*E2ETest.java`
   - Example: `ClientAnnotationE2ETest.java`

### Test Naming Convention

```java
// Pattern: methodName_scenario_expectedBehavior
@Test
void serialize_withAutoCalculateLength_shouldCalculateCorrectly() {
    // Arrange
    TestMessage message = new TestMessage();

    // Act
    byte[] result = serializer.serialize(message);

    // Assert
    assertThat(result).hasSize(100);
}
```

### Test Coverage Requirements

- **Minimum coverage**: 70% overall
- **Critical components**: 90%+ coverage
  - Serializer/Deserializer
  - Interceptor chain
  - AutoConfiguration

### Writing Good Tests

```java
@Test
void testName_scenario_expectedBehavior() {
    // Arrange - Set up test data and dependencies
    TcpMessageSerializer serializer = new TcpMessageSerializer();
    TestMessage message = new TestMessage();
    message.setField1("value");

    // Act - Execute the method under test
    byte[] result = serializer.serialize(message);

    // Assert - Verify the outcome
    assertThat(result).isNotNull();
    assertThat(result).hasSize(100);
}
```

## Commit Message Guidelines

We follow a semantic commit message format for clarity and automated changelog generation.

### Format

```
[type] Subject line (max 72 characters)

Optional body: Detailed description of changes
- Bullet point 1
- Bullet point 2

Optional footer: Related issues, breaking changes
```

### Types

- `[feat]` - New feature
- `[fix]` - Bug fix
- `[docs]` - Documentation changes
- `[style]` - Code style changes (formatting, no logic change)
- `[refactor]` - Code refactoring (no feature change)
- `[test]` - Adding or updating tests
- `[chore]` - Build process, dependency updates

### Examples

```
[feat] Add MetricsInterceptor for Micrometer integration

- Implement ClientInterceptor interface
- Add request duration and count metrics
- Support custom tags (method, status, error)
- Add configuration properties for enabling/disabling
```

```
[fix] Fix auto-calculate field padding removal

The deserializer was incorrectly preserving padding for auto-calculated
fields. This change removes padding after validation.

Closes #123
```

```
[docs] Update README with v1.1.0 interceptor features

- Add interceptor chain documentation
- Include built-in interceptor examples
- Add custom interceptor implementation guide
```

## Pull Request Process

### Before Submitting

1. **Update your branch**
   ```bash
   git fetch upstream
   git rebase upstream/master
   ```

2. **Run all tests**
   ```bash
   ./gradlew clean test
   ```

3. **Check code style**
   - Ensure code follows [coding standards](#coding-standards)
   - Add JavaDoc for public APIs
   - Remove unused imports

4. **Update documentation**
   - Update README.md if adding new features
   - Add examples for new functionality
   - Update CHANGELOG.md (if applicable)

### Submitting a Pull Request

1. **Push your branch**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request on GitHub**
   - Use a clear, descriptive title
   - Reference related issues (e.g., "Closes #123")
   - Provide detailed description of changes
   - Include screenshots for UI changes (if applicable)

3. **PR Description Template**
   ```markdown
   ## Description
   Brief description of what this PR does

   ## Type of Change
   - [ ] Bug fix (non-breaking change which fixes an issue)
   - [ ] New feature (non-breaking change which adds functionality)
   - [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
   - [ ] Documentation update

   ## Testing
   - [ ] Unit tests added/updated
   - [ ] Integration tests added/updated
   - [ ] E2E tests added/updated
   - [ ] All tests passing

   ## Checklist
   - [ ] Code follows project coding standards
   - [ ] Self-review completed
   - [ ] Comments added for complex logic
   - [ ] Documentation updated
   - [ ] No new warnings introduced
   - [ ] CHANGELOG.md updated (if applicable)

   ## Related Issues
   Closes #(issue number)
   ```

4. **Respond to Review Feedback**
   - Address reviewer comments
   - Push additional commits to the same branch
   - Request re-review when ready

### After Merge

1. **Delete your feature branch**
   ```bash
   git branch -d feature/your-feature-name
   git push origin --delete feature/your-feature-name
   ```

2. **Update your local repository**
   ```bash
   git checkout master
   git pull upstream master
   ```

## Reporting Issues

### Before Reporting

1. **Search existing issues** to avoid duplicates
2. **Try the latest version** to see if the issue is already fixed
3. **Gather information**:
   - Java version
   - Spring Boot version
   - Full stack trace (if applicable)
   - Minimal reproduction steps

### Issue Template

```markdown
## Description
Clear description of the issue

## Environment
- Java Version: 21
- Spring Boot Version: 4.0.0
- Library Version: 1.1.0
- OS: Windows 11 / macOS / Linux

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
What you expected to happen

## Actual Behavior
What actually happened

## Stack Trace (if applicable)
```
Full stack trace here
```

## Additional Context
Any other relevant information
```

### Issue Types

- **Bug Report** - Something isn't working as expected
- **Feature Request** - Suggest a new feature or enhancement
- **Documentation** - Improvements to documentation
- **Question** - Ask for help or clarification

## Development Tips

### IDE Setup (IntelliJ IDEA)

1. **Import Project**
   - File → Open → Select `build.gradle`
   - Use Gradle wrapper

2. **Enable Annotations Processing**
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Enable annotation processing

3. **Code Style**
   - Settings → Editor → Code Style → Java
   - Set indent: 4 spaces
   - Set continuation indent: 4 spaces

### Debugging

```java
// Enable debug logging in tests
logging.level.hyun.messageconnecter=DEBUG
logging.level.hyun.messageconnecter.interceptor=TRACE
```

### Common Issues

1. **Gradle Daemon Lock** (Windows)
   ```bash
   ./gradlew --no-daemon build
   ```

2. **Test Failures**
   - Check if MockTcpServer port is already in use
   - Verify test fixture data matches expected format

3. **Encoding Issues**
   - Ensure test files are UTF-8 encoded
   - Check `charset` parameter in `@TcpMessage` and `@TcpClient`

## Questions?

If you have questions about contributing:

- Check [AGENT_GUIDE.md](.docs/AGENT_GUIDE.md) for detailed development guidelines
- Check [README.md](README.md) for usage examples
- Open a discussion on GitHub
- Open an issue with the "question" label

Thank you for contributing to TCP Client Spring Boot Starter! 🎉
