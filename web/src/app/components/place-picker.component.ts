import { Component, ElementRef, OnDestroy, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { City, geoResultToCity, searchCities } from '../core/cities';
import { I18nService } from '../core/i18n.service';

/**
 * Birth-place combobox: type a city name (English or Hindi) and pick from the
 * bundled database — coordinates and timezone come along for free, so users
 * never face raw latitude/longitude fields.
 *
 * The bundled list can't hold every village, so when it returns few matches we
 * also query the backend geocoder (debounced) and append those live results —
 * flagged as "online" — so any town or village still resolves. Emits null when
 * the text no longer corresponds to a selected place.
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
          @for (c of results(); track c.name + c.region + (c.online ? '·o' : ''); let i = $index) {
            <li [id]="inputId() + '-opt-' + i"
                role="option"
                [attr.aria-selected]="i === active()"
                [class.is-active]="i === active()"
                (mousedown)="$event.preventDefault(); pick(c)">
              <span class="options__city">
                {{ label(c) }}
                @if (c.online) { <span class="options__tag" [title]="i18n.t('place.online')">🌐</span> }
              </span>
              <span class="options__region">{{ region(c) }}</span>
            </li>
          } @empty {
            @if (!searching()) {
              <li class="options__none">{{ i18n.t('charts.place.noResults') }}</li>
            }
          }
          @if (searching()) {
            <li class="options__none">{{ i18n.t('place.searching') }}</li>
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
    .options__tag { font-size: 11px; margin-left: 4px; opacity: .8; }
    .options__region { font-size: 12px; color: var(--ink-3); white-space: nowrap; }
    .options__none { color: var(--ink-3); font-size: 13px; cursor: default; }
    .options__none:hover { background: none; }
  `],
})
export class PlacePickerComponent implements OnDestroy {
  readonly i18n = inject(I18nService);
  private readonly api = inject(ApiService);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly inputId = input('place');
  readonly changed = output<City | null>();

  readonly query = signal('');
  readonly results = signal<City[]>([]);
  readonly open = signal(false);
  readonly active = signal(-1);
  readonly searching = signal(false);

  private selected: City | null = null;
  private bundled: City[] = [];

  /** Once bundled matches drop below this, we also ask the online geocoder. */
  private static readonly ONLINE_THRESHOLD = 5;
  private static readonly MAX_RESULTS = 8;

  /** Debounced online-lookup pipeline; the latest query wins (switchMap). */
  private readonly lookups = new Subject<string>();
  private readonly sub: Subscription;

  constructor() {
    this.sub = this.lookups.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(q => {
        this.searching.set(true);
        return this.api.geocode(q).pipe(catchError(() => of([])));
      }),
    ).subscribe(hits => {
      this.searching.set(false);
      this.mergeOnline(hits.map(geoResultToCity));
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

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
    this.bundled = found;
    this.results.set(found);
    this.open.set(q.trim().length >= 2 && !this.selected);
    this.active.set(found.length > 0 ? 0 : -1);
    // Thin bundled coverage → reach out to the geocoder for anything else.
    if (!this.selected && q.trim().length >= 3 && found.length < PlacePickerComponent.ONLINE_THRESHOLD) {
      this.lookups.next(q.trim());
    } else {
      this.searching.set(false);
    }
  }

  /** Append geocoder hits after the bundled matches, de-duplicated and capped. */
  private mergeOnline(online: City[]): void {
    // A pick or a cleared box happened while the request was in flight — ignore.
    if (this.selected || !this.open()) return;
    const seen = new Set(this.bundled.map(c => `${c.name}|${c.region}`.toLowerCase()));
    const extra = online.filter(c => !seen.has(`${c.name}|${c.region}`.toLowerCase()));
    const merged = [...this.bundled, ...extra].slice(0, PlacePickerComponent.MAX_RESULTS);
    this.results.set(merged);
    if (this.active() < 0 && merged.length > 0) this.active.set(0);
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
    this.searching.set(false);
    this.changed.emit(c);
  }

  closeSoon(): void {
    // Delay so option mousedown wins over blur.
    setTimeout(() => this.open.set(false), 120);
  }

  /** Reset after a successful submit. */
  clear(): void {
    this.selected = null;
    this.bundled = [];
    this.query.set('');
    this.results.set([]);
    this.open.set(false);
    this.searching.set(false);
  }

  private display(c: City): string {
    return `${this.label(c)}, ${this.region(c)}`;
  }
}
