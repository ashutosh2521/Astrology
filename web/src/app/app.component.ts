import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ApiService } from './core/api.service';
import { I18nService } from './core/i18n.service';
import { PlatformService } from './core/platform.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="shell">
      <!-- Auspicious invocation — placed at the very top, as tradition holds. -->
      <p class="invocation" lang="sa">{{ i18n.t('invocation') }}</p>

      <header class="topbar">
        <a routerLink="/" class="brand">
          <span class="brand__mark">✦</span>
          <span class="brand__name">Kundli</span>
          <span class="brand__sub">{{ i18n.t('brand.sub') }}</span>
        </a>
        <nav class="nav">
          <div class="langs" role="group" aria-label="Language / भाषा">
            <button type="button" lang="hi"
                    [class.is-active]="i18n.lang() === 'hi'"
                    (click)="i18n.setLang('hi')">हिंदी</button>
            <button type="button" lang="en"
                    [class.is-active]="i18n.lang() === 'en'"
                    (click)="i18n.setLang('en')">English</button>
          </div>
        </nav>
      </header>

      <router-outlet />

      <footer class="footer">
        <span class="health" [class.health--down]="!healthy()">
          <span class="health__dot" aria-hidden="true"></span>
          {{ healthy() ? i18n.t('health.up') : i18n.t('health.down') }}
        </span>
        <span class="muted small">{{ i18n.t('footer.note') }}</span>
      </footer>
    </div>
  `,
  styles: [`
    .invocation {
      margin: 0;
      padding: 12px 0 0;
      text-align: center;
      font-family: var(--font-display);
      font-size: 15px;
      letter-spacing: 0.04em;
      color: var(--gold-text);
    }

    .topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      padding: 26px 0 34px;
    }
    .brand {
      display: flex;
      align-items: baseline;
      gap: 10px;
      text-decoration: none !important;
    }
    .brand__mark {
      color: var(--gold-text);
      font-size: 20px;
      transform: translateY(1px);
    }
    .brand__name {
      font-family: var(--font-display);
      font-size: 24px;
      color: var(--ink);
      letter-spacing: 0.02em;
    }
    .brand__sub {
      font-size: 12px;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: var(--ink-3);
    }
    .nav { display: flex; gap: 6px; align-items: center; }
    .langs {
      display: inline-flex;
      margin-left: 12px;
      border: 1px solid var(--border);
      border-radius: 999px;
      overflow: hidden;
    }
    .langs button {
      background: none;
      border: none;
      min-height: 44px;
      padding: 7px 15px;
      font: 600 12.5px/1 var(--font-body);
      color: var(--ink-3);
      cursor: pointer;
      transition: color .12s ease, background .12s ease;
    }
    .langs button.is-active { color: var(--gold-text); background: var(--gold-soft); }
    .langs button:hover:not(.is-active) { color: var(--ink); }
    .nav a {
      color: var(--ink-2);
      font-weight: 600;
      font-size: 14px;
      padding: 9px 16px;
      border-radius: 999px;
      text-decoration: none !important;
      transition: color .12s ease, background .12s ease;
    }
    .nav a:hover { color: var(--ink); }
    .nav a.is-active { color: var(--gold-text); background: var(--gold-soft); }

    .footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-top: 60px;
      padding-top: 18px;
      border-top: 1px solid var(--border-soft);
    }
    .health {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--good);
    }
    .health--down { color: var(--bad); }
    .health__dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: currentColor;
      box-shadow: 0 0 8px currentColor;
    }
    @media (max-width: 640px) {
      .brand__sub { display: none; }
      .footer { flex-direction: column; align-items: flex-start; }
    }
  `],
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly platform = inject(PlatformService);
  readonly i18n = inject(I18nService);
  readonly healthy = signal(true);

  ngOnInit(): void {
    document.documentElement.lang = this.i18n.lang();
    // Android hardware / gesture back = history.back(); root-level back = exit.
    // No-op on the web.
    this.platform.installAndroidBackHandler();
    this.api.health().subscribe({
      next: h => this.healthy.set(h.ephemeris === 'UP'),
      error: () => this.healthy.set(false),
    });
  }
}
