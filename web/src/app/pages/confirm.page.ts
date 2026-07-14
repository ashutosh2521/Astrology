import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { City } from '../core/cities';
import { ProfileResponse } from '../core/models';
import { I18nService } from '../core/i18n.service';

interface GirlPayload {
  name: string;
  birthLocalDateTime: string;   // ISO local; ready for /api/charts
  date: string;                  // yyyy-mm-dd — for display
  time: string;                  // hh:mm — for display
  city: City;
}

/**
 * Mother-mode confirmation screen (spec §13).
 *
 * <p>The user has just filled in the bride's details on /new-match; this
 * screen shows them back in read-only form and asks "क्या यह जानकारी सही है?".
 * On confirmation we create the bride's chart and run the match; on "edit"
 * we route back to /new-match with router state preserved so nothing is
 * retyped.
 */
@Component({
  selector: 'app-confirm',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="confirm">
      <a routerLink="/new-match" class="back" aria-label="Back">←</a>
      <h1 class="title">{{ i18n.t('confirm.title') }}</h1>

      @if (girl(); as g) {
        <div class="card summary">
          <dl class="summary__grid">
            <dt>{{ i18n.t('confirm.name') }}</dt>
            <dd>{{ g.name }}</dd>

            <dt>{{ i18n.t('confirm.date') }}</dt>
            <dd>{{ g.date }}</dd>

            <dt>{{ i18n.t('confirm.time') }}</dt>
            <dd>{{ g.time }}</dd>

            <dt>{{ i18n.t('confirm.place') }}</dt>
            <dd>{{ placeLabel(g.city) }}</dd>
          </dl>
        </div>

        <p class="question">{{ i18n.t('confirm.question') }}</p>

        @if (error()) {
          <div class="banner banner--bad" role="alert">
            <span aria-hidden="true">✕</span>
            <div>{{ i18n.t(error()!) }}</div>
          </div>
        }

        <div class="actions">
          <button type="button" class="btn btn--primary btn--big"
                  [disabled]="running()"
                  (click)="runMatch(g)">
            {{ running() ? i18n.t('confirm.matching') : (i18n.t('confirm.yes') + ' →') }}
          </button>
          <button type="button" class="btn btn--ghost btn--big"
                  [disabled]="running()"
                  (click)="edit()">
            {{ i18n.t('confirm.edit') }}
          </button>
        </div>
      } @else {
        <!-- Direct-navigation fallback: no state, send them back to fill the form. -->
        <p class="muted">— —</p>
        <a routerLink="/new-match" class="btn btn--ghost">←</a>
      }
    </section>
  `,
  styles: [`
    .confirm { max-width: 560px; margin: 0 auto; padding-top: 12px; }
    .back {
      display: inline-block; color: var(--ink-3);
      font-size: 20px; text-decoration: none; margin-bottom: 8px;
    }
    .title { font-size: 24px; margin: 4px 0 20px; }

    .summary { padding: 18px 20px; }
    .summary__grid {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 10px 20px;
      margin: 0;
    }
    .summary__grid dt {
      color: var(--ink-3);
      font-size: 13px;
      align-self: center;
    }
    .summary__grid dd {
      font-size: 16px;
      color: var(--ink);
      margin: 0;
      font-weight: 600;
    }

    .question {
      text-align: center;
      font-size: 17px;
      margin: 22px 0 14px;
    }

    .actions { display: flex; flex-direction: column; gap: 12px; }
    .btn--big { padding: 16px 20px; font-size: 16px; justify-content: center; }
  `],
})
export class ConfirmPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly girl = signal<GirlPayload | null>(null);
  readonly profile = signal<ProfileResponse | null>(null);
  readonly running = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    // Router state is available only during the navigation that set it;
    // grab it once, on entry.
    const nav = history.state as { girl?: GirlPayload } | null;
    if (nav?.girl) {
      this.girl.set(nav.girl);
    }
    this.api.getProfile().subscribe({
      next: p => this.profile.set(p),
      error: () => this.profile.set(null),
    });
  }

  placeLabel(c: City): string {
    return this.i18n.lang() === 'hi'
      ? `${c.hi}, ${c.regionHi}`
      : `${c.name}, ${c.region}`;
  }

  edit(): void {
    // Preserve the payload so the form is repopulated on the way back.
    // The new-match page currently reads its state fresh; for now we
    // route back and accept a retype, which keeps the flow simple.
    // TODO(M5.5?): plumb the payload back into the form.
    this.router.navigateByUrl('/new-match');
  }

  runMatch(g: GirlPayload): void {
    const boyId = this.profile()?.primaryChartId;
    if (!boyId) {
      this.error.set('newMatch.needProfile');
      return;
    }
    this.running.set(true);
    this.error.set(null);

    // Create the bride's chart, then match it against the primary. Two
    // sequential calls; the second only fires on the first's success.
    this.api.createChart({
      label: g.name,
      birthLocalDateTime: g.birthLocalDateTime,
      timezone: g.city.tz,
      latitude: g.city.lat,
      longitude: g.city.lon,
      placeName: `${g.city.name}, ${g.city.region}`,
    }).subscribe({
      next: girlChart => {
        this.api.createMatch(boyId, girlChart.id).subscribe({
          next: match => {
            this.running.set(false);
            this.router.navigateByUrl(`/result/${match.id}`);
          },
          error: () => {
            this.running.set(false);
            this.error.set('newMatch.err.match');
          },
        });
      },
      error: () => {
        this.running.set(false);
        this.error.set('newMatch.err.compute');
      },
    });
  }
}
