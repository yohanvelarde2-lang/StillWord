#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/tests
java -jar tools/ecj.jar -8 -encoding UTF-8 -d build/tests \
  app/src/main/java/org/lockheart/abide/{Policy,MobiText,ForegroundWindow,BookBookmark}.java \
  tests/stubs/android/view/accessibility/AccessibilityWindowInfo.java \
  tests/stubs/android/content/SharedPreferences.java \
  tests/{PolicyTest,MobiTextTest,ForegroundWindowTest,BookBookmarkTest}.java
for suite in PolicyTest MobiTextTest ForegroundWindowTest BookBookmarkTest; do
  java -cp build/tests "org.lockheart.abide.$suite"
done
node --check app/src/main/assets/app.js
node tests/home.cjs
