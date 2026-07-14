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
  // The Android WebView needs an explicit http(s) scheme to satisfy
  // Chromium security assumptions. localhost + https means /api requests
  // still resolve through Nginx on the server (same origin as production).
  server: {
    androidScheme: 'https',
  },
  android: {
    // Portrait-only — mother-mode UI is entirely tuned for portrait.
    // Users on a phone should never see a rotated Kundli report.
    allowMixedContent: false,
  },
};

export default config;
