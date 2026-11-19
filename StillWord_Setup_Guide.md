# StillWord 0.2.2

## What changed in 0.2.2: battery

- **Social Guard no longer checks four times a second all day.** It now reacts to Android's own app-switch signals and screen on/off/unlock, and checks rapidly for only 3 seconds after each one. While Facebook or Instagram stays open, it wakes only when access can change, such as at a six-hour reset or when a grace pass becomes available again.
- **StillWord's own screens pause their timers in the background.** The home screen, the page's 1-second refresh, the Kindle screen and the book reader all stop working when you leave them.
- **Fewer storage writes.** Reading progress is saved every 5 seconds while you read, and immediately when the goal completes, unlocks, or you leave the screen. Unchanged state is never rewritten.

Behaviour and settings are unchanged. After updating, repeat Phone check 1 below (the quick Recent Apps switching test), since the guard's timing changed.

## Install over StillWord or Abide

1. Open `StillWord_0.2.2.apk` and tap **Update**. Keep the original installation in place to retain your reading goal, Bible bookmark, imported Bible and grace-pass history.
2. Open StillWord. In **Rhythm**, check that Social Guard says **Connected**. Android may require you to re-enable **StillWord Social Guard** under Accessibility after the update. If Android shows a Restricted setting message, use the app's **App info → ⋮ → Allow restricted settings** option if available, then return to Accessibility.

## Read on a Kindle or in StillWord

- **Read from Kindle** opens a focus screen with Philippians 4:8 and a countdown toward your current reading goal. Leave it visible on your phone while you read on your Kindle device. It keeps the phone display awake, and the timer pauses if you switch apps, lock the phone or tap Pause timer.
- **Upload a book** opens the Android file picker. Choose a PDF, EPUB or MOBI file. StillWord makes a temporary local copy so you can start reading. Tap **Save to Download Library** to keep the copy. Reopen it from **Download Library** to read full screen.
- **Previous** and **Next** turn pages or sections. **Close** goes to Download Library and **Home** goes to StillWord’s start screen. Each saved book remembers its own last page and scroll position. The slim page indicator also shows your reading goal timer. Tap it for timer controls and the unlock action.
- PDF pages retain their layout. EPUB and MOBI display text. This release supports standard, DRM-free EPUB text books, unencrypted PDFs and classic DRM-free MOBI with uncompressed or PalmDOC text. HUFF/CDIC MOBI, Kindle Format 8, encrypted books and fixed-layout EPUBs are outside the reader's support. Import files up to 200 MiB. EPUB and MOBI text extraction also has size limits.
- Bible reading, Kindle time and time spent reading an imported book contribute to the same 5- or 10-minute goal. When the goal is complete, use **View John 8:32 & unlock**, then tap **Unlock social apps**.

## Phone checks

1. Before the goal is complete, open Facebook and check that StillWord covers it. Swipe up and hold for Recent Apps, select Facebook and repeat several times with quick swipes and taps. Check that the guard returns each time. Repeat with Instagram.
2. Open Kindle mode. Check that the timer counts down while the screen is visible and pauses when you switch away or lock the phone.
3. Import a PDF, an EPUB and a classic DRM-free MOBI. Turn pages, save each to Download Library, close StillWord and reopen the saved books. Switch between two books, then check that each resumes at its own page and scroll position. Check that Home and Close go to their respective screens.
4. Check that an existing Bible bookmark and an imported authorized Bible pack still open after the update.

The APK signature matches StillWord 0.2.0, the Android package ID is retained, and the bundled Bible is identical. The bookmark migration and separate-book persistence checks passed, along with the existing automated checks. The phone checks above still need to be run on an Android device.
