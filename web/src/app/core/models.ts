/** Mirrors the backend API DTOs. */

export interface ChartResponse {
  id: number;
  label: string;
  birthLocalDateTime: string;
  timezone: string;
  latitude: number;
  longitude: number;
  placeName: string | null;
  utcInstant: string;
  moonRashi: string;
  moonNakshatra: string;
  moonPada: number;
  warnings: string[];
  ayanamsa: string;
  precision: string;
  createdAt: string;
}

export interface CreateChartRequest {
  label: string;
  birthLocalDateTime: string;
  timezone: string;
  latitude: number;
  longitude: number;
  placeName: string | null;
}

/**
 * One koota's structured outcome. Backend adds new fields (code,
 * personAValue, personBValue, ruleApplied, doshaPresent) so a stored
 * result can be re-rendered in either language from stable enum codes
 * rather than by parsing the human-readable detail string.
 *
 * The legacy fields (koota, points, maxPoints, detail) are retained
 * for backward compat with the existing Angular components; new UI
 * work (Mother Mode) should key off {@link code} + {@link ruleApplied}.
 */
export interface KootaScore {
  /**
   * Programmatic identifier — one of VARNA, VASHYA, TARA, YONI,
   * GRAHA_MAITRI, GANA, BHAKOOT, NADI. Stable across UI language and
   * across engine refactors.
   */
  code: 'VARNA' | 'VASHYA' | 'TARA' | 'YONI' | 'GRAHA_MAITRI' | 'GANA' | 'BHAKOOT' | 'NADI';
  /** Display name in English ("Varna", "Graha Maitri"). */
  koota: string;
  /** Title-case Sanskrit value for person A — e.g. "Kshatriya", "Aadi", "Horse". */
  personAValue: string;
  /** Title-case Sanskrit value for person B — same shape as personAValue. */
  personBValue: string;
  points: number;
  maxPoints: number;
  /**
   * True only for Nadi (same-Nadi) or Bhakoot (dosha pair). The overall
   * result also carries a DoshaStatus for these two, with cancellation.
   */
  doshaPresent: boolean;
  /**
   * Rule branch that produced the score — e.g. SAME_MOON_SIGN_LORD,
   * SWORN_ENEMY_YONI_PAIR, DOSHA_PAIR_5_9. Translation is a frontend
   * responsibility; the code is stable.
   */
  ruleApplied: string;
  /** Legacy free-text explanation. Used for the hover-reveal UI. */
  detail: string;
}

export interface DoshaStatus {
  name: string;
  present: boolean;
  cancelled: boolean;
  reason: string;
  effective: boolean;
}

export interface AshtakootResult {
  kootas: KootaScore[];
  totalPoints: number;
  maxPoints: number;
  doshas: DoshaStatus[];
  verdict: string;
}

/**
 * Declared rule system a result was produced under. Every match response
 * carries this so a stored verdict is independently attributable to the
 * exact convention that produced it.
 */
export interface RuleMetadata {
  ayanamsa: string;                 // e.g. "LAHIRI"
  matchingSystem: string;           // e.g. "NORTH_INDIAN_ASHTAKOOTA"
  ashtakootaRuleVersion: string;    // e.g. "ashtakoot-v0-provisional"
  manglikRuleVersion: string;       // e.g. "not-implemented" until Milestone 4 lands
  ephemerisMode: string;            // e.g. "SWISS_EPHEMERIS_FULL"
}

// ---------------- Manglik (Kuja Dosha) ----------------
//
// Mirrors codes.ashutoshkumar.kundli.manglik.* on the backend. Same
// "engine emits codes, frontend translates" split as the Ashtakoot side.

/** Per-person Manglik overall state. v1 emits NOT_MANGLIK / MANGLIK only. */
export type ManglikState = 'NOT_MANGLIK' | 'MANGLIK' | 'PARTIAL_MANGLIK';

/** One of the three reference points from which Mars's house is counted. */
export type ManglikReference = 'LAGNA' | 'MOON' | 'VENUS';

/**
 * Mars house from a single reference point, and whether that house is one
 * of the v1 trigger houses ({1, 2, 4, 7, 8, 12}).
 */
export interface ReferencePointResult {
  marsHouse: number;  // 1..12
  present: boolean;
}

/** Per-person Manglik result — the structured shape the app spec requires. */
export interface ManglikStatus {
  status: ManglikState;
  fromLagna: ReferencePointResult;
  fromMoon: ReferencePointResult;
  fromVenus: ReferencePointResult;
  triggeredReferences: ManglikReference[];
  cancellations: string[];   // empty in v1
  ruleVersion: string;       // "manglik-v1.0"
}

/** Couple-level Manglik verdict. */
export interface ManglikCompatibility {
  personA: ManglikStatus;
  personB: ManglikStatus;
  compatibility: 'NEITHER_MANGLIK' | 'REQUIRES_DETAILED_REVIEW';
  ruleVersion: string;
}

export interface MatchResponse {
  id: number;
  boyChartId: number;
  girlChartId: number;
  boyLabel: string | null;
  girlLabel: string | null;
  result: AshtakootResult;
  /** Null only on legacy records that predate Ascendant/Mars/Venus storage. */
  manglik: ManglikCompatibility | null;
  /** Backward-compat: same value as ruleMetadata.ashtakootaRuleVersion. */
  rulesVersion: string;
  ruleMetadata: RuleMetadata;
  createdAt: string;
}

export interface HealthResponse {
  app: string;
  ephemeris: string;
}
