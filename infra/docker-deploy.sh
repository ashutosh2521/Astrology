#!/usr/bin/env bash
#
# Build the three images LOCALLY, ship them to the server by SFTP, and bring the stack up
# remotely. Run this FROM YOUR WORKSTATION (needs Docker + ssh/sftp access to the host).
#
#   infra/docker-deploy.sh <ssh-user>@<host>
#   e.g.  infra/docker-deploy.sh ec2-user@kundli.ashutoshkumar.codes
#
# Prerequisites on the server (one-time, see DOCKER.md):
#   * Docker Engine + compose plugin installed, and the ssh user in the `docker` group
#     (so no sudo is needed here).
#   * /etc/kundli/*.env present, and /opt/kundli/ephe populated with the .se1 files.
#
set -euo pipefail

SSH_TARGET="${1:?usage: docker-deploy.sh <ssh-user>@<host>}"
REMOTE_DIR="/opt/kundli/deploy"
IMAGES=(kundli-backend:latest kundli-ephemeris:latest kundli-monitoring:latest)
TAR="kundli-images.tar.gz"

cd "$(git rev-parse --show-toplevel)"
COMPOSE="infra/docker-compose.yml"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }

log "Building images locally"
# Build with plain `docker build`, NOT `docker compose build`: compose would load the whole
# model first — including the env_file: /etc/kundli/*.env references, which exist only on the
# server — and fail here on the workstation. The build itself needs no runtime env files.
# Image tags below match the image: names in docker-compose.yml, so `docker save` and the
# server-side `docker compose up` resolve them.
docker build -f backend/Dockerfile -t kundli-backend:latest .
docker build -t kundli-ephemeris:latest ephemeris-service
docker build -t kundli-monitoring:latest monitoring

log "Saving images to $TAR ($(printf '%s ' "${IMAGES[@]}"))"
docker save "${IMAGES[@]}" | gzip > "$TAR"
ls -lh "$TAR"

log "Shipping bundle + compose file to $SSH_TARGET:$REMOTE_DIR via SFTP"
ssh "$SSH_TARGET" "mkdir -p $REMOTE_DIR"
sftp "$SSH_TARGET" <<SFTP
put $TAR $REMOTE_DIR/
put $COMPOSE $REMOTE_DIR/docker-compose.yml
bye
SFTP

log "Loading images and (re)starting the stack on the server"
ssh "$SSH_TARGET" "cd $REMOTE_DIR \
    && gunzip -c $TAR | docker load \
    && docker compose -f docker-compose.yml up -d --no-build \
    && docker image prune -f"

log "Health check — backend may take ~30s to boot (Spring Boot + ephemeris warmup)"
ok=0
for i in $(seq 1 20); do
    if ssh "$SSH_TARGET" 'curl -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1'; then
        ssh "$SSH_TARGET" 'curl -fsS http://127.0.0.1:8080/api/health; echo'
        ok=1; break
    fi
    sleep 3
done
[[ "$ok" -eq 1 ]] || { echo "backend health check failed after ~60s — inspect logs with:
  ssh $SSH_TARGET 'cd $REMOTE_DIR && docker compose logs --tail=50 backend ephemeris'"; exit 1; }

rm -f "$TAR"
log "Deploy complete. Nginx/TLS on the host proxies https://kundli.ashutoshkumar.codes -> 127.0.0.1:8080"
