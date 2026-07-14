import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { MatchResponse } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Simple match-history list. Mother-mode entry point for past matches;
 * each row links to /result/:id to open the same Mother-mode result view.
 */
@Component({
  selector: 'app-history',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="history">
      <a routerLink="/" class="back" aria-label="Home">←</a>
      <h1 class="title">{{ i18n.t('history.title') }}</h1>

      @if (loading()) {
        @for (i of [1, 2, 3]; track i) {
          <div class="skeleton" style="height: 74px; margin-bottom: 10px"></div>
        }
      } @else if (matches().length === 0) {
        <div class="card--inset empty">
          <p class="muted">{{ i18n.t('history.empty') }}</p>
          <a routerLink="/new-match" class="btn btn--primary">
            {{ i18n.t('history.newMatch') }}
          </a>
        </div>
      } @else {
        <ul class="list">
          @for (m of matches(); track m.id) {
            <li>
              <a [routerLink]="['/result', m.id]" class="card row">
                <div class="row__main">
                  <div class="row__names">
                    {{ m.boyLabel ?? 'A' }}
                    <span class="row__amp">&</span>
                    {{ m.girlLabel ?? 'B' }}
                  </div>
                  <div class="row__meta muted small">
                    {{ m.createdAt.substring(0, 10) }}
                  </div>
                </div>
                <div class="row__score" [class]="'row__score--' + m.recommendation">
                  <div class="row__points mono">{{ formatScore(m.result.totalPoints) }}</div>
                  <div class="row__cat">
                    {{ i18n.t('motherResult.category.' + m.recommendation) }}
                  </div>
                </div>
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: [`
    .history { max-width: 560px; margin: 0 auto; padding-top: 12px; }
    .back { display: inline-block; color: var(--ink-3); font-size: 20px;
            text-decoration: none; margin-bottom: 8px; }
    .title { font-size: 24px; margin: 4px 0 18px; }

    .empty { padding: 30px 20px; text-align: center; }
    .empty p { margin: 0 0 16px; }

    .list { list-style: none; margin: 0; padding: 0; }
    .list li { margin-bottom: 10px; }

    .row {
      display: flex; align-items: center; justify-content: space-between;
      gap: 14px; padding: 14px 16px;
      text-decoration: none !important;
      transition: background .12s ease, border-color .12s ease;
    }
    .row:hover { border-color: var(--gold); }
    .row__main { min-width: 0; }
    .row__names {
      font-size: 15px; font-weight: 600; color: var(--ink);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .row__amp { color: var(--ink-3); margin: 0 6px; }
    .row__meta { margin-top: 2px; }

    .row__score { text-align: right; flex-shrink: 0; }
    .row__points { font-size: 22px; line-height: 1; }
    .row__cat { font-size: 11px; margin-top: 4px; color: var(--ink-3); }
    .row__score--STRONG .row__cat  { color: var(--good); }
    .row__score--MODERATE .row__cat{ color: #d9b45e; }
    .row__score--WEAK .row__cat    { color: var(--bad); }
  `],
})
export class HistoryPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly i18n = inject(I18nService);

  readonly matches = signal<MatchResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.listMatches().subscribe({
      next: ms => {
        // Newest first — persisted createdAt is ISO, so lex sort suffices.
        this.matches.set(ms.slice().sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  formatScore(n: number): string {
    const s = n.toFixed(1);
    return s.endsWith('.0') ? s.slice(0, -2) : s;
  }
}
