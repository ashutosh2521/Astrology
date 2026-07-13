# Backend

Java 21 / Spring Boot 3.3 / Maven. Hosts the **REST API**, **SQLite persistence**, the
**ephemeris client**, and the **Ashtakoot matching engine**.

The engine remains deliberately **pure and Spring-free** — plain functions over two
`MoonChart`s and static JSON reference tables — so each koota is unit-testable in
isolation and the accuracy regression suite can pin it to Drik Panchang independently.
Spring wires it as a bean; it never depends on Spring.

## Layout

```
src/main/java/.../kundli/
  KundliApplication.java   Spring Boot entrypoint (creates the SQLite data dir)
  config/                  KundliProperties (accuracy policy knobs) + beans
  chart/                   BirthChart entity/repo/service + EphemerisClient
  match/                   MatchRecord entity/repo + MatchService (bridges charts → engine)
  api/                     controllers, DTOs, error handling, health
  ashtakoot/
    model/        Rashi, Nakshatra, Graha, Varna, Gana, Nadi, Yoni, VashyaGroup, Relation, MoonChart
    koota/        one scorer per koota (Varna … Nadi) + KootaScorer interface
    tables/       ReferenceTables — loads + validates all JSON at startup
    AshtakootEngine.java   runs all 8 kootas, applies dosha cancellation, builds the verdict
src/main/resources/ashtakoot/   the 7 reference tables (static JSON)
src/test/java/...               per-koota + engine + service + API integration tests
```

## API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/charts` | Create a birth chart: resolves local time + IANA tz → UTC, calls the ephemeris service once, caches the full chart JSON, attaches accuracy warnings |
| `GET/DELETE` | `/api/charts`, `/api/charts/{id}` | List / fetch / delete charts |
| `POST` | `/api/matches` | Match two stored charts through the Ashtakoot engine (no ephemeris call) and persist the result with a rules version |
| `GET` | `/api/matches`, `/api/matches/{id}` | List / fetch stored matches |
| `GET` | `/api/health` | App + ephemeris-precision health in one probe |

Accuracy policies owned here (not in the ephemeris service): the ~1° boundary-warning
threshold, and the pre-1950 historical-timezone warning — both configurable in
`application.properties`.

## Persistence

SQLite file DB (`KUNDLI_DB_PATH`, default `./data/kundli.db`), single-connection pool
(single-writer engine), portable column types only — the Postgres upgrade path is a
dialect swap. Stored matches carry `rulesVersion` (`ashtakoot-v0-provisional` until the
tables are Drik Panchang-verified) so old verdicts stay attributable to the rule set
that produced them.

## Run

```bash
cd backend
mvn spring-boot:run     # expects the ephemeris service on 127.0.0.1:8001
```

## Design points

- **Tables are data, not code** (spec requirement). `ReferenceTables.load()` reads the JSON
  and runs strict completeness validation — a missing Rashi/Nakshatra key or an incomplete
  matrix throws at load, never a silently wrong score at match time.
- **Directional kootas** (Varna, Gana, Graha Maitri, Tara) take `boy` and `girl` explicitly.
- **Doshas carry cancellation** (Nadi, Bhakoot): each is reported as present + cancelled +
  reason, and the verdict folds in only *effective* (present, uncancelled) doshas — because
  raw totals without cancellation give practically-wrong verdicts.

## ⚠️ Accuracy status

Several table values are tradition-variable and are currently **provisional**. See
[`VERIFICATION.md`](./VERIFICATION.md) for exactly what is trustworthy vs. what must be
cross-checked against Drik Panchang before any real verdict is trusted.

## Build & test

```bash
cd backend
mvn test
```
