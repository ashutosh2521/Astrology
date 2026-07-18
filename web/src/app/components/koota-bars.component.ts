import { Component, inject, input } from '@angular/core';
import { KootaScore } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Per-koota breakdown: thin single-hue bars (magnitude vs. max) on a muted
 * track, rounded data ends, direct labels on every row (name + points), and a
 * hover/tap reveal of the engine's human-readable `detail` explanation.
 */
@Component({
  selector: 'app-koota-bars',
  standalone: true,
  template: `
    <ul class="kootas" role="list">
      @for (k of kootas(); track k.koota) {
        <li class="row" [attr.tabindex]="0">
          <div class="row__head">
            <span class="row__name">{{ i18n.koota(k.koota) }}</span>
            <span class="row__pts mono">
              {{ k.points }}<span class="row__ptsmax"> / {{ k.maxPoints }}</span>
            </span>
          </div>
          <div class="row__track" aria-hidden="true">
            <div class="row__fill"
                 [class.row__fill--zero]="k.points === 0"
                 [style.width.%]="k.maxPoints > 0 ? (k.points / k.maxPoints) * 100 : 0"></div>
          </div>
          <!--
            Per-person values (e.g. Gana: Rakshasa vs Deva, Yoni: Horse vs
            Sheep). This is the concrete "why" behind each score, previously
            only visible in the print table.
          -->
          <div class="row__values">
            <span class="val">
              @if (boyLabel()) { <span class="val__who">{{ boyLabel() }}:</span> }
              <span class="val__v">{{ i18n.kootaValue(k.code, k.personAValue) }}</span>
            </span>
            <span class="val__vs" aria-hidden="true">↔</span>
            <span class="val">
              @if (girlLabel()) { <span class="val__who">{{ girlLabel() }}:</span> }
              <span class="val__v">{{ i18n.kootaValue(k.code, k.personBValue) }}</span>
            </span>
          </div>
          <div class="row__detail">{{ k.detail }}</div>
        </li>
      }
    </ul>
  `,
  styles: [`
    .kootas {
      list-style: none;
      margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 14px;
    }
    .row { outline: none; border-radius: var(--radius-sm); }
    .row__head {
      display: flex; justify-content: space-between; align-items: baseline;
      margin-bottom: 6px;
    }
    .row__name { font-weight: 600; font-size: 14px; color: var(--ink); }
    .row__pts { font-size: 14px; color: var(--ink); }
    .row__ptsmax { color: var(--ink-3); font-size: 12.5px; }
    .row__track {
      height: 7px;
      border-radius: 4px;
      background: var(--surface-3);
      overflow: hidden;
    }
    .row__fill {
      height: 100%;
      border-radius: 4px;
      background: var(--gold);
      min-width: 0;
      transition: width .7s cubic-bezier(.22,.8,.36,1);
    }
    /* A zero score still shows a 4px nub so "0" is visibly anchored, not missing */
    .row__fill--zero { min-width: 4px; background: var(--surface-3); }

    .row__values {
      display: flex; align-items: baseline; gap: 8px;
      margin-top: 7px;
      font-size: 13px;
    }
    .val { color: var(--ink-2); }
    .val__who { color: var(--ink-3); font-size: 11.5px; margin-right: 3px; }
    .val__v { font-weight: 600; color: var(--ink); }
    .val__vs { color: var(--ink-3); font-size: 12px; }

    .row__detail {
      max-height: 0;
      overflow: hidden;
      font-size: 12.5px;
      color: var(--ink-3);
      transition: max-height .22s ease, margin-top .22s ease;
    }
    .row:hover .row__detail,
    .row:focus .row__detail,
    .row:focus-within .row__detail {
      max-height: 4em;
      margin-top: 6px;
    }
  `],
})
export class KootaBarsComponent {
  readonly i18n = inject(I18nService);
  readonly kootas = input.required<KootaScore[]>();
  /** Optional person names shown beside each per-person value. */
  readonly boyLabel = input<string | null>(null);
  readonly girlLabel = input<string | null>(null);
}
