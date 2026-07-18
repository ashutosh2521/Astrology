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
import { PlatformService } from './platform.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly platform = inject(PlatformService);

  /**
   * Empty on web (same-origin, relative /api), absolute production origin in the
   * native build (the WebView has no backend at https://localhost). See
   * {@link PlatformService.apiBaseUrl}.
   */
  private readonly base = this.platform.apiBaseUrl;

  private url(path: string): string {
    return `${this.base}${path}`;
  }

  listCharts(): Observable<ChartResponse[]> {
    return this.http.get<ChartResponse[]>(this.url('/api/charts'));
  }

  createChart(req: CreateChartRequest): Observable<ChartResponse> {
    return this.http.post<ChartResponse>(this.url('/api/charts'), req);
  }

  deleteChart(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/api/charts/${id}`));
  }

  createMatch(boyChartId: number, girlChartId: number): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(this.url('/api/matches'), { boyChartId, girlChartId });
  }

  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(this.url('/api/health'));
  }

  /** Mother-mode primary profile. `configured: false` on first run. */
  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(this.url('/api/profile'));
  }

  /** Point the primary profile at an existing chart id. */
  setPrimaryChart(chartId: number): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(this.url('/api/profile'), { chartId });
  }

  listMatches(): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(this.url('/api/matches'));
  }

  getMatch(id: number | string): Observable<MatchResponse> {
    return this.http.get<MatchResponse>(this.url(`/api/matches/${id}`));
  }

  getChart(id: number): Observable<ChartResponse> {
    return this.http.get<ChartResponse>(this.url(`/api/charts/${id}`));
  }

  /**
   * Online birthplace lookup — used by the place-picker when a typed place
   * isn't in the bundled city list. Returns [] (never errors the UI) when the
   * geocoder is unreachable or disabled.
   */
  geocode(query: string): Observable<GeoResult[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<GeoResult[]>(this.url('/api/geocode'), { params });
  }
}
