import { Component, computed, inject, input } from '@angular/core';
import { ManglikCompatibility, ManglikReference, ManglikStatus } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Manglik (Kuja Dosha) card. Shows both partners' per-reference results
 * (Lagna / Moon / Venus) with the specific Mars house each produced,
 * followed by the couple-level compatibility verdict.
 *
 * <p>Visual states — status is never conveyed by color alone:
 *   NEITHER_MANGLIK         → ✓ good     "Neither partner is Manglik"
 *   REQUIRES_DETAILED_REVIEW→ ✕ serious  "Detailed review required"
 *
 * <p>Explicitly displays a disclaimer that v1 does not implement
 * cancellation rules — the spec forbids the "Manglik-Manglik cancels"
 * shortcut and this card must not appear to make such a claim.
 */
@Component({
  selector: 'app-manglik-card',
  standalone: true,
  template: `
    <div class="manglik card" [class]="'manglik--' + kind()">
      <header class="manglik__head">
        <span class="manglik__icon" aria-hidden="true">{{ icon() }}</span>
        <div>
          <h3 class="manglik__title">{{ i18n.t('match.manglik') }}</h3>
          <p class="manglik__verdict">
            {{ i18n.t('manglik.compat.' + manglik().compatibility) }}
          </p>
        </div>
      </header>

      <div class="manglik__grid">
        <section class="person">
          <h4 class="person__name">
            {{ boyLabel() ?? 'A' }}
            <span class="person__state" [class]="'person__state--' + manglik().personA.status">
              {{ i18n.t('manglik.state.' + manglik().personA.status) }}
            </span>
          </h4>
          <ul class="refs">
            <li>{{ refLine('LAGNA',  manglik().personA) }}</li>
            <li>{{ refLine('MOON',   manglik().personA) }}</li>
            <li>{{ refLine('VENUS',  manglik().personA) }}</li>
          </ul>
          @for (c of manglik().personA.cancellations; track c) {
            <p class="cancel small">{{ i18n.t('manglik.cancellation.' + c) }}</p>
          }
        </section>

        <section class="person">
          <h4 class="person__name">
            {{ girlLabel() ?? 'B' }}
            <span class="person__state" [class]="'person__state--' + manglik().personB.status">
              {{ i18n.t('manglik.state.' + manglik().personB.status) }}
            </span>
          </h4>
          <ul class="refs">
            <li>{{ refLine('LAGNA',  manglik().personB) }}</li>
            <li>{{ refLine('MOON',   manglik().personB) }}</li>
            <li>{{ refLine('VENUS',  manglik().personB) }}</li>
          </ul>
          @for (c of manglik().personB.cancellations; track c) {
            <p class="cancel small">{{ i18n.t('manglik.cancellation.' + c) }}</p>
          }
        </section>
      </div>

      <p class="manglik__note">{{ i18n.t('manglik.note') }}</p>
      <p class="muted small">
        {{ i18n.t('manglik.rules') }}: {{ manglik().ruleVersion }}
      </p>
    </div>
  `,
  styles: [`
    .manglik { padding: 18px 20px; }
    .manglik__head {
      display: flex; align-items: flex-start; gap: 12px;
      margin-bottom: 12px;
    }
    .manglik__icon { font-size: 20px; line-height: 1.2; }
    .manglik__title { font-size: 18px; margin: 0 0 2px; }
    .manglik__verdict {
      margin: 0;
      font-weight: 600;
      font-size: 14px;
      color: var(--ink);
    }

    .manglik__grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin: 10px 0 12px;
    }
    @media (max-width: 560px) { .manglik__grid { grid-template-columns: 1fr; } }

    .person {
      background: var(--surface);
      border: 1px solid var(--border-soft);
      border-radius: var(--radius-sm);
      padding: 12px 14px;
    }
    .person__name {
      font-size: 14px; margin: 0 0 8px;
      display: flex; justify-content: space-between; align-items: baseline;
      gap: 8px;
    }
    .person__state {
      font-size: 12px; font-weight: 600;
      padding: 2px 8px; border-radius: 999px;
      background: var(--surface-3);
    }
    .person__state--NOT_MANGLIK { color: var(--good); }
    .person__state--MANGLIK     { color: var(--bad); }
    .person__state--PARTIAL_MANGLIK { color: #d9b45e; }

    .refs {
      list-style: none;
      margin: 0; padding: 0;
      font-size: 12.5px;
      color: var(--ink-2);
    }
    .refs li { padding: 2px 0; }

    .cancel {
      margin: 6px 0 0;
      color: var(--good);
      font-weight: 600;
      font-size: 12px;
    }

    .manglik__note {
      margin: 10px 0 6px;
      font-size: 12px;
      color: var(--ink-3);
      line-height: 1.5;
    }

    .manglik--good { border-color: rgba(58, 162, 97, .3); }
    .manglik--good .manglik__icon, .manglik--good .manglik__verdict { color: var(--good); }
    .manglik--bad  { border-color: rgba(224, 85, 79, .35); }
    .manglik--bad .manglik__icon, .manglik--bad .manglik__verdict   { color: var(--bad); }
  `],
})
export class ManglikCardComponent {
  readonly i18n = inject(I18nService);
  readonly manglik = input.required<ManglikCompatibility>();
  readonly boyLabel = input<string | null>(null);
  readonly girlLabel = input<string | null>(null);

  readonly kind = computed(() =>
    this.manglik().compatibility === 'NEITHER_MANGLIK' ? 'good' : 'bad');

  readonly icon = computed(() => (this.kind() === 'good' ? '✓' : '✕'));

  /**
   * Render one reference line for a person: "Ascendant: Mars in house 3"
   * or "Ascendant: Mars in house 8 — Manglik".
   */
  refLine(ref: ManglikReference, s: ManglikStatus): string {
    const point = ref === 'LAGNA' ? s.fromLagna
      : ref === 'MOON' ? s.fromMoon
      : s.fromVenus;
    const key = point.present ? 'manglik.house.trigger' : 'manglik.house.n';
    return `${this.i18n.t('manglik.ref.' + ref)}: ${this.i18n.t(key, { n: point.marsHouse })}`;
  }
}
