import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AshtakootResult, DoshaStatus, MatchResponse, RecommendationCategory } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Mother-mode result view (spec §14).
 *
 * <p>Deliberately simple:
 *   • Big total-guna number + Hindi recommendation banner.
 *   • Three status pills: Nadi Dosha / Bhakoot Dosha / Manglik status.
 *   • The disclaimer paragraph — this is a preliminary check, not a verdict.
 *
 * <p>Detailed per-koota breakdown, per-reference Manglik grid etc. live
 * on the /advanced page. Mother mode stays a screen my mother can act
 * on in 30 seconds without scrolling.
 */
@Component({
  selector: 'app-mother-result',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="result">
      <a routerLink="/" class="back" aria-label="Home">←</a>

      @if (loading()) {
        <div class="skeleton" style="height: 200px; margin-top: 24px"></div>
      } @else {
        @if (match(); as m) {
        <!-- Hero: total gunas + Hindi banner -->
        <div class="card hero" [class]="'hero--' + m.recommendation">
          <div class="hero__score">
            <span class="hero__points mono">{{ formatScore(m.result.totalPoints) }}</span>
            <span class="hero__of">/ {{ m.result.maxPoints.toFixed(0) }}</span>
          </div>
          <div class="hero__label">{{ i18n.t('motherResult.total') }}</div>
          <div class="hero__category">
            {{ i18n.t('motherResult.category.' + m.recommendation) }}
          </div>
        </div>

        <div class="pills">
          <div class="pill" [class]="'pill--' + doshaKind(nadi())">
            <div class="pill__label">{{ i18n.t('motherResult.nadi') }}</div>
            <div class="pill__value">{{ doshaLabel(nadi()) }}</div>
          </div>
          <div class="pill" [class]="'pill--' + doshaKind(bhakoot())">
            <div class="pill__label">{{ i18n.t('motherResult.bhakoot') }}</div>
            <div class="pill__value">{{ doshaLabel(bhakoot()) }}</div>
          </div>
          <div class="pill" [class]="'pill--' + manglikKind()">
            <div class="pill__label">{{ i18n.t('motherResult.manglik') }}</div>
            <div class="pill__value">{{ manglikLabel() }}</div>
          </div>
        </div>

        <p class="disclaimer">{{ i18n.t('motherResult.disclaimer') }}</p>

        <div class="actions">
          <div class="actions__row">
            <a [routerLink]="['/print', m.id]" target="_blank" rel="noopener"
               class="btn btn--ghost btn--sm">
              {{ i18n.t('motherResult.pdf') }}
            </a>
            <button type="button" class="btn btn--ghost btn--sm" (click)="share(m)">
              {{ i18n.t('motherResult.share') }}
            </button>
          </div>

          <a routerLink="/new-match" class="btn btn--primary">
            {{ i18n.t('motherResult.newMatchAgain') }}
          </a>
          <a routerLink="/" class="btn btn--ghost">
            {{ i18n.t('motherResult.backHome') }}
          </a>
        </div>

        <p class="muted small">{{ i18n.t('match.rules') }}: {{ m.rulesVersion }}</p>

        <p class="muted small">
          <a routerLink="/advanced/match" class="small">
            {{ i18n.t('mother.home.advanced') }}
          </a>
        </p>
        } @else {
          <p class="muted">—</p>
        }
      }
    </section>
  `,
  styles: [`
    .result { max-width: 560px; margin: 0 auto; padding-top: 12px; }
    .back { display: inline-block; color: var(--ink-3); font-size: 20px;
            text-decoration: none; margin-bottom: 8px; }

    .hero {
      padding: 28px 22px;
      text-align: center;
      margin-bottom: 18px;
    }
    .hero__score { display: flex; justify-content: center; align-items: baseline; gap: 6px; }
    .hero__points {
      font-family: var(--font-display);
      font-size: 56px; line-height: 1; color: var(--ink);
    }
    .hero__of { color: var(--ink-3); font-size: 20px; }
    .hero__label {
      color: var(--ink-3); font-size: 12px; letter-spacing: 0.14em;
      text-transform: uppercase; margin-top: 6px;
    }
    .hero__category {
      font-size: 20px; font-weight: 600;
      margin-top: 12px; color: var(--ink);
    }
    .hero--STRONG   { border-color: rgba(58, 162, 97, .35); }
    .hero--STRONG .hero__category   { color: var(--good); }
    .hero--MODERATE { border-color: rgba(179, 134, 28, .35); }
    .hero--MODERATE .hero__category { color: #d9b45e; }
    .hero--WEAK     { border-color: rgba(224, 85, 79, .35); }
    .hero--WEAK .hero__category     { color: var(--bad); }

    .pills {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
      margin-bottom: 18px;
    }
    @media (max-width: 480px) { .pills { grid-template-columns: 1fr; } }
    .pill {
      background: var(--surface); border: 1px solid var(--border-soft);
      border-radius: var(--radius-sm);
      padding: 14px 14px;
      text-align: center;
    }
    .pill__label { font-size: 12px; color: var(--ink-3); margin-bottom: 4px; }
    .pill__value { font-size: 16px; font-weight: 600; }
    .pill--good .pill__value { color: var(--good); }
    .pill--warn .pill__value { color: #d9b45e; }
    .pill--bad .pill__value  { color: var(--bad); }

    .disclaimer {
      background: var(--surface); border: 1px solid var(--border-soft);
      border-radius: var(--radius-sm);
      padding: 14px 16px;
      font-size: 13.5px; line-height: 1.6;
      color: var(--ink-2);
      margin: 8px 0 20px;
    }

    .actions { display: flex; flex-direction: column; gap: 10px; margin-bottom: 18px; }
    .actions__row { display: flex; gap: 10px; margin-bottom: 6px; }
    .actions__row .btn { flex: 1; justify-content: center; }
    .actions .btn { justify-content: center; padding: 14px 18px; font-size: 15px; }
    .actions .btn--sm { padding: 10px 14px; font-size: 13.5px; }
  `],
})
export class MotherResultPage implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  readonly i18n = inject(I18nService);

  readonly match = signal<MatchResponse | null>(null);
  readonly loading = signal(true);

  /**
   * Text-summary share. Uses the native Web Share API when available
   * (Android Chrome, iOS Safari); falls back to a wa.me deep link that
   * opens WhatsApp with the pre-filled message on every other browser.
   * Text mirrors the brief §17 "Example summary" verbatim in structure.
   */
  share(m: MatchResponse): void {
    const text = this.buildSummary(m);
    const nav = navigator as Navigator & { share?: (data: ShareData) => Promise<void> };
    if (nav.share) {
      nav.share({ title: this.i18n.t('report.title'), text }).catch(() => this.whatsapp(text));
    } else {
      this.whatsapp(text);
    }
  }

  private whatsapp(text: string): void {
    const url = `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank', 'noopener');
  }

  private buildSummary(m: MatchResponse): string {
    const title = this.i18n.t('report.title');
    const names = `${m.boyLabel ?? 'A'} ${this.i18n.lang() === 'hi' ? 'एवं' : '&'} ${m.girlLabel ?? 'B'}`;
    const total = `${this.i18n.t('motherResult.total')}: ${this.formatScore(m.result.totalPoints)}/${m.result.maxPoints.toFixed(0)}`;
    const nadi = `${this.i18n.t('motherResult.nadi')}: ${this.doshaLabel(this.nadi())}`;
    const bhakoot = `${this.i18n.t('motherResult.bhakoot')}: ${this.doshaLabel(this.bhakoot())}`;
    const manglik = `${this.i18n.t('motherResult.manglik')}: ${this.manglikLabel()}`;
    const cat = `${this.i18n.t('report.category')}: ${this.i18n.t('motherResult.category.' + m.recommendation)}`;
    const short = this.i18n.lang() === 'hi'
      ? 'यह केवल प्रारंभिक जांच है। विवाह का अंतिम निर्णय विस्तृत परामर्श के बाद लें।'
      : 'This is a preliminary check only. Consult an experienced astrologer before any marriage decision.';
    return [title, names, '', total, nadi, bhakoot, manglik, '', cat, '', short].join('\n');
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.loading.set(false); return; }
    this.http.get<MatchResponse>(`/api/matches/${id}`).subscribe({
      next: m => { this.match.set(m); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  formatScore(n: number): string {
    // "%.1f/…" mirror of the backend verdict; drop the trailing .0 when clean.
    const s = n.toFixed(1);
    return s.endsWith('.0') ? s.slice(0, -2) : s;
  }

  private doshaByName(name: string): DoshaStatus | undefined {
    return this.match()?.result.doshas.find(d => d.name === name);
  }

  nadi(): DoshaStatus | undefined { return this.doshaByName('Nadi'); }
  bhakoot(): DoshaStatus | undefined { return this.doshaByName('Bhakoot'); }

  doshaKind(d: DoshaStatus | undefined): 'good' | 'warn' | 'bad' {
    if (!d || !d.present) return 'good';
    return d.cancelled ? 'warn' : 'bad';
  }

  doshaLabel(d: DoshaStatus | undefined): string {
    if (!d || !d.present) return this.i18n.t('motherResult.dosha.no');
    if (d.cancelled) return this.i18n.t('motherResult.dosha.cancelled');
    return this.i18n.t('motherResult.dosha.yes');
  }

  manglikKind(): 'good' | 'bad' {
    const m = this.match()?.manglik;
    return m && m.compatibility === 'NEITHER_MANGLIK' ? 'good' : 'bad';
  }

  manglikLabel(): string {
    const m = this.match()?.manglik;
    if (!m) return '—';
    return this.i18n.t('motherResult.manglik.' + m.compatibility);
  }
}
