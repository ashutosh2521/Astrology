import { Component, ElementRef, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { City, searchCities } from '../core/cities';
import { I18nService } from '../core/i18n.service';

/**
 * Birth-place combobox: type a city name (English or Hindi) and pick from the
 * bundled database — coordinates and timezone come along for free, so users
 * never face raw latitude/longitude fields. Emits null when the text no
 * longer corresponds to a selected city.
 */
@Component({
  selector: 'app-place-picker',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="picker">
      <input
        [id]="inputId()"
        type="text"
        role="combobox"
        autocomplete="off"
        [attr.aria-expanded]="open()"
        [attr.aria-activedescendant]="active() >= 0 ? inputId() + '-opt-' + active() : null"
        [placeholder]="i18n.t('charts.place.ph')"
        [ngModel]="query()"
        (ngModelChange)="onQuery($event)"
        (keydown)="onKey($event)"
        (focus)="onQuery(query())"
        (blur)="closeSoon()"
      />
      @if (open()) {
        <ul class="options" role="listbox">
          @for (c of results(); track c.name + c.region; let i = $index) {
            <li [id]="inputId() + '-opt-' + i"
                role="option"
                [attr.aria-selected]="i === active()"
                [class.is-active]="i === active()"
                (mousedown)="$event.preventDefault(); pick(c)">
              <span class="options__city">{{ label(c) }}</span>
              <span class="options__region">{{ region(c) }}</span>
            </li>
          } @empty {
            <li class="options__none">{{ i18n.t('charts.place.noResults') }}</li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    .picker { position: relative; }
    .options {
      position: absolute;
      z-index: 30;
      top: calc(100% + 6px);
      left: 0; right: 0;
      margin: 0; padding: 6px;
      list-style: none;
      background: var(--surface-2);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      box-shadow: var(--shadow);
      max-height: 280px;
      overflow-y: auto;
    }
    .options li {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 10px;
      padding: 9px 11px;
      border-radius: 7px;
      cursor: pointer;
    }
    .options li.is-active, .options li:hover { background: var(--gold-soft); }
    .options__city { font-weight: 600; font-size: 14px; color: var(--ink); }
    .options__region { font-size: 12px; color: var(--ink-3); white-space: nowrap; }
    .options__none { color: var(--ink-3); font-size: 13px; cursor: default; }
    .options__none:hover { background: none; }
  `],
})
export class PlacePickerComponent {
  readonly i18n = inject(I18nService);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly inputId = input('place');
  readonly changed = output<City | null>();

  readonly query = signal('');
  readonly results = signal<City[]>([]);
  readonly open = signal(false);
  readonly active = signal(-1);

  private selected: City | null = null;

  label(c: City): string {
    return this.i18n.lang() === 'hi' ? c.hi : c.name;
  }

  region(c: City): string {
    return this.i18n.lang() === 'hi' ? c.regionHi : c.region;
  }

  onQuery(q: string): void {
    this.query.set(q);
    if (this.selected && q !== this.display(this.selected)) {
      this.selected = null;
      this.changed.emit(null);
    }
    const found = searchCities(q);
    this.results.set(found);
    this.open.set(q.trim().length >= 2 && !this.selected);
    this.active.set(found.length > 0 ? 0 : -1);
  }

  onKey(e: KeyboardEvent): void {
    if (!this.open()) return;
    const n = this.results().length;
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (n > 0) this.active.update(a => (a + 1) % n);
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (n > 0) this.active.update(a => (a - 1 + n) % n);
        break;
      case 'Enter': {
        const c = this.results()[this.active()];
        if (c) { e.preventDefault(); this.pick(c); }
        break;
      }
      case 'Escape':
        this.open.set(false);
        break;
    }
  }

  pick(c: City): void {
    this.selected = c;
    this.query.set(this.display(c));
    this.open.set(false);
    this.changed.emit(c);
  }

  closeSoon(): void {
    // Delay so option mousedown wins over blur.
    setTimeout(() => this.open.set(false), 120);
  }

  /** Reset after a successful submit. */
  clear(): void {
    this.selected = null;
    this.query.set('');
    this.results.set([]);
    this.open.set(false);
  }

  private display(c: City): string {
    return `${this.label(c)}, ${this.region(c)}`;
  }
}
