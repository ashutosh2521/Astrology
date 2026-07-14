# Ashtakoot — Verification Status

The engine structure, the domain model, and the algorithmic kootas are trustworthy.
Several **reference-table values are tradition-variable** and are encoded provisionally
pending cross-check against **Drik Panchang** (our chosen reference convention). This
file is the single source of truth for what still needs confirming.

**Nothing marked PROVISIONAL below should be trusted for a real verdict until it has a
regression test pinning it to Drik Panchang.** The unit tests currently pin the *rules*,
not the disputed values.

## Milestone 3 status (in progress)

Milestone 3 has two phases:

- **Phase A — structural, done.** The engine now emits the structured shape the brief
  asks for on every koota result:
  `{ code, koota, personAValue, personBValue, points, maxPoints, doshaPresent,
     ruleApplied, detail }`. `code` is a stable `KootaCode` enum, `ruleApplied` names
  the specific branch that fired (e.g. `SAME_MOON_SIGN_LORD`, `DOSHA_PAIR_6_8`,
  `DEFAULT_YONI_PAIR_PROVISIONAL`). Translations stay in the Angular frontend, per
  the audit's split. See `KootaScore.java`, `KootaCode.java`, `Titles.java` and the
  eight scorers under `koota/`. Locked in by `KootaStructuredOutputTest` (12 tests).
- **Phase B — data, blocked on human/Drik-Panchang.** The four provisional matrices
  (rows 1–5 in the table below) still hold placeholder cells. `koota_reference_cases.json`
  has been seeded to 29 cases (up from 13) covering every provisional cell path and
  every rule branch. Every case is `PENDING_DRIK_PANCHANG`, expected `null`. Filling
  these is a data-entry task that must be done against a live Drik Panchang session:
  fill each case's `expected` block, flip status to `FILLED`, run `mvn test`, and drive
  any resulting mismatch to zero by correcting the specific table cell the failure
  points at.

The declared rule version stays `ashtakoot-v0-provisional` (visible on every match
response via `ruleMetadata.ashtakootaRuleVersion`) until Phase B is complete. The
next value it becomes is `ashtakoota-v1.0`, and only when zero cases are pending and
zero cases fail.

## High confidence (verified by construction; still add regression tests)

| Item | File | Notes |
|------|------|-------|
| Rashi lords | `rashi_to_lord.json` | Unambiguous. |
| Natural friendship (Naisargika) | `rashi_to_lord.json` | Standard Parashari table. |
| Varna mapping | `rashi_to_varna.json` | Elemental (water/fire/earth/air). |
| Gana classification | `nakshatra_to_gana.json` | Standard 9/9/9. |
| Nadi classification | `nakshatra_to_nadi.json` | Standard period-6 zigzag. |
| Nakshatra→Yoni animal | `nakshatra_to_yoni.json` | Standard 27→14 mapping. |
| Bhakoot dosha pairs (2/12, 5/9, 6/8) | `bhakoot_rules.json` | Standard. |
| Tara scoring (mod-9, {3,5,7} bad) | `TaraKoota.java` | Algorithmic, standard method. |

## PROVISIONAL — must be verified against Drik Panchang

| # | Item | File | What's uncertain |
|---|------|------|------------------|
| 1 | **Vashya group assignment** | `rashi_to_vashya_group.json` | Whole-sign simplification; Dhanu/Makara classically split by half-sign. |
| 2 | **Vashya 5×5 matrix** | `rashi_to_vashya_group.json` | **Placeholder** (diagonal=2, off-diagonal=1). Real matrix has 0/0.5/1/2 cells. |
| 3 | **Yoni 14×14 gradations** | `nakshatra_to_yoni.json` | Only same=4 and 7 sworn-enemy pairs=0 are set; friend(3)/neutral(2) cells are placeholder 2. |
| 4 | **Graha Maitri bands** | `rashi_to_lord.json` | Relationship-pair → points bands vary by source. |
| 5 | **Gana 3×3 matrix** | `nakshatra_to_gana.json` | Symmetric values used; a directional variant exists. |
| 6 | **Nadi cancellation convention** | `nadi_cancellation_rules.json` | Spec decision #1. `sameRashiDifferentNakshatra` may be vacuous (see note). |
| 7 | **Bhakoot cancellation convention** | `bhakoot_rules.json` | Spec decision #2. Same-lord / mutual-friend rules are one common convention. |
| 8 | **Cancellation → points?** | `AshtakootEngine.java` | We do NOT restore koota points on cancellation (dosha still scores 0). Confirm Drik Panchang's behavior. |

### Note on Nadi rule #6
Within a single Rashi (~2.25 Nakshatras) consecutive Nakshatras never share a Nadi, so
"same Rashi + different Nakshatra + same Nadi" is close to impossible in practice. The
rule is kept because it's a documented convention, but the pada-based cancellation is the
one that actually fires. Confirm which conditions Drik Panchang actually applies.

## Out of scope for v1 (deferred, per spec)
- **Mangal Dosha (Kuja Dosha)** — needs Lagna/house analysis; the ephemeris service already
  stores full chart data so this can be added later without a recompute.

## The real gate
Per the spec's validation strategy: the regression suite of reference charts (ordinary +
boundary cases) with Nakshatra/Rashi/koota scores hand-verified against Drik Panchang — not
these unit tests — is what turns the PROVISIONAL rows above into verified ones.

**The harness now exists** (see [`docs/regression.md`](../docs/regression.md)):
- Koota cases: `backend/src/test/java/.../regression/KootaRegressionTest.java` +
  `backend/src/test/resources/regression/koota_reference_cases.json`
- Chart cases: `ephemeris-service/tests/regression/`

Cases are seeded with inputs that exercise each PROVISIONAL cell above; their `expected`
values are `PENDING_DRIK_PANCHANG` and the cases skip until filled. **To verify a row above:**
fill the koota case(s) that exercise it from Drik Panchang, set status `FILLED`, run
`mvn test`, and drive any mismatch to zero by correcting the table cell. A green filled case
is a verified cell.

## Which seeded cases target which provisional row

Use this as the checklist while filling `koota_reference_cases.json` and
`chart_reference_cases.json`. Any provisional row whose column is empty here needs
a new case; that is the next data-entry TODO.

| Row | Provisional item | Seeded cases exercising it |
|-----|------------------|----------------------------|
| 1   | Vashya group (whole-sign) | `varna-vaishya-shudra-boy-higher`, `ordinary-pair-1`, `ordinary-pair-2` (every Vashya evaluation lands here) — also every koota case exercises Vashya as a side effect |
| 2   | Vashya 5×5 matrix (placeholder) | Same as row 1 — every case's Vashya score is a matrix lookup; once cases fill, mismatches point directly at the wrong cell |
| 3   | Yoni 14×14 gradations | `yoni-sworn-enemy`, `yoni-serpent-mongoose-sworn-enemy`, `yoni-same-animal-horse`, plus the non-same non-enemy Yoni pair inside every ordinary case |
| 4   | Graha Maitri bands | `grahamaitri-mutual-enemy` (BAND_ENEMY_ENEMY), `grahamaitri-mutual-friend-sun-moon` (BAND_FRIEND_FRIEND), `grahamaitri-same-lord-jupiter` (SAME_MOON_SIGN_LORD full), plus the mixed bands inside ordinary pairs |
| 5   | Gana 3×3 matrix (symmetric?) | `gana-deva-vs-rakshasa`, `gana-same-rakshasa`, `gana-manushya-rakshasa-directional`, `gana-rakshasa-manushya-reverse` — filling the last two settles symmetric vs. directional |
| 6   | Nadi cancellation convention | `nadi-dosha-identical` (not cancelled), `nadi-dosha-cancel-different-pada`, `nadi-antya-antya-same-cross-rashi` (Rashi differs → sameRashi flag doesn't fire) |
| 7   | Bhakoot cancellation convention | `bhakoot-cancel-same-lord`, `bhakoot-2-12`, `bhakoot-5-9`, `bhakoot-6-8`, plus `bhakoot-7-7-opposite-signs-no-dosha` and `bhakoot-3-11-no-dosha` for the non-dosha branches |
| 8   | Cancellation → points restoration? | Currently covered inferentially: any FILLED cancellation case with a `nadi` or `bhakoot` expected value tells us whether Drik Panchang restores the 7/8. Confirm explicitly by comparing the same case's `total` against the sum of expected kootas — if they don't add up, restoration is happening |

Nothing here fabricates values. Every "expected" cell is `null` until a human sits with
Drik Panchang and fills it. That step is the release gate for `ashtakoota-v1.0`.
