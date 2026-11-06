# Ayyıldız Tube 2.0

Native Android-App von Erol Studio auf Basis von [LibreTube](https://github.com/libre-tube/LibreTube) (GPL-3.0-or-later). Die verwendete LibreTube-Version steht in `upstream-version.txt`. Videos laufen über den nativen Media3-Player mit SABR-Unterstützung, nicht über eine WebView.

- App-Name: Ayyıldız Tube
- Paketname: `com.ayyildiztube.app` (Test-Builds: `com.ayyildiztube.app.debug`)
- Mindestversion: Android 8.0

Der Testbericht mit allen geprüften und ungeprüften Funktionen steht in [TEST-REPORT.md](TEST-REPORT.md).

## Aufbau des Repositorys

Dieses Repository enthält nur unsere Änderungen. Der LibreTube-Code wird beim Build von der offiziellen Quelle geladen.

| Pfad | Inhalt |
| --- | --- |
| `upstream-version.txt` | Die LibreTube-Version, auf der die App aufbaut |
| `tools/rebrand_libretube.py` | Patcht LibreTube und bricht ab, wenn eine Stelle nicht exakt passt |
| `overlay/` | Neue und ersetzte Dateien: Update-Funktion, Icon, Menü, Texte |
| `branding/` | Logo als SVG und PNG |
| `.github/workflows/android-build.yml` | Debug-APK, Unit-Tests, Lint und Build-Bericht |
| `.github/workflows/release.yml` | Signierte Release-APK, Prüfsumme, Quellcode-Archiv und GitHub Release |
| `.github/workflows/auto-update.yml` | Übernimmt neue LibreTube-Versionen automatisch |

## Lokal bauen

Benötigt werden JDK 17, das Android SDK 36 und Python 3.

```bash
git clone --depth 1 --branch "$(cat upstream-version.txt)" https://github.com/libre-tube/LibreTube.git upstream
```

```bash
python3 tools/rebrand_libretube.py upstream
```

```bash
cd upstream && ./gradlew assembleDebug testDebugUnitTest lintDebug
```

## Eine neue Version veröffentlichen

Ein Tag startet den Release-Workflow. Die Versionsnummer wird aus dem Tag übernommen.

```bash
git tag v2.0.1
```

```bash
git push origin v2.0.1
```

Der Workflow braucht diese GitHub-Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` und `KEY_PASSWORD`. Fehlt eines davon, bricht er ab.

**Wichtig:** Der Signaturschlüssel darf nie ins Repository. Geht er verloren, lassen sich bestehende Installationen nicht mehr aktualisieren und müssen neu installiert werden. Bewahre deshalb eine Sicherungskopie außerhalb des Rechners auf.

## Automatische Übernahme von LibreTube

Jeden Tag prüft GitHub Actions, ob es eine neue stabile LibreTube-Version gibt. Lässt sie sich patchen, bauen und bestehen die Unit-Tests, wird ohne Freigabe eine neue App-Version veröffentlicht, und installierte Apps bieten das Update beim nächsten Start an.

- Die Prüfung testet keine echte Videowiedergabe. Eine fehlerhafte Version kann also bei allen Nutzern landen.
- Scheitert ein Schritt, wird nichts veröffentlicht, und im Repository entsteht ein Issue. Dann muss jemand das Patch-Skript anpassen.
- GitHub schaltet zeitgesteuerte Workflows nach 60 Tagen ohne Aktivität im Repository ab. Unter „Actions“ lassen sie sich wieder einschalten.

Der Lauf lässt sich unter „Actions“ auch von Hand starten. Mit der Option `force` entsteht eine neue App-Version, auch wenn LibreTube unverändert ist.

## Update-Funktion

Die App fragt beim Start die neueste Version bei GitHub Releases ab. Eine APK wird nur zur Installation angeboten, wenn Quelle, SHA-256-Prüfsumme, Paketname, Versionsnummer und Signatur stimmen. Die Installation bestätigst du immer selbst im Android-Dialog. Dafür muss das Repository öffentlich sein.

## Lizenz

GPL-3.0-or-later, siehe [LICENSE](LICENSE) und [NOTICE.md](NOTICE.md). Wer die APK weitergibt, muss auch den Quellcode verfügbar machen. Zu jedem Release gehört dafür ein Quellcode-Archiv.

Ayyıldız Tube steht in keiner Verbindung zu YouTube, Google oder dem LibreTube-Projekt. Die Nutzung kann gegen die Nutzungsbedingungen von YouTube verstoßen. Prüfe Marken- und Namensrechte vor einer öffentlichen Verbreitung.
