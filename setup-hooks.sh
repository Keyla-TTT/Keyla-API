#!/bin/sh

# Get the absolute path to the repository
REPO_ROOT=$(git rev-parse --show-toplevel)

# Configure Git to use the hooks from .githooks
git config core.hooksPath "$REPO_ROOT/.githooks"

echo "Git hooks have been configured successfully!"
echo "The hooks will be loaded from: $REPO_ROOT/.githooks" 