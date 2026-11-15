#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" \
 app/src/main/java/com/pooyan/namakadetv/StreamDetector.java \
 app/src/main/java/com/pooyan/namakadetv/PlaybackResumePolicy.java \
 app/src/main/java/com/pooyan/namakadetv/CatalogPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/OpenFilms.java \
 app/src/main/java/com/pooyan/namakadetv/PosterPolicy.java \
 tests/StreamDetectorTest.java tests/PlaybackResumePolicyTest.java \
 tests/CatalogPolicyTest.java tests/OpenFilmsTest.java tests/PosterPolicyTest.java
java -cp "$TMP" StreamDetectorTest
java -cp "$TMP" PlaybackResumePolicyTest
java -cp "$TMP" CatalogPolicyTest
java -cp "$TMP" OpenFilmsTest
java -cp "$TMP" PosterPolicyTest
python3 scripts/check-symbols.py
javac -d "$TMP" tools/JavaParseCheck.java
java -cp "$TMP" JavaParseCheck app/src/main/java
python3 - <<'PYTEST'
from pathlib import Path
from xml.etree import ElementTree as ET
ET.parse('app/src/main/AndroidManifest.xml')
src=Path('app/src/main/java/com/pooyan/namakadetv/MainActivity.java').read_text()
assert 'new WebView(' not in src
assert 'CatalogPolicy.search' in src
assert 'openRail()' in src
assert 'PlayerActivity.EXTRA_URL' in src
assert "versionName '0.2'" in Path('app/build.gradle').read_text()
assert "applicationIdSuffix '.preview'" in Path('app/build.gradle').read_text()
joined=''.join(p.read_text(errors='ignore') for p in Path('app/src').rglob('*.java'))
assert 'mobile.namakade.com:8443/serieses/rivals?' not in joined
assert 'sk-proj-' not in joined
print('PASS native cinema UI, manifest, safe preview package, no embedded private URL')
PYTEST
