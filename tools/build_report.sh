#!/usr/bin/env bash
# Prints a Markdown build report and fails if the APK is missing, too small or wrongly branded.
# Usage: build_report.sh <patched source folder>
set -uo pipefail

SRC=${1:?Pfad zum gepatchten Quellcode fehlt}
APK=$(find "$SRC/app/build/outputs/apk/debug" -name '*.apk' 2>/dev/null | head -1)
AAPT2=$(find "${ANDROID_HOME:-}/build-tools" -name aapt2 2>/dev/null | sort | tail -1)
MIN_BYTES=$((5 * 1024 * 1024))
status=0

echo '# Ayyıldız Tube Build-Bericht'
echo
echo "- Zeitpunkt: $(date -u '+%Y-%m-%d %H:%M UTC')"
echo "- Commit: ${GITHUB_SHA:-lokal}"
echo "- LibreTube-Basis: $(git -C "$SRC" describe --tags --always 2>/dev/null || echo unbekannt)"
echo

echo '## APK'
if [ -z "$APK" ]; then
  echo '**FEHLER: keine APK gefunden.**'
  status=1
else
  bytes=$(stat -c %s "$APK")
  echo "- Datei: $(basename "$APK")"
  echo "- Größe: $((bytes / 1024 / 1024)) MB"
  echo "- SHA-256: $(sha256sum "$APK" | cut -d' ' -f1)"
  if [ "$bytes" -lt "$MIN_BYTES" ]; then
    echo '- **FEHLER: Die APK ist verdächtig klein.**'
    status=1
  fi
  if [ -n "$AAPT2" ]; then
    badging=$("$AAPT2" dump badging "$APK")
    echo "- Paket: $(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$badging")"
    echo "- Version: $(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$badging" | head -1)"
    echo "- Name: $(sed -n "s/^application-label:'\(.*\)'/\1/p" <<<"$badging")"
    if ! grep -q "package: name='com.ayyildiztube.app" <<<"$badging"; then
      echo '- **FEHLER: falscher Paketname.**'
      status=1
    fi
  fi
fi
echo

echo '## Unit-Tests'
python3 - "$SRC" <<'PY'
import glob, sys
import xml.etree.ElementTree as ET

tests = failed = 0
for path in glob.glob(sys.argv[1] + '/app/build/test-results/testDebugUnitTest/*.xml'):
    root = ET.parse(path).getroot()
    tests += int(root.get('tests'))
    failed += int(root.get('failures')) + int(root.get('errors'))
print(f'- {tests} Tests, {failed} fehlgeschlagen')
PY
echo

echo '## Lint'
lint="$SRC/app/build/reports/lint-results-debug.xml"
if [ -f "$lint" ]; then
  echo "- Fehler: $(grep -c 'severity="Error"' "$lint")"
  echo "- Warnungen: $(grep -c 'severity="Warning"' "$lint")"
else
  echo '- kein Lint-Bericht vorhanden'
fi
echo
echo '> Dieser Bericht belegt nur Kompilierung und statische Prüfungen. Die Videowiedergabe wird hier nicht getestet.'

exit $status
