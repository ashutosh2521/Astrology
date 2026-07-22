# Android Build (signed APK for private install)

This is the last-mile procedure for turning the committed Angular + Capacitor
scaffolding into a signed Android APK you can side-load on your mother's phone.
None of the steps below need Play Store publishing.

Runtime doesn't have the Android SDK, so every step here happens on a **dev
machine** with:

- **Node 22.12+** (matches the version pinned in `backend/pom.xml`).
- **Java 17** (Android Gradle Plugin 8.x needs 17 exactly — not 11, not 21).
- **Android Studio Ladybug (2024.2)** or newer, or a standalone `sdkmanager`
  + `platform-tools` install if you prefer the CLI.
- **Android SDK Platform 34** and **build-tools 34.0.0**.

## 1. One-time platform install

```bash
cd web

# Regenerate node_modules from the committed lockfile.
npm ci --no-audit --no-fund

# Create the android/ Gradle project. This is a one-time step — the
# resulting android/ folder is committed to git after this runs.
npx cap add android

# Sanity-check that Capacitor recognised the platform.
npx cap doctor
```

After `cap add android` finishes, you'll have `web/android/` with a Gradle
project pre-wired to `dist/web/browser`. Commit that whole folder EXCEPT the
paths already in `.gitignore` (`.gradle/`, `build/`, `local.properties`, `.idea/`,
`*.iml`, `capacitor-cordova-android-plugins/`).

## 2. Generate the raster PWA icons from the SVG sources

Two scalable SVG sources live in `web/public/icons/`:

- `icon.svg` — the rounded-corner app mark (favicon, Apple touch icon, the
  `"any"` manifest PNGs).
- `icon-maskable.svg` — a **full-bleed** variant whose mark sits inside the
  Android maskable safe zone (central ~80%), so launchers can crop it to a
  circle / squircle without clipping the star.

From these we rasterise: `icon-192.png`, `icon-512.png`,
`icon-maskable.png` (512), `apple-touch-icon.png` (180) and a
multi-resolution `favicon.ico`.

### Option A — one command with Python (recommended, no system deps)

```bash
pip install cairosvg pillow
cd web/public
python3 - <<'PY'
import cairosvg, io
from PIL import Image
def r(p, s):
    png = cairosvg.svg2png(url=p, output_width=s, output_height=s)
    return Image.open(io.BytesIO(png)).convert("RGBA")
r("icons/icon.svg", 192).save("icons/icon-192.png")
r("icons/icon.svg", 512).save("icons/icon-512.png")
r("icons/icon-maskable.svg", 512).save("icons/icon-maskable.png")
r("icons/icon.svg", 180).save("icons/apple-touch-icon.png")
r("icons/icon.svg", 256).save(
    "favicon.ico", format="ICO",
    sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])
PY
```

### Option B — rsvg-convert / ImageMagick / Inkscape

```bash
cd web/public/icons
# pick your tool; repeat for each size
rsvg-convert -w 192 -h 192 icon.svg          -o icon-192.png
rsvg-convert -w 512 -h 512 icon.svg          -o icon-512.png
rsvg-convert -w 512 -h 512 icon-maskable.svg -o icon-maskable.png
rsvg-convert -w 180 -h 180 icon.svg          -o apple-touch-icon.png
# favicon.ico (ImageMagick):
magick -background none icon.svg -define icon:auto-resize=16,32,48,64 ../favicon.ico
```

Commit the generated PNGs and `favicon.ico`. They're small and part of the
release artefact.

## 3. Generate the Android launcher icons

Android's launcher uses its own icon system (`mipmap-*dpi`). Simplest path:

```bash
# In Android Studio, right-click app/src/main/res → New → Image Asset,
# pick the icon.svg as source. Studio writes to every density bucket.
```

Or, if you prefer CLI, use `cordova-res` (works with Capacitor projects):

```bash
npm install --save-dev cordova-res
mkdir -p web/resources
cp web/public/icons/icon.svg web/resources/icon.svg
# cordova-res needs a 1024x1024 PNG source, not SVG:
rsvg-convert -w 1024 -h 1024 web/public/icons/icon.svg \
  -o web/resources/icon.png
cd web && npx cordova-res android --skip-config --copy
```

## 4. One-time signing keystore

Generate ONCE per device you plan to install on. Keep the keystore file
outside the repo (it's in `.gitignore` but be paranoid). If you lose it, the
next release can't upgrade in place; the user has to uninstall + reinstall.

```bash
# Anywhere outside the repo:
keytool -genkey -v -keystore kundli-release.keystore \
        -alias kundli -keyalg RSA -keysize 2048 -validity 10000

# You'll be prompted for:
#   keystore password         (write it down, put in a password manager)
#   key password              (make it the same as the keystore password)
#   name, org, city, country  (any real answer is fine for a private APK)
```

Reference the keystore from a `local.properties` file inside `web/android/`
(never committed):

```properties
# web/android/local.properties  (git-ignored)
sdk.dir=/path/to/your/Android/sdk
kundli.keystore.file=/absolute/path/to/kundli-release.keystore
kundli.keystore.password=YOUR_KEYSTORE_PASSWORD
kundli.key.alias=kundli
kundli.key.password=YOUR_KEY_PASSWORD
```

And wire the release signingConfig in `web/android/app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            def props = new Properties()
            file("$rootDir/local.properties").withInputStream { props.load(it) }
            storeFile     file(props.getProperty('kundli.keystore.file'))
            storePassword props.getProperty('kundli.keystore.password')
            keyAlias      props.getProperty('kundli.key.alias')
            keyPassword   props.getProperty('kundli.key.password')
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 5. Build the signed release APK

```bash
cd web

# Fresh Angular production build → copies into android/app/src/main/assets/public.
npm run cap:sync

# Then the Gradle release build. On the first run it downloads Gradle
# itself (~200 MB) and every Android AAR — allow 10-15 min offline.
cd android
./gradlew assembleRelease

# The signed APK lands at:
#   web/android/app/build/outputs/apk/release/app-release.apk
```

Verify the APK was actually signed:

```bash
apksigner verify --verbose \
  app/build/outputs/apk/release/app-release.apk
# Expect: "Verifies", scheme v1/v2/v3 all "true".
```

## 6. Install on a physical phone

Enable **Developer options** → **USB debugging** on the phone first
(Settings → About → tap Build Number 7×, then Settings → Developer options).

```bash
adb install -r web/android/app/build/outputs/apk/release/app-release.apk
```

Or, for the fully-offline case:

1. Copy `app-release.apk` to the phone via USB, Bluetooth or a chat app.
2. Open the file on the phone; Android will prompt "Install unknown apps"
   → grant permission for the file manager you opened it from.
3. Tap Install. The launcher icon appears with the gold ✦ Kundli mark.

## 7. First-run smoke checklist

On the phone, in this order — every item must work before the APK is
handed to family:

- [ ] Launch icon shows the gold ✦ Kundli mark on the dark background.
- [ ] App opens on the Mother-mode home ("नमस्ते माँ 🙏") in Hindi by default.
- [ ] Language toggle in the header flips between हिंदी and English.
- [ ] "Set up primary profile" wizard reaches the /advanced Chart form.
- [ ] Creating a chart with a bundled city (e.g. Ranchi, Jharkhand) succeeds
      — the ephemeris backend on kundli.ashutoshkumar.codes must be reachable
      over the phone's data connection.
- [ ] "Set as primary" bounces to Mother home.
- [ ] "नई कुंडली मिलाएँ" → confirm → result screen renders with the right
      Hindi category banner.
- [ ] "📄 PDF" opens the print view; from Chrome's print dialog inside the
      WebView, "Save as PDF" writes the file to /Downloads.
- [ ] "↗ साझा करें" opens the native Android share sheet (WhatsApp, Gmail,
      Bluetooth, etc. as options).
- [ ] Android back button on the result page goes to the confirm page, then
      to the new-match form, then to the home, then to the previous app
      (i.e. exits Kundli).
- [ ] Airplane mode: launching the app still shows the shell (service
      worker) but any /api call fails cleanly with a red banner — no stale
      score served from cache.

## 8. What CI would need

We don't run Android CI today; the human procedure above is the release
gate. If we ever want it automated:

- GitHub Actions with `actions/setup-java@v4` (17) + `android-actions/setup-android@v3`.
- Cache `~/.gradle`.
- Secrets: `KEYSTORE_B64` (base64-encoded `kundli-release.keystore`),
  keystore + key passwords.
- On tag push: `npm ci` → `npm run cap:sync` → `./gradlew assembleRelease`
  → upload the APK as a release asset.

That's future work — a private APK for one family doesn't need it.
