# Kundli Matching

A personal-scale Vedic kundli (horoscope) matching application. **Accuracy of the
astrological calculations is the core requirement** — the app is only useful if the
matching results are correct. Every architectural decision below is subordinate to that.

## License

This project is licensed under the **GNU Affero General Public License v3.0** (see
[`LICENSE`](./LICENSE)). This is a deliberate choice: the calculation engine depends on
[Swiss Ephemeris](https://www.astro.com/swisseph/) (via `pyswisseph`), which is
dual-licensed AGPL-3.0 / commercial. Because this app serves users over a network,
AGPL's section 13 ("Remote Network Interaction") obliges us to offer the source to
those users. We satisfy that by open-sourcing the entire project under the same license
rather than mixing licenses across components.

## Architecture

Single EC2 instance, all components run as plain host processes managed by **systemd** —
no Docker. Nginx terminates TLS and serves the web client's static build; the two backend
processes bind to localhost only and are never reachable from outside the host.

```
Angular web client ─┐
                     ├──> Nginx (TLS, static + reverse proxy) ──> Spring Boot API ──> SQLite (file)
Ionic/Android app ───┘        (kundli.ashutoshkumar.codes)              │
                                                                        └──> Python ephemeris service
                                                                             (pyswisseph, 127.0.0.1 only)
```

- **Nginx** — TLS via Let's Encrypt, serves the Angular static build, reverse-proxies
  `/api` to Spring Boot.
- **Spring Boot** — owns users, birth profiles, matches; runs the Ashtakoot rules engine;
  calls the ephemeris service. Single fat jar under systemd.
- **Python ephemeris service** — pure astronomical calculator over Swiss Ephemeris. Bound
  to `127.0.0.1` only. Under systemd (uvicorn/gunicorn).
- **SQLite** — single file on disk. No server process. Backed up by nightly copy to S3.
- **Monitoring** — a loopback-only **Spring Boot Admin** server (`monitoring/`) tracks the
  backend's health (including the ephemeris precision guard) and emails on outage. Reached
  over an SSH tunnel, never exposed. See [`infra/DEPLOY.md`](./infra/DEPLOY.md) §7.

Security groups: only 443 (and 22 from a trusted IP) open. The ephemeris service and the
database are unreachable externally by virtue of localhost binding + a file on disk.

### Domain

`ashutoshkumar.codes` is the umbrella domain for multiple apps; this one lives at
`kundli.ashutoshkumar.codes`. Future apps get their own subdomain (own Nginx server block,
own local backend port) on the same host.

## Locked-in decisions (this design pass)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Database | **SQLite** | Personal scale, single-writer load. Schema kept portable so Postgres is a clean upgrade path if ever needed. |
| Containers | **No Docker** | Nothing here needs container isolation/portability at this scale. systemd + localhost binding gives the same "internal only" guarantee. |
| License | **AGPL-3.0**, whole project | Required by the Swiss Ephemeris dependency for network-served software; single license avoids ambiguity. |
| Ayanamsa | **Lahiri**, passed explicitly on every call | Standard for Vedic matching; explicit so a future change can never be a silent default flip. |
| Precision | **Full Swiss Ephemeris (`.se1` files)** | Startup self-check + per-response echo must confirm we are NOT in Moshier fallback. |
| Nadi / Bhakoot cancellation convention | **Mirror Drik Panchang** | One consistent reference source for both the cancellation logic and the regression suite. |
| Mangal Dosha | **Deferred past v1** | Needs Lagna/house analysis + geocoding precision Ashtakoot doesn't. Data model stores full chart data so it can be added later without recompute. |
| Timezone → UTC resolution | **Owned by Java** | Keeps India-specific pre-1950 historical-offset logic testable in the backend; Python stays a pure "UTC instant + coords → sky" calculator. |

## Java ↔ Python API contract (summary)

One internal endpoint: **compute chart**. Called only at profile creation/edit time, not
per match — the result is cached in `birth_charts`.

- **Request** (resolved by Java first): a UTC instant, decimal lat/lon, explicit ayanamsa.
- **Response** (computed by Python): sidereal longitude for all grahas + Ascendant (v1
  only consumes the Moon, but the rest is nearly free to compute and enables later doshas
  without a contract change); for the Moon — Rashi, Nakshatra, Pada, and **raw degrees into
  the current Nakshatra/Rashi** (boundary classification is a Java-side policy threshold,
  not baked into the ephemeris service); and a precision-mode field for a per-chart audit trail.

Matching itself is pure table-driven Java logic over two already-computed charts — zero
calls to Python.

## Accuracy & validation

- Reference charts (ordinary + deliberate Nakshatra/Rashi boundary cases) with outputs
  hand-verified against **Drik Panchang**.
- Regression suite runs as a required gate on any change touching the ephemeris service or
  the koota engine — from the first commit, not retrofitted. The harness is in place (see
  [`docs/regression.md`](./docs/regression.md)): chart cases validate ephemeris output and
  koota cases validate the engine against Drik Panchang. Seeded cases skip until their
  reference values are filled, then fail on any mismatch — that's what promotes the
  provisional tables (see [`backend/VERIFICATION.md`](./backend/VERIFICATION.md)) to verified.
- Ashtakoot reference tables are **static JSON**, validated at startup (matrix dimensions,
  no missing Rashi/Nakshatra keys), so each koota is independently unit-testable.

## Build order

Accuracy-first — the riskiest, hardest-to-fix-later parts come first:

1. **Python ephemeris service** — validated against Drik Panchang in isolation.
2. **Ashtakoot rules engine** (Java) — table-driven, with the regression suite as the gate.
3. **Spring Boot orchestration** — API, persistence, auth.
4. **Angular web client.**
5. **Ionic/Android wrapper** (packaging the same Angular app).
6. **Infra/deploy** — stood up in parallel once the API contract is stable.

## Repository layout

```
ephemeris-service/   Python ephemeris microservice (pyswisseph)
backend/             Spring Boot API + Ashtakoot engine + reference tables + embedded web build
web/                 Angular web client (built into the backend jar at package time)
mobile/              Ionic + Capacitor Android wrapper (later)
monitoring/          Spring Boot Admin server — health dashboard + email alerts
infra/               Nginx, systemd units, deploy script + runbook (see infra/DEPLOY.md)
docs/                Design notes, ADRs, the original spec
```

### Single-jar deployment

`mvn package` in `backend/` builds the Angular app via `frontend-maven-plugin` (pinned
Node, reproducible) and embeds it under the jar's `static/`; Spring Boot serves the SPA
with an index.html fallback for deep links (`SpaWebConfig`). One deployable artifact —
Nginx's job reduces to TLS + reverse proxy. Skip the web build during backend iteration
with `-Dskip.web`.
