# Kundli Matching App — Project Spec

## Overview
A personal-scale kundli (Vedic horoscope) matching application. Accuracy of the
astrological calculations is the core requirement — the app is only useful if the
matching results are correct. Everything else is secondary to that.

## Tech stack
- **Backend:** Java, Spring Boot
- **Web frontend:** Angular
- **Mobile:** Ionic + Angular + Capacitor, **Android only** (no iOS for now)
- **Astrology calculation engine:** Python microservice using `pyswisseph`
  (Swiss Ephemeris bindings), called internally by the Spring Boot backend —
  not exposed to the internet
- **Database:** Postgres, run in Docker on the same host
- **Hosting:** Single EC2 instance with an Elastic IP, Nginx reverse proxy,
  TLS via Let's Encrypt
- **Scale:** personal/small project, low expected traffic — architecture is
  intentionally simple (single instance, no load balancer/RDS needed yet)

## Architecture (single EC2 instance)
```
Angular web client ─┐
                     ├──> Nginx (TLS) ──> Spring Boot API ──> Postgres (Docker)
Ionic/Android app ───┘                          │
                                                  └──> Python ephemeris service
                                                       (pyswisseph, internal only)
```
- Security groups: only 443 (and 22 from a trusted IP) open externally.
  Postgres and the Python service are never reachable from outside the
  Docker network.
- Nightly `pg_dump` to S3 recommended for backups.

## Accuracy requirements (critical — see rationale below)
1. **Swiss Ephemeris must run in full precision mode.** Verify at startup
   that the real `.se1` ephemeris data files are loaded — some wrappers
   silently fall back to a lower-precision analytical model (Moshier) if the
   files are missing. This must be caught, not silently accepted.
2. **Ayanamsa: Lahiri**, used explicitly (not a hidden default). This is the
   standard for Vedic/Indian kundli matching and the main source of
   disagreement between astrology tools if left ambiguous.
3. **Birth time precision matters a lot.** The Moon moves ~13–14°/day, so a
   30-minute error in birth time can shift Nakshatra/Rashi and change the
   whole match result.
   - UI must capture exact hour:minute (seconds if known).
   - Calculation layer should **detect and flag boundary cases** — if the
     Moon's position is within ~1° of a Nakshatra/Rashi transition, surface a
     warning that small time uncertainty could change the result.
4. **Timezone handling:** standard IANA tz data is fine for modern births.
   If supporting birth dates before ~1950 (especially in India), historical
   UTC offset tables are needed, not just current tz rules.
5. **Geocoding precision:** matters less for pure Ashtakoot (Moon/Nakshatra
   based) matching, more if Lagna/house-based doshas are added later. Be
   explicit in the data model about what precision each feature actually
   needs.

## Validation strategy (do this before trusting any output)
- Build a **regression test suite** that cross-checks calculated
  Nakshatra/Rashi/koota scores against an established reference (e.g. Drik
  Panchang) for a fixed set of test charts — include edge cases sitting near
  Nakshatra/Rashi boundaries.
- Run this suite automatically on any change to calculation code, so an
  ephemeris library upgrade or refactor can't silently break accuracy.

## Matching logic scope (to be detailed further)
- Core: Ashtakoot (8-koota, 36-point) system — Varna, Vashya, Tara, Yoni,
  Graha Maitri, Gana, Bhakoot, Nadi.
- Must also implement standard **exception/cancellation rules** (e.g. Nadi
  dosha cancellation conditions, Bhakoot dosha exceptions for specific rashi
  pairs) — raw point totals without these produce technically-scored but
  practically-wrong verdicts.

## Ashtakoot scoring — detailed breakdown

Full 8-koota system, 36 points max. Each koota below is computed from the
Moon Rashi and/or Nakshatra of both people (from Layer 1 chart generation).
All lookup tables should be static reference data (JSON/config), not
hardcoded logic — this keeps them independently unit-testable and easy to
verify against a reference source.

| # | Koota | Max pts | Based on | Logic type |
|---|-------|---------|----------|------------|
| 1 | Varna | 1 | Rashi → Varna category (4 tiers) | Hierarchy comparison — boy's tier ≥ girl's tier |
| 2 | Vashya | 2 | Rashi → Vashya group (5 groups) | 5×5 compatibility matrix, partial credit possible |
| 3 | Tara | 3 | Nakshatra distance, both directions | Computed: count nakshatra positions mod 9 each way, map remainder to score |
| 4 | Yoni | 4 | Nakshatra → Yoni animal (14 animals) | 14×14 compatibility matrix (same/friend/neutral/enemy) |
| 5 | Graha Maitri | 5 | Rashi → ruling planet (7 grahas) | 7×7 planetary friendship matrix, directional, most nuanced scoring bands |
| 6 | Gana | 6 | Nakshatra → Gana (Deva/Manushya/Rakshasa) | 3×3 matrix, directional (boy vs girl matters) |
| 7 | Bhakoot | 7 | Rashi-to-Rashi distance (1-12, 2-12, 5-9, 6-8, etc.) | All-or-nothing (7 or 0); has cancellation exceptions |
| 8 | Nadi | 8 | Nakshatra → Nadi (Aadi/Madhya/Antya) | Same nadi = 0 (dosha), different = full 8; has cancellation exceptions |

### Reference tables needed (static, load at startup)
- `rashi_to_varna.json` — 12 Rashi → 4 Varna tiers
- `rashi_to_vashya_group.json` — 12 Rashi → 5 Vashya groups + 5×5 compatibility matrix
- `nakshatra_to_yoni.json` — 27 Nakshatra → 14 Yoni animals + 14×14 compatibility matrix
- `rashi_to_lord.json` — 12 Rashi → ruling planet (7 grahas, excl. Rahu/Ketu) + 7×7 friendship matrix
- `nakshatra_to_gana.json` — 27 Nakshatra → 3 Gana + 3×3 directional matrix
- `bhakoot_distance_rules.json` — Rashi distance → dosha/no-dosha + cancellation conditions
- `nadi_cancellation_rules.json` — Nadi dosha cancellation conditions (tradition-specific, decide convention upfront)

### Decisions to lock in before implementation
1. **Nadi dosha exception/cancellation convention** — more than one tradition exists; pick one and document it.
2. **Bhakoot dosha exception conditions** — e.g. same Moon sign exceptions, lord-based cancellations.
3. **Mangal Dosha (Kuja Dosha)** — not part of the 8 kootas but commonly checked alongside Ashtakoot in real-world matching. Decide if it's in scope for v1 or a later addition.

## Open items for next design pass
- Full Ashtakoot scoring rules + exception conditions (detailed rule set)
- Exact Java (Spring Boot) ↔ Python (ephemeris service) API contract
- Data model for birth charts, users, saved matches
- Android build pipeline via Capacitor, Google Play closed-testing
  requirements (14-day / 12-tester minimum for new developer accounts)
