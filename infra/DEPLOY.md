# Deploying Kundli Matching

Build-order step 6. This is the one-time host setup plus the repeatable deploy for the
single-EC2, no-Docker, systemd architecture described in the root [`README.md`](../README.md):

```
Nginx (TLS) ──> Spring Boot :8080 (serves API + embedded Angular) ──> SQLite file
                                     └──> Python ephemeris :8001 (127.0.0.1 only)

Monitoring:  Spring Boot Admin :9090 (127.0.0.1 only) ── scrapes ──> backend Actuator :8081
             (dashboard over SSH tunnel; emails alerts). See §7.
```

Everything below runs on the EC2 host over SSH. Conventions used throughout:

| Path | What |
|------|------|
| `/opt/kundli/src` | git checkout (build source; also holds the ephemeris venv) |
| `/opt/kundli/backend/kundli-backend.jar` | deployed fat jar |
| `/opt/kundli/ephe` | Swiss Ephemeris `.se1` data files (read-only to the service) |
| `/var/lib/kundli/kundli.db` | SQLite database (systemd `StateDirectory`) |
| `/etc/kundli/*.env` | secrets/config, `640 root:kundli` |
| `kundli` | unprivileged system user both services run as |

---

## 1. One-time host setup

### 1.1 Packages

Amazon Linux 2023 / RHEL family:
```bash
sudo dnf install -y java-21-amazon-corretto-headless maven python3 python3-pip \
                    nginx certbot python3-certbot-nginx sqlite awscli git
```
Debian/Ubuntu equivalents: `openjdk-21-jdk-headless maven python3-venv python3-pip
nginx certbot python3-certbot-nginx sqlite3 awscli git`.

Confirm the toolchain the build needs:
```bash
java -version   # 21.x
mvn -version    # uses the same JDK 21
```
The Angular build is handled by the Maven `frontend-maven-plugin` (it downloads a pinned
Node under `target/`), so you do **not** need Node installed on the host.

### 1.2 Service user and directories
```bash
sudo useradd --system --home-dir /opt/kundli --shell /usr/sbin/nologin kundli
sudo mkdir -p /opt/kundli/backend /opt/kundli/monitoring /opt/kundli/ephe /etc/kundli /var/www/certbot
sudo git clone https://github.com/ashutosh2521/astrology.git /opt/kundli/src
sudo chown -R kundli:kundli /opt/kundli
```

### 1.3 Swiss Ephemeris data files

The `.se1` files are **not** in the repo (large binaries; AGPL data downloaded at deploy
time). Without them the ephemeris service **refuses to start** — that is the intended
"no silent Moshier fallback" guard, not a bug.

Download the main planetary files from the official Swiss Ephemeris data mirror into
`/opt/kundli/ephe`:
```bash
cd /opt/kundli/ephe
sudo -u kundli curl -fLO https://raw.githubusercontent.com/aloistr/swisseph/master/ephe/sepl_18.se1  # planets
sudo -u kundli curl -fLO https://raw.githubusercontent.com/aloistr/swisseph/master/ephe/semo_18.se1  # moon
# Add other date-range files if you support births outside the sepl_18/semo_18 window.
ls -l /opt/kundli/ephe/*.se1
```

### 1.4 Environment files
```bash
sudo cp /opt/kundli/src/infra/env/backend.env.example   /etc/kundli/backend.env
sudo cp /opt/kundli/src/infra/env/ephemeris.env.example /etc/kundli/ephemeris.env
sudo cp /opt/kundli/src/infra/env/admin.env.example     /etc/kundli/admin.env
sudo cp /opt/kundli/src/infra/env/backup.env.example    /etc/kundli/backup.env   # optional
sudo chgrp kundli /etc/kundli/*.env && sudo chmod 640 /etc/kundli/*.env
```
Edit `/etc/kundli/admin.env` and set at least a strong `KUNDLI_ADMIN_PASSWORD` (see §7 for
the email settings). Edit `/etc/kundli/backup.env` and set `KUNDLI_S3_BUCKET` if you want the
nightly backup.

### 1.5 Install the systemd units
```bash
sudo cp /opt/kundli/src/infra/systemd/*.service /etc/systemd/system/
sudo cp /opt/kundli/src/infra/backup/kundli-backup.{service,timer} /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable kundli-ephemeris.service kundli-backend.service kundli-admin.service
sudo systemctl enable --now kundli-backup.timer      # optional, if S3 is configured
```
Leave the app services *enabled but not started* — the first `deploy.sh` run builds the
jars and starts them.

---

## 2. First deploy

```bash
sudo /opt/kundli/src/infra/deploy.sh
```
This pulls the latest source, builds the jar (Angular included), sets up the Python venv,
starts both services, and health-checks them. See [§4](#4-repeat-deploys) for what it does
on every subsequent run.

Verify locally before touching TLS:
```bash
curl -s http://127.0.0.1:8001/health         # ephemeris — must be 200, not 503
curl -s http://127.0.0.1:8080/api/health     # backend — {"app":"UP","ephemeris":"UP"}
```
A `503` from `/health` means the `.se1` files aren't being found — recheck [§1.3](#13-swiss-ephemeris-data-files)
and `EPHE_PATH` in `/etc/kundli/ephemeris.env`.

---

## 3. Nginx + TLS

DNS: point an `A`/`AAAA` record for `kundli.ashutoshkumar.codes` at the instance's public IP.
Security group: open `443` to the world and `22` only from your trusted IP. **Do not** open
`8080` or `8001` — those stay on loopback.

```bash
sudo cp /opt/kundli/src/infra/nginx/kundli.ashutoshkumar.codes.conf \
        /etc/nginx/conf.d/kundli.ashutoshkumar.codes.conf   # or sites-available + symlink on Debian
```

Obtain the certificate. The committed config references Let's Encrypt cert paths and the
`/var/www/certbot` webroot challenge location, so:
```bash
sudo certbot certonly --webroot -w /var/www/certbot -d kundli.ashutoshkumar.codes
sudo nginx -t && sudo systemctl reload nginx
```
(If `options-ssl-nginx.conf` / `ssl-dhparams.pem` don't exist yet, install them with
`certbot` once, or run `sudo certbot --nginx -d kundli.ashutoshkumar.codes` to let certbot
manage the block on first issue.) Renewal is automatic via certbot's own timer; add an
`--deploy-hook "systemctl reload nginx"` if you used `certonly`.

Confirm end to end: `https://kundli.ashutoshkumar.codes/api/health` returns `UP`.

---

## 4. Repeat deploys

Every code change afterwards is one command on the host:
```bash
sudo /opt/kundli/src/infra/deploy.sh            # deploy the checked-out branch's tip
sudo /opt/kundli/src/infra/deploy.sh <git-ref>  # or a specific branch/tag/SHA
```
It fetches, fast-forwards, verifies the `.se1` files are present, rebuilds the jar,
refreshes the venv, restarts ephemeris then backend, and fails loudly if either health
check doesn't come up.

---

## 5. Operating

```bash
# Status & logs
systemctl status kundli-backend kundli-ephemeris
journalctl -u kundli-backend -f
journalctl -u kundli-ephemeris -n 100

# Restart just one service
sudo systemctl restart kundli-ephemeris

# Backups
systemctl list-timers kundli-backup.timer
sudo systemctl start kundli-backup.service      # run one now
```

**Restore** from a backup: stop the backend, replace the DB, restart:
```bash
sudo systemctl stop kundli-backend
aws s3 cp s3://your-bucket/kundli/kundli-<STAMP>.db.gz - | gunzip \
  | sudo -u kundli tee /var/lib/kundli/kundli.db >/dev/null
sudo systemctl start kundli-backend
```

## 6. Rollback

Deploy the previous known-good ref — the jar is rebuilt from that source:
```bash
sudo /opt/kundli/src/infra/deploy.sh <previous-tag-or-sha>
```
The SQLite schema uses `ddl-auto=update` (additive), so rolling code back is generally safe;
a restore ([§5](#5-operating)) is only needed if a migration was destructive.

## 7. Monitoring (Spring Boot Admin)

A small **Spring Boot Admin** server (the `monitoring/` module) watches the backend and
emails you when something breaks. The backend registers with it and exposes Actuator on a
**loopback-only** port (`8081`, never proxied by Nginx); the Admin server scrapes that and
tracks the `ephemeris` health component too — so a **degraded Swiss Ephemeris precision**
(the 503 guard) shows up as DOWN and triggers an alert, not just a hard crash.

`deploy.sh` builds and restarts it alongside the backend; the unit was installed and enabled
in [§1.5](#15-install-the-systemd-units). It runs on `127.0.0.1:9090` and is **never exposed
to the internet** — no Nginx block, no open port.

### Email alerts

Edit `/etc/kundli/admin.env` (already copied in [§1.4](#14-environment-files)) and fill in
your SMTP details and alert address — `KUNDLI_SMTP_HOST/PORT/USER/PASSWORD`, `KUNDLI_ALERT_TO`,
`KUNDLI_ALERT_FROM`. Amazon SES SMTP or a Gmail app-password both work; the template has an SES
example. Leave `KUNDLI_SMTP_HOST` empty to run the dashboard without email. Restart after edits:
```bash
sudo systemctl restart kundli-admin
```
You'll get an email whenever the backend (or the ephemeris component) transitions DOWN/OFFLINE,
and again when it recovers.

> Note: the Admin server runs *on the same host*, so it can't email you if the whole box is
> down. To catch total-host outages, point a free external pinger (Healthchecks.io / UptimeRobot)
> at `https://kundli.ashutoshkumar.codes/api/health` as well.

### Viewing the dashboard

The dashboard isn't public. Open an SSH tunnel from your machine, then browse locally:
```bash
ssh -L 9090:127.0.0.1:9090 your-ec2-host
# then open http://localhost:9090 and log in with KUNDLI_ADMIN_USER / KUNDLI_ADMIN_PASSWORD
```

### Disabling it

Set `KUNDLI_ADMIN_ENABLED=false` in `/etc/kundli/backend.env` (the backend stops registering),
then `sudo systemctl disable --now kundli-admin`. The backend runs fine without it.
