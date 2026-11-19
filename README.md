# StillWord 0.2.2

Native Android personal test app for Johnny sensei. Java host and Accessibility overlay; bundled local HTML/CSS/JS reader. No runtime dependencies, ads, accounts, network access, or AI features.

Read `StillWord_Setup_Guide.md` for installation, exact product behavior, limitations, licensing, and verification status.

## Build

Use JDK 17, Android SDK platform 35, and Android build tools 35 or newer. Run:

```bash
export ANDROID_JAR=/your/android-sdk/platforms/android-35/android.jar
export ANDROID_BUILD_TOOLS=/your/android-sdk/build-tools/35.0.0
bash build.sh
```

Output: `build/StillWord_0.2.2.apk`. The direct build uses only SDK tools and JDK utilities. `tools/ecj.jar` is an optional fallback when a local JDK compiler is unavailable; it is included in the source package and excluded from the APK. Install a full JDK for a normal build.

Alternatively, open this directory in Android Studio, select JDK 17 and Gradle 8.9 with Android Gradle Plugin 8.7.3, sync, and build the debug variant. No Gradle wrapper is bundled. The included `development.keystore` uses conventional development credentials (`android`, alias `androiddebugkey`) to preserve update compatibility for this test build. Keep the same development certificate for test updates. Use a separate, private production signing identity for release. Increment versionCode for updates.

## Source map

- `Policy.java`: pure six-hour and rolling-24-hour rules.
- `Store.java`: private persistent state and duration choices.
- `MainActivity.java`: local WebView host, foreground/idle reading timer, notification, import, and in-app hold.
- `GuardService.java` and `ForegroundWindow.java`: recheck the foreground package from Android windows when a task resumes. The guard reads the package name and draws the reminder overlay.
- `ReadingActivity.java`, `BookFiles.java`, `BookContent.java` and `MobiText.java`: Kindle timer, local imports, offline library and file readers.
- `Bible.java`: bundled Bible and validated, atomic authorized-pack import.
- `app/src/main/assets/`: actual offline UI and complete WEB pack.

## Tests

```bash
bash tests/run.sh
```

`tests/ui.cjs` uses Playwright plus a Chromium executable supplied via `ABIDE_CHROME` and an explicitly simulated native bridge. It serves the real app assets locally and tests UI flows. It is not an Android integration test. No test bridge or time acceleration is bundled in the APK.

## Authorized Bible packs

The bundled pack is `app/src/main/assets/bible.json`. An imported pack uses exactly the same shape:

```json
{
  "format": "abide-bible-1",
  "translation": "Authorized translation name",
  "abbreviation": "ABC",
  "copyright": "Exact required rights notice and permission details",
  "books": [
    {
      "id": "GEN",
      "name": "Genesis",
      "chapters": [
        [{"n": "1", "t": "Authorized verse text"}]
      ]
    }
  ]
}
```

The example is illustrative, not importable: a real pack must contain all 66 books in the canonical IDs/order found in the bundled pack, and all 1,189 chapters. Each chapter contains verse records. Required gate records are PHP 4:8 and JHN 8:32. Verse labels support numbers, a lower-case suffix, or a range, to accommodate licensed editions. Packs are limited to 15 MiB. UI text is escaped before rendering. The pack import does not establish permission or verify translation provenance; the rights holder must authorize the supplied text. Do not scrape or copy restricted NIV text from another app.

If authorized NIV redistribution is approved, a developer can replace the bundled pack with the licensed edition and include the exact licensed copyright and product requirements. Preserve text fidelity and test the imported pack. Do not distribute any imported proprietary pack with source unless its license explicitly permits it.

The complete World English Bible (Protestant Edition) text is public domain. It was obtained from eBible.org’s `engwebp_vpl.zip` on November 1, 2026 and converted without paraphrasing. Verse formatting follows the source text-only export; editorial headings and footnotes are not included. `prepare_bible.py` can regenerate the JSON when the original source archive is placed beside it. Download source: https://ebible.org/Scriptures/engwebp_vpl.zip

## Known limits

Physical Android integration remains untested. Blocking covers the listed package names only and depends on the enabled service. It can be bypassed through Android settings, force stop, uninstalling, web versions, profiles/clones, or clock changes. Timing cannot prove comprehension. Read progress and pass history reset if app data is cleared. No boot receiver is used: Android manages the enabled Accessibility service’s lifecycle, so reboot reconnection must be verified on target devices. Imported Bible file selection uses Android’s document picker; cloud storage providers may require internet to obtain the initial file, after which the saved pack is offline.

StillWord is the personal build name. Device testing, name clearance, production signing, permission/policy review, and NIV licensing are separate public-release steps.
