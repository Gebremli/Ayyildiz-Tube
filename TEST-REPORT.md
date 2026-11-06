# Testbericht Ayyıldız Tube 2.0.0

Stand: 6. November 2026. Geprüft wurde die signierte Release-APK `com.ayyildiztube.app`, Version 2.0.0, auf Basis von LibreTube v32.1.

**Testumgebung:** Android-Emulator (Android 14, x86_64, Pixel-Auflösung 1080 × 2400) unter Windows mit WHPX. **Es gab keinen Test auf einem echten Smartphone.**

Vor dem ersten Test wurde auch das unveränderte LibreTube v32.1 gebaut, um eigene Fehler von vorhandenen zu trennen.

## Build

| Prüfung | Ergebnis |
| --- | --- |
| Debug-APK lokal | Erfolgreich |
| Release-APK lokal, mit R8 minimiert und signiert | Erfolgreich, 8,7 MB |
| Unit-Tests | 14 von 14 bestanden, davon 5 neue für die Update-Logik |
| Lint | 581 Meldungen der Stufe „Error“, fast alle fehlende Fremdsprachen-Übersetzungen aus LibreTube; der Build bricht dadurch nicht ab |
| GitHub-Actions-Build | Erfolgreich: Kompilierung, Tests, Lint, APK-Prüfung und Artefakt |
| GitHub-Actions-Release | Erfolgreich: signierte APK, Prüfsumme und Quellcode-Archiv veröffentlicht |
| Signatur des Releases | Zertifikat `e6225c8c…5b48e2`, identisch mit dem lokal erzeugten Schlüssel |

## Im Emulator getestet

| Funktion | Ergebnis | Nachweis |
| --- | --- | --- |
| App-Start | Funktioniert | Keine Abstürze im Crash-Log während aller Tests |
| Videosuche | Funktioniert | Suche „big buck bunny“ lieferte Treffer |
| Vorschaubilder und Titel | Funktioniert | Sichtprüfung |
| Videowiedergabe | Funktioniert | Position 5,9 s → 23,9 s in 18,03 s Echtzeit; Stream über SABR, Status OK |
| Nativer Player, kein WebView-Player | Funktioniert | Log zeigt `SabrStream`, Formate 248 (VP9) und 251 (Opus) |
| Release-APK nach R8 | Funktioniert | Wiedergabe, SABR und SponsorBlock in der minimierten APK geprüft |
| Vollbild | Funktioniert | Querformat, Wiedergabe lief weiter |
| Qualität wählen | Funktioniert | Nach Auswahl von 360p lud der Stream Format 243 |
| Geschwindigkeit | Funktioniert | Player meldete `speed=1.5` |
| Bild-in-Bild | Funktioniert | Fenster auf dem Startbildschirm, Position lief weiter |
| Hintergrundwiedergabe | Funktioniert | Position 18,0 s → 30,0 s in 12 s bei ausgeschaltetem Bildschirm |
| Wechsel zwischen Videos | Funktioniert | 4 Wechsel direkt nacheinander, richtige Titel, kein Absturz |
| Livestream | Funktioniert | Ein Stream von der Startseite lief |
| Short | Funktioniert | Spielt im normalen Player im Hochformat |
| SponsorBlock automatisch | Funktioniert | Sprung von 37,2 s auf 55,1 s über das Segment 40,2–55,7 s |
| SponsorBlock ausschalten | Funktioniert | Ausgeschaltet lief dasselbe Segment ohne Sprung durch |
| Lokales Abo ohne Konto | Funktioniert | Feed des Kanals wurde nach App-Neustart angezeigt |
| Verlauf | Funktioniert | Nach App-Neustart vorhanden |
| Offline-Meldung | Funktioniert | Deutscher Hinweis mit „Erneut versuchen“ |
| Untere Navigation | Funktioniert | Alle 5 Tabs geöffnet |
| Deutsche Oberfläche | Funktioniert | Alle besuchten Bereiche deutsch; 1 fehlender Text ergänzt |
| App-Icon | Funktioniert | Sichtprüfung in der App-Übersicht |
| Update-Erkennung beim Start | Funktioniert | Test-Build 1.9.9 fand das echte Release v2.0.0 und zeigte beide Versionen |
| Update-Download und Prüfung | Funktioniert | Prüfsumme, Paketname, Version und Signatur bestanden; danach Android-Installer |
| Installation nur mit Zustimmung | Funktioniert | Android fragte selbst nach, auch die Erlaubnis für die Quelle |
| Bestehende Installation bleibt erhalten | Funktioniert | Version 2.0.0, erstes Installationsdatum unverändert, Verlauf aus 1.9.9 noch vorhanden |
| Wiedergabe mit der APK aus GitHub Actions | Funktioniert | Position 10,2 s → 19,2 s nach dem Update |
| Kein Update-Hinweis auf aktueller Version | Funktioniert | 2.0.0 startete ohne Dialog |

**Werbung:** In den 6 getesteten Videos erschien keine Werbung. Das ist eine Stichprobe und kein Nachweis vollständiger Werbefreiheit. In Videos eingebaute Sponsorenteile werden nur übersprungen, wenn sie in der SponsorBlock-Datenbank eingetragen sind.

## Automatische Übernahme von LibreTube

| Prüfung | Ergebnis | Nachweis |
| --- | --- | --- |
| Lauf ohne neue Version | Funktioniert | Meldete „Keine neue Version“, nichts gebaut oder veröffentlicht |
| Erzwungener Durchlauf | Funktioniert | Build, Tests, Tag und Release v2.0.1 ohne manuellen Eingriff |
| Signatur von v2.0.1 | Funktioniert | Gleiches Zertifikat wie v2.0.0, Prüfsumme stimmt |
| Patch-Skript auf v32.1 | Funktioniert | Quellcode identisch mit dem getesteten Stand |

**Nicht getestet:** Es gab noch keine echte neue LibreTube-Version, der Lauf wurde erzwungen und nutzte weiter v32.1. Ob künftige Versionen sich patchen lassen, zeigt sich erst dann. Auch die Fehlermeldung als Issue wurde nie ausgelöst. Die Automatik prüft keine Videowiedergabe, eine fehlerhafte Version kann also bei allen Nutzern landen.

## Noch nicht getestet

- **Abwehr manipulierter Updates:** Getestet wurde nur der Erfolgsfall. Eine falsche Prüfsumme oder fremde Signatur wurde nicht im echten Ablauf provoziert. Das Lesen der Prüfsumme und der Versionsvergleich sind per Unit-Test geprüft.
- **Abos über ein Update hinweg:** Als Datenbeweis diente der Verlauf. Ein Abo wurde dabei nicht geprüft, weil der Testklick auf „Abonnieren“ vom Update-Dialog abgefangen wurde.
- **Echtes Smartphone:** Hardware-Decoder, Akku und Hersteller-Energiesparmodi sind ungeprüft.
- **Andere Android-Versionen:** Nur Android 14 wurde getestet.
- **Meldung bei Wiedergabefehlern:** Der neue Hinweis bei Fehlern während der Wiedergabe ist eingebaut, wurde aber nie ausgelöst.
- **Weitere Funktionen:** Downloads, eigene Playlists, Lesezeichen, Benachrichtigungen und die Mediensteuerung auf dem Sperrbildschirm.
- **Dauerbetrieb:** YouTube kann seine Schnittstellen jederzeit ändern. Die Tests belegen nur den Stand vom 6. November 2026.

## Bekannte Einschränkungen

- **Shorts:** Der Tab zeigt eine Liste aus einer `#shorts`-Suche, keinen Wisch-Feed. Die Liste enthält auch lange Videos und Livestreams.
- **Startseite:** Sie zeigt zunächst Live-Trends aus LibreTubes Voreinstellung. Kategorie und Region lassen sich oben wechseln.
- **Merkliste:** Das ist die umbenannte LibreTube-Bibliothek mit Verlauf, Downloads, Playlists und Lesezeichen.
- **Trends und Downloads:** Sie sind wegen der Grenze von 5 Menüpunkten nicht mehr in der unteren Leiste. Trends liegt auf der Startseite, Downloads in der Merkliste.
- **Google-Anmeldung:** Sie ist nicht eingebaut, weil es dafür keine offiziell unterstützte Lösung für diese App gibt.
- **Bisherige App 1.5:** Sie lässt sich nicht aktualisieren, wenn sie anders signiert ist oder einen anderen Paketnamen hat. Dann ist eine Neuinstallation nötig.
- **Update-Dialog:** Nach „Später“ erscheint er bei jedem App-Start wieder und auch beim Öffnen von Videolinks. Eine Version lässt sich nicht dauerhaft überspringen.
- **Erste Aktualisierung:** Android verlangt einmalig die Erlaubnis, Apps aus Ayyıldız Tube zu installieren. Diese Systemseite ist je nach Gerät anders beschriftet.
- **Test-Builds:** Die Debug-APK heißt `com.ayyildiztube.app.debug` und kann sich nicht selbst aktualisieren.

## Behobene Fehler

| Fehler | Ursache | Lösung |
| --- | --- | --- |
| SponsorBlock übersprang nie automatisch | LibreTube-Migration setzte bei Neuinstallation alle Kategorien auf „manuell“ | Migration greift nur noch bei echten Altwerten |
| Update-Prüfung hätte LibreTube-Versionen angeboten | Abfrage zeigte auf das LibreTube-Repository und verglich Ziffern falsch | Eigene Update-Funktion mit Versionsvergleich |
| Wiedergabefehler blieben stumm | `onPlayerError` versuchte nur erneut abzuspielen | Verständliche deutsche Meldung |
| Oben stand weiter „LibreTube“ | Schriftzug war im Code fest eingetragen | Durch „Ayyıldız Tube“ ersetzt |
| Altes Skript brauchte Pillow und ersetzte nicht alle Icons | PNG-Verarbeitung und unvollständiges Patchen | Vektor-Icon für alle Varianten, nur noch Python-Standardbibliothek |
| GitHub Actions lief nie | Workflow lag im Hauptordner statt in `.github/workflows/` | An den richtigen Ort verschoben |
| Build brach beim SDK-Setup ab | `setup-android@v3` verlangte das entfernte Paket `tools` | Vorinstalliertes SDK des Runners verwendet |
| Bericht meldete Erfolg ohne APK | Die Pipe mit `tee` verschluckte den Fehlercode | `pipefail` gesetzt |
