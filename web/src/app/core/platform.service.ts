import { Injectable } from '@angular/core';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { Share, ShareOptions } from '@capacitor/share';

/**
 * Isolates the "am I running inside Capacitor's WebView?" test and the
 * two native integrations we need. Kept as one file so the rest of the
 * app never imports `@capacitor/*` directly.
 *
 * <p>Every method degrades gracefully to a browser equivalent:
 * - {@link installAndroidBackHandler} is a no-op on the web.
 * - {@link share} uses Capacitor's native share sheet on Android/iOS,
 *   the browser's Web Share API when the app runs in a mobile browser
 *   that supports it, and a WhatsApp deep-link everywhere else.
 */
@Injectable({ providedIn: 'root' })
export class PlatformService {

  /** True when running inside a Capacitor WebView (Android or iOS build). */
  isNative(): boolean {
    return Capacitor.isNativePlatform();
  }

  /**
   * Origin to prefix backend API calls with.
   *
   * <p>Web build → {@code ''}: Spring Boot serves the SPA and {@code /api} from the
   * same origin, so relative paths resolve correctly.
   *
   * <p>Native build → the absolute production origin: the Capacitor WebView serves the
   * bundled app from {@code https://localhost}, so a relative {@code /api/...} would hit
   * the WebView's own assets (where no backend exists) and fail. These requests go
   * cross-origin to the deployed API; the CapacitorHttp plugin (enabled in
   * {@code capacitor.config.ts}) proxies them natively, so there is no CORS preflight
   * against the server. If the deployment domain ever changes, update it here.
   */
  readonly apiBaseUrl = Capacitor.isNativePlatform()
    ? 'https://kundli.ashutoshkumar.codes'
    : '';

  /**
   * Wire Android's hardware/gesture back button to the browser's history
   * stack. If there's nothing to go back to, exit the app rather than
   * getting stuck on the home page.
   *
   * <p>Web builds never get this handler — the browser's native back
   * behaviour is already what we want there.
   */
  installAndroidBackHandler(): void {
    if (!this.isNative()) {
      return;
    }
    // The listener is registered once for the app's lifetime. We don't
    // remove it — the process dies with the WebView on Android exit.
    App.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back();
      } else {
        App.exitApp();
      }
    });
  }

  /**
   * Share text (and optionally a title + URL). Native share sheet on
   * Android/iOS; Web Share API in the browser; WhatsApp URL fallback.
   * Never throws — a share failure is surfaced by the WhatsApp fallback
   * so the user's action is never lost.
   */
  async share(opts: { title?: string; text: string; url?: string }): Promise<void> {
    if (this.isNative()) {
      try {
        const payload: ShareOptions = { text: opts.text };
        if (opts.title) payload.title = opts.title;
        if (opts.url)   payload.url   = opts.url;
        await Share.share(payload);
        return;
      } catch {
        // Fall through to WhatsApp URL.
      }
    }

    const nav = navigator as Navigator & { share?: (data: ShareData) => Promise<void> };
    if (nav.share) {
      try {
        await nav.share({ title: opts.title, text: opts.text, url: opts.url });
        return;
      } catch {
        // User dismissed / not permitted — fall through.
      }
    }

    this.whatsappFallback(opts.text);
  }

  private whatsappFallback(text: string): void {
    const url = `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank', 'noopener');
  }
}
