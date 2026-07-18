# infra/

Deployment infrastructure for the single-EC2, no-Docker, systemd architecture (root
[`README.md`](../README.md), build-order step 6). Nginx terminates TLS and reverse-proxies
to Spring Boot (`:8080`), which serves the API and the embedded Angular build and calls the
Python ephemeris service (`127.0.0.1:8001`). SQLite is a file on disk.

**Start here: [`DEPLOY.md`](./DEPLOY.md)** — one-time host setup, first deploy, TLS, and the
repeatable deploy command.

## Contents

| Path | What |
|------|------|
| `DEPLOY.md` | The runbook — host setup → deploy → TLS → operate → rollback. |
| `deploy.sh` | Repeatable deploy: pull, build jar, refresh venv, restart, health-check. Runs on the host. |
| `systemd/kundli-ephemeris.service` | Ephemeris service unit. Loopback bind hardcoded; hardened sandbox. |
| `systemd/kundli-backend.service` | Backend unit. `StateDirectory=kundli` owns `/var/lib/kundli`. |
| `nginx/kundli.ashutoshkumar.codes.conf` | Server block: HTTP→HTTPS redirect, ACME webroot, TLS reverse proxy. |
| `env/*.env.example` | Templates copied to `/etc/kundli/*.env` (chmod 640). No secrets committed. |
| `backup/` | Nightly `sqlite3 .backup` → S3, as a systemd service + timer. |

## Conventions

- Both services run as the unprivileged system user **`kundli`**.
- Config/secrets live in `/etc/kundli/*.env`, never in the repo — only `.example` templates are committed.
- Ports `8080` (backend) and `8001` (ephemeris) are **loopback-only**; the security group opens
  `443` (and `22` from a trusted IP) — nothing else.
- The ephemeris service refuses to start without the Swiss Ephemeris `.se1` files, which are
  downloaded at deploy time and never committed (see `DEPLOY.md` §1.3). That is the intended
  "no silent Moshier fallback" guard.
