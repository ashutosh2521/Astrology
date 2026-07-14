import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { City } from '../core/cities';
import { ProfileResponse } from '../core/models';
import { I18nService } from '../core/i18n.service';
import { PlacePickerComponent } from '../components/place-picker.component';
import { DateFieldComponent } from '../components/date-field.component';

/**
 * Mother-mode new-match form. Asks ONLY for the bride's details.
 *
 * Latitude, longitude and timezone are never shown — the PlacePicker
 * resolves them from the bundled city database, and the manual entry
 * escape hatch lives on /advanced (not here).
 *
 * On submit, this page collects the details and navigates to /confirm
 * carrying them via router state; that keeps the confirmation screen a
 * true safety step (spec §13) and lets the user hit back to edit without
 * losing what they've entered.
 */
@Component({
  selector: 'app-new-match',
  standalone: true,
  imports: [FormsModule, RouterLink, PlacePickerComponent, DateFieldComponent],
  template: `
    <section class="new-match">
      <a routerLink="/" class="back" aria-label="Home">←</a>
      <h1 class="title">{{ i18n.t('newMatch.title') }}</h1>
      <p class="intro muted">{{ i18n.t('newMatch.intro') }}</p>

      @if (needProfile()) {
        <div class="banner banner--warn">
          <span aria-hidden="true">◭</span>
          <div>
            {{ i18n.t('newMatch.needProfile') }}
            <a routerLink="/advanced">{{ i18n.t('mother.setup.cta') }}</a>
          </div>
        </div>
      } @else {
        <form class="card form" (ngSubmit)="next()" #f="ngForm">
          <div class="field">
            <label for="girlName">{{ i18n.t('newMatch.girlName') }}</label>
            <input id="girlName" name="girlName" required
                   [(ngModel)]="form.name"
                   [placeholder]="i18n.t('newMatch.girlNamePh')"
                   autocomplete="off" />
          </div>

          <div class="field">
            <label>{{ i18n.t('newMatch.date') }}</label>
            <app-date-field [ariaLabel]="i18n.t('newMatch.date')"
                            (changed)="form.date = $event" />
          </div>

          <div class="field">
            <label for="time">{{ i18n.t('newMatch.time') }}</label>
            <input id="time" name="time" type="time" required [(ngModel)]="form.time" />
            <span class="hint">{{ i18n.t('newMatch.timeHint') }}</span>
          </div>

          <div class="field">
            <label for="place">{{ i18n.t('newMatch.place') }}</label>
            <app-place-picker inputId="place" (changed)="onCity($event)" />
          </div>

          <button class="btn btn--primary btn--big" type="submit"
                  [disabled]="!canSubmit(f.invalid)">
            {{ i18n.t('newMatch.next') }} →
          </button>
        </form>
      }
    </section>
  `,
  styles: [`
    .new-match { max-width: 560px; margin: 0 auto; padding-top: 12px; }
    .back {
      display: inline-block; color: var(--ink-3);
      font-size: 20px; text-decoration: none;
      margin-bottom: 8px;
    }
    .title { font-size: 26px; margin: 4px 0 6px; }
    .intro { margin: 0 0 20px; line-height: 1.5; }

    .form { display: flex; flex-direction: column; gap: 16px; padding: 20px 22px; }
    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 480px) { .grid-2 { grid-template-columns: 1fr; } }
    .form .btn { justify-content: center; }
    .btn--big { padding: 16px 20px; font-size: 16px; }
  `],
})
export class NewMatchPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  private readonly picker = viewChild(PlacePickerComponent);

  readonly profile = signal<ProfileResponse | null>(null);

  form: {
    name: string;
    date: string;
    time: string;
    city: City | null;
  } = { name: '', date: '', time: '', city: null };

  ngOnInit(): void {
    this.api.getProfile().subscribe({
      next: p => this.profile.set(p),
      error: () => this.profile.set(null),
    });
  }

  needProfile(): boolean {
    const p = this.profile();
    return p != null && !p.configured;
  }

  onCity(c: City | null): void {
    this.form.city = c;
  }

  canSubmit(formInvalid: boolean | null): boolean {
    // The date-field lives outside ngForm; it emits '' until a valid date is set.
    return !formInvalid && this.form.date !== '' && this.form.city != null;
  }

  next(): void {
    const f = this.form;
    if (!f.name || !f.date || !f.time || !f.city) return;
    // Normalise time to hh:mm:00 — the <input type=time> yields hh:mm.
    const time = f.time.length === 5 ? `${f.time}:00` : f.time;
    // Router state carries the whole payload to /confirm. Deliberately not
    // stashed in a service — router navigation lets the user hit browser
    // back and edit without losing the picked city.
    this.router.navigateByUrl('/confirm', {
      state: {
        girl: {
          name: f.name.trim(),
          birthLocalDateTime: `${f.date}T${time}`,
          date: f.date,
          time: f.time,
          city: f.city,
        },
      },
    });
  }
}
