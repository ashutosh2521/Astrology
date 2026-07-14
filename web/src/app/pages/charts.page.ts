import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ChartResponse, ProfileResponse } from '../core/models';
import { City } from '../core/cities';
import { I18nService } from '../core/i18n.service';
import { PlacePickerComponent } from '../components/place-picker.component';

@Component({
  selector: 'app-charts-page',
  standalone: true,
  imports: [FormsModule, RouterLink, PlacePickerComponent],
  template: `
    <section class="hero">
      <h1>{{ i18n.t('charts.title') }}</h1>
      <p class="muted">{{ i18n.t('charts.intro') }}</p>
    </section>

    @if (error()) {
      <div class="banner banner--bad fade-in" role="alert">
        <span aria-hidden="true">✕</span>
        <div>{{ i18n.t(error()!) }}</div>
      </div>
    }

    <div class="layout">
      <!-- ————— Create form ————— -->
      <form class="card form" (ngSubmit)="create()" #f="ngForm">
        <h2 class="form__title">{{ i18n.t('charts.form.title') }}</h2>

        <div class="field">
          <label for="label">{{ i18n.t('charts.name') }}</label>
          <input id="label" name="label" [(ngModel)]="form.label" required
                 [placeholder]="i18n.t('charts.name.ph')" autocomplete="off" />
        </div>

        <div class="grid-2">
          <div class="field">
            <label for="date">{{ i18n.t('charts.date') }}</label>
            <input id="date" name="date" type="date" [(ngModel)]="form.date" required />
          </div>
          <div class="field">
            <label for="time">{{ i18n.t('charts.time') }}</label>
            <input id="time" name="time" type="time" [(ngModel)]="form.time" required />
            <span class="hint">{{ i18n.t('charts.time.hint') }}</span>
          </div>
        </div>

        <div class="field">
          <label for="place">{{ i18n.t('charts.place') }}</label>
          <app-place-picker inputId="place" (changed)="onCity($event)" />
        </div>

        <button type="button" class="manual-toggle" (click)="manual.set(!manual())">
          {{ manual() ? i18n.t('charts.manual.hide') : i18n.t('charts.manual.show') }}
        </button>

        @if (manual()) {
          <div class="manual fade-in">
            <div class="field">
              <label for="placeName">{{ i18n.t('charts.manual.place') }}</label>
              <input id="placeName" name="placeName" [(ngModel)]="form.placeName"
                     [placeholder]="i18n.t('charts.manual.place.ph')" autocomplete="off" />
            </div>
            <div class="grid-2">
              <div class="field">
                <label for="lat">{{ i18n.t('charts.lat') }}</label>
                <input id="lat" name="lat" type="number" step="0.0001" min="-90" max="90"
                       [(ngModel)]="form.latitude" placeholder="25.5941" />
              </div>
              <div class="field">
                <label for="lon">{{ i18n.t('charts.lon') }}</label>
                <input id="lon" name="lon" type="number" step="0.0001" min="-180" max="180"
                       [(ngModel)]="form.longitude" placeholder="85.1376" />
              </div>
            </div>
            <div class="field">
              <label for="tz">{{ i18n.t('charts.tz') }}</label>
              <select id="tz" name="tz" [(ngModel)]="form.timezone">
                @for (z of timezones; track z) {
                  <option [value]="z">{{ z }}</option>
                }
              </select>
            </div>
          </div>
        }

        <button class="btn btn--primary" type="submit" [disabled]="!canSubmit(f.invalid) || saving()">
          {{ saving() ? i18n.t('charts.submitting') : i18n.t('charts.submit') }}
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
            <p class="muted">{{ i18n.t('charts.empty') }}</p>
          </div>
        } @else {
          @for (c of charts(); track c.id) {
            <article class="card chart fade-in">
              <header class="chart__head">
                <h3 class="chart__name">{{ c.label }}</h3>
                <div class="chart__actions">
                  <button class="btn btn--ghost btn--sm" type="button"
                          [disabled]="isPrimary(c)"
                          (click)="setAsPrimary(c)">
                    {{ isPrimary(c) ? i18n.t('advanced.primary.already') : i18n.t('advanced.primary.set') }}
                  </button>
                  <button class="btn btn--danger-ghost" type="button"
                          (click)="remove(c)"
                          [attr.aria-label]="i18n.t('charts.deleteAria') + ' ' + c.label">
                    {{ i18n.t('charts.delete') }}
                  </button>
                </div>
              </header>

              <div class="chart__badges">
                <span class="chip chip--gold" [title]="i18n.t('charts.moonSign')">
                  ☾ {{ i18n.rashi(c.moonRashi) }}
                </span>
                <span class="chip" [title]="i18n.t('charts.moonNak')">
                  {{ i18n.nakshatra(c.moonNakshatra) }} · {{ i18n.t('charts.pada') }} {{ c.moonPada }}
                </span>
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
          <a routerLink="/match" class="btn btn--ghost list__cta">{{ i18n.t('charts.cta') }}</a>
        }
      </div>
    </div>
  `,
  styles: [`
    .hero { max-width: 620px; margin-bottom: 30px; }
    .hero h1 { font-size: 34px; }

    .layout {
      display: grid;
      grid-template-columns: 400px 1fr;
      gap: 26px;
      align-items: start;
    }
    @media (max-width: 880px) { .layout { grid-template-columns: 1fr; } }

    .form { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 20px; }
    .form__title { font-size: 20px; margin: 0; }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form .btn { margin-top: 4px; justify-content: center; }

    .manual-toggle {
      align-self: flex-start;
      background: none;
      border: none;
      padding: 0;
      font: 500 13px/1.4 var(--font-body);
      color: var(--gold-text);
      cursor: pointer;
      text-decoration: underline dotted;
    }
    .manual { display: flex; flex-direction: column; gap: 12px;
              border-top: 1px dashed var(--border); padding-top: 14px; }

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
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  private readonly picker = viewChild(PlacePickerComponent);

  readonly charts = signal<ChartResponse[]>([]);
  readonly profile = signal<ProfileResponse | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  /** Manual location entry — the escape hatch when the city isn't bundled. */
  readonly manual = signal(false);

  /** Full IANA list from the platform when available; sensible fallback otherwise. */
  readonly timezones: string[] = (() => {
    const supported = (Intl as any).supportedValuesOf?.('timeZone') as string[] | undefined;
    const list = supported ?? ['Asia/Kolkata', 'Asia/Dubai', 'Europe/London', 'America/New_York', 'UTC'];
    // India first — the primary audience.
    return ['Asia/Kolkata', ...list.filter(z => z !== 'Asia/Kolkata')];
  })();

  private city: City | null = null;

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
    this.api.getProfile().subscribe({
      next: p => this.profile.set(p),
      error: () => {},
    });
  }

  isPrimary(c: ChartResponse): boolean {
    return this.profile()?.primaryChartId === c.id;
  }

  setAsPrimary(c: ChartResponse): void {
    this.api.setPrimaryChart(c.id).subscribe({
      next: p => {
        this.profile.set(p);
        // Bounce to Mother-mode home so the setup wizard is complete.
        this.router.navigateByUrl('/');
      },
      error: () => this.error.set('charts.err.compute'),
    });
  }

  onCity(c: City | null): void {
    this.city = c;
    if (c) {
      this.form.latitude = c.lat;
      this.form.longitude = c.lon;
      this.form.timezone = c.tz;
      this.form.placeName = `${c.name}, ${c.region}`;
    }
  }

  canSubmit(formInvalid: boolean | null): boolean {
    if (formInvalid) return false;
    const hasLocation = this.city != null
      || (this.form.latitude != null && this.form.longitude != null);
    return hasLocation;
  }

  refresh(): void {
    this.api.listCharts().subscribe({
      next: cs => { this.charts.set(cs.slice().reverse()); this.loading.set(false); },
      // Error signals hold message *keys* (or raw backend text, which t() passes
      // through untouched) so banners re-render when the language changes.
      error: () => { this.error.set('charts.err.load'); this.loading.set(false); },
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
        this.form = {
          ...this.form,
          label: '', date: '', time: '', placeName: '', latitude: null, longitude: null,
        };
        this.city = null;
        this.picker()?.clear();
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.error ?? 'charts.err.compute');
      },
    });
  }

  remove(c: ChartResponse): void {
    if (!confirm(this.i18n.t('charts.deleteConfirm', { name: c.label }))) return;
    this.api.deleteChart(c.id).subscribe({
      next: () => this.charts.update(cs => cs.filter(x => x.id !== c.id)),
      error: () => this.error.set('charts.err.delete'),
    });
  }
}
