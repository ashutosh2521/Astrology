import { Component, computed, inject, input } from '@angular/core';
import { DoshaStatus } from '../core/models';
import { I18nService } from '../core/i18n.service';

/**
 * Dosha status card. Three states, each carrying icon + label + reason —
 * status is never conveyed by color alone:
 *   absent    → ✓ good      "No <name> dosha"
 *   cancelled → ◐ warning   "<name> dosha cancelled" + reason
 *   effective → ✕ serious   "<name> dosha applies" + reason
 */
@Component({
  selector: 'app-dosha-card',
  standalone: true,
  template: `
    <div class="dosha card--inset" [class]="'dosha dosha--' + kind()">
      <span class="dosha__icon" aria-hidden="true">{{ icon() }}</span>
      <div>
        <div class="dosha__title">{{ title() }}</div>
        <div class="dosha__reason">{{ i18n.doshaReason(dosha().reason) }}</div>
      </div>
    </div>
  `,
  styles: [`
    .dosha {
      display: flex;
      gap: 13px;
      align-items: flex-start;
      padding: 15px 17px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-soft);
      background: var(--surface);
    }
    .dosha__icon {
      font-size: 16px;
      line-height: 1.4;
    }
    .dosha__title { font-weight: 600; font-size: 14px; }
    .dosha__reason { font-size: 12.5px; color: var(--ink-3); margin-top: 2px; }

    .dosha--good  { border-color: rgba(58, 162, 97, .3); }
    .dosha--good .dosha__icon,  .dosha--good .dosha__title  { color: var(--good); }
    .dosha--warn  { border-color: rgba(179, 134, 28, .35); }
    .dosha--warn .dosha__icon,  .dosha--warn .dosha__title  { color: #d9b45e; }
    .dosha--bad   { border-color: rgba(224, 85, 79, .35); }
    .dosha--bad .dosha__icon,   .dosha--bad .dosha__title   { color: var(--bad); }
  `],
})
export class DoshaCardComponent {
  readonly i18n = inject(I18nService);
  readonly dosha = input.required<DoshaStatus>();

  readonly kind = computed(() => {
    const d = this.dosha();
    if (!d.present) return 'good';
    return d.cancelled ? 'warn' : 'bad';
  });

  readonly icon = computed(() =>
    ({ good: '✓', warn: '◐', bad: '✕' })[this.kind()]);

  title(): string {
    const d = this.dosha();
    const name = this.i18n.doshaName(d.name);
    if (!d.present) return this.i18n.t('dosha.none', { name });
    return d.cancelled
      ? this.i18n.t('dosha.cancelled', { name })
      : this.i18n.t('dosha.applies', { name });
  }
}
