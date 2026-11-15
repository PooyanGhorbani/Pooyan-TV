#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" \
    app/src/main/java/com/pooyan/namakadetv/StreamDetector.java \
    app/src/main/java/com/pooyan/namakadetv/SiteCatalog.java \
    app/src/main/java/com/pooyan/namakadetv/SearchEngine.java \
    app/src/main/java/com/pooyan/namakadetv/PosterPolicy.java \
    app/src/main/java/com/pooyan/namakadetv/SourceUpdatePolicy.java \
    app/src/main/java/com/pooyan/namakadetv/PlaybackResumePolicy.java \
    app/src/main/java/com/pooyan/namakadetv/SourceDiagnostics.java \
    tests/StreamDetectorTest.java tests/SiteCatalogTest.java \
    tests/SearchEngineTest.java tests/PosterPolicyTest.java tests/SourceUpdatePolicyTest.java \
    tests/PlaybackResumePolicyTest.java tests/SourceDiagnosticsTest.java
java -cp "$TMP" StreamDetectorTest
java -cp "$TMP" SiteCatalogTest
java -cp "$TMP" SearchEngineTest
java -cp "$TMP" PosterPolicyTest
java -cp "$TMP" SourceUpdatePolicyTest
java -cp "$TMP" PlaybackResumePolicyTest
java -cp "$TMP" SourceDiagnosticsTest
javac -d "$TMP" tools/JavaParseCheck.java
java -cp "$TMP" JavaParseCheck app/src/main/java
python3 - <<'PY'
import xml.etree.ElementTree as ET
ET.parse('app/src/main/AndroidManifest.xml')
print('PASS AndroidManifest.xml XML parse')
from pathlib import Path
manifest = Path('app/src/main/AndroidManifest.xml').read_text()
assert '@string/app_name' in manifest
assert 'Pooyan TV' in Path('app/src/main/res/values/strings.xml').read_text()
assert 'Pooyan TV' in Path('README_FA.md').read_text()
assert 'mobile.namakade.com:8443/serieses/rivals?' not in ''.join(p.read_text(errors='ignore') for p in Path('app/src').rglob('*.java'))
print('PASS Pooyan TV branding and no embedded private Namakade link')
PY
