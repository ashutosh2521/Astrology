import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ChartResponse } from '../core/models';

@Component({
  selector: 'app-charts-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section class="hero">
      <h1>Birth Charts</h1>
      <p class="muted">
        Enter the exact birth time — to the minute, seconds if known. The Moon moves
        ~13° a day, so near a Nakshatra boundary even a small error can change the match.
      </p>
    </section>

    @if (error()) {
      <div class="banner banner--bad fade-in" role="alert">
        <span aria-hidden="true">✕</span>
        <div>{{ error() }}</div>
      </div>
    }

    <div class="layout">
      <!-- ————— Create form ————— -->
      <form class="card form" (ngSubmit)="create()" #f="ngForm">
        <h2 class="form__title">New chart</h2>

        <div class="field">
          <label for="label">Name</label>
          <input id="label" name="label" [(ngModel)]="form.label" required
                 placeholder="Person’s name" autocomplete="off" />
        </div>

        <div class="grid-2">
          <div class="field">
            <label for="date">Birth date</label>
            <input id="date" name="date" type="date" [(ngModel)]="form.date" required />
          </div>
          <div class="field">
            <label for="time">Birth time</label>
            <input id="time" name="time" type="time" step="1" [(ngModel)]="form.time" required />
            <span class="hint">hh:mm:ss — seconds if known</span>
          </div>
        </div>

        <div class="field">
          <label for="tz">Timezone of birthplace</label>
          <select id="tz" name="tz" [(ngModel)]="form.timezone" required>
            @for (z of timezones; track z) {
              <option [value]="z">{{ z }}</option>
            }
          </select>
        </div>

        <div class="field">
          <label for="place">Place of birth</label>
          <input id="place" name="place" [(ngModel)]="form.placeName"
                 placeholder="City, Country" autocomplete="off" />
        </div>

        <div class="grid-2">
          <div class="field">
            <label for="lat">Latitude</label>
            <input id="lat" name="lat" type="number" step="0.0001" min="-90" max="90"
                   [(ngModel)]="form.latitude" required placeholder="28.6139" />
          </div>
          <div class="field">
            <label for="lon">Longitude</label>
            <input id="lon" name="lon" type="number" step="0.0001" min="-180" max="180"
                   [(ngModel)]="form.longitude" required placeholder="77.2090" />
          </div>
        </div>

        <button class="btn btn--primary" type="submit" [disabled]="f.invalid || saving()">
          {{ saving() ? 'Computing chart…' : 'Compute chart ✦' }}
        </button>
      </form>

      <!-- ————— Chart list ————— -->
      <div class="list">
        @if (loading()) {
          @for (i of [1, 2, 3]; track i) {
            <div class="skeleton" style="height: 118px"></div>
          }
        } @else if (charts().length === 0) {
          <div class="card--inset empty">
            <div class="empty__mark" aria-hidden="true">✦</div>
            <p class="muted">No charts yet. Create the first one to begin matching.</p>
          </div>
        } @else {
          @for (c of charts(); track c.id) {
            <article class="card chart fade-in">
              <header class="chart__head">
                <h3 class="chart__name">{{ c.label }}</h3>
                <button class="btn btn--danger-ghost" type="button"
                        (click)="remove(c)" [attr.aria-label]="'Delete chart for ' + c.label">
                  Delete
                </button>
              </header>

              <div class="chart__badges">
                <span class="chip chip--gold" title="Moon sign">☾ {{ c.moonRashi }}</span>
                <span class="chip" title="Moon nakshatra & pada">{{ c.moonNakshatra }} · Pada {{ c.moonPada }}</span>
              </div>

              <p class="chart__meta muted small">
                {{ c.birthLocalDateTime.replace('T', ' · ') }} — {{ c.timezone }}
                @if (c.placeName) { <span> — {{ c.placeName }}</span> }
              </p>

              @for (w of c.warnings; track w) {
                <div class="banner banner--warn small">
                  <span aria-hidden="true">◭</span>
                  <div>{{ w }}</div>
                </div>
              }
            </article>
          }
          <a routerLink="/match" class="btn btn--ghost list__cta">Run a match →</a>
        }
      </div>
    </div>
  `,
  styles: [`
    .hero { max-width: 620px; margin-bottom: 30px; }
    .hero h1 { font-size: 34px; }

    .layout {
      display: grid;
      grid-template-columns: 380px 1fr;
      gap: 26px;
      align-items: start;
    }
    @media (max-width: 880px) { .layout { grid-template-columns: 1fr; } }

    .form { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 20px; }
    .form__title { font-size: 20px; margin: 0; }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form .btn { margin-top: 4px; justify-content: center; }

    .list { display: flex; flex-direction: column; gap: 16px; }
    .list__cta { align-self: flex-end; }

    .empty { text-align: center; padding: 44px 20px; border-radius: var(--radius); }
    .empty__mark { font-size: 26px; color: var(--gold-text); margin-bottom: 6px; }

    .chart { padding: 19px 22px; }
    .chart__head { display: flex; justify-content: space-between; align-items: center; }
    .chart__name { font-size: 19px; margin: 0; }
    .chart__badges { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 8px; }
    .chart__meta { margin: 0 0 4px; }
    .chart .banner { margin-top: 9px; }
  `],
})
export class ChartsPage implements OnInit {
  private readonly api = inject(ApiService);

  readonly charts = signal<ChartResponse[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  /** Full IANA list from the platform when available; sensible fallback otherwise. */
  readonly timezones: string[] = (() => {
    const supported = (Intl as any).supportedValuesOf?.('timeZone') as string[] | undefined;
    const list = supported ?? ['Asia/Kolkata', 'Asia/Dubai', 'Europe/London', 'America/New_York', 'UTC'];
    // India first — the primary audience.
    return ['Asia/Kolkata', ...list.filter(z => z !== 'Asia/Kolkata')];
  })();

  form = {
    label: '',
    date: '',
    time: '',
    timezone: 'Asia/Kolkata',
    placeName: '',
    latitude: null as number | null,
    longitude: null as number | null,
  };

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.api.listCharts().subscribe({
      next: cs => { this.charts.set(cs.slice().reverse()); this.loading.set(false); },
      error: () => { this.error.set('Could not load charts — is the backend running?'); this.loading.set(false); },
    });
  }

  create(): void {
    const f = this.form;
    if (!f.label || !f.date || !f.time || f.latitude == null || f.longitude == null) return;
    // hh:mm from <input type=time> without seconds → normalize to hh:mm:00
    const time = f.time.length === 5 ? `${f.time}:00` : f.time;
    this.saving.set(true);
    this.error.set(null);
    this.api.createChart({
      label: f.label.trim(),
      birthLocalDateTime: `${f.date}T${time}`,
      timezone: f.timezone,
      latitude: f.latitude,
      longitude: f.longitude,
      placeName: f.placeName.trim() || null,
    }).subscribe({
      next: c => {
        this.charts.update(cs => [c, ...cs]);
        this.saving.set(false);
        this.form = { ...this.form, label: '', date: '', time: '', placeName: '' };
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.error ?? 'Chart computation failed.');
      },
    });
  }

  remove(c: ChartResponse): void {
    if (!confirm(`Delete the chart for “${c.label}”?`)) return;
    this.api.deleteChart(c.id).subscribe({
      next: () => this.charts.update(cs => cs.filter(x => x.id !== c.id)),
      error: () => this.error.set('Delete failed.'),
    });
  }
}
