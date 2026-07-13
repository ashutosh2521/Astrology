import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ChartResponse, MatchResponse } from '../core/models';
import { ScoreRingComponent } from '../components/score-ring.component';
import { KootaBarsComponent } from '../components/koota-bars.component';
import { DoshaCardComponent } from '../components/dosha-card.component';

@Component({
  selector: 'app-match-page',
  standalone: true,
  imports: [FormsModule, RouterLink, ScoreRingComponent, KootaBarsComponent, DoshaCardComponent],
  template: `
    <section class="hero">
      <h1>Match</h1>
      <p class="muted">Ashtakoot — the eight-fold compatibility of two Moon charts, out of 36 points.</p>
    </section>

    @if (charts().length < 2 && !loading()) {
      <div class="card--inset empty">
        <div class="empty__mark" aria-hidden="true">✦</div>
        <p class="muted">You need at least two charts to run a match.</p>
        <a routerLink="/" class="btn btn--ghost">Create charts →</a>
      </div>
    } @else {
      <div class="picker card">
        <div class="picker__side">
          <div class="field">
            <label for="boy">Boy’s chart</label>
            <select id="boy" name="boy" [(ngModel)]="boyId">
              <option [ngValue]="null" disabled>Select…</option>
              @for (c of charts(); track c.id) {
                <option [ngValue]="c.id">{{ c.label }} — ☾ {{ c.moonRashi }}, {{ c.moonNakshatra }}</option>
              }
            </select>
          </div>
        </div>

        <button class="picker__swap btn btn--ghost" type="button" (click)="swap()"
                aria-label="Swap boy and girl charts" title="Swap">⇄</button>

        <div class="picker__side">
          <div class="field">
            <label for="girl">Girl’s chart</label>
            <select id="girl" name="girl" [(ngModel)]="girlId">
              <option [ngValue]="null" disabled>Select…</option>
              @for (c of charts(); track c.id) {
                <option [ngValue]="c.id">{{ c.label }} — ☾ {{ c.moonRashi }}, {{ c.moonNakshatra }}</option>
              }
            </select>
          </div>
        </div>

        <button class="btn btn--primary picker__go" type="button"
                (click)="run()" [disabled]="!canRun() || matching()">
          {{ matching() ? 'Matching…' : 'Match ✦' }}
        </button>
      </div>

      @if (error()) {
        <div class="banner banner--bad fade-in" role="alert">
          <span aria-hidden="true">✕</span>
          <div>{{ error() }}</div>
        </div>
      }

      @if (match(); as m) {
        <section class="result fade-in" aria-live="polite">
          <div class="card result__hero">
            <app-score-ring [points]="m.result.totalPoints" [max]="m.result.maxPoints" />
            <div class="result__verdict">
              <div class="result__names">
                {{ m.boyLabel }} <span class="muted">&</span> {{ m.girlLabel }}
              </div>
              <p class="result__text">{{ m.result.verdict }}</p>
              @if (chartWarnings().length > 0) {
                <div class="banner banner--warn small">
                  <span aria-hidden="true">◭</span>
                  <div>
                    @for (w of chartWarnings(); track w) { <div>{{ w }}</div> }
                  </div>
                </div>
              }
              <p class="muted small">Rule set: {{ m.rulesVersion }}</p>
            </div>
          </div>

          <div class="result__grid">
            <div class="card">
              <h2 class="section-title">Koota breakdown</h2>
              <app-koota-bars [kootas]="m.result.kootas" />
            </div>
            <div class="doshas">
              <h2 class="section-title">Doshas</h2>
              @for (d of m.result.doshas; track d.name) {
                <app-dosha-card [dosha]="d" />
              }
            </div>
          </div>
        </section>
      }
    }
  `,
  styles: [`
    .hero { max-width: 620px; margin-bottom: 26px; }
    .hero h1 { font-size: 34px; }

    .empty { text-align: center; padding: 44px 20px; border-radius: var(--radius);
             display: flex; flex-direction: column; align-items: center; gap: 10px; }
    .empty__mark { font-size: 26px; color: var(--gold-text); }

    .picker {
      display: grid;
      grid-template-columns: 1fr auto 1fr auto;
      gap: 14px;
      align-items: end;
      margin-bottom: 26px;
    }
    .picker__swap { padding: 11px 14px; }
    @media (max-width: 760px) {
      .picker { grid-template-columns: 1fr; }
      .picker__swap { justify-self: center; }
    }

    .result__hero {
      display: flex;
      gap: 34px;
      align-items: center;
      margin-bottom: 26px;
    }
    @media (max-width: 640px) { .result__hero { flex-direction: column; text-align: center; } }
    .result__names {
      font-family: var(--font-display);
      font-size: 24px;
      margin-bottom: 6px;
    }
    .result__text { margin: 0 0 12px; color: var(--ink-2); }
    .result__verdict .banner { margin-bottom: 10px; }

    .result__grid {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 26px;
      align-items: start;
    }
    @media (max-width: 880px) { .result__grid { grid-template-columns: 1fr; } }

    .section-title { font-size: 19px; margin-bottom: 16px; }
    .doshas { display: flex; flex-direction: column; gap: 12px; }
    .doshas .section-title { margin-bottom: 4px; }
  `],
})
export class MatchPage implements OnInit {
  private readonly api = inject(ApiService);

  readonly charts = signal<ChartResponse[]>([]);
  readonly loading = signal(true);
  readonly matching = signal(false);
  readonly match = signal<MatchResponse | null>(null);
  readonly error = signal<string | null>(null);

  boyId: number | null = null;
  girlId: number | null = null;

  canRun(): boolean {
    return this.boyId != null && this.girlId != null;
  }

  /** Accuracy warnings from both charts, surfaced beside the verdict. */
  readonly chartWarnings = computed(() => {
    const m = this.match();
    if (!m) return [];
    const byId = new Map(this.charts().map(c => [c.id, c]));
    const boy = byId.get(m.boyChartId);
    const girl = byId.get(m.girlChartId);
    return [
      ...(boy?.warnings ?? []).map(w => `${boy!.label}: ${w}`),
      ...(girl?.warnings ?? []).map(w => `${girl!.label}: ${w}`),
    ];
  });

  ngOnInit(): void {
    this.api.listCharts().subscribe({
      next: cs => { this.charts.set(cs); this.loading.set(false); },
      error: () => { this.error.set('Could not load charts.'); this.loading.set(false); },
    });
  }

  swap(): void {
    [this.boyId, this.girlId] = [this.girlId, this.boyId];
  }

  run(): void {
    if (this.boyId == null || this.girlId == null) return;
    if (this.boyId === this.girlId) {
      this.error.set('Pick two different charts.');
      return;
    }
    this.matching.set(true);
    this.error.set(null);
    this.api.createMatch(this.boyId, this.girlId).subscribe({
      next: m => { this.match.set(m); this.matching.set(false); },
      error: err => {
        this.matching.set(false);
        this.error.set(err?.error?.error ?? 'Match failed.');
      },
    });
  }
}
