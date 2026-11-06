package com.github.libretube.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test
    fun newerVersionsAreDetected() {
        assertTrue(UpdateVersion.isNewer("v2.0.1", "2.0.0"))
        assertTrue(UpdateVersion.isNewer("v2.1", "2.0.9"))
        assertTrue(UpdateVersion.isNewer("v2.10.0", "2.9.0"))
        assertTrue(UpdateVersion.isNewer("3.0.0", "2.99.99"))
    }

    @Test
    fun equalOrOlderVersionsAreNotOffered() {
        assertFalse(UpdateVersion.isNewer("v2.0.0", "2.0.0"))
        assertFalse(UpdateVersion.isNewer("v2.0", "2.0.0"))
        assertFalse(UpdateVersion.isNewer("v1.9.9", "2.0.0"))
    }

    @Test
    fun invalidVersionsAreNeverNewer() {
        assertFalse(UpdateVersion.isNewer("nightly", "2.0.0"))
        assertFalse(UpdateVersion.isNewer("v3.0.0-beta", "2.0.0"))
        assertFalse(UpdateVersion.isNewer("", "2.0.0"))
        assertFalse(UpdateVersion.isNewer("v3.0.0", "unknown"))
        assertNull(UpdateVersion.parse("v99999999999.0"))
    }

    @Test
    fun checksumFilesAreParsed() {
        val hash = "A".repeat(64)
        assertEquals("a".repeat(64), UpdateVersion.parseSha256("$hash  Ayyildiz-Tube.apk\n"))
        assertEquals("a".repeat(64), UpdateVersion.parseSha256(hash))
    }

    @Test
    fun invalidChecksumFilesAreRejected() {
        assertNull(UpdateVersion.parseSha256(""))
        assertNull(UpdateVersion.parseSha256("abc  file.apk"))
        assertNull(UpdateVersion.parseSha256("z".repeat(64)))
    }
}
