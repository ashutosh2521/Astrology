import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor config for the Android wrapper. The Angular build under
 * `dist/web/browser` is the same production build the web deployment
 * uses — Capacitor just packages it into a WebView-hosted native app.
 *
 * On the dev machine:
 *   npm run build          → produces dist/web/browser
 *   npx cap add android    → creates the android/ Gradle project (once)
 *   npx cap sync android   → copies dist/web/browser into android/app/src/main/assets/public
 *   npx cap open android   → opens Android Studio for signing + build
 *
 * See docs/ANDROID_BUILD.md for the full step-by-step.
 */
const config: CapacitorConfig = {
  appId: 'codes.ashutoshkumar.kundli',
  appName: 'Kundli',
  webDir: 'dist/web/browser',
  // The Android WebView needs an explicit http(s) scheme to satisfy Chromium
  // security assumptions, so the bundled app is served from https://localhost.
  // That means relative /api paths resolve to the WebView itself, not the server —
  // ApiService therefore prefixes an absolute production origin in native builds
  // (see PlatformService.apiBaseUrl).
  server: {
    androidScheme: 'https',
  },
  plugins: {
    // Route fetch/XHR through native HTTP in the app. The API calls above are
    // cross-origin (https://localhost -> kundli.ashutoshkumar.codes); native HTTP
    // makes them succeed without a CORS preflight, so the backend needs no
    // Capacitor-specific CORS config. No effect on the web build.
    CapacitorHttp: {
      enabled: true,
    },
  },
  android: {
    // Portrait-only — mother-mode UI is entirely tuned for portrait.
    // Users on a phone should never see a rotated Kundli report.
    allowMixedContent: false,
  },
};

export default config;
