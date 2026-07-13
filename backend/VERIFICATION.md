# Ashtakoot — Verification Status

The engine structure, the domain model, and the algorithmic kootas are trustworthy.
Several **reference-table values are tradition-variable** and are encoded provisionally
pending cross-check against **Drik Panchang** (our chosen reference convention). This
file is the single source of truth for what still needs confirming.

**Nothing marked PROVISIONAL below should be trusted for a real verdict until it has a
regression test pinning it to Drik Panchang.** The unit tests currently pin the *rules*,
not the disputed values.

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
Per the spec's validation strategy: build the regression suite of reference charts (ordinary
+ boundary cases) with Nakshatra/Rashi/koota scores hand-verified against Drik Panchang, and
run it on every change to calculation code. That suite — not these unit tests — is what turns
the PROVISIONAL rows above into verified ones.
