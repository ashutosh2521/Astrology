# Accuracy Regression Suite

This is the mechanism the spec calls for: cross-check calculated values against an
established reference (**Drik Panchang**) for a fixed set of charts, and run it on every
change to calculation code so an ephemeris upgrade or a table edit can't silently break
accuracy.

There are two halves, matching the two places accuracy lives:

| Half | Validates | Location |
|------|-----------|----------|
| **Chart regression** | Moon Rashi/Nakshatra/Pada from birth data (ephemeris + Lahiri ayanamsa) | `ephemeris-service/tests/regression/` |
| **Koota regression** | The 8-koota scores + dosha cancellation (engine + reference tables) | `backend/src/test/.../regression/` |

They are separate on purpose: a chart failure points at the ephemeris/ayanamsa; a koota
failure points at a reference-table value. Keeping them decoupled means a single failure
tells you *which* layer is wrong.

## How a case works

Each case ships with its **inputs filled** and its **`expected` block null**, status
`PENDING_DRIK_PANCHANG`. In that state the case is **skipped** — the suite stays green but
is honestly marked incomplete (skipped, not passed). This is deliberate: an empty suite
that reports "all green" is worse than one that shows what still needs verifying.

To verify a case:
1. Reproduce it in the relevant Drik Panchang tool (links are in each fixture's `_source`).
2. Copy the reference values into `expected` and set `status` to `FILLED`.
3. Run the suite. If the engine/ephemeris disagrees, the case **fails** and names the exact
   koota/field that's wrong — that's your signal to correct the provisional table cell
   (see `backend/VERIFICATION.md`).

A filled, passing case is a verified case. That is what promotes a PROVISIONAL table to
trusted.

## The provisional tables this locks down

All the tradition-variable values flagged in `backend/VERIFICATION.md` — the Vashya matrix,
Yoni gradations, Graha Maitri bands, Gana matrix, and the Nadi/Bhakoot cancellation
conventions — are verified precisely by filling koota cases that exercise them and driving
the mismatches to zero.

## Running

```bash
# Koota regression (runs anywhere; pending cases skip)
cd backend && mvn test

# Chart regression (skips without swisseph + .se1 data; real work where ephemeris is present)
cd ephemeris-service && pytest
```

## Choosing cases

- **Cover every koota and every cancellation branch** — the seeded koota cases already do
  (Nadi dosha + both cancellations, all three Bhakoot dosha distances + a cancellation,
  Varna direction, Gana Deva/Rakshasa, Yoni enemy, Graha Maitri enemy, plus ordinary pairs).
- **Add boundary charts** — births with the Moon within ~1° of a Nakshatra/Rashi edge. These
  are the highest-value chart cases: they're where ayanamsa or precision drift first shows up,
  and they tie directly to the spec's birth-time-precision concern.
