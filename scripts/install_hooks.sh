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

# The pre-push gate runs the full test suite (several minutes). Git opens the
# SSH connection to the remote *before* the hook runs, so an idle connection
# can be dropped mid-check; git then dies with SIGPIPE (141) even though the
# hook passed. Server keepalives prevent that.
if git remote get-url origin 2>/dev/null | grep -qE '^(git@|ssh://)'; then
  if ! git config --get core.sshCommand >/dev/null 2>&1; then
    git config --local core.sshCommand \
      "ssh -o ServerAliveInterval=20 -o ServerAliveCountMax=15"
    echo "✓ SSH keepalive enabled for long pre-push gates (core.sshCommand)."
  fi
fi

echo "✓ Git hooks installed from .githooks/."
echo "    pre-commit : fast checks on staged files (PHP lint + Pint, flutter analyze)"
echo "    pre-push   : full project health check (scripts/check_project.sh)"
echo
echo "  Bypass temporarily with:  git commit --no-verify | git push --no-verify"
echo "  Uninstall with:           git config --unset core.hooksPath"