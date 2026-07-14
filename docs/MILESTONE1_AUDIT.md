# Milestone 1 — Repository Audit

**Branch:** `claude/architecture-discussion-hthqcx`
**Scope:** inspection only, no significant code changes.
**Goal:** make the astronomical, location-resolution, Ashtakoota and Manglik
calculations transparent, deterministic, versioned, testable and defensible
before writing any new feature.

The audit answers each numbered request from the Milestone 1 brief. Where I
found a real defect I have named it. Where I found something *provisional but
already flagged in the repo* (mostly in `backend/VERIFICATION.md`) I say so
and do not re-litigate it.

---

## 1. Current architecture

The stack matches what the brief describes; no rewrite is warranted.

```
                         Nginx (TLS, static + reverse proxy /api → 8080)
                                         │
   Angular 18 SPA (web/)  ───────────────┤
   (embedded into the                    │
    Spring Boot jar's                    ▼
    classpath:/static/)          Spring Boot 3.3 / Java 21
                                 (backend/  port 8080, localhost too)
                                         │
                     ┌───────────────────┴────────────┐
                     ▼                                ▼
         SQLite file DB                   Python FastAPI 0.111
         (birth_charts, matches)          (ephemeris-service/  port 8001)
         Hikari max-pool=1                Bind: 127.0.0.1 only
         Portable columns                 pyswisseph 2.10.3.2
                                          Swiss Ephemeris .se1 files
                                          (not committed; see .gitignore)
```

- Backend is a **single-jar deployment** (`mvn package`) — Angular is built by
  `frontend-maven-plugin` (Node 22.12.0, pinned) and copied into
  `classpath:/static/`. `SpaWebConfig` serves the SPA and falls back to
  `index.html` for deep links; **`/api/**` correctly bypasses the fallback so
  unknown API paths return a real 404** (not 200 HTML — a silent-failure
  category the code specifically prevents).
- **AGPL-3.0** on the whole tree because Swiss Ephemeris is AGPL-or-commercial
  and serving it over a network triggers §13.
- **No Docker** — deliberate; systemd + `127.0.0.1` binding on the ephemeris
  service provides the same internal-only guarantee at this scale.
- **Postgres was in the original spec** (`docs/spec.md`) but the locked-in
  decision is SQLite; portable column types keep Postgres a dialect-swap away.

### File layout (real, current)

```
ephemeris-service/          Python microservice (astronomy only)
  app/main.py               FastAPI app; lifespan self_check
  app/ephemeris.py          Swiss Ephemeris wrapper + Moshier guard
  app/astro.py              Pure Rashi/Nakshatra/Pada math
  app/models.py             Pydantic request/response
  app/config.py             EPHE_PATH, host, port
  tests/test_astro.py       10 unit tests, no ephemeris needed
  tests/regression/         chart_reference_cases.json + test_chart_regression.py
                            (all cases PENDING → SKIPPED today)

backend/                    Spring Boot API + Ashtakoot engine
  pom.xml                   web / data-jpa / validation / sqlite-jdbc / hibernate-community-dialects
  src/main/java/.../kundli/
    KundliApplication.java  Creates ./data/ before Spring starts
    config/                 KundliProperties, AppConfig (RestClient, engine bean), SpaWebConfig
    chart/                  BirthChart entity/repo, BirthChartService, EphemerisClient
    match/                  MatchRecord entity/repo, MatchService
    api/                    ChartController, MatchController, HealthController,
                            ChartDtos, ApiExceptionHandler
    ashtakoot/
      model/                Rashi, Nakshatra, Graha, Varna, Vashya, Gana, Yoni, Nadi, Relation, MoonChart
      koota/                8 scorers + KootaScorer interface
      tables/               ReferenceTables (loads + validates JSON at startup)
      AshtakootEngine.java  Runs 8 kootas + Nadi/Bhakoot cancellation + verdict
      AshtakootResult, KootaScore, DoshaStatus (records)
  src/main/resources/
    application.properties  Ephemeris URL, boundary & historical-TZ thresholds
    ashtakoot/              7 JSON reference tables (rashi_to_varna, rashi_to_vashya_group,
                            nakshatra_to_yoni, rashi_to_lord, nakshatra_to_gana,
                            nakshatra_to_nadi, bhakoot_rules, nadi_cancellation_rules)
  src/test/java/...         6 test classes (see §11)
  src/test/resources/regression/koota_reference_cases.json  (13 cases, all PENDING)

web/                        Angular 18 SPA
  angular.json, package.json (Angular 18.2, TS 5.5, Karma configured but ZERO *.spec.ts)
  src/index.html            Boots <app-root>
  src/app/
    app.component.ts        Shell: brand, nav (Charts / Match), lang picker (hi/en), health dot
    app.routes.ts           / → ChartsPage; /match → MatchPage; ** → /
    app.config.ts           HttpClient, Router, Zone change detection
    core/
      api.service.ts        listCharts / createChart / deleteChart / createMatch / health
      models.ts             DTO interfaces mirroring backend
      i18n.service.ts       Hindi+English UI strings + Rashi/Nakshatra/Koota/Graha lookups
      cities.ts             Bundled place DB (India + diaspora) with search()
    components/
      place-picker.component.ts   Combobox over cities.ts (autocomplete, English+Hindi)
      score-ring.component.ts     Hero SVG ring
      koota-bars.component.ts     Per-koota bar with hover-reveal detail
      dosha-card.component.ts     Absent / cancelled / effective states
    pages/
      charts.page.ts        Chart create + list. Uses place-picker + a hidden manual-entry escape hatch
      match.page.ts         Pick boy + girl chart → run match → verdict + rings + doshas

docs/                       README already at repo root
  spec.md                   Original spec (has Postgres; superseded on that point by root README)
  regression.md             How the two regression halves work

LICENSE                     AGPL-3.0
README.md                   Locked-in decisions table
```

### Duplicate directories / accidentally committed / dead code

- **No duplicate directories.** The old spec still names `mobile/` and
  `infra/`; those directories don't exist yet — not a duplication, just a
  future placeholder in prose.
- **No generated files committed** (`target/`, `dist/`, `.angular/`,
  `node_modules/`, `.se1`, `*.db`, `.venv/` are all ignored).
- **No dead code** worth flagging. `Instant` import in
  `BirthChartServiceTest` is unused but harmless.

---

## 2. End-to-end request flow

### 2a. Chart creation (`POST /api/charts`)

```
Angular ChartsPage
  ├─ user picks City from PlacePicker (fills lat/lon/tz/placeName)
  └─ POST /api/charts {label, birthLocalDateTime, timezone, latitude, longitude, placeName}
        │
        ▼
Nginx  →  Spring Boot ChartController.create()
        │
        ▼
BirthChartService.create(cmd)
  ├─ LocalDateTime.parse(birthLocalDateTime)   ← ISO local, e.g. 1998-10-25T16:20:00
  ├─ ZoneId.of(timezone)                        ← IANA, e.g. Asia/Kolkata
  ├─ Instant utc = local.atZone(zone).toInstant()
  ├─ if local.year < historicalTzWarningBeforeYear(1950): add warning
  ├─ ephemeris.computeChart(utc, lat, lon)     ← §2c
  ├─ if moonDegreesToNakshatraBoundary < 1.0°: add warning
  ├─ if moonDegreesToRashiBoundary     < 1.0°: add warning
  └─ repository.save(new BirthChart(...))       ← SQLite INSERT
        │
        ▼
ChartResponse.from(entity) → 201 Created
```

### 2b. Match creation (`POST /api/matches`)

```
Angular MatchPage
  └─ POST /api/matches {boyChartId, girlChartId}
        │
        ▼
MatchController.create → MatchService.match(boyId, girlId)
  ├─ charts.get(boyId), charts.get(girlId)          ← SQLite SELECTs
  ├─ engine.match(MoonChart(boy), MoonChart(girl))  ← §4  (pure Java, ZERO ephemeris calls)
  ├─ mapper.writeValueAsString(result)              ← full AshtakootResult persisted
  └─ repository.save(new MatchRecord(...,
        rulesVersion="ashtakoot-v0-provisional"))
        │
        ▼
MatchResponse → 201 Created
```

### 2c. `EphemerisClient.computeChart(utc, lat, lon)` (Java → Python)

`AppConfig.ephemerisRestClient` builds a `RestClient` with a
`SimpleClientHttpRequestFactory` (JDK `HttpURLConnection`, connect=5s,
read=20s). The body is **pre-serialised as a `String`** so the wire uses
plain HTTP/1.1 with `Content-Length` — this is a real deliberate fix
(commit `666e0a8`): the default JDK `HttpClient` attempted an h2c upgrade
that uvicorn drops, producing a spurious 422 "body missing". Not a bug,
worth remembering.

```
Java payload: {"utc":"1998-10-25T10:50:00Z","latitude":23.3441,
               "longitude":85.3096,"ayanamsa":"LAHIRI"}

Java precision check on response:
    if response.precision != "SWISS_EPHEMERIS_FULL":
        throw EphemerisUnavailableException(...)   ← defense in depth
```

The service response body is stored **verbatim** in `birth_charts.chart_json`
(TEXT column, `@Lob`), so later Manglik/Lagna features can read stored charts
without recompute.

---

## 3. Birth-chart calculation flow (Python)

```
FastAPI POST /chart
  │
  ▼
julian_day_ut(year, month, day, ut_hours)   → swe.julday(y, m, d, ut, GREG_CAL)
                                              (ut = hour + minute/60 + second/3600)
  │
  ▼
compute_grahas(jd):
  for name, body in GRAHAS.items():           ← Sun, Moon, Mars, Mercury, Jupiter,
      _ensure_thread_configured()                Venus, Saturn, Rahu(=MEAN_NODE)
      values, ret = swe.calc_ut(jd, body,
          FLG_SWIEPH | FLG_SIDEREAL | FLG_SPEED)
      if ret < 0:              raise PrecisionError
      if ret & FLG_MOSEPH:     raise PrecisionError   ← Moshier-fallback guard
      → GrahaPosition(name, longitude=values[0]%360, speed=values[3])
  # Ketu = (Rahu + 180) % 360, speed 0.0
compute_ascendant(jd, lat, lon):
  swe.houses_ex(jd, lat, lon, b"P", FLG_SIDEREAL) → ascmc[0] % 360

astro.py: Rashi/Nakshatra/Pada purely from sidereal longitude.
  RASHI_SPAN     = 30.0
  NAKSHATRA_SPAN = 360 / 27 ≈ 13.3333
  PADA_SPAN      = NAKSHATRA_SPAN / 4 ≈ 3.3333
  degrees_to_boundary = min(degrees_in, span - degrees_in)   ← Java uses this for warnings
```

### Correct and reliable in the Python service

- **Full-precision guard** works: `FLG_SWIEPH` requested; if returned flags
  carry `FLG_MOSEPH`, the request 503s and startup refuses.
- **Ayanamsa Lahiri** set explicitly per-thread (`swe.set_sid_mode(SIDM_LAHIRI, 0, 0)`)
  and required on every request (Pydantic enum) — never a hidden default.
- **Thread-local re-configuration** (`_ensure_thread_configured`, commit
  `e69c779`) is the fix for a real hazard: pyswisseph state is thread-local,
  and a sync FastAPI endpoint scheduled on the threadpool would silently
  fall back to Moshier with the default (non-Lahiri) ayanamsa on
  unconfigured threads (~0.9° error — enough to flip a Nakshatra). Both
  endpoints are `async def` for extra safety, and every calc still
  re-asserts the config.
- Pure `astro.py` math: `_placement()` uses integer division on
  `lon % 360`, guards the exact `0/360` edge, and reports
  `degrees_to_boundary` as `min(into, span-into)`. The unit tests already
  exercise Ashwini↔Bharani boundary at ±0.001°.

### Provisional / open in the Python service

- **Rahu = `swe.MEAN_NODE`.** This is a rule-convention choice (mean vs.
  true node) that is NOT declared anywhere. Rahu is not consumed by any
  koota today (Graha Maitri excludes Rahu/Ketu), but it will matter once
  Manglik/Lagna doshas arrive. **Add this to the declared rule set.**
- **Ascendant house system: Placidus (`b"P"`)** — for the Ascendant
  *point* it doesn't matter (Ascendant = ecliptic ∩ eastern horizon,
  independent of house system). For **house cusps** it does, and Manglik
  needs *houses* (mars-in-1/2/4/7/8/12 from Lagna). We'll need to state
  a house-system convention when v1 Manglik is turned on. Whole-sign
  houses (Bhava Chalit's simple form) is the mainstream Vedic default;
  this is currently undeclared.
- **UTC parsing edge:** `req.utc` is a Pydantic `datetime`. A naive
  `datetime` (no tzinfo) will be treated as UTC by the current code
  (`req.utc.hour + minute/60 + second/3600` ignores tz). Java always
  sends `Instant.toString()` (ISO with `Z`), so the field arrives
  tz-aware in practice — but the service does not defensively reject a
  naive datetime. Low risk, worth documenting.

---

## 4. Ashtakoota calculation flow (Java)

Zero ephemeris calls at match time. Pure functions over two `MoonChart`s.

```
AshtakootEngine.match(boyChart, girlChart)
  for scorer in [Varna, Vashya, Tara, Yoni, GrahaMaitri, Gana, Bhakoot, Nadi]:
      KootaScore = scorer.score(boy, girl, tables)   ← returns points, max, detail
      total += points
  nadiDosha    = engine.nadiDosha(boy, girl)         ← present? cancelled? reason
  bhakootDosha = engine.bhakootDosha(boy, girl)
  verdict      = engine.verdict(total, [nadi, bhakoot])
  → AshtakootResult(kootas, totalPoints, 36, [nadi, bhakoot], verdict)
```

Every koota (per file):

| # | Koota | Max | Inputs (from MoonChart) | How it scores |
|---|-------|-----|-------------------------|---------------|
| 1 | Varna | 1 | Boy Rashi → Varna, Girl Rashi → Varna | `boy.rank >= girl.rank ? 1 : 0` |
| 2 | Vashya | 2 | Both Rashis → VashyaGroup | `vashyaMatrix[boyGroup][girlGroup]` (5×5) |
| 3 | Tara | 3 | Both Nakshatra ordinals | Each direction: `count % 9`; remainder∈{3,5,7} = 0, else 1.5. Sum. |
| 4 | Yoni | 4 | Both Nakshatras → Yoni animal | Same → 4; enemy-pair → 0; else `defaultScore` |
| 5 | Graha Maitri | 5 | Both Rashis → Lord (Graha) | Same lord → 5; else band lookup on relation(boy→girl, girl→boy) |
| 6 | Gana | 6 | Both Nakshatras → Gana | `ganaMatrix[boy][girl]` (3×3) |
| 7 | Bhakoot | 7 | `boy.rashi.countTo(girl.rashi)` and reverse | `Set{count_ab, count_ba}` ∈ {{2,12},{5,9},{6,8}} → 0 else 7 |
| 8 | Nadi | 8 | Both Nakshatras → Nadi | Same Nadi → 0 (dosha); else 8 |

**Cancellation** (Nadi + Bhakoot) is applied by the engine, not the koota:

- Nadi cancels if `sameRashi ∧ ¬sameNakshatra` (flag) OR
  `sameNakshatra ∧ ¬samePada` (flag).
- Bhakoot cancels if lords equal (flag) OR lords are mutual friends (flag).
- **A cancelled dosha still scores 0 in its koota.** The convention here is
  documented but not verified against Drik Panchang — the raw-points side
  does not restore the 7 or 8 on cancellation. Flagged item #8 in
  VERIFICATION.md.

### Complete / reliable in the engine

- **Rashi lords, natural friendship (Naisargika Maitri), Varna
  elemental mapping, Gana classification, Nadi classification,
  Nakshatra→Yoni animal, Bhakoot dosha pairs (2/12, 5/9, 6/8),
  Tara mod-9 algorithm.** All checked; the unit tests pin the algorithms.
- **Reference-table loader.** `ReferenceTables.load()` reads each JSON,
  builds EnumMaps, and runs `validate()`: every Rashi has Varna / Vashya
  group / lord; every Nakshatra has Yoni / Gana / Nadi; every 5×5, 3×3,
  7×7 matrix cell exists. Any hole throws at boot, not at match time.
- **Directionality** (`boy`, `girl`) is passed explicitly to every koota,
  and the Vashya / Gana matrix reads `row=boy col=girl`, so a directional
  variant is a table edit, not a code change.

### Provisional / risky in the engine — the honest list

Everything below is already flagged in `backend/VERIFICATION.md`; I have
re-verified it against the JSON.

| # | Item | File | Concrete finding |
|---|------|------|------------------|
| P1 | **Vashya 5×5 matrix is a placeholder** | `rashi_to_vashya_group.json` | Every diagonal cell is 2, every off-diagonal is 1. The real table has 0 / 0.5 / 1 / 2 cells. Score is not currently trustworthy. |
| P2 | **Vashya group uses whole-sign only** | same | Dhanu/Makara classically split by half-sign; here they're single groups. |
| P3 | **Yoni gradations are placeholder 2** | `nakshatra_to_yoni.json` | Only `sameScore=4` and 7 sworn-enemy pairs (=0) are real. Everything else falls into `defaultScore=2` — collapses friend/neutral/enemy(non-sworn) into one bucket. |
| P4 | **Graha Maitri bands are guessed** | `rashi_to_lord.json` | `FRIEND_FRIEND=5, FRIEND_NEUTRAL=4, NEUTRAL_NEUTRAL=3, FRIEND_ENEMY=1, NEUTRAL_ENEMY=0.5, ENEMY_ENEMY=0`. Numbers differ by source (some traditions use 1/0.5/0.25 etc.). Unverified. |
| P5 | **Gana matrix is provisional** | `nakshatra_to_gana.json` | `DEVA↔MANUSHYA=5, DEVA↔RAKSHASA=1, MANUSHYA↔RAKSHASA=0`. Symmetric today; a directional variant exists. |
| P6 | **Nadi cancellation convention** | `nadi_cancellation_rules.json` | Flags: cancelIfSameRashiDifferentNakshatra=true, cancelIfSameNakshatraDifferentPada=true. First flag is almost vacuous (consecutive Nakshatras never share a Nadi within a Rashi); the pada rule is the one that fires. Unverified. |
| P7 | **Bhakoot cancellation convention** | `bhakoot_rules.json` | Flags: cancelIfSameLord=true, cancelIfLordsAreMutualFriends=true. Common convention; not confirmed against Drik Panchang. |
| P8 | **Cancellation → koota points restoration is NOT done** | `AshtakootEngine.java` | A cancelled Nadi/Bhakoot dosha still scores 0. Whether the 7/8 should be restored on cancellation is convention-variable and undecided. |
| P9 | **Rahu = mean node** | `ephemeris-service/app/ephemeris.py` | Mean vs. true Rahu is undeclared. Not consumed today; will matter for Manglik cancellations that involve nodes. |
| P10 | **House system for Lagna houses = Placidus** | `ephemeris-service/app/ephemeris.py` | Fine for the Lagna point itself; will matter when Manglik needs "Mars in house X". Vedic default is whole-sign; undeclared. |

---

## 5. Location-resolution flow (current)

The system today does **not** call any external geocoding API. Location comes
entirely from a bundled JS database.

```
Angular PlacePicker
  ├─ user types ≥ 2 chars
  ├─ searchCities(q, 8)  ← prefix-first on name/hi + substring on aliases/state
  ├─ user picks City
  ↓
Charts page onCity(c):
  form.latitude  = c.lat
  form.longitude = c.lon
  form.timezone  = c.tz         ← IANA
  form.placeName = "Ranchi, Jharkhand"

Manual escape hatch (button "Enter location manually"):
  raw latitude/longitude number inputs (step 0.0001, min/max clamped)
  raw IANA timezone <select> populated from Intl.supportedValuesOf('timeZone')
```

### Coverage

- `web/src/app/core/cities.ts`: **293 Indian cities** (every state/UT
  capital, all million-plus cities, most Hindi-belt district towns), plus
  **41 diaspora cities** (UAE, Saudi, Nepal, Bangladesh, Sri Lanka, Pakistan,
  Malaysia, Singapore, HK, Japan, Australia, NZ, UK, EU, Canada, US, Kenya,
  South Africa, Mauritius). Every entry has a Hindi name.
- **Ranchi is present**: `['Ranchi', 'राँची', 'Jharkhand', 23.3441, 85.3096]`,
  Asia/Kolkata. Matches the user's canonical profile exactly.
- Search is prefix-first over `name` and `hi`, then substring over
  `aliases + state`.

### Gaps against the brief

- The manual entry escape hatch is on the **normal chart form**, not a
  separate "administrator" screen. Mother Mode needs it gone from the
  primary form.
- No "did you mean" flow when the query matches nothing — the picker just
  shows "no results". That's acceptable, but Mother-Mode language wants
  `जन्म स्थान नहीं मिला। कृपया शहर के साथ राज्य भी लिखें।`
- **No canonical `Place` object** is stored on the chart entity — only
  the individual `latitude`, `longitude`, `timezone`, `placeName` fields.
  A future normalized `Place` record would let the app treat re-used
  places (e.g. Ranchi) as the same key. Not blocking v1.
- The bundled coordinates need one round of verification against a
  geocoding source before we call them regression data — this is the
  brief's "verified through the selected geocoding source" step.

---

## 6. Provisional and risky rules (single consolidated list)

From §3 and §4 above:

- P1 Vashya 5×5 matrix (placeholder)
- P2 Vashya group assignment (whole-sign shortcut)
- P3 Yoni 14×14 gradations (placeholder 2 for all non-same, non-sworn-enemy)
- P4 Graha Maitri bands (unverified numbers)
- P5 Gana matrix (symmetric; directional variant undecided)
- P6 Nadi cancellation convention (2 flags, unverified)
- P7 Bhakoot cancellation convention (2 flags, unverified)
- P8 Cancellation vs. koota points restoration (not done)
- P9 Rahu = mean node (undeclared choice)
- P10 House system = Placidus for house math (undeclared; only Lagna point safe)
- **Manglik is not implemented at all** (deferred in the current code). This
  is the biggest gap against the user's brief.

---

## 7. Incomplete lookup tables (exact rows)

1. `backend/src/main/resources/ashtakoot/rashi_to_vashya_group.json` — the
   entire `matrix` block is a placeholder. Needs `0 / 0.5 / 1 / 2` cells
   from a documented source.
2. `backend/src/main/resources/ashtakoot/nakshatra_to_yoni.json` — the
   14×14 matrix is expressed as `sameScore + enemyPairs + defaultScore=2`.
   Needs full friend / neutral / enemy(non-sworn) gradations.
3. `backend/src/main/resources/ashtakoot/rashi_to_lord.json` — the
   `bands` block (`FRIEND_FRIEND` etc.) is provisional; needs sourced
   values.
4. `backend/src/main/resources/ashtakoot/nakshatra_to_gana.json` — the
   3×3 `matrix` block; decide symmetric vs. directional and cite.
5. `backend/src/main/resources/ashtakoot/nadi_cancellation_rules.json` —
   confirm both flags against Drik Panchang (or the chosen convention).
6. `backend/src/main/resources/ashtakoot/bhakoot_rules.json` — confirm
   both cancellation flags; consider whether same-Rashi (1/1) or
   opposite-Rashi (7/7) merit extra Rashi-specific exceptions.
7. **Missing table:** `manglik_rules.json` — houses that trigger Manglik
   (v1: 1, 2, 4, 7, 8, 12), reference points (Lagna / Moon / Venus), and
   the rule-version string.

---

## 8. Missing and skipped tests

### Skipped (present but pending Drik Panchang values)

- `backend/src/test/resources/regression/koota_reference_cases.json` — all
  **13 cases** have `status: PENDING_DRIK_PANCHANG` and `expected: null`.
  `KootaRegressionTest.referenceCases` uses `Assumptions.assumeTrue(FILLED)`,
  so all 13 are skipped today. The `fixtureIsWellFormed` test *does* run
  and guards well-formedness.
- `ephemeris-service/tests/regression/chart_reference_cases.json` — all
  **3 cases** are pending. `test_chart_matches_reference` uses
  `pytest.skip()`, so all 3 are skipped today. `test_fixture_is_well_formed`
  runs.

### Missing entirely (no test file exists)

- **Ashutosh Kumar canonical profile test.** No test asserting that
  `1998-10-25T16:20:00` in `Asia/Kolkata` (Ranchi, 23.3441/85.3096)
  resolves to `1998-10-25T10:50:00Z` and to a specific Moon
  Rashi/Nakshatra/Pada, and is NEVER interpreted as 04:20 AM.
- **UTC-date-change boundary.** No test for a birth after 18:30 IST that
  crosses to next-day UTC (e.g. `1998-10-25T20:00:00 Asia/Kolkata` →
  `1998-10-25T14:30:00Z`, still same UTC date; and a real crosser like
  `1998-10-25T23:59 IST` → `1998-10-25T18:29Z`; and `2000-01-01T02:00 IST`
  → `1999-12-31T20:30Z`).
- **Ascendant boundary sensitivity.** No test that a ±1 minute time shift
  near an Ascendant sign transition flips the Rashi; no test that the
  Ascendant lands where expected for a known birth.
- **Mars / Venus placement.** Nothing asserts anything about Mars or
  Venus — the two grahas Manglik needs.
- **Nakshatra Pada boundary on the Java side.** Python side has boundary
  tests; the Java `BirthChartService.create` boundary-warning threshold is
  unit-tested against mocked distances but not against a real Moon
  placement.
- **Every individual koota against a verified expected value.** Unit tests
  today assert *logic*: "same Nadi → 0", "same Lord → 5". They do not
  assert "boy Ashwini-1 + girl Rohini-2 → Varna 1, Vashya 1, Tara 3,
  Yoni 2, Graha Maitri 4, Gana 6, Bhakoot 7, Nadi 8, total 32." That
  requires a filled regression fixture — currently all pending.
- **All Manglik tests.** Feature not implemented.
- **Any Angular UI test.** Karma/Jasmine are wired in `package.json` and
  `angular.json`, but there are **zero `*.spec.ts` files**. UI has no
  test coverage.

### Coverage requested by the brief, tracked against the current suite

The brief asks for ≥100 astronomical + ≥50 matching cases, each verified.
Today: **3 chart cases + 13 koota cases, all pending.** Both fixture
formats exist and their harnesses skip cleanly until values are filled
(which is the honest behavior — I would keep this exact pattern).

---

## 9. Potential calculation errors, API risks, DB risks, F/E mismatches

### Real defects (I would fix these under Milestone 2)

**D1. Rashi/Nakshatra names displayed in ALL-CAPS in every UI string.**
  - **Where:** `backend/.../api/ChartDtos.java` lines 52–53 return
    `Rashi.ofNumber(...).name()` — i.e. the Java enum's uppercase name
    (`MESHA`, `ROHINI`, `MEENA`). Same for Nakshatra (`ASHWINI`,
    `UTTARA_ASHADHA`).
  - **Effect:** The frontend's Hindi lookup tables in
    `web/src/app/core/i18n.service.ts` are keyed as **title case** — e.g.
    `RASHI_HI['Mesha']`, `NAKSHATRA_HI['Ashwini']`, and
    `NAKSHATRA_HI['Purva Ashadha']` (with a space). The uppercase
    enum-form values coming from the backend never match, so:
    - English users see `MESHA`, `PURVA_ASHADHA` (visually wrong).
    - Hindi users **also** see `MESHA`, `PURVA_ASHADHA` (fallback
      returns raw input; the Hindi vocabulary never resolves).
  - **Scope:** every chart card, every match selector, every score
    label.
  - **Fix:** either return the display form from the backend (title case
    "Mesha", "Purva Ashadha") or fold-case in the frontend before
    lookup. Backend is the natural source of the display form — it
    already has `Rashi.english()`; we'd add a `sanskritTitle()`.

**D2. Historical timezone accuracy for pre-1950 births is only a warning,
never a correction.**
  - `BirthChartService.create` adds a text warning when `year < 1950`
    but still calls `local.atZone(zone).toInstant()`. Java uses the
    IANA rules for whatever `zone` resolves to. For Asia/Kolkata IANA
    *does* carry historical LMT+HMT data, so a Kolkata birth in 1930
    is fine. A **Ranchi 1930** birth is NOT — Ranchi's LMT (~85.3°E ≈
    +5:41) is not IANA. Not an urgent v1 issue; my mother's target is
    modern births.

### Sensitivities that are handled but worth naming

- **Precision defense in depth.** The Python service refuses Moshier
  fallback (`FLG_MOSEPH` in returned flags → 503). The Java client
  **also** refuses non-`SWISS_EPHEMERIS_FULL` responses. Both layers
  guard the accuracy invariant.
- **h2c upgrade breakage** on the JDK HttpClient was already fixed in
  `666e0a8` by pre-serialising the body and using
  `SimpleClientHttpRequestFactory`. Do not revert.
- **Thread-local Swiss Ephemeris state** was already fixed in
  `e69c779`. Do not revert.

### API risks

- No auth on any endpoint. Fine for local-only single-instance v1;
  needs a family PIN before this ever runs on the public domain.
- No rate limiting; endpoints are cheap and localhost-bound at match
  time. Charts hit the ephemeris service. Not a v1 blocker.
- `HealthController.health()` calls the ephemeris `/health` on every
  hit; fine at this scale, worth caching if we ever scale.
- No CSRF token on the SPA — same-origin cookieless request; no session
  yet, so nothing to CSRF against. Reconsider when auth lands.

### DB risks

- `spring.jpa.hibernate.ddl-auto=update`. Any column rename is a manual
  migration. Adding columns (Manglik status, Ascendant, Mars/Venus
  storage) is safe; renames/deletes will require a migration tool. This
  is a **known**, not a bug.
- Hikari `maximum-pool-size=1` — deliberate, SQLite is a single-writer
  engine. Do not raise.
- No indexes; personal scale, `matches.boyChartId` / `girlChartId`
  scans are irrelevant at N < a few hundred.

### Frontend/backend mismatches

- **D1 already listed above.** The single biggest one.
- Frontend re-implements the verdict-band logic (18 / 25 / 33) inside
  `i18n.service.ts` so it can render in Hindi. Backend's
  `AshtakootEngine.verdict()` builds an English string. Two sources of
  truth — bearable today (both use the same thresholds), but any change
  to the thresholds must be made in both places or the recommendation
  categories will diverge. Milestone 5 formalises this by moving
  thresholds behind `KundliProperties` and returning **category codes**
  from the backend (e.g. `GOOD` / `ACCEPTABLE` / `WEAK`) that the
  frontend translates.
- Frontend interface `DoshaStatus` has `effective: boolean`. Java's
  `DoshaStatus` record only exposes `isEffective()` as a method
  (not a record component). Jackson **does** serialise the `isX()`
  accessor as `effective` in JSON, so the wire form matches. Working
  correctly, worth remembering when refactoring the record.
- `ChartResponse.warnings: List<String>` is derived by splitting the
  entity's stored newline-joined `warnings` string. If a warning ever
  contains a `\n` this loses fidelity. Low risk; storing warnings as a
  JSON array in the entity would be strictly better and is trivial —
  fold into Milestone 2.

---

## 10. Exact files that need modification (per milestone)

Grouped by milestone. Every path is repo-relative.

### Milestone 2 — Astronomical foundation & versioning (small, safe first)

- `backend/src/main/java/codes/ashutoshkumar/kundli/api/ChartDtos.java`
  → return title-case Rashi/Nakshatra names ("Mesha", "Purva Ashadha")
    to fix **D1**. Cheapest, highest UX ROI.
- `backend/src/main/java/codes/ashutoshkumar/kundli/ashtakoot/model/Rashi.java`
  → add a `sanskritTitle()` returning "Mesha" / "Vrishabha" / … (source
    of truth for D1).
- `backend/src/main/java/codes/ashutoshkumar/kundli/ashtakoot/model/Nakshatra.java`
  → add a `sanskritTitle()` returning "Ashwini" / "Purva Ashadha" / …
- `backend/src/main/java/codes/ashutoshkumar/kundli/chart/BirthChart.java`
  → add columns for Ascendant longitude + Mars/Venus longitude (or
    reuse `chartJson` — I prefer explicit columns for Manglik queries).
- `backend/src/main/java/codes/ashutoshkumar/kundli/chart/EphemerisClient.java`
  → parse `ascendant.longitude`, `grahas[Mars].longitude`,
    `grahas[Venus].longitude` from the already-received response.
- `backend/src/main/java/codes/ashutoshkumar/kundli/config/KundliProperties.java`
  → add `ashtakootaRuleVersion`, `manglikRuleVersion`, `ayanamsa`,
    `matchingSystem`, `ephemerisMode` metadata.
- `backend/src/main/java/codes/ashutoshkumar/kundli/api/MatchController.java`
  → expose the rule-version metadata block on `MatchResponse`.
- `ephemeris-service/app/ephemeris.py`
  → declare `NODE_MODE = "MEAN"` in a comment and (optionally) put it
    in the response's `precision` block so it's audit-visible.
- `backend/src/test/java/codes/ashutoshkumar/kundli/chart/BirthChartServiceTest.java`
  → add the canonical **Ashutosh 1998-10-25 16:20 Asia/Kolkata → UTC
    1998-10-25T10:50:00Z** test. Also add a "never AM" negative case.
- `ephemeris-service/tests/regression/chart_reference_cases.json`
  → seed the Ashutosh case (still pending Drik Panchang for Moon
    values, no fabricated expected).

### Milestone 3 — Ashtakoota v1.0

- `backend/src/main/resources/ashtakoot/rashi_to_vashya_group.json`
  → real 5×5 matrix + source citation.
- `backend/src/main/resources/ashtakoot/nakshatra_to_yoni.json`
  → full 14×14 gradations + source citation.
- `backend/src/main/resources/ashtakoot/rashi_to_lord.json`
  → real Graha Maitri bands + source citation.
- `backend/src/main/resources/ashtakoot/nakshatra_to_gana.json`
  → decide symmetric vs. directional + source citation.
- `backend/src/main/resources/ashtakoot/nadi_cancellation_rules.json`
  → confirm both flags against Drik Panchang; source note.
- `backend/src/main/resources/ashtakoot/bhakoot_rules.json`
  → confirm both flags against Drik Panchang; source note.
- `backend/src/main/java/codes/ashutoshkumar/kundli/match/MatchService.java`
  → bump `RULES_VERSION` from `ashtakoot-v0-provisional` to
    `ashtakoota-v1.0` **only after** the koota_reference_cases are
    filled and green.
- `backend/src/main/java/codes/ashutoshkumar/kundli/ashtakoot/AshtakootResult.java`
  → return structured per-koota block matching the brief's shape
    (code, nameEnglish, nameHindi, personAValue, personBValue,
    score, maxScore, doshaPresent, ruleApplied, explanationEnglish,
    explanationHindi). Keep translations OUT of the engine — the
    engine returns codes; frontend or a thin translation layer maps
    to Hindi/English strings.
- `backend/src/test/resources/regression/koota_reference_cases.json`
  → fill the 13 seeded cases from Drik Panchang; add more to reach
    the ≥50-matches release-gate target.

### Milestone 4 — Manglik v1.0

- `backend/src/main/java/codes/ashutoshkumar/kundli/manglik/ManglikEngine.java`
  (**new**) — per-person check across Lagna, Moon, Venus for houses
  1/2/4/7/8/12; returns per-reference-point structured result.
- `backend/src/main/java/codes/ashutoshkumar/kundli/manglik/ManglikStatus.java`
  (**new**) — record with `fromLagna`, `fromMoon`, `fromVenus`,
  `triggeredReferences`, `cancellations` (empty in v1),
  `ruleVersion`.
- `backend/src/main/java/codes/ashutoshkumar/kundli/manglik/ManglikCompatibility.java`
  (**new**) — couple result: `personAStatus`, `personBStatus`,
  `compatibility` ∈ {NEUTRAL, REQUIRES_DETAILED_REVIEW}, no invented
  cancellations.
- `backend/src/main/java/codes/ashutoshkumar/kundli/chart/BirthChart.java`
  → **already listed under M2** — Ascendant + Mars + Venus columns.
- `backend/src/main/java/codes/ashutoshkumar/kundli/api/MatchController.java`
  → surface `manglik` alongside `ashtakoot` result.
- `backend/src/main/resources/manglik/manglik_rules.json` (**new**) —
  houses list, reference points, ruleVersion, source citation.
- `backend/src/test/java/codes/ashutoshkumar/kundli/manglik/ManglikEngineTest.java`
  (**new**) — parametrised over Mars-in-house-N (N=1..12) × ref
  point (Lagna, Moon, Venus). ≥ 36 cases just from that grid.
- `web/src/app/components/manglik-card.component.ts` (**new**).
- `web/src/app/core/i18n.service.ts` → Hindi strings for Manglik
  statuses.

### Milestone 5 — Mother Mode

- `backend/src/main/java/codes/ashutoshkumar/kundli/chart/BirthChart.java`
  → add `isPrimary boolean` (Ashutosh) OR introduce a `Profile`
  entity that owns the primary chart id — I recommend a `Profile`
  entity to keep future users clean.
- `backend/src/main/java/codes/ashutoshkumar/kundli/profile/` (**new**)
  → `Profile` entity, repository, service (get/set primary chart).
- `backend/src/main/java/codes/ashutoshkumar/kundli/api/ProfileController.java`
  (**new**).
- `web/src/app/pages/mother-home.page.ts` (**new**) — home screen
  from the brief.
- `web/src/app/pages/new-match.page.ts` (**new**) — form asking only
  the girl's name/date/time/place, showing Ashutosh preselected.
- `web/src/app/pages/confirm.page.ts` (**new**) — the
  हाँ, मिलान करें / जानकारी बदलें screen.
- `web/src/app/pages/history.page.ts` (**new**) — पिछले मिलान देखें.
- `web/src/app/app.routes.ts` → wire the above.
- `web/src/app/pages/charts.page.ts` → keep for "advanced" mode
  (behind a settings toggle), not the default home.
- `backend/src/main/resources/application.properties` → move
  recommendation thresholds (18 / 25 / 33) into config.
- `backend/src/main/java/codes/ashutoshkumar/kundli/config/KundliProperties.java`
  → add `recommendationThresholds` record.
- `backend/src/main/java/codes/ashutoshkumar/kundli/ashtakoot/AshtakootEngine.java`
  → verdict returns a category code instead of a formatted string;
    thresholds injected.

### Milestone 6 — PDF & sharing

- `backend/pom.xml` → add a PDF dependency (OpenPDF or Apache PDFBox).
  I lean OpenPDF for simple templated Hindi/English layouts.
- `backend/src/main/java/codes/ashutoshkumar/kundli/report/` (**new**)
  → report service that renders `AshtakootResult` + `ManglikStatus`
    to a 3–5 page PDF.
- `backend/src/main/java/codes/ashutoshkumar/kundli/api/ReportController.java`
  (**new**) → `GET /api/matches/{id}/report.pdf`, non-guessable id.
- Web PDF share button (native share sheet on Android via Capacitor
  Share API — piggybacks on Milestone 7).

### Milestone 7 — Android

- `web/src/manifest.webmanifest` (**new**) — PWA manifest.
- `web/src/app/service-worker` (**new**) — Angular service worker
  via `ng add @angular/pwa`.
- `mobile/` (**new**) — Capacitor project consuming the same
  Angular build. `capacitor.config.ts`, `android/` gradle project.
- Signing config (`android/keystore.properties.example`) — never
  commit the actual keystore.

### Milestone 8 — Accuracy validation

- Fill both regression fixtures to the ≥100 chart + ≥50 match
  targets. This is data work, not code.
- `docs/ASTROLOGY_VALIDATION.md` (**new**) — the mismatch log the
  brief asks for.

---

## 11. Recommended implementation order (why this order)

The order below is the same as the brief's Milestones 2 → 8, with two
small precedence rules I add:

1. **Fix D1 first**, before any Mother Mode work. Without it the Hindi
   home screen would still display `MESHA` / `UTTARA_ASHADHA`. Half a
   day of work; unlocks everything downstream.
2. **Store Ascendant + Mars + Venus longitudes in the birth_chart entity
   before touching Manglik.** Without it we'd need to re-parse the
   `chart_json` blob every time. Half a day of migration + 2 lines in
   `EphemerisClient` and the DTO.

Both of the above are inside Milestone 2. Then:

- M2 (astronomical foundation + versioning + D1 fix + entity extension +
  the canonical Ashutosh test)
- M3 (fill the four provisional tables, cite each cell, drive
  koota_reference_cases to green)
- M4 (Manglik on top of the extended entity + tests + UI card)
- M5 (Mother Mode UI, primary-profile persistence, config-driven
  thresholds)
- M6 (PDF using the now-stable structured result)
- M7 (Capacitor / signed APK / device test)
- M8 (fill fixtures to target coverage; pandit review; run with mother)

### Effort (working days, one-person)

Estimates are honest ranges. I've assumed we're aiming for correctness
and small reviewable diffs, not speed.

| Milestone | Effort | Notes |
|-----------|--------|-------|
| M2 | **3–4 d** | D1 fix (0.5), Ascendant/Mars/Venus storage (1), rule-version metadata (0.5), Ashutosh canonical + UTC-boundary tests (1), one seeded chart-fixture case (0.5). |
| M3 | **5–7 d** | The four provisional tables. Bulk of the time is *sourcing* (finding a defensible authoritative table for each) + filling koota_reference_cases + verifying each cell drove a green case. Not coding. |
| M4 | **3–4 d** | ManglikEngine + 36-case parametrised test + wire into MatchService + Angular card + Hindi strings. |
| M5 | **4–5 d** | Profile entity, mother-home / new-match / confirm / history pages, config-driven thresholds, wiring the girl-only form. |
| M6 | **3–4 d** | PDF templating (bilingual is the fiddly part), sharing wiring. |
| M7 | **2–3 d** | PWA manifest + service worker + Capacitor + Android project + signed APK + device smoke test. |
| M8 | **ongoing** | 100 charts + 50 matches is real data-entry work; realistically 2 weeks of steady evening work by an astrologer or by you sitting with Drik Panchang. |

Total to Version 1 (M2–M7, ex-validation): **~20–27 working days.**
Validation (M8) is what actually gates calling the app "accurate" and
runs alongside.

---

## 12. Questions that genuinely block accuracy

I'd need answers before Milestones 3–4 land. None of these block M2.

1. **Which authoritative source do we cite for the Vashya 5×5 matrix?**
   B. V. Raman's tables? Drik Panchang's published rules? "Muhurta" by
   Iyer? — the app must cite exactly one and use its numbers verbatim.
2. **Same question for Yoni 14×14 gradations and Graha Maitri bands.**
   Different sources give visibly different half-point patterns.
3. **Gana matrix: symmetric or directional?** If directional, whose
   convention (a Deva boy + Rakshasa girl scores differently from the
   reverse)?
4. **Nadi cancellation:** the code encodes "same Nakshatra different
   Pada" and "same Rashi different Nakshatra". Do we accept both,
   pick one, or add "different Nakshatra Lord"?
5. **Bhakoot cancellation:** should we add same-Nakshatra-Lord? Nabhasa
   Yoga cancellations? Or stop at same-lord / mutual-friend?
6. **Should a cancelled Nadi/Bhakoot dosha *restore* its koota points?**
   (Current behaviour: no. Some traditions: yes.)
7. **Rahu:** mean node or true node? Only matters when nodal Manglik
   cancellations arrive; declare now, not later.
8. **House system for Manglik:** whole-sign (Bhava Chalit simple form)
   or Placidus? Vedic default is whole-sign; the code today uses
   Placidus for `houses_ex` because only the Ascendant point was
   consumed. I recommend whole-sign for Manglik.
9. **Manglik "partial" thresholds:** are we OK with a strict Version 1
   that returns only `MANGLIK` / `NOT_MANGLIK` (per reference point),
   and defers `PARTIAL_MANGLIK` to v1.1 with named strength rules?
10. **Recommendation thresholds:** 18 / 25 / 33 is the current
    engine's convention. Do we adopt the brief's 18 / 24 / 25+ split
    (Below 18 / 18–24 / Above 24) for Mother Mode? I recommend the
    brief's thresholds because they map directly to the Hindi UI
    categories the user asked for.

---

## 13. First small and safe code changes (after audit — inside M2)

These are the changes I would make first, in this order, each a
separate commit:

1. **Add title-case display accessors** on `Rashi` and `Nakshatra`
   (`sanskritTitle()`), and change `ChartDtos.ChartResponse.from` to
   emit them. Adjust `KootaScore.detail` if any code paths embed enum
   names. Add a `RashiTest` / `NakshatraTest` unit assertion.
   *This alone fixes the entire Hindi UI being broken today.*
2. **Add the canonical Ashutosh test** in `BirthChartServiceTest`:
   `1998-10-25T16:20:00` in `Asia/Kolkata` from Ranchi
   `(23.3441, 85.3096)` → UTC `1998-10-25T10:50:00Z`, moon-boundary
   handling as-is. Also add a negative test that
   `1998-10-25T04:20:00` produces a **different** UTC, guaranteeing
   the AM/PM interpretation is not silently coerced.
3. **Rule-version metadata block.** Add
   `MatchResponse.ruleMetadata` = `{ ayanamsa, matchingSystem,
   ashtakootaRuleVersion, manglikRuleVersion, ephemerisMode }`
   derived from `KundliProperties` (existing) and
   `MatchService.RULES_VERSION` (existing) plus new fields for
   Manglik+matchingSystem. This is the shape the brief asked for on
   every result.
4. **Extend `BirthChart` entity + `EphemerisClient` parse** to store
   Ascendant longitude, Mars longitude, Venus longitude. No behaviour
   change yet — this is groundwork for Milestone 4 Manglik.
5. **Fixture seeds.** Add one fully-inputted Ashutosh chart case to
   `chart_reference_cases.json` with `status: PENDING_DRIK_PANCHANG`
   and `expected: null`. Do NOT invent expected values.

No feature changes. No table edits. No provisional flag flips. The
accuracy story does not change; the transparency and testability
story does.

---

## Appendix A — What was already good (kept as-is)

I want to be explicit about the parts I would NOT touch. The current
codebase gets a lot right; not rewriting these is part of "preserve
working code":

- Ephemeris precision guard (both server and client).
- Explicit Lahiri per-request; no hidden defaults.
- Per-thread Swiss Ephemeris re-configuration (subtle, easy to
  regress, currently correct).
- Backend owns local→UTC and boundary-warning policy; Python owns
  astronomy. This split is right.
- `ReferenceTables.load()` completeness validation at boot.
- SQLite decision + portable columns + `matches.rulesVersion`.
- SpaWebConfig fallback that *excludes* `/api/**`.
- Bundled `cities.ts` for Indian coverage. Ranchi is present with the
  right coordinates for the primary profile.
- Angular i18n structure (single service, all keys in both languages,
  parametric substitutions) — solid base for Mother Mode.
- Directional koota interface (`boy`, `girl` explicit) — a switch
  from symmetric to directional Gana is a table edit, not a code
  edit.

---

*End of Milestone 1 audit.*
