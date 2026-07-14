import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ChartResponse,
  CreateChartRequest,
  GeoResult,
  HealthResponse,
  MatchResponse,
  ProfileResponse,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  listCharts(): Observable<ChartResponse[]> {
    return this.http.get<ChartResponse[]>('/api/charts');
  }

  createChart(req: CreateChartRequest): Observable<ChartResponse> {
    return this.http.post<ChartResponse>('/api/charts', req);
  }

  deleteChart(id: number): Observable<void> {
    return this.http.delete<void>(`/api/charts/${id}`);
  }

  createMatch(boyChartId: number, girlChartId: number): Observable<MatchResponse> {
    return this.http.post<MatchResponse>('/api/matches', { boyChartId, girlChartId });
  }

  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>('/api/health');
  }

  /** Mother-mode primary profile. `configured: false` on first run. */
  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>('/api/profile');
  }

  /** Point the primary profile at an existing chart id. */
  setPrimaryChart(chartId: number): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>('/api/profile', { chartId });
  }

  listMatches(): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>('/api/matches');
  }

  getMatch(id: number | string): Observable<MatchResponse> {
    return this.http.get<MatchResponse>(`/api/matches/${id}`);
  }

  getChart(id: number): Observable<ChartResponse> {
    return this.http.get<ChartResponse>(`/api/charts/${id}`);
  }

  /**
   * Online birthplace lookup — used by the place-picker when a typed place
   * isn't in the bundled city list. Returns [] (never errors the UI) when the
   * geocoder is unreachable or disabled.
   */
  geocode(query: string): Observable<GeoResult[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<GeoResult[]>('/api/geocode', { params });
  }
}
