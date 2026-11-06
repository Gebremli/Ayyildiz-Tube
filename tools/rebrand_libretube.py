#!/usr/bin/env python3
"""Turn a clean LibreTube v32.1 checkout into Ayyıldız Tube.

The native player, SABR support, SponsorBlock and the GPL licence files stay untouched.
Every text patch must match exactly once; if upstream changes, the script stops with an
error instead of producing a half-patched app. Only the Python standard library is needed.
"""
from __future__ import annotations

import argparse
import re
import shutil
import sys
from pathlib import Path

NAME = "Ayyıldız Tube"
APP_ID = "com.ayyildiztube.app"
DEFAULT_VERSION = "2.0.0"
UPSTREAM_VERSION = "32.1"
REPO_URL = "https://github.com/Gebremli/Ayyildiz-Tube"
ROOT = Path(__file__).resolve().parents[1]
OVERLAY = ROOT / "overlay"
JAVA = Path("app/src/main/java/com/github/libretube")
RES = Path("app/src/main/res")

ADAPTIVE_ICON = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ayyildiz_icon_background" />
    <foreground android:drawable="@drawable/ayyildiz_icon_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""


def version_code(version: str) -> int:
    major, minor, patch = (int(part) for part in version.split("."))
    if minor > 99 or patch > 99:
        raise ValueError(f"Version {version}: Minor und Patch dürfen höchstens 99 sein.")
    return major * 10000 + minor * 100 + patch


def patch_file(src: Path, relative: Path | str, replacements: list[tuple[str, str]]) -> None:
    path = src / relative
    content = path.read_text(encoding="utf-8")
    for needle, replacement in replacements:
        count = content.count(needle)
        if count != 1:
            raise RuntimeError(f"{relative}: erwartete exakt 1 Stelle, gefunden {count}: {needle!r}")
        content = content.replace(needle, replacement)
    path.write_text(content, encoding="utf-8")


def patch_gradle(src: Path, version: str) -> None:
    patch_file(src, "app/build.gradle.kts", [
        ('applicationId = "com.github.libretube"', f'applicationId = "{APP_ID}"'),
        ("versionCode = 72", f"versionCode = {version_code(version)}"),
        (f'versionName = "{UPSTREAM_VERSION}"', f'versionName = "{version}"'),
        ('resValue("string", "app_name", "LibreTube")', f'resValue("string", "app_name", "{NAME}")'),
        ('resValue("string", "app_name", "LibreTube Debug")',
         f'resValue("string", "app_name", "{NAME} Test")'),
    ])


def patch_strings(src: Path) -> None:
    """Replace the visible name in all languages; string keys are left alone."""
    for path in sorted((src / RES).glob("values*/strings.xml")):
        content = path.read_text(encoding="utf-8")
        patched = re.sub(r">([^<]*)<", lambda m: m.group(0).replace("LibreTube", NAME), content)
        path.write_text(patched, encoding="utf-8")

    # The "website" card keeps pointing to libretube.dev and now credits the original project.
    for folder, label in (("values", "Based on LibreTube"), ("values-de", "Basiert auf LibreTube")):
        path = src / RES / folder / "strings.xml"
        content, count = re.subn(
            r'<string name="website">[^<]*</string>',
            f'<string name="website">{label}</string>',
            content := path.read_text(encoding="utf-8"),
        )
        if count != 1:
            raise RuntimeError(f"{path}: Eintrag 'website' nicht eindeutig ({count})")
        path.write_text(content, encoding="utf-8")


def patch_icons(src: Path) -> None:
    """All selectable launcher icons become the Ayyıldız icon; minSdk 26 always uses these files."""
    icons = sorted((src / RES / "mipmap-anydpi-v26").glob("*.xml"))
    if not icons:
        raise RuntimeError("mipmap-anydpi-v26 enthält keine Icons")
    for icon in icons:
        icon.write_text(ADAPTIVE_ICON, encoding="utf-8")

    # Upstream's debug build type ships its own launcher resources, which would override ours.
    for variant in ("debug", "release"):
        shutil.rmtree(src / "app/src" / variant / "res", ignore_errors=True)

    patch_file(src, RES / "xml/appearance_settings.xml", [
        ('app:key="icon_change"\n            app:title="@string/app_icon" />',
         'app:isPreferenceVisible="false"\n            app:key="icon_change"\n'
         '            app:title="@string/app_icon" />'),
    ])


def patch_navigation(src: Path) -> None:
    patch_file(src, JAVA / "ui/activities/MainActivity.kt", [
        ("""    private fun navigateToBottomSelectedItem(item: MenuItem): Boolean {
""", """    private fun navigateToBottomSelectedItem(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsTab) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return false
        }

"""),
        ("""        searchItem.collapseActionView()

        return item.onNavDestinationSelected(navController)
""", """        searchItem.collapseActionView()

        if (item.itemId == R.id.shortsTab) {
            if (navController.currentDestination?.id != R.id.searchResultFragment) {
                navController.navigate(NavDirections.showSearchResults("#shorts"))
            }
            return true
        }

        return item.onNavDestinationSelected(navController)
"""),
        ("PreferenceHelper.getBoolean(PreferenceKeys.AUTOMATIC_UPDATE_CHECKS, false)",
         "PreferenceHelper.getBoolean(PreferenceKeys.AUTOMATIC_UPDATE_CHECKS, true)"),
    ])
    # Shorts and settings are no navigation destinations and must never be the start page.
    patch_file(src, JAVA / "helpers/NavBarHelper.kt", [
        ("        return if (pref == Int.MAX_VALUE) {\n", "        val startId = if (pref == Int.MAX_VALUE) {\n"),
        ("""            defaultNavItems[pref].itemId
        }
""", """            defaultNavItems[pref].itemId
        }
        return startId.takeUnless { it == R.id.shortsTab || it == R.id.settingsTab }
            ?: R.id.homeFragment
"""),
    ])


def patch_updates(src: Path) -> None:
    patch_file(src, JAVA / "api/ExternalApi.kt", [
        ("https://api.github.com/repos/libre-tube/LibreTube/releases/latest",
         "https://api.github.com/repos/Gebremli/Ayyildiz-Tube/releases/latest"),
    ])
    patch_file(src, RES / "xml/general_settings.xml", [
        ('android:defaultValue="false"\n            android:icon="@drawable/ic_download_filled"\n'
         '            app:key="automatic_update_checks"',
         'android:defaultValue="true"\n            android:icon="@drawable/ic_download_filled"\n'
         '            app:key="automatic_update_checks"'),
    ])
    patch_file(src, "app/src/main/AndroidManifest.xml", [
        ('<uses-permission android:name="android.permission.WAKE_LOCK" />',
         '<uses-permission android:name="android.permission.WAKE_LOCK" />\n'
         '    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />'),
        ("    </application>", """        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.updates"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/ayyildiz_update_paths" />
        </provider>
    </application>"""),
    ])


def patch_errors(src: Path) -> None:
    """Replace the silent playback failure with messages the user can understand."""
    patch_file(src, JAVA / "ui/fragments/PlayerFragment.kt", [
        ("""            super.onPlayerError(error)
            try {
                playerController.play()
""", """            super.onPlayerError(error)
            context?.let {
                val isNetworkError = error.errorCode in
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED..
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                val message = if (isNetworkError) R.string.player_error_network else R.string.player_error_source
                android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_LONG).show()
            }
            try {
                playerController.play()
"""),
    ])
    patch_file(src, JAVA / "services/OnlinePlayerService.kt", [
        ("toastFromMainDispatcher(e.localizedMessage.orEmpty())",
         "toastFromMainDispatcher(getString(R.string.stream_load_error, e.localizedMessage.orEmpty()))"),
    ])


def patch_sponsorblock(src: Path) -> None:
    """Upstream's migration treats a missing value as legacy "visible" and forces every category
    to manual on a fresh install, so sponsors would never be skipped automatically."""
    patch_file(src, JAVA / "helpers/PreferenceHelper.kt", [
        ('val stored = getString(key, "visible")', 'val stored = getString(key, "")'),
    ])


def patch_identity(src: Path) -> None:
    patch_file(src, JAVA / "helpers/ThemeHelper.kt", [
        ('accentColor = if (DynamicColors.isDynamicColorAvailable()) "my" else "blue"',
         'accentColor = "red"'),
        ("\"Libre<span  style='color:$hexColor';>Tube</span>\"",
         "\"Ayyıldız <span  style='color:$hexColor';>Tube</span>\""),
    ])
    patch_file(src, JAVA / "ui/activities/AboutActivity.kt", [
        ('const val GITHUB_URL = "https://github.com/libre-tube/LibreTube"',
         f'const val GITHUB_URL = "{REPO_URL}"'),
    ])
    patch_file(src, RES / "layout/activity_about.xml", [
        ('app:title="@string/about" />',
         'app:subtitle="@string/ayyildiz_developer"\n            app:title="@string/about" />'),
    ])


def apply_overlay(src: Path) -> int:
    count = 0
    for file in OVERLAY.rglob("*"):
        if file.is_file():
            target = src / file.relative_to(OVERLAY)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, target)
            count += 1
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description=f"LibreTube v{UPSTREAM_VERSION} -> {NAME}")
    parser.add_argument("source", type=Path, help=f"Pfad zum LibreTube-v{UPSTREAM_VERSION}-Quellcode")
    parser.add_argument("--version", default=DEFAULT_VERSION, help="App-Version, z. B. 2.0.1")
    args = parser.parse_args()

    if not re.fullmatch(r"\d+\.\d+\.\d+", args.version):
        parser.error("--version muss das Format X.Y.Z haben.")
    src = args.source.resolve()
    gradle = src / "app/build.gradle.kts"
    if not gradle.exists() or not (src / "gradlew").exists():
        parser.error("Das ist kein vollständiger LibreTube-Quellcodeordner.")
    if f'versionName = "{UPSTREAM_VERSION}"' not in gradle.read_text(encoding="utf-8"):
        parser.error(f"Nur LibreTube v{UPSTREAM_VERSION} ist geprüft, oder der Ordner ist schon gepatcht.")

    patch_gradle(src, args.version)
    patch_strings(src)
    patch_icons(src)
    patch_navigation(src)
    patch_updates(src)
    patch_errors(src)
    patch_sponsorblock(src)
    patch_identity(src)
    copied = apply_overlay(src)

    print(f"OK: {NAME} {args.version} (versionCode {version_code(args.version)}), {copied} Overlay-Dateien.")
    print("Dies ist nur ein Quellcode-Patch, noch kein erfolgreicher Build.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
