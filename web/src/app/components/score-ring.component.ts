import { Component, computed, input } from '@angular/core';

/**
 * Hero score: a large number with a slim gold progress ring.
 * The ring encodes magnitude only (single-hue meter on a muted track);
 * the verdict text beside it carries the qualitative judgement, so
 * meaning never rides on color alone.
 */
@Component({
  selector: 'app-score-ring',
  standalone: true,
  template: `
    <div class="ring" role="img"
         [attr.aria-label]="'Score ' + points() + ' of ' + max() + ' points'">
      <svg [attr.width]="size" [attr.height]="size" [attr.viewBox]="'0 0 ' + size + ' ' + size">
        <circle class="ring__track"
                [attr.cx]="c" [attr.cy]="c" [attr.r]="r"
                fill="none" stroke-width="7" />
        <circle class="ring__fill"
                [attr.cx]="c" [attr.cy]="c" [attr.r]="r"
                fill="none" stroke-width="7" stroke-linecap="round"
                [attr.stroke-dasharray]="circumference"
                [attr.stroke-dashoffset]="dashOffset()"
                [attr.transform]="'rotate(-90 ' + c + ' ' + c + ')'" />
      </svg>
      <div class="ring__center">
        <div class="ring__points mono">{{ points() }}</div>
        <div class="ring__max">of {{ max() }}</div>
      </div>
    </div>
  `,
  styles: [`
    .ring { position: relative; display: inline-block; }
    .ring__track { stroke: var(--surface-3); }
    .ring__fill {
      stroke: var(--gold);
      filter: drop-shadow(0 0 6px rgba(184, 134, 45, .45));
      transition: stroke-dashoffset .8s cubic-bezier(.22,.8,.36,1);
    }
    .ring__center {
      position: absolute; inset: 0;
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
    }
    .ring__points {
      font-family: var(--font-display);
      font-size: 44px;
      line-height: 1;
      color: var(--ink);
    }
    .ring__max {
      margin-top: 4px;
      font-size: 12px;
      letter-spacing: .1em;
      text-transform: uppercase;
      color: var(--ink-3);
    }
  `],
})
export class ScoreRingComponent {
  readonly points = input.required<number>();
  readonly max = input.required<number>();

  readonly size = 168;
  readonly c = this.size / 2;
  readonly r = this.c - 8;
  readonly circumference = 2 * Math.PI * this.r;

  readonly dashOffset = computed(() => {
    const frac = this.max() > 0 ? Math.min(this.points() / this.max(), 1) : 0;
    return this.circumference * (1 - frac);
  });
}
