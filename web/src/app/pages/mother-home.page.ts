import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ProfileResponse } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Mother-mode home. Hindi-first, mobile-first, three states:
 *
 *   1. Loading         — skeleton while /api/profile returns.
 *   2. Not configured  — first-run wizard link ("Set up your son's chart first").
 *   3. Configured      — primary chart summary + [नई कुंडली मिलाएँ] + [पिछले मिलान देखें].
 *
 * Deliberately narrow: no charts list, no match picker, no advanced
 * settings. Advanced flows live under /advanced/* for dev/admin use.
 */
@Component({
  selector: 'app-mother-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="mother-home">
      <header class="hero">
        <h1 class="hero__namaste">{{ i18n.t('mother.home.namaste') }}</h1>
      </header>

      @if (loading()) {
        <div class="skeleton" style="height: 90px"></div>
        <div class="skeleton" style="height: 60px; margin-top: 12px"></div>
      } @else if (!profile()?.configured) {
        <!-- Setup wizard branch -->
        <div class="card setup">
          <h2 class="setup__title">{{ i18n.t('mother.setup.title') }}</h2>
          <p class="setup__body">{{ i18n.t('mother.setup.body') }}</p>
          <a routerLink="/advanced" class="btn btn--primary">
            {{ i18n.t('mother.setup.cta') }}
          </a>
        </div>
      } @else {
        <!-- Configured branch: the two primary CTAs -->
        <div class="card primary-chart">
          <div class="primary-chart__row">
            <span class="primary-chart__badge" aria-hidden="true">☾</span>
            <div>
              <div class="primary-chart__name">{{ profile()?.primaryChart?.label ?? '—' }}</div>
              <div class="primary-chart__ready muted small">
                {{ i18n.t('mother.home.readyLine', { name: profile()?.primaryChart?.label ?? '' }) }}
              </div>
            </div>
          </div>
        </div>

        <div class="actions">
          <a routerLink="/new-match" class="btn btn--primary btn--big">
            {{ i18n.t('mother.home.newMatch') }}
          </a>
          <a routerLink="/history" class="btn btn--ghost btn--big">
            {{ i18n.t('mother.home.history') }}
          </a>
        </div>

        <p class="muted small mother-home__advanced">
          <a routerLink="/advanced">{{ i18n.t('mother.home.advanced') }}</a>
        </p>
      }
    </section>
  `,
  styles: [`
    .mother-home {
      max-width: 560px;
      margin: 0 auto;
    }
    .hero { text-align: center; padding: 34px 0 20px; }
    .hero__namaste {
      font-size: 30px;
      font-family: var(--font-display);
      color: var(--ink);
      margin: 0;
      line-height: 1.3;
    }

    .setup { padding: 24px 22px; text-align: center; }
    .setup__title { font-size: 20px; margin: 0 0 8px; }
    .setup__body { color: var(--ink-2); margin: 0 0 16px; font-size: 15px; line-height: 1.55; }

    .primary-chart { padding: 20px 22px; margin-bottom: 18px; }
    .primary-chart__row { display: flex; align-items: center; gap: 14px; }
    .primary-chart__badge {
      width: 44px; height: 44px; border-radius: 50%;
      display: inline-flex; align-items: center; justify-content: center;
      background: var(--gold-soft);
      color: var(--gold-text);
      font-size: 22px;
    }
    .primary-chart__name { font-size: 18px; font-weight: 600; color: var(--ink); }
    .primary-chart__ready { margin-top: 2px; }

    .actions { display: flex; flex-direction: column; gap: 14px; margin: 8px 0 22px; }
    .btn--big {
      padding: 18px 22px;
      font-size: 17px;
      justify-content: center;
      text-align: center;
    }

    .mother-home__advanced { text-align: center; margin: 20px 0 0; }
    .mother-home__advanced a { color: var(--ink-3); text-decoration: underline dotted; }

    @media (max-width: 480px) {
      .hero { padding: 26px 0 16px; }
      .hero__namaste { font-size: 26px; }
      .primary-chart, .setup { padding: 18px 16px; }
    }
  `],
})
export class MotherHomePage implements OnInit {
  private readonly api = inject(ApiService);
  readonly i18n = inject(I18nService);

  readonly profile = signal<ProfileResponse | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    // Force Hindi as the Mother-mode default — the switch stays available in
    // the header for anyone (dev, teenage sibling) who prefers English.
    if (this.i18n.lang() !== 'hi') {
      this.i18n.setLang('hi');
    }
    this.api.getProfile().subscribe({
      next: p => { this.profile.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
