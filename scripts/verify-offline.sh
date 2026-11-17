#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" \
 app/src/main/java/com/pooyan/namakadetv/StreamDetector.java \
 app/src/main/java/com/pooyan/namakadetv/PlaybackResumePolicy.java \
 app/src/main/java/com/pooyan/namakadetv/FarsiPlexUrlPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/PosterPolicy.java \
 tests/StreamDetectorTest.java tests/PlaybackResumePolicyTest.java \
 tests/FarsiPlexUrlPolicyTest.java tests/PosterPolicyTest.java
java -cp "$TMP" StreamDetectorTest
java -cp "$TMP" PlaybackResumePolicyTest
java -cp "$TMP" FarsiPlexUrlPolicyTest
java -cp "$TMP" PosterPolicyTest
python3 scripts/check-symbols.py
javac -d "$TMP" tools/JavaParseCheck.java
java -cp "$TMP" JavaParseCheck app/src/main/java
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
ET.parse('app/src/main/AndroidManifest.xml')
sources='\n'.join(p.read_text() for p in Path('app/src/main/java').rglob('*.java'))
main=Path('app/src/main/java/com/pooyan/namakadetv/MainActivity.java').read_text()
gradle=Path('app/build.gradle').read_text()
assert 'new WebView' not in sources
assert 'FarsiPlexRepository' in main and 'FarsiPlexParser' in sources
assert 'source.list(' in main and 'source.detail(' in main
assert 'PlayerActivity.EXTRA_URL' in main
assert "versionName '0.4'" in gradle
assert "applicationIdSuffix '.preview'" in gradle
assert 'Cinemeta' not in main and 'namakade.com' not in main
assert 'sk-proj-' not in sources and 'Authorization: Bearer' not in sources
print('PASS native UI, FarsiPlex-only catalog, media player, Android manifest, preview isolation')
PY
