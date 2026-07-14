import { Component, effect, inject, input, output, signal } from '@angular/core';
import { I18nService } from '../core/i18n.service';

/**
 * Birth-date entry as three explicit fields — day, month (by name), year — in
 * dd · month · yyyy order.
 *
 * Native `<input type="date">` renders in the browser/OS locale, so the same
 * markup shows dd/mm/yyyy for one user and mm/dd/yyyy or yyyy-mm-dd for another
 * — confusing, and its calendar popup is painful for birth dates decades back.
 * Naming the month removes all dd-vs-mm ambiguity, and dropdowns mean almost no
 * typing. Emits an ISO `yyyy-mm-dd` string (or '' while incomplete/invalid) so
 * callers keep the same value shape the old date input produced.
 */
@Component({
  selector: 'app-date-field',
  standalone: true,
  template: `
    <div class="date-field" role="group" [attr.aria-label]="ariaLabel()">
      <label class="date-field__part">
        <span class="date-field__cap">{{ i18n.t('date.day') }}</span>
        <select [attr.aria-label]="i18n.t('date.day')"
                [value]="day() ?? ''" (change)="onDay($any($event.target).value)">
          <option value="" disabled>{{ i18n.t('date.dayPh') }}</option>
          @for (d of days; track d) { <option [value]="d">{{ d }}</option> }
        </select>
      </label>

      <label class="date-field__part date-field__part--month">
        <span class="date-field__cap">{{ i18n.t('date.month') }}</span>
        <select [attr.aria-label]="i18n.t('date.month')"
                [value]="month() ?? ''" (change)="onMonth($any($event.target).value)">
          <option value="" disabled>{{ i18n.t('date.monthPh') }}</option>
          @for (m of months(); track m.num) { <option [value]="m.num">{{ m.name }}</option> }
        </select>
      </label>

      <label class="date-field__part">
        <span class="date-field__cap">{{ i18n.t('date.year') }}</span>
        <input type="number" inputmode="numeric" [attr.aria-label]="i18n.t('date.year')"
               [min]="minYear" [max]="maxYear" [placeholder]="i18n.t('date.yearPh')"
               [value]="year() ?? ''" (input)="onYear($any($event.target).value)" />
      </label>
    </div>
  `,
  styles: [`
    .date-field { display: flex; gap: 10px; align-items: flex-end; }
    .date-field__part { display: flex; flex-direction: column; gap: 4px; flex: 1 1 0; min-width: 0; }
    .date-field__part--month { flex: 1.4 1 0; }
    .date-field__cap { font-size: 11px; color: var(--ink-3); letter-spacing: .02em; }
    .date-field select, .date-field input { width: 100%; }
    /* Kill the number-spinner so the year reads as a plain 4-digit box. */
    .date-field input::-webkit-outer-spin-button,
    .date-field input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
    .date-field input[type=number] { -moz-appearance: textfield; appearance: textfield; }
  `],
})
export class DateFieldComponent {
  readonly i18n = inject(I18nService);

  /** Optional ISO seed (yyyy-mm-dd) to prefill the three parts. */
  readonly value = input<string>('');
  readonly ariaLabel = input<string>('Birth date');

  /** Emits ISO yyyy-mm-dd, or '' whenever the date is incomplete or invalid. */
  readonly changed = output<string>();

  readonly minYear = 1900;
  readonly maxYear = new Date().getFullYear();
  readonly days = Array.from({ length: 31 }, (_, i) => i + 1);

  readonly day = signal<number | null>(null);
  readonly month = signal<number | null>(null);
  readonly year = signal<number | null>(null);

  private lastEmitted = '';

  readonly months = () => {
    const names = this.i18n.lang() === 'hi' ? MONTHS_HI : MONTHS_EN;
    return names.map((name, i) => ({ num: i + 1, name }));
  };

  constructor() {
    // Seed from an incoming ISO value (e.g. when a form is repopulated).
    effect(() => {
      const iso = this.value();
      const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
      if (m) {
        this.year.set(+m[1]);
        this.month.set(+m[2]);
        this.day.set(+m[3]);
        this.lastEmitted = iso;
      }
    });
  }

  onDay(v: string): void { this.day.set(v ? +v : null); this.emit(); }
  onMonth(v: string): void { this.month.set(v ? +v : null); this.emit(); }
  onYear(v: string): void {
    const n = parseInt(v, 10);
    this.year.set(Number.isNaN(n) ? null : n);
    this.emit();
  }

  /** Reset all three parts (used after a successful submit). */
  clear(): void {
    this.day.set(null);
    this.month.set(null);
    this.year.set(null);
    this.lastEmitted = '';
    this.changed.emit('');
  }

  private emit(): void {
    const iso = this.iso();
    if (iso !== this.lastEmitted) {
      this.lastEmitted = iso;
      this.changed.emit(iso);
    }
  }

  /** Valid ISO date, or '' if any part is missing or the day is out of range for the month. */
  private iso(): string {
    const d = this.day(), m = this.month(), y = this.year();
    if (!d || !m || !y || y < this.minYear || y > this.maxYear) return '';
    if (d > daysInMonth(m, y)) return '';
    const mm = String(m).padStart(2, '0');
    const dd = String(d).padStart(2, '0');
    return `${y}-${mm}-${dd}`;
  }
}

function daysInMonth(month: number, year: number): number {
  return new Date(year, month, 0).getDate();
}

export const MONTHS_EN = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export const MONTHS_HI = [
  'जनवरी', 'फ़रवरी', 'मार्च', 'अप्रैल', 'मई', 'जून',
  'जुलाई', 'अगस्त', 'सितंबर', 'अक्टूबर', 'नवंबर', 'दिसंबर',
];

/** Format an ISO yyyy-mm-dd as "dd Month yyyy" in the given language. */
export function formatIsoDate(iso: string, lang: 'en' | 'hi'): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  if (!m) return iso ?? '';
  const names = lang === 'hi' ? MONTHS_HI : MONTHS_EN;
  return `${+m[3]} ${names[+m[2] - 1]} ${m[1]}`;
}
