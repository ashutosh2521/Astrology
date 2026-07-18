import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { ChartResponse, DoshaStatus, KootaScore, ManglikStatus, MatchResponse } from '../core/models';
import { I18nService } from '../core/i18n.service';

interface Bundle {
  match: MatchResponse;
  boy: ChartResponse | null;
  girl: ChartResponse | null;
}

/**
 * Print-friendly bilingual report at /print/:id. Covers the 18 blocks
 * the brief §17 lists, laid out for A4-scale printing:
 *
 *   Page 1 — header + names, hero total + category, three dosha pills,
 *            both birth profiles, warnings, strengths, points needing
 *            attention.
 *   Page 2 — 8-koota table, Nadi/Bhakoot detail, Manglik per-reference
 *            grid, calculation metadata (ayanamsa, rule versions,
 *            ephemeris mode), full disclaimer.
 *
 * Uses the browser's native print stack — no server-side PDF library,
 * no font-bundling problem. Hindi and English both render via the app's
 * existing web fonts. The user's "Save as PDF" is one browser dialog away.
 *
 * Auto-fires window.print() once the data lands; a manual button also
 * appears (screen-only) in case the first dialog is dismissed.
 */
@Component({
  selector: 'app-print-report',
  standalone: true,
  imports: [RouterLink],
  template: `
    <!-- Screen-only toolbar; hidden on print via media query below. -->
    <div class="toolbar no-print">
      <a routerLink="/result/{{ match()?.id }}" class="btn btn--ghost btn--sm">← {{ i18n.t('report.close') }}</a>
      <button type="button" class="btn btn--primary btn--sm" (click)="doPrint()">
        {{ i18n.t('report.print') }}
      </button>
    </div>

    @if (loading()) {
      <div class="skeleton no-print" style="height:400px; max-width:720px; margin:60px auto;"></div>
    } @else {
      @if (bundle(); as b) {
        <article class="report">
          <!-- ===== PAGE 1 ===== -->

          <header class="report__head">
            <p class="report__invocation" lang="sa">{{ i18n.t('invocation') }}</p>
            <h1 class="report__title">{{ i18n.t('report.title') }}</h1>
            <p class="report__names">
              {{ b.boy?.label ?? '—' }} <span class="amp">&</span> {{ b.girl?.label ?? '—' }}
            </p>
            <p class="report__sub muted small">{{ i18n.t('report.subtitle') }}</p>
          </header>

          <section class="hero" [class]="'hero--' + b.match.recommendation">
            <div class="hero__number">
              <span class="hero__points mono">{{ formatScore(b.match.result.totalPoints) }}</span>
              <span class="hero__of">/ {{ b.match.result.maxPoints.toFixed(0) }}</span>
            </div>
            <div class="hero__label small muted">{{ i18n.t('report.total') }}</div>
            <div class="hero__cat">
              {{ i18n.t('motherResult.category.' + b.match.recommendation) }}
            </div>
          </section>

          <section class="pills">
            <div class="pill" [class]="'pill--' + doshaKind(nadi(b))">
              <div class="pill__label small">{{ i18n.t('motherResult.nadi') }}</div>
              <div class="pill__value">{{ doshaLabel(nadi(b)) }}</div>
            </div>
            <div class="pill" [class]="'pill--' + doshaKind(bhakoot(b))">
              <div class="pill__label small">{{ i18n.t('motherResult.bhakoot') }}</div>
              <div class="pill__value">{{ doshaLabel(bhakoot(b)) }}</div>
            </div>
            <div class="pill" [class]="'pill--' + manglikKind(b)">
              <div class="pill__label small">{{ i18n.t('motherResult.manglik') }}</div>
              <div class="pill__value">{{ manglikCoupleLabel(b) }}</div>
            </div>
          </section>

          <section class="profiles">
            <h2 class="section-title">{{ i18n.t('report.section.profiles') }}</h2>
            <div class="profiles__grid">
              <div class="profile">
                <div class="profile__name">{{ b.boy?.label ?? '—' }}</div>
                <dl>
                  <dt>{{ i18n.t('report.person.date') }}</dt>
                  <dd>{{ profileDate(b.boy) }}</dd>
                  <dt>{{ i18n.t('report.person.time') }}</dt>
                  <dd>{{ profileTime(b.boy) }}</dd>
                  <dt>{{ i18n.t('report.person.place') }}</dt>
                  <dd>{{ b.boy?.placeName ?? '—' }}</dd>
                  <dt>{{ i18n.t('report.person.timezone') }}</dt>
                  <dd>{{ b.boy?.timezone ?? '—' }}</dd>
                  <dt>{{ i18n.t('report.person.moonRashi') }}</dt>
                  <dd>{{ i18n.rashi(b.boy?.moonRashi ?? '') }} — {{ i18n.nakshatra(b.boy?.moonNakshatra ?? '') }} ({{ i18n.t('report.person.pada') }} {{ b.boy?.moonPada }})</dd>
                </dl>
                @if (isAshutosh(b.boy)) {
                  <p class="profile__note small">{{ i18n.t('report.ashutosh.timeConfirm') }}</p>
                }
              </div>
              <div class="profile">
                <div class="profile__name">{{ b.girl?.label ?? '—' }}</div>
                <dl>
                  <dt>{{ i18n.t('report.person.date') }}</dt>
                  <dd>{{ profileDate(b.girl) }}</dd>
                  <dt>{{ i18n.t('report.person.time') }}</dt>
                  <dd>{{ profileTime(b.girl) }}</dd>
                  <dt>{{ i18n.t('report.person.place') }}</dt>
                  <dd>{{ b.girl?.placeName ?? '—' }}</dd>
                  <dt>{{ i18n.t('report.person.timezone') }}</dt>
                  <dd>{{ b.girl?.timezone ?? '—' }}</dd>
                  <dt>{{ i18n.t('report.person.moonRashi') }}</dt>
                  <dd>{{ i18n.rashi(b.girl?.moonRashi ?? '') }} — {{ i18n.nakshatra(b.girl?.moonNakshatra ?? '') }} ({{ i18n.t('report.person.pada') }} {{ b.girl?.moonPada }})</dd>
                </dl>
              </div>
            </div>
          </section>

          <section class="split">
            <div>
              <h2 class="section-title">{{ i18n.t('report.strengths') }}</h2>
              <ul class="bullet good">
                @for (s of strengths(b); track s) { <li>{{ s }}</li> }
                @if (strengths(b).length === 0) { <li class="muted">{{ i18n.t('noneListed') }}</li> }
              </ul>
            </div>
            <div>
              <h2 class="section-title">{{ i18n.t('report.attention') }}</h2>
              <ul class="bullet warn">
                @for (a of attentionItems(b); track a) { <li>{{ a }}</li> }
                @if (attentionItems(b).length === 0) { <li class="muted">{{ i18n.t('noneListed') }}</li> }
              </ul>
            </div>
          </section>

          <!-- Explicit page break so the koota table starts fresh on page 2. -->
          <div class="page-break"></div>

          <!-- ===== PAGE 2 ===== -->

          <section>
            <h2 class="section-title">{{ i18n.t('report.section.kootas') }}</h2>
            <table class="koota-table">
              <thead>
                <tr>
                  <th>{{ i18n.t('report.koota.header.koota') }}</th>
                  <th>{{ i18n.t('report.koota.header.boy') }}</th>
                  <th>{{ i18n.t('report.koota.header.girl') }}</th>
                  <th class="right">{{ i18n.t('report.koota.header.score') }}</th>
                </tr>
              </thead>
              <tbody>
                @for (k of b.match.result.kootas; track k.code) {
                  <tr>
                    <td>{{ i18n.koota(k.koota) }}</td>
                    <td>{{ k.personAValue }}</td>
                    <td>{{ k.personBValue }}</td>
                    <td class="right mono">{{ k.points }} / {{ k.maxPoints }}</td>
                  </tr>
                }
                <tr class="total-row">
                  <td colspan="3"><strong>{{ i18n.t('report.total') }}</strong></td>
                  <td class="right mono"><strong>{{ formatScore(b.match.result.totalPoints) }} / {{ b.match.result.maxPoints.toFixed(0) }}</strong></td>
                </tr>
              </tbody>
            </table>
          </section>

          <section>
            <h2 class="section-title">{{ i18n.t('report.section.doshas') }}</h2>
            <ul class="dosha-list">
              @for (d of b.match.result.doshas; track d.name) {
                <li>
                  <span class="dosha-list__name">{{ i18n.doshaName(d.name) }}:</span>
                  {{ doshaLabel(d) }}
                  <span class="muted small">— {{ i18n.doshaReason(d.reason) }}</span>
                </li>
              }
            </ul>
          </section>

          @if (b.match.manglik) {
            <section>
              <h2 class="section-title">{{ i18n.t('report.section.manglik') }}</h2>
              <table class="manglik-table">
                <thead>
                  <tr>
                    <th>{{ i18n.t('report.manglik.person') }}</th>
                    <th>{{ i18n.t('report.manglik.state') }}</th>
                    <th>{{ i18n.t('report.manglik.fromLagna') }}</th>
                    <th>{{ i18n.t('report.manglik.fromMoon') }}</th>
                    <th>{{ i18n.t('report.manglik.fromVenus') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>{{ b.boy?.label ?? 'A' }}</td>
                    <td>{{ i18n.t('manglik.state.' + b.match.manglik.personA.status) }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personA, 'LAGNA') }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personA, 'MOON') }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personA, 'VENUS') }}</td>
                  </tr>
                  <tr>
                    <td>{{ b.girl?.label ?? 'B' }}</td>
                    <td>{{ i18n.t('manglik.state.' + b.match.manglik.personB.status) }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personB, 'LAGNA') }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personB, 'MOON') }}</td>
                    <td>{{ manglikRefLabel(b.match.manglik.personB, 'VENUS') }}</td>
                  </tr>
                </tbody>
              </table>
              @if (b.match.manglik.personA.cancellations.length > 0
                  || b.match.manglik.personB.cancellations.length > 0) {
                <ul class="cancel-list">
                  @for (c of b.match.manglik.personA.cancellations; track c) {
                    <li><strong>{{ b.boy?.label ?? 'A' }}:</strong>
                        {{ i18n.t('manglik.cancellation.' + c) }}</li>
                  }
                  @for (c of b.match.manglik.personB.cancellations; track c) {
                    <li><strong>{{ b.girl?.label ?? 'B' }}:</strong>
                        {{ i18n.t('manglik.cancellation.' + c) }}</li>
                  }
                </ul>
              }
              <p class="small muted">{{ i18n.t('manglik.note') }}</p>
            </section>
          }

          <section>
            <h2 class="section-title">{{ i18n.t('report.section.rules') }}</h2>
            <dl class="meta">
              <dt>{{ i18n.t('report.rules.ayanamsa') }}</dt>
              <dd>{{ b.match.ruleMetadata.ayanamsa }}</dd>
              <dt>{{ i18n.t('report.rules.matching') }}</dt>
              <dd>{{ b.match.ruleMetadata.matchingSystem }}</dd>
              <dt>{{ i18n.t('report.rules.ashtakoota') }}</dt>
              <dd>{{ b.match.ruleMetadata.ashtakootaRuleVersion }}</dd>
              <dt>{{ i18n.t('report.rules.manglik') }}</dt>
              <dd>{{ b.match.ruleMetadata.manglikRuleVersion }}</dd>
              <dt>{{ i18n.t('report.rules.ephemeris') }}</dt>
              <dd>{{ b.match.ruleMetadata.ephemerisMode }}</dd>
              <dt>{{ i18n.t('report.rules.reportId') }}</dt>
              <dd class="mono">match-{{ b.match.id }}</dd>
              <dt>{{ i18n.t('report.rules.printedAt') }}</dt>
              <dd>{{ printedAt() }}</dd>
            </dl>
          </section>

          <section class="disclaimer">
            <h2 class="section-title">{{ i18n.t('report.disclaimer.title') }}</h2>
            <p>{{ i18n.t('motherResult.disclaimer') }}</p>
          </section>
        </article>
      }
    }
  `,
  styles: [`
    :host { display: block; background: white; color: #111; }

    .toolbar {
      display: flex; justify-content: space-between; gap: 12px;
      max-width: 800px; margin: 12px auto; padding: 0 16px;
    }

    .report {
      max-width: 800px;
      margin: 0 auto;
      background: white;
      color: #111;
      padding: 32px 44px 40px;
      box-sizing: border-box;
      font-family: var(--font-body);
    }

    .report__head { border-bottom: 2px solid #b8862d; padding-bottom: 14px; margin-bottom: 22px; }
    .report__invocation { margin: 0 0 8px; font-family: var(--font-display); font-size: 15px;
                          letter-spacing: 0.04em; color: #b8862d; }
    .report__title { font-family: var(--font-display); font-size: 26px; margin: 0; color: #111; }
    .report__names { font-size: 18px; margin: 6px 0 2px; color: #111; }
    .amp { color: #888; margin: 0 4px; }
    .report__sub { margin: 0; }

    .hero {
      display: flex; flex-direction: column; align-items: center;
      padding: 18px 12px 14px;
      border: 1px solid #d5d5d5;
      border-radius: 8px;
      margin-bottom: 16px;
    }
    .hero--STRONG   { border-color: #7cba98; }
    .hero--MODERATE { border-color: #d9b45e; }
    .hero--WEAK     { border-color: #e0554f; }
    .hero__number { display: flex; align-items: baseline; gap: 4px; }
    .hero__points { font-family: var(--font-display); font-size: 40px; line-height: 1; color: #111; }
    .hero__of { color: #666; font-size: 18px; }
    .hero__label { text-transform: uppercase; letter-spacing: .14em; color: #666; margin: 4px 0; }
    .hero__cat { font-size: 17px; font-weight: 700; margin-top: 6px; }
    .hero--STRONG .hero__cat   { color: #3a8b60; }
    .hero--MODERATE .hero__cat { color: #a1791d; }
    .hero--WEAK .hero__cat     { color: #c14a44; }

    .pills { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 22px; }
    .pill {
      border: 1px solid #d5d5d5;
      border-radius: 6px;
      padding: 8px 10px;
      text-align: center;
      background: #fafafa;
    }
    .pill__label { color: #666; }
    .pill__value { font-weight: 600; font-size: 14px; margin-top: 2px; }
    .pill--good { border-color: #7cba98; }
    .pill--good .pill__value { color: #3a8b60; }
    .pill--warn { border-color: #d9b45e; }
    .pill--warn .pill__value { color: #a1791d; }
    .pill--bad { border-color: #e0554f; }
    .pill--bad .pill__value { color: #c14a44; }

    .section-title {
      font-family: var(--font-display);
      font-size: 15px;
      margin: 18px 0 8px;
      color: #111;
      border-bottom: 1px solid #d5d5d5;
      padding-bottom: 4px;
    }

    .profiles__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .profile { border: 1px solid #eaeaea; border-radius: 6px; padding: 12px 14px; }
    .profile__name { font-weight: 600; font-size: 15px; margin-bottom: 6px; }
    .profile dl { display: grid; grid-template-columns: max-content 1fr; gap: 4px 12px; margin: 0; font-size: 12.5px; }
    .profile dt { color: #666; }
    .profile dd { margin: 0; color: #111; }
    .profile__note { color: #a1791d; margin: 8px 0 0; font-style: italic; }

    .split { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    ul.bullet { margin: 0; padding-left: 18px; font-size: 13px; }
    ul.bullet li { margin: 3px 0; }
    ul.bullet.good li { color: #2a6b46; }
    ul.bullet.warn li { color: #8a3d38; }
    ul.bullet li.muted { color: #999; list-style: none; margin-left: -18px; }

    .page-break { break-before: page; page-break-before: always; height: 0; }

    table.koota-table, table.manglik-table {
      width: 100%; border-collapse: collapse; font-size: 12.5px; margin: 4px 0 8px;
    }
    .koota-table th, .koota-table td,
    .manglik-table th, .manglik-table td {
      border: 1px solid #ddd; padding: 6px 8px; text-align: left;
    }
    .koota-table th, .manglik-table th { background: #f4f0e6; font-weight: 600; }
    .koota-table .right, .manglik-table .right { text-align: right; }
    .koota-table .total-row td { background: #faf6ea; font-weight: 600; }

    ul.dosha-list { list-style: none; padding: 0; margin: 0 0 6px; font-size: 13px; }
    ul.dosha-list li { padding: 3px 0; }
    .dosha-list__name { font-weight: 600; margin-right: 4px; }

    ul.cancel-list { padding-left: 18px; margin: 4px 0 6px; font-size: 12.5px; color: #2a6b46; }
    ul.cancel-list li { margin: 2px 0; }

    dl.meta {
      display: grid; grid-template-columns: max-content 1fr max-content 1fr;
      column-gap: 16px; row-gap: 4px; font-size: 12px; margin: 0;
    }
    dl.meta dt { color: #666; }
    dl.meta dd { margin: 0; }

    .disclaimer {
      margin-top: 20px;
      background: #f8f5ec;
      border: 1px solid #e6d9a8;
      border-radius: 6px;
      padding: 12px 16px;
    }
    .disclaimer p { margin: 0; font-size: 12.5px; line-height: 1.55; color: #4a3d18; }

    .mono { font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, monospace; }
    .small { font-size: 11.5px; }
    .muted { color: #999; }
    .right { text-align: right; }

    /* ---------- @media print ---------- */
    @media print {
      :host, body { background: white !important; }
      .no-print, app-root > .shell > .topbar, app-root > .shell > .footer { display: none !important; }
      .report { padding: 0 !important; max-width: none !important; }
      /* Force backgrounds to print — Chromium honours color-adjust: exact */
      * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
      @page { size: A4; margin: 14mm 14mm 16mm; }
      .section-title { break-after: avoid; }
      table, .profile, .pill, .disclaimer { break-inside: avoid; }
    }
  `],
})
export class PrintReportPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  readonly i18n = inject(I18nService);

  readonly match = signal<MatchResponse | null>(null);
  readonly bundle = signal<Bundle | null>(null);
  readonly loading = signal(true);
  readonly printedAt = signal<string>(new Date().toLocaleString());

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.loading.set(false); return; }
    this.api.getMatch(id).pipe(
      switchMap(m => forkJoin({
        match: of(m),
        boy: this.api.getChart(m.boyChartId),
        girl: this.api.getChart(m.girlChartId),
      })),
    ).subscribe({
      next: b => {
        this.match.set(b.match);
        this.bundle.set(b);
        this.loading.set(false);
        // Auto-fire the print dialog once layout settles. Wrapping in
        // requestAnimationFrame + a small timeout gives the browser room
        // to lay out fonts and grids before it snapshots the page.
        requestAnimationFrame(() => setTimeout(() => window.print(), 200));
      },
      error: () => this.loading.set(false),
    });
  }

  doPrint(): void {
    window.print();
  }

  formatScore(n: number): string {
    const s = n.toFixed(1);
    return s.endsWith('.0') ? s.slice(0, -2) : s;
  }

  profileDate(c: ChartResponse | null): string {
    if (!c?.birthLocalDateTime) return '—';
    return c.birthLocalDateTime.substring(0, 10);
  }

  profileTime(c: ChartResponse | null): string {
    if (!c?.birthLocalDateTime) return '—';
    // ISO local looks like 1998-10-25T16:20 or 1998-10-25T16:20:00.
    const t = c.birthLocalDateTime.substring(11, 19);
    return t.length >= 5 ? t : '—';
  }

  /**
   * Heuristic Ashutosh detection to trigger the "confirmed 16:20 (4:20 PM)
   * — not 4:20 AM" note the brief mandates. Case-insensitive name match on
   * "Ashutosh" — deliberately narrow so the note never appears on the
   * bride's card.
   */
  isAshutosh(c: ChartResponse | null): boolean {
    if (!c?.label) return false;
    return c.label.toLowerCase().includes('ashutosh')
        || c.label.includes('अशुतोष');
  }

  nadi(b: Bundle): DoshaStatus | undefined {
    return b.match.result.doshas.find(d => d.name === 'Nadi');
  }

  bhakoot(b: Bundle): DoshaStatus | undefined {
    return b.match.result.doshas.find(d => d.name === 'Bhakoot');
  }

  doshaKind(d: DoshaStatus | undefined): 'good' | 'warn' | 'bad' {
    if (!d || !d.present) return 'good';
    return d.cancelled ? 'warn' : 'bad';
  }

  doshaLabel(d: DoshaStatus | undefined): string {
    if (!d || !d.present) return this.i18n.t('motherResult.dosha.no');
    if (d.cancelled) return this.i18n.t('motherResult.dosha.cancelled');
    return this.i18n.t('motherResult.dosha.yes');
  }

  manglikKind(b: Bundle): 'good' | 'bad' {
    const m = b.match.manglik;
    return m && m.compatibility === 'NEITHER_MANGLIK' ? 'good' : 'bad';
  }

  manglikCoupleLabel(b: Bundle): string {
    const m = b.match.manglik;
    if (!m) return '—';
    return this.i18n.t('motherResult.manglik.' + m.compatibility);
  }

  manglikRefLabel(s: ManglikStatus, ref: 'LAGNA' | 'MOON' | 'VENUS'): string {
    const point = ref === 'LAGNA' ? s.fromLagna
      : ref === 'MOON' ? s.fromMoon
      : s.fromVenus;
    const house = this.i18n.t('report.manglik.house', { n: point.marsHouse });
    // Star the trigger so it's visually obvious in monochrome too.
    return point.present ? `${house} ★` : house;
  }

  /** Rules-derived "positive points" the report can safely list. */
  strengths(b: Bundle): string[] {
    const s: string[] = [];
    const n = this.nadi(b);
    if (!n || !n.present) s.push(this.i18n.t('report.strength.nadiOk'));
    const bh = this.bhakoot(b);
    if (!bh || !bh.present) s.push(this.i18n.t('report.strength.bhakootOk'));
    if (b.match.manglik?.compatibility === 'NEITHER_MANGLIK') {
      s.push(this.i18n.t('report.strength.manglikOk'));
    }
    // High-confidence Kootas: only flag when the koota scored FULL.
    for (const k of b.match.result.kootas) {
      if (k.points === k.maxPoints) {
        if (k.code === 'VARNA') s.push(this.i18n.t('report.strength.varnaOk'));
        else if (k.code === 'TARA') s.push(this.i18n.t('report.strength.taraFull'));
      }
    }
    if (b.match.recommendation === 'STRONG') {
      s.push(this.i18n.t('report.strength.categoryStrong'));
    }
    return s;
  }

  attentionItems(b: Bundle): string[] {
    const a: string[] = [];
    const n = this.nadi(b);
    if (n && n.present && !n.cancelled) a.push(this.i18n.t('report.attention.nadi'));
    const bh = this.bhakoot(b);
    if (bh && bh.present && !bh.cancelled) a.push(this.i18n.t('report.attention.bhakoot'));
    if (b.match.manglik && b.match.manglik.compatibility === 'REQUIRES_DETAILED_REVIEW') {
      a.push(this.i18n.t('report.attention.manglik'));
    }
    if (b.match.recommendation === 'WEAK') {
      a.push(this.i18n.t('report.attention.categoryWeak'));
    }
    // Aggregate boundary + historical-tz warnings from both charts.
    const boundary = [b.boy, b.girl].some(c => c?.warnings.some(w => w.toLowerCase().includes('boundary')));
    if (boundary) a.push(this.i18n.t('report.attention.boundary'));
    const historical = [b.boy, b.girl].some(c => c?.warnings.some(w => w.toLowerCase().includes('historical')));
    if (historical) a.push(this.i18n.t('report.attention.historicalTz'));
    return a;
  }
}
