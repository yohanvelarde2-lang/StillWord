# ABIDE — Word Before the World

Android personal test build · Version 0.1.0 · November 1, 2026

A quiet place to read Scripture before opening social media.

## Install on your Pixel

1. Download **ABIDE_0.1.0.apk** on your phone and open it. If Android asks, allow your browser or file manager to install this APK, then finish installation.
2. Open **ABIDE → Rhythm → Enable Social Guard**. Read the disclosure, then tap **I agree · open settings**.
3. In Android Accessibility, find **ABIDE Social Guard** under downloaded/installed apps and enable it.
4. If Android displays **Restricted setting**, open **Settings → Apps → ABIDE → ⋮ → Allow restricted settings**, confirm with your PIN or fingerprint, and return to Accessibility to enable the service. Wording and availability vary by Android version. Only grant this access to the ABIDE file you intended to install.
5. In **Rhythm**, tap **Allow notifications** and grant permission. This is for the reading-complete notification; the reader still works if notifications are denied.
6. Choose **5 minutes** or **10 minutes**. The initial default is 5. A change applies immediately in an untouched window; otherwise it begins at the next reset.

ABIDE installs as a separate app. You can open it from LOCKHEART or another launcher.

## Your reading rhythm

- The first reading place is **Genesis 1**. Scroll to read, then tap **Next chapter** to continue. The book/chapter picker lets you choose another passage.
- Your book, chapter, approximate visible verse, and text size are saved on the phone. Reading position continues across six-hour resets and app restarts.
- **A− / A+** adjust the text size.
- Reading time counts only while the Bible reader is visible, focused, and the screen is on and unlocked. It pauses outside the reader and after 90 seconds without a touch or scroll. Touch or scroll to resume.
- Short reading sessions add together within the current window. Scrolling is not automatic. The timer measures attentive screen time; it cannot verify comprehension or eye movement.
- On completing the chosen goal, ABIDE displays **John 8:32** and posts **“You can now open Facebook or Instagram”** if notification permission is granted.
- Tap **Unlock social apps**, then **Open Facebook** or **Open Instagram**. A notification tapped after the next reset cannot unlock the new reading window.
- Facebook and Instagram then remain available until the next six-hour boundary. The schedule is anchored to the first app launch, rather than to midnight. For example, a first launch at 08:30 creates boundaries at 14:30, 20:30, 02:30, and 08:30.
- At a boundary, the reading-time requirement starts again. If a protected app is in front and the guard service is running, the Bible reminder returns. Your Bible position is kept.

The reminder shows **Philippians 4:8**, a **Read the Bible** button, and: **“Grab your devotional notebook and pen now and start reading the bible”**. Both reminder verses use the currently selected translation.

## Which apps are covered?

ABIDE guards the installed Facebook, Facebook Lite, Instagram, and Instagram Lite apps in the same Android profile. **Facebook Messenger is left available.**

It does not block social-media websites in browsers, cloned apps, other Android profiles, or every social service. It does not hide notifications or remove social apps from your phone.

The Android Accessibility service must remain enabled and connected. Android may stop or restrict services; check **Rhythm → Social Guard** if blocking stops. The app is a voluntary focus aid: settings, force stop, uninstalling, other profiles, and clock changes can affect or bypass it. It does not prevent you from controlling or removing it.

## Grace pass: one override in any 24 hours

Open **Need a grace pass?** on the Today screen, use **Rhythm → Open grace pass**, or use the hold button on the social-app reminder.

Keep one finger on the hold button continuously for **45 seconds**. Lifting your finger, moving it outside the button, leaving the app, locking the phone, or crossing into a new reading window cancels the hold.

A completed grace pass opens the protected apps for the **remainder of the current six-hour window**. It does not permanently disable ABIDE, and it does not mark Bible reading complete. The next pass becomes available **24 hours after the previous successful pass**, rather than at the next calendar day. This is the chosen interpretation of the requested one-time disable feature.

## Bible edition: please read before sharing

**This build includes the complete World English Bible (WEB), Protestant Edition: 66 books and 1,189 chapters. It does not include the NIV.** All bundled Bible text works offline immediately, including both reminder verses.

The requested full offline NIV cannot be bundled for public distribution without appropriate permission. Biblica states that Bible use in apps requires permission, and licensing the full text is available to registered legal entities. Approval must cover the particular offline distribution you intend. Free access to NIV in another publisher’s app does not grant permission to copy its text into ABIDE.

ABIDE includes an **Import an authorized Bible pack** option. It accepts a complete `abide-bible-1` JSON file supplied under a license permitting its use here. It saves the pack privately on the device and works offline after import. The reader, Philippians 4:8 reminder, and John 8:32 release screen then use that pack. An import confirmation is not itself a license. Ordinary PDF/EPUB files and another Bible app’s protected downloads cannot be imported directly.

If NIV is essential, keep this as the functional test build while obtaining permission through your ministry/legal entity. Do not advertise this build as an NIV app.

## Privacy

No account, ads, analytics, or internet permission. No Bible downloads are needed. Reading progress, preferences, override history, and any imported Bible stay in app-private device storage. The app does not read messages, screen text, passwords, or keystrokes. Its Accessibility service observes foreground app package names to decide when to show the reminder; those events are not logged or shared.

Android’s generic Accessibility warning describes broad possible capabilities. ABIDE’s service is configured with window-content retrieval disabled. Notification permission is optional. Selecting a Bible file grants access only to the selected document. Android backup is disabled for this build; uninstalling removes the stored reading progress and imported Bible.

## First Pixel test

1. Enable Social Guard and open Facebook, then Instagram. Each should show Philippians 4:8 and the notebook tip. Check that Messenger still opens.
2. Tap **Read the Bible**. Read and scroll for the chosen duration. Try briefly switching apps and locking the screen; those intervals should not add to reading time.
3. At completion, check John 8:32, the notification, **Unlock social apps**, and the social-app opening buttons.
4. Reopen ABIDE and check that your reading position is kept. Check the next natural six-hour reset.
5. If needed, test one continuous 45-second grace pass. Confirm that another pass is unavailable for 24 hours.

Do not change the phone clock to test resets. The build uses wall time for persisted six-hour windows and the rolling 24-hour limit; manual clock changes are not tamper-proof.

## Validation and release status

The APK compiled, its Android manifest and permission list were inspected, and APK v2/v3 signatures verified. The policy passed 21 automated checks covering both reading goals, explicit unlocking, exact cycle boundaries, skipped cycles, stale unlock attempts, override expiry, and the rolling 24-hour rule. Browser checks passed for the actual reader assets, chapter navigation, text sizing, gate/release flows, grace states, and 320-pixel layouts. The bundled Bible was checked for all 66 books, 1,189 chapters, and its final verse.

**The APK has not been installed or tested on a physical Android device or Android emulator in this session.** The browser checks exercise the UI with a simulated native bridge; they do not verify Android service behavior. Pixel testing is needed for accessibility overlays, background operation, notification taps, screen focus, continuous holds, and reboot behavior.

This is a development-signed personal test build, not a Google Play release. Before a wider release: complete device testing, obtain any NIV offline-distribution license, clear the working name **ABIDE**, create a production signing identity and app bundle, and complete Google Play’s current Accessibility API disclosures/declaration and privacy requirements. The source package preserves the development signing key for compatible test updates; it is not a production secret and must not be used as the public-release signing identity.

## Sources

- NIV permission requirements: https://www.biblica.com/permissions/
- WEB source and public-domain terms: https://ebible.org/find/details.php?id=engwebp
- Android restricted settings: https://support.google.com/android/answer/12623953
- Android Accessibility API: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Google Play Accessibility policy: https://support.google.com/googleplay/android-developer/answer/10964491

ABIDE — a little Word, a quieter world.
