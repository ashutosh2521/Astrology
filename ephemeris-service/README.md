# Ephemeris Service

Internal-only Python microservice. Given a **UTC instant** and **coordinates**, it
returns sidereal (**Lahiri**) chart data computed with **Swiss Ephemeris** at full
precision. It is the accuracy-critical core of the app and is built and validated
first, in isolation, before anything consumes it.

It knows nothing about timezones (the Java backend resolves local time → UTC) and
nothing about matching (the Java backend runs the Ashtakoot engine). Its only job is
astronomy.

## Accuracy guarantees

1. **No silent Moshier fallback.** Every calculation asserts the ephemeris that was
   *actually used* was the file-based full-precision one. If the `.se1` files are
   missing, the service refuses to start (`self_check` at startup) and `/chart`
   returns `503` rather than a degraded answer. See `app/ephemeris.py`.
2. **Lahiri ayanamsa, explicit.** Set in code at startup; also required on every
   request so any future change is a visible contract change, never a silent default.
3. **Boundary distances, not verdicts.** The Moon block reports raw
   `degrees_to_boundary`; whether that is "close enough to warn" is a policy
   threshold owned by the backend.

## Endpoints

- `GET /health` — liveness; re-asserts full precision (503 if degraded).
- `POST /chart` — compute a chart.

Request:
```json
{
  "utc": "1990-05-15T09:23:00Z",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "ayanamsa": "LAHIRI"
}
```

Response (abridged): sidereal longitude + Rashi for every graha and the Ascendant,
plus a Moon block with Rashi, Nakshatra, Pada and boundary distances, and a
`precision` field for a per-chart audit trail.

## Ephemeris data files

The `.se1` files are **not** committed (large binaries; see root `.gitignore`).
At deploy time, download them from Astrodienst into the directory named by
`EPHE_PATH` (default `./ephe`). Without them the service will correctly refuse to start.

## Run locally

```bash
cd ephemeris-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
# Provide real .se1 files first, then:
EPHE_PATH=./ephe uvicorn app.main:app --host 127.0.0.1 --port 8001
```

## Tests

```bash
pytest                     # pure-math unit tests (no ephemeris files needed)
```

The end-to-end **accuracy regression suite** — charts cross-checked against Drik
Panchang, including deliberate Nakshatra/Rashi boundary cases — is added next and
requires the real `.se1` data files.

## Configuration

| Env var | Default | Meaning |
|---------|---------|---------|
| `EPHE_PATH` | `./ephe` | Directory of `.se1` data files |
| `EPHEMERIS_HOST` | `127.0.0.1` | Bind host — internal only, never `0.0.0.0` |
| `EPHEMERIS_PORT` | `8001` | Bind port |
