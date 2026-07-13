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

export interface MatchResponse {
  id: number;
  boyChartId: number;
  girlChartId: number;
  boyLabel: string | null;
  girlLabel: string | null;
  result: AshtakootResult;
  rulesVersion: string;
  createdAt: string;
}

export interface HealthResponse {
  app: string;
  ephemeris: string;
}
