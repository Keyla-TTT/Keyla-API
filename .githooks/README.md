# Git Hooks

This directory contains Git hooks for the project. These hooks ensure code quality by running scalafmt and scalafix before each commit.

## Setup

### Option 1: Using Dev Containers (Recommended)

The project includes Dev Container configuration for a consistent development environment. To use it:

1. Install [Docker](https://www.docker.com/products/docker-desktop)
2. Install [VS Code](https://code.visualstudio.com/)
3. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
4. Open the project in VS Code
5. When prompted, click "Reopen in Container" or use the command palette (F1) and select "Dev Containers: Reopen in Container"

The Dev Container will automatically:
- Set up the correct Scala and sbt versions
- Configure Git hooks
- Install necessary VS Code extensions
- Set up formatting and linting

### Option 2: Manual Setup

If you prefer to work outside of Dev Containers, run the following command from the root of the repository:

```bash
./setup-hooks.sh
```

This will configure Git to use the hooks from this directory.

## Available Hooks

- `pre-commit`: Runs scalafmt and scalafix before each commit to ensure code formatting and style consistency.

## Requirements

- sbt must be installed and available in your PATH
- scalafmt and scalafix must be configured in your build.sbt

## Troubleshooting

If you encounter any issues:

1. Make sure sbt is installed and available in your PATH
2. Verify that scalafmt and scalafix are properly configured in your build.sbt
3. Check that the hooks are executable (`chmod +x .githooks/*`)
4. If using Dev Containers, ensure Docker is running and you have sufficient resources allocated 