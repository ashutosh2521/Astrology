# Containerized deployment (Docker)

An **alternative** to the systemd/jar runbook in [`DEPLOY.md`](./DEPLOY.md), for the
"build one reproducible artifact locally, ship it, run it on the server" workflow. The
root [`README.md`](../README.md) records a deliberate *no-Docker* decision for the systemd
path; this file is the containerized option kept alongside it, not a replacement. Both
deploy the same three services and reuse the same `/etc/kundli/*.env` files, the same
host `.se1` data files, and the same host Nginx + TLS.

```
Host Nginx (TLS) ──> 127.0.0.1:8080  ┌─────────── docker network "kundli" ───────────┐
                                     │  backend  ──http://ephemeris:8001──> ephemeris │
Host 127.0.0.1:9090 (SSH tunnel) ──> │  (:8080/:8081)          (no published port)    │
                                     │      └──registers──> monitoring (:9090)        │
                                     └────────────────────────────────────────────────┘
   Volumes:  kundli-db (named) -> backend /var/lib/kundli   |   host /opt/kundli/ephe:ro -> ephemeris
```

Only **backend** (`8080`) and **monitoring** (`9090`) are published, both on `127.0.0.1`.
The **ephemeris** container has no published port — it's reachable only by the backend on
the internal network, preserving the "internal-only" guarantee. Nginx/TLS stay on the
host exactly as in [`DEPLOY.md` §3](./DEPLOY.md).

---

## 1. One-time server setup

Everything the systemd runbook needs **except** the JDK/Maven/Python build toolchain — the
server only runs containers now.

### 1.1 Install Docker + compose plugin (Amazon Linux 2023)
```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
# Run docker without sudo (so the deploy script needs no sudo). Log out/in after this.
sudo usermod -aG docker "$USER"
# Compose plugin:
sudo dnf install -y docker-compose-plugin || {
  sudo mkdir -p /usr/libexec/docker/cli-plugins
  sudo curl -fL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
       -o /usr/libexec/docker/cli-plugins/docker-compose
  sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose
}
docker compose version
```
You still need Nginx + certbot on the host for TLS:
```bash
sudo dnf install -y nginx certbot python3-certbot-nginx
```

### 1.2 Env files and directories
Same as [`DEPLOY.md` §1.4](./DEPLOY.md) — the containers read these unchanged:
```bash
sudo mkdir -p /etc/kundli /opt/kundli/ephe /opt/kundli/deploy /var/www/certbot
sudo git clone https://github.com/ashutosh2521/astrology.git /opt/kundli/src   # for the env examples
sudo cp /opt/kundli/src/infra/env/backend.env.example   /etc/kundli/backend.env
sudo cp /opt/kundli/src/infra/env/ephemeris.env.example /etc/kundli/ephemeris.env
sudo cp /opt/kundli/src/infra/env/admin.env.example     /etc/kundli/admin.env
sudo chmod 640 /etc/kundli/*.env
```
Set a strong `KUNDLI_ADMIN_PASSWORD` in `admin.env` (and SMTP details if you want email
alerts). Leave `backend.env` / `ephemeris.env` defaults as-is — the compose file overrides
the inter-service URLs; `EPHE_PATH=/opt/kundli/ephe` and `KUNDLI_DB_PATH=/var/lib/kundli/kundli.db`
are already correct for the container mounts.

> The env files must be readable by the docker daemon at `up` time. `640 root:root` is fine
> since the deploy loads them via the daemon (root). If you run compose as a non-root user
> and hit a permissions error reading them, `sudo chmod 644 /etc/kundli/*.env` (they hold a
> dashboard password + SMTP creds, so prefer keeping the host locked down over widening this).

### 1.3 Swiss Ephemeris data (unchanged, still host-side)
```bash
cd /opt/kundli/ephe
sudo curl -fLO https://www.astro.com/ftp/swisseph/ephe/sepl_18.se1   # planets
sudo curl -fLO https://www.astro.com/ftp/swisseph/ephe/semo_18.se1   # moon
ls -l /opt/kundli/ephe/*.se1
```
Mounted read-only into the ephemeris container. Missing files ⇒ the container's `/health`
returns 503 by design (no silent Moshier fallback).

---

## 2. Deploy from your workstation

One command builds all three images locally, ships them by SFTP, and starts the stack:
```bash
infra/docker-deploy.sh ec2-user@kundli.ashutoshkumar.codes
```
What it does: `docker compose build` → `docker save | gzip` → SFTP the tarball +
`docker-compose.yml` to `/opt/kundli/deploy` → `docker load` → `docker compose up -d
--no-build` → backend health check. Repeat deploys are the same one command.

Your **laptop needs only Docker** — the JDK, Maven, Node, and Python toolchains all live
inside the image build stages and are thrown away.

Verify locally on the server before TLS:
```bash
curl -s http://127.0.0.1:8080/api/health     # {"app":"UP","ephemeris":"UP"}
docker compose -f /opt/kundli/deploy/docker-compose.yml ps
```

---

## 3. Nginx + TLS (host, unchanged)

Identical to [`DEPLOY.md` §3](./DEPLOY.md) — the config reverse-proxies to `127.0.0.1:8080`,
which is now the backend container's published port:
```bash
sudo cp /opt/kundli/src/infra/nginx/kundli.ashutoshkumar.codes.conf \
        /etc/nginx/conf.d/kundli.ashutoshkumar.codes.conf
sudo certbot certonly --webroot -w /var/www/certbot -d kundli.ashutoshkumar.codes
sudo nginx -t && sudo systemctl reload nginx
curl -s https://kundli.ashutoshkumar.codes/api/health
```

---

## 4. Operating

```bash
cd /opt/kundli/deploy
docker compose ps                       # status
docker compose logs -f backend          # follow one service
docker compose logs -f ephemeris
docker compose restart ephemeris        # restart one service
docker compose down                     # stop the stack (keeps volumes/data)
docker compose up -d --no-build         # start from loaded images
```

**Monitoring dashboard** (never public) — over an SSH tunnel, same as the systemd setup:
```bash
ssh -L 9090:127.0.0.1:9090 ec2-user@kundli.ashutoshkumar.codes
# then browse http://localhost:9090 (login: KUNDLI_ADMIN_USER / KUNDLI_ADMIN_PASSWORD)
```

---

## 5. Backups

The SQLite DB now lives in the named volume `kundli_kundli-db` instead of a bare host path,
so the systemd backup unit's `KUNDLI_DB_PATH` file copy doesn't apply as-is. Back it up by
dumping from the volume:
```bash
# Consistent copy straight out of the volume to S3 (adjust bucket):
docker run --rm -v kundli_kundli-db:/data alpine \
    sh -c 'cat /data/kundli.db' | gzip \
  | aws s3 cp - s3://your-bucket/kundli/kundli-$(date +%F).db.gz
```
Restore: stop the backend container, write the DB back into the volume, start it:
```bash
cd /opt/kundli/deploy && docker compose stop backend
aws s3 cp s3://your-bucket/kundli/kundli-<STAMP>.db.gz - | gunzip \
  | docker run --rm -i -v kundli_kundli-db:/data alpine sh -c 'cat > /data/kundli.db'
docker compose start backend
```

---

## 6. Rollback

`docker save` older images with a version tag before overwriting `:latest`, or rebuild from
a previous git ref locally and re-run `docker-deploy.sh`. The SQLite schema uses additive
`ddl-auto=update`, so rolling the code back is generally safe without a DB restore.

---

## Systemd vs. Docker — pick one, not both

Don't run the systemd units and the containers at the same time: both bind `127.0.0.1:8080`
/`:9090` and both want the SQLite DB. If you're switching an existing systemd host to Docker,
`sudo systemctl disable --now kundli-backend kundli-ephemeris kundli-admin` first, and migrate
the DB from `/var/lib/kundli/kundli.db` into the `kundli_kundli-db` volume using the restore
recipe in §5.
