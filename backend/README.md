# Backend

Java 21 / Maven. Currently hosts the **Ashtakoot matching engine**; the Spring Boot API
layer (REST, persistence, auth, calling the ephemeris service) is added in a later step.

The engine is deliberately **pure and Spring-free** — plain functions over two `MoonChart`s
and static JSON reference tables — so each koota is unit-testable in isolation and the
accuracy regression suite can pin it to Drik Panchang independently.

## Layout

```
src/main/java/.../ashtakoot/
  model/        Rashi, Nakshatra, Graha, Varna, Gana, Nadi, Yoni, VashyaGroup, Relation, MoonChart
  koota/        one scorer per koota (Varna … Nadi) + KootaScorer interface
  tables/       ReferenceTables — loads + validates all JSON at startup
  AshtakootEngine.java   runs all 8 kootas, applies dosha cancellation, builds the verdict
src/main/resources/ashtakoot/   the 7 reference tables (static JSON)
src/test/java/...               per-koota + engine tests
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
