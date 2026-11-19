#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?Set ANDROID_JAR to platforms/android-35/android.jar}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to a build-tools directory}"
mkdir -p build/classes build/dex
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o build/resources.zip
python3 - <<'PY'
from pathlib import Path
text=Path('app/src/main/AndroidManifest.xml').read_text()
Path('build/AndroidManifest.xml').write_text(text.replace('<manifest ', '<manifest package="org.lockheart.abide" ',1))
PY
"$ANDROID_BUILD_TOOLS/aapt2" link -o build/base.apk -I "$ANDROID_JAR" --manifest build/AndroidManifest.xml --java build/generated -A app/src/main/assets build/resources.zip
find app/src/main/java build/generated -name '*.java' > build/sources.txt
if command -v javac >/dev/null; then
 javac -encoding UTF-8 --release 8 -cp "$ANDROID_JAR" -d build/classes @build/sources.txt
else
 java -jar tools/ecj.jar -8 -encoding UTF-8 -bootclasspath "$ANDROID_JAR:$ANDROID_BUILD_TOOLS/core-lambda-stubs.jar" -d build/classes @build/sources.txt
fi
python3 - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/classes.jar','w',zipfile.ZIP_DEFLATED) as z:
 for p in Path('build/classes').rglob('*.class'): z.write(p,p.relative_to('build/classes'))
PY
"$ANDROID_BUILD_TOOLS/d8" --release --min-api 26 --lib "$ANDROID_JAR" --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in Path('build/dex').glob('*.dex'): z.write(p,p.name)
PY
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f development.keystore ]; then
 keytool -genkeypair -keystore development.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=ABIDE Development,O=Lockheart,C=JP'
fi
"$ANDROID_BUILD_TOOLS/apksigner" sign --ks development.keystore --ks-pass pass:android --key-pass pass:android --out build/StillWord_0.2.2.apk build/aligned.apk
"$ANDROID_BUILD_TOOLS/apksigner" verify --verbose build/StillWord_0.2.2.apk
printf 'Built: %s/build/StillWord_0.2.2.apk\n' "$PWD"
