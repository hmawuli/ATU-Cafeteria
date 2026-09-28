#!/usr/bin/env bash
#
# Install the repository git hooks for the current clone.
#
# Uses `core.hooksPath` so hooks are versioned with the repo (.githooks/)
# instead of living in the untracked .git/hooks directory.
#
# Usage:  scripts/install_hooks.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

git config core.hooksPath .githooks
echo "✓ Git hooks installed from .githooks/."
echo "    pre-commit : fast checks on staged files (PHP lint + Pint, flutter analyze)"
echo "    pre-push   : full project health check (scripts/check_project.sh)"
echo
echo "  Bypass temporarily with:  git commit --no-verify | git push --no-verify"
echo "  Uninstall with:           git config --unset core.hooksPath"