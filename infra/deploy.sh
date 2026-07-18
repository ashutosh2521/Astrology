#!/usr/bin/env bash
#
# Repeatable deploy: pull latest source, build the backend jar (with the embedded Angular
# build), refresh the ephemeris venv, and restart both services. Run ON THE EC2 HOST.
#
#   sudo /opt/kundli/src/infra/deploy.sh [git-ref]
#
# First-time host setup (create the user, install units, obtain TLS certs, download the
# .se1 files) is a one-time manual runbook — see infra/DEPLOY.md. This script assumes that
# has already been done.
set -euo pipefail

SRC_DIR="${KUNDLI_SRC_DIR:-/opt/kundli/src}"
BACKEND_DIR="${KUNDLI_BACKEND_DIR:-/opt/kundli/backend}"
MON_DIR="${KUNDLI_MON_DIR:-/opt/kundli/monitoring}"
EPHE_DIR="${KUNDLI_EPHE_DIR:-/opt/kundli/ephe}"
APP_USER="${KUNDLI_USER:-kundli}"
GIT_REF="${1:-}"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
die() { printf '\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || die "run as root (sudo): needs to restart systemd services."
[[ -d "$SRC_DIR/.git" ]] || die "$SRC_DIR is not a git checkout. Do first-time setup per infra/DEPLOY.md."

# 1. Pull latest source.
log "Updating source in $SRC_DIR"
git -C "$SRC_DIR" fetch --prune origin
if [[ -n "$GIT_REF" ]]; then
    git -C "$SRC_DIR" checkout "$GIT_REF"
    git -C "$SRC_DIR" pull --ff-only origin "$GIT_REF" || true
else
    git -C "$SRC_DIR" pull --ff-only
fi
log "Now at $(git -C "$SRC_DIR" rev-parse --short HEAD) — $(git -C "$SRC_DIR" log -1 --pretty=%s)"

# 2. Sanity: ephemeris data must be present, or the service will (correctly) refuse to start.
log "Checking Swiss Ephemeris data files"
compgen -G "$EPHE_DIR/*.se1" >/dev/null || die "no .se1 files in $EPHE_DIR — see infra/DEPLOY.md (Ephemeris data)."

# 3. Build the backend jar (embeds the Angular build via frontend-maven-plugin).
log "Building backend jar (this compiles the Angular app too)"
( cd "$SRC_DIR/backend" && mvn -q -B clean package -DskipTests )
JAR="$(ls -t "$SRC_DIR"/backend/target/kundli-backend-*.jar | head -1)"
[[ -n "$JAR" ]] || die "build produced no jar."
log "Built $JAR"

install -o "$APP_USER" -g "$APP_USER" -m 0644 "$JAR" "$BACKEND_DIR/kundli-backend.jar"

# 3b. Build the monitoring (Spring Boot Admin) jar. Skipped only if the module is absent
#     (e.g. deploying an older ref) so this script stays compatible with pre-monitoring tags.
if [[ -f "$SRC_DIR/monitoring/pom.xml" ]]; then
    log "Building monitoring jar"
    ( cd "$SRC_DIR/monitoring" && mvn -q -B clean package -DskipTests )
    MON_JAR="$(ls -t "$SRC_DIR"/monitoring/target/kundli-monitoring-*.jar | head -1)"
    [[ -n "$MON_JAR" ]] || die "monitoring build produced no jar."
    install -o "$APP_USER" -g "$APP_USER" -m 0644 "$MON_JAR" "$MON_DIR/kundli-monitoring.jar"
    log "Built $MON_JAR"
    DEPLOY_MONITORING=1
else
    log "No monitoring/ module at this ref — skipping."
    DEPLOY_MONITORING=0
fi

# 4. Refresh the ephemeris virtualenv.
log "Refreshing ephemeris virtualenv"
VENV="$SRC_DIR/ephemeris-service/.venv"
[[ -d "$VENV" ]] || python3 -m venv "$VENV"
"$VENV/bin/pip" install --quiet --upgrade pip
"$VENV/bin/pip" install --quiet -r "$SRC_DIR/ephemeris-service/requirements.txt"
chown -R "$APP_USER:$APP_USER" "$VENV"

# 5. Restart services (ephemeris first — the backend depends on it).
log "Restarting services"
systemctl restart kundli-ephemeris.service
systemctl restart kundli-backend.service
# Restart monitoring only if its unit is installed on this host.
if [[ "$DEPLOY_MONITORING" -eq 1 ]] && systemctl list-unit-files kundli-admin.service >/dev/null 2>&1; then
    systemctl restart kundli-admin.service || log "kundli-admin restart failed (non-fatal) — check its unit/env."
fi

# 6. Health checks — the ephemeris /health returns 503 if it degraded to Moshier fallback.
log "Health checks"
sleep 4
curl -fsS http://127.0.0.1:8001/health >/dev/null && echo "  ephemeris: OK" \
    || die "ephemeris health check failed — check: journalctl -u kundli-ephemeris -n 50"
for i in $(seq 1 10); do
    if curl -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
        echo "  backend: OK"; break
    fi
    [[ $i -eq 10 ]] && die "backend health check failed — check: journalctl -u kundli-backend -n 50"
    sleep 2
done
# Monitoring is non-critical to serving traffic, so a failure here warns, doesn't abort.
if [[ "$DEPLOY_MONITORING" -eq 1 ]] && systemctl is-active --quiet kundli-admin.service; then
    curl -fsS http://127.0.0.1:9090/actuator/health >/dev/null 2>&1 && echo "  monitoring: OK" \
        || log "monitoring health check failed — check: journalctl -u kundli-admin -n 50"
fi

log "Deploy complete."
