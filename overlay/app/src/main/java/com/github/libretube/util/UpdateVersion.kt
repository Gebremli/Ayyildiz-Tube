package com.github.libretube.util

/**
 * Pure helpers for the Ayyıldız Tube updater, free of Android classes so they can be unit tested.
 */
object UpdateVersion {
    private val VERSION_REGEX = Regex("""^v?(\d+(?:\.\d+)*)$""")
    private val SHA256_REGEX = Regex("""^[0-9a-fA-F]{64}$""")

    /** Parses tags such as "v2.0.1" or "2.0.1"; returns null for anything else. */
    fun parse(tag: String): List<Int>? {
        val match = VERSION_REGEX.matchEntire(tag.trim()) ?: return null
        return match.groupValues[1].split(".").map { it.toIntOrNull() ?: return null }
    }

    /** True only if [remote] is strictly newer than [current]. Unparsable versions are never newer. */
    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = parse(remote) ?: return false
        val currentParts = parse(current) ?: return false
        for (index in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    /** Reads the hash from `sha256sum` output ("<hash>  <file>"); returns it in lower case. */
    fun parseSha256(content: String): String? {
        val hash = content.trim().split(Regex("""\s+""")).firstOrNull() ?: return null
        return hash.takeIf { SHA256_REGEX.matches(it) }?.lowercase()
    }
}
