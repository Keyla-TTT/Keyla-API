# Keyla-API

A modern typing test API built with Scala 3, featuring real-time analytics, user management, and comprehensive typing test functionality.

## Features

- 📊 **Real-time Analytics**: Advanced typing statistics and performance analytics
- 👤 **User Management**: Complete profile management system
- 🎯 **Typing Tests**: Configurable typing tests with multiple languages and difficulty levels
- 📚 **Dictionary Support**: Multi-language dictionary support with custom dictionary loading
- 🔧 **Configuration Management**: Dynamic configuration with hot-reload capabilities
- 📖 **API Documentation**: Auto-generated Swagger/OpenAPI documentation
- 🗄️ **Flexible Storage**: Support for both in-memory and MongoDB storage
- 🎨 **Code Quality**: Enforced code formatting with Scalafmt and Scalafix

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Configuration](#configuration)
- [Contributing](#contributing)

## Installation

### Prerequisites

- Java 17 or higher

### Download from Releases

The easiest way to get started is to download a pre-built release:

1. **Visit the [Releases page](https://github.com/your-username/Keyla-API/releases)**
2. **Download the latest `keyla-api-<version>.zip`**
3. **Extract and run** - no compilation required!

### Quick Start

#### Option 1: Download from GitHub Releases (Recommended)

1. **Download the latest release**
   - Go to the [Releases page](https://github.com/Keyla-TTT/Keyla-API/releases)
   - Download the `keyla-api-<version>.zip` file
   - Extract the zip file to your desired location

2. **Run the application**
   You will find a script to run the application inside the extracted folder

#### Option 2: Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/Keyla-TTT/Keyla-API.git
   cd Keyla-API
   ```
   
2. **Run the application**
   ```bash
   sbt run
   ```
The API will be available at `http://localhost:9999` with Swagger documentation at `http://localhost:9999/docs`.


### Configuration

The application uses a JSON configuration file that is automatically created on first run.
Check the output logs for the location of the generated configuration file.


#### Dictionary Configuration

Dictionaries are stored in the user's home directory:
- **Linux/macOS**: `~/keyla/dictionaries/`
- **Windows**: `C:\Users\{username}\keyla\dictionaries\`

You can add custom dictionaries by placing `.txt` or `.json` files in the appropriate language subdirectory.

## API Documentation

Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:9999/docs`
- **OpenAPI Spec**: `http://localhost:9999/docs/docs.yaml`

## Development

### Prerequisites

- Java 17+
- Git

### Setup Development Environment

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd Keyla-API
   ```

2. **Install pre-commit hooks**
   ```bash
   sbt installGitHook
   ```

3. **Run tests**
   ```bash
   sbt test
   ```

### Development Workflow

#### Code Quality Tools

The project enforces code quality through several tools:

- **Scalafmt**: Automatic code formatting
- **Scalafix**: Code linting and refactoring
- **Conventional Commits**: Enforced commit message format
- **Pre-commit Hooks**: Automatic code formatting on commit

#### Available sbt Tasks

```bash
# Development
sbt compile          # Compile the project
sbt test            # Run all tests
sbt testQuick       # Run only failed tests
sbt scalafmtAll     # Format all code
sbt scalafixAll     # Apply all scalafix rules

# Git Hooks
sbt installGitHook  # Install pre-commit hooks
sbt conventionalCommits  # Setup conventional commits validation

# Documentation
sbt doc             # Generate ScalaDoc documentation

# Packaging
sbt Universal/packageBin
```

#### Pre-commit Hooks

The project includes pre-commit hooks that automatically:
1. Run Scalafmt and Scalafix on staged Scala files
2. Validate commit messages follow conventional commit format
3. Auto-fix formatting issues if possible

To install the hooks:
```bash
sbt installGitHook
```

#### Conventional Commits

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for commit messages. The format is:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert`

**Examples**:
```bash
git commit -m "feat: add user analytics endpoint"
git commit -m "fix(api): resolve memory leak in test service"
git commit -m "docs: update API documentation"
```

### Project Structure

```
src/
├── main/scala/
│   ├── api/                    # HTTP API layer
│   │   ├── controllers/        # Request handlers
│   │   ├── endpoints/          # Tapir endpoint definitions
│   │   ├── models/             # API request/response models
│   │   ├── routes/             # Route definitions
│   │   └── services/           # Business logic services
│   ├── analytics/              # Analytics and statistics
│   ├── config/                 # Configuration management
│   ├── typingTest/             # Typing test core logic
│   └── users_management/       # User profile management
├── test/scala/                 # Test suite
└── main/resources/
    └── dictionaries/           # Default dictionary files
```

### Testing

The project includes comprehensive tests:

- **Unit Tests**: Individual component testing
- **Integration Tests**: API endpoint testing
- **Repository Tests**: Data persistence testing

Run tests with:
```bash
sbt test                    # All tests
sbt testOnly *ApiSpec      # API tests only
sbt testOnly *RepositorySpec # Repository tests only
```

### Adding New Features

1. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Follow the architecture**
   - Add endpoints in `api/endpoints/`
   - Add controllers in `api/controllers/`
   - Add services in `api/services/`
   - Add models in `api/models/`

3. **Write tests**
   - Unit tests for business logic
   - Integration tests for API endpoints

4. **Update documentation**
   - Update this README if needed
   - Add/update API documentation

5. **Commit with conventional format**
   ```bash
   git commit -m "feat: add your new feature"
   ```

## Configuration

### Configuration File

The application creates a default configuration file on first run. You can customize:

- Database settings (MongoDB URI, database name)
- Server settings (host, port, thread pool)
- Dictionary settings (base path, auto-creation)
- Logging configuration

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Install pre-commit hooks (`sbt installGitHook`)
4. Make your changes
5. Run tests (`sbt test`)
6. Format code (`sbt scalafmtAll && sbt scalafixAll`)
7. Commit with conventional format
8. Push to your branch (`git push origin feature/amazing-feature`)
9. Open a Pull Request

### Code Style

- Follow Scala 3 best practices
- Use Scalafmt for formatting
- Write comprehensive tests
- Use conventional commit messages
- Document public APIs

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions, issues, or contributions, please:
- Open an issue on GitHub
- Check the API documentation at `/docs`
- Review the test cases for usage examples

## Acknowledgements

- Inspired by various typing test applications and APIs