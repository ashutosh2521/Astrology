import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ChartResponse,
  CreateChartRequest,
  HealthResponse,
  MatchResponse,
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
}
