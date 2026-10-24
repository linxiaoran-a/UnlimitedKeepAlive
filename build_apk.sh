#!/usr/bin/env bash
# Build UnlimitedKeepAlive.apk (settings UI) with plain Android SDK command-line tools.
set -euo pipefail
cd "$(dirname "$0")"

: "${ANDROID_SDK:?set ANDROID_SDK to your SDK root}"
BT="${BUILD_TOOLS:-$ANDROID_SDK/build-tools/34.0.0}"
AJAR="$ANDROID_SDK/platforms/android-34/android.jar"

mkdir -p build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/unsigned.apk -I "$AJAR" \
    --manifest AndroidManifest.xml build/res.zip

# Cross-platform classpath separator: ; on Windows, : on Unix
SEP=":"
[ "$(uname -s | cut -c1-5)" = "MINGW" ] && SEP=";"

find src -name '*.java' | sort > build/sources.txt
cat > build/javac_args.txt <<EOF
-encoding
UTF-8
-source
8
-target
8
-cp
$AJAR
-d
build/classes
EOF
cat build/sources.txt >> build/javac_args.txt
javac @build/javac_args.txt

if [ -f "$BT/d8" ]; then D8=("$BT/d8")
else D8=(java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8); fi
"${D8[@]}" --release --min-api 26 --lib "$AJAR" \
    --output build/dex $(find build/classes -name "*.class" | sort)

cp build/unsigned.apk build/withdex.apk
python3 - <<'EOF'
import zipfile
z = zipfile.ZipFile('build/withdex.apk', 'a')
z.write('build/dex/classes.dex', 'classes.dex', zipfile.ZIP_STORED)
z.close()
EOF

"$BT/zipalign" -f -p 4 build/withdex.apk build/aligned.apk

KS=build/debug.keystore
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android -dname "CN=Android Debug"
fi
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" \
    --ks-pass pass:android --key-pass pass:android \
    --out UnlimitedKeepAlive.apk build/aligned.apk

echo "OK: UnlimitedKeepAlive.apk"
