#!/usr/bin/env bash
#
# One-time preparation of an Ubuntu server for ATU Cafeteria Deploy. Run AS
# ROOT on the target server (this machine has no root — this runs on the host).
#
#   sudo bash scripts/prepare_host.sh
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "✗ Run as root on the target server: sudo bash scripts/prepare_host.sh" >&2
  exit 1
fi

echo "◆ Installing Docker engine + Compose plugin…"
curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
sh /tmp/get-docker.sh

echo "◆ Enabling and starting the Docker daemon…"
systemctl enable --now docker

echo "◆ Verifying…"
docker version --format 'server: {{.Server.Version}}' || true
docker compose version

echo "✅ Host ready. Now from the repo on this server:"
echo "   cp backend/.env.example backend/.env   # fill secrets"
echo "   export APP_ENV=production"
echo "   bash scripts/deploy_docker.sh"