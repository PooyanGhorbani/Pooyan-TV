#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" \
 app/src/main/java/com/pooyan/namakadetv/StreamDetector.java \
 app/src/main/java/com/pooyan/namakadetv/PlaybackResumePolicy.java \
 app/src/main/java/com/pooyan/namakadetv/FarsiPlexUrlPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/FarsiPlexTitle.java \
 app/src/main/java/com/pooyan/namakadetv/CatalogIdentity.java \
 app/src/main/java/com/pooyan/namakadetv/CatalogMerge.java \
 app/src/main/java/com/pooyan/namakadetv/ImageUrlPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/PosterPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/TvPosterFocusPolicy.java \
 app/src/main/java/com/pooyan/namakadetv/AiResolverUrlPolicy.java \
 tests/StreamDetectorTest.java tests/PlaybackResumePolicyTest.java \
 tests/FarsiPlexUrlPolicyTest.java tests/PosterPolicyTest.java \
 tests/TvPosterFocusPolicyTest.java tests/AiResolverUrlPolicyTest.java \
 tests/CatalogMergeSmokeTest.java tests/ImagePolicySmokeTest.java
java -cp "$TMP" StreamDetectorTest
java -cp "$TMP" PlaybackResumePolicyTest
java -cp "$TMP" FarsiPlexUrlPolicyTest
java -cp "$TMP" PosterPolicyTest
java -cp "$TMP" TvPosterFocusPolicyTest
java -cp "$TMP" AiResolverUrlPolicyTest
java -cp "$TMP" CatalogMergeSmokeTest
java -cp "$TMP" ImagePolicySmokeTest
python3 scripts/check-symbols.py
javac -d "$TMP" tools/JavaParseCheck.java
java -cp "$TMP" JavaParseCheck app/src/main/java
python3 - <<'PYSOURCECHECK'
from pathlib import Path
from xml.etree import ElementTree as ET
ET.parse('app/src/main/AndroidManifest.xml')
sources='\n'.join(p.read_text() for p in Path('app/src/main/java').rglob('*.java'))
main=Path('app/src/main/java/com/pooyan/namakadetv/MainActivity.java').read_text()
gradle=Path('app/build.gradle').read_text()
assert 'new WebView' not in sources
assert 'UnifiedRepository' in main and 'NamakadeRepository' in sources
assert 'source.list(' in main and 'source.detail(' in main and 'source.search(' in main
assert 'TvPosterFocusPolicy' in main and 'requestFocus' in main
assert 'FarsiPlexPlayback.resolveDetailed' in sources
assert 'PlayerActivity.EXTRA_URL' in main
assert "versionName '0.5.0'" in gradle
assert "applicationIdSuffix '.preview050'" in gradle
assert 'sk-proj-' not in sources and 'DEEPSEEK_API_KEY' not in sources
assert 'AiResolverClient' in main and 'resolveWithAI' in main
assert 'FarsiPlex Edition' not in main and 'فیلم‌های FarsiPlex' not in main
print('PASS provider-neutral cinematic UI, playback path, Android manifest, preview isolation')
PYSOURCECHECK
