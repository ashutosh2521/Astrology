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

export interface KootaScore {
  koota: string;
  points: number;
  maxPoints: number;
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

export interface MatchResponse {
  id: number;
  boyChartId: number;
  girlChartId: number;
  boyLabel: string | null;
  girlLabel: string | null;
  result: AshtakootResult;
  /** Backward-compat: same value as ruleMetadata.ashtakootaRuleVersion. */
  rulesVersion: string;
  ruleMetadata: RuleMetadata;
  createdAt: string;
}

export interface HealthResponse {
  app: string;
  ephemeris: string;
}
