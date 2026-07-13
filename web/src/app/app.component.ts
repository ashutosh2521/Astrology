import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from './core/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <header class="topbar">
        <a routerLink="/" class="brand">
          <span class="brand__mark">✦</span>
          <span class="brand__name">Kundli</span>
          <span class="brand__sub">Vedic Compatibility</span>
        </a>
        <nav class="nav">
          <a routerLink="/" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">Charts</a>
          <a routerLink="/match" routerLinkActive="is-active">Match</a>
        </nav>
      </header>

      <router-outlet />

      <footer class="footer">
        <span class="health" [class.health--down]="!healthy()">
          <span class="health__dot" aria-hidden="true"></span>
          {{ healthy() ? 'Ephemeris: full precision' : 'Ephemeris: unavailable' }}
        </span>
        <span class="muted small">Lahiri ayanamsa · Swiss Ephemeris</span>
      </footer>
    </div>
  `,
  styles: [`
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
    .nav { display: flex; gap: 6px; }
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
  readonly healthy = signal(true);

  ngOnInit(): void {
    this.api.health().subscribe({
      next: h => this.healthy.set(h.ephemeris === 'UP'),
      error: () => this.healthy.set(false),
    });
  }
}
