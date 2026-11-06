package com.github.libretube.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.obj.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** An update step failed; [messageRes] is the explanation shown to the user. */
class UpdateFailedException(@StringRes val messageRes: Int) : Exception()

/**
 * Downloads a release APK, verifies it and hands it to the Android package installer.
 *
 * The file is only offered for installation if all of these hold:
 * - it comes from this project's GitHub Releases over HTTPS
 * - its SHA-256 matches the published checksum file
 * - its package name equals the installed app
 * - its version code is higher than the installed one
 * - it is signed with the same certificate as the installed app
 *
 * The final decision stays with the user: Android always shows its own confirmation dialog.
 */
object UpdateInstaller {
    private const val RELEASE_URL_PREFIX =
        "https://github.com/Gebremli/Ayyildiz-Tube/releases/download/"
    private const val MAX_APK_BYTES = 250L * 1024 * 1024
    private const val UPDATE_DIR = "updates"
    private const val APK_NAME = "ayyildiz-tube-update.apk"

    suspend fun downloadAndVerify(
        context: Context,
        release: UpdateInfo,
        onProgress: (percent: Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            ?: throw UpdateFailedException(R.string.update_error_no_apk)
        val checksumAsset = release.assets.firstOrNull { it.name == "${apkAsset.name}.sha256" }
            ?: throw UpdateFailedException(R.string.update_error_no_checksum)

        listOf(apkAsset.downloadUrl, checksumAsset.downloadUrl).forEach {
            if (!it.startsWith(RELEASE_URL_PREFIX)) {
                throw UpdateFailedException(R.string.update_error_source)
            }
        }

        val expectedHash = UpdateVersion.parseSha256(fetchText(checksumAsset.downloadUrl))
            ?: throw UpdateFailedException(R.string.update_error_no_checksum)

        val directory = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        val apk = File(directory, APK_NAME)
        apk.delete()

        try {
            val actualHash = download(apkAsset.downloadUrl, apk, onProgress)
            if (actualHash != expectedHash) {
                throw UpdateFailedException(R.string.update_error_checksum)
            }
            verifyPackage(context, apk)
        } catch (e: Exception) {
            apk.delete()
            throw e
        }
        apk
    }

    /** @return false if the user first has to allow installing from this app */
    fun startInstall(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri()
                )
            )
            return false
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        return true
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        return RetrofitInstance.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateFailedException(R.string.update_error_download)
            response.body.string().take(1024)
        }
    }

    private suspend fun download(url: String, target: File, onProgress: (Int) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val request = Request.Builder().url(url).build()

        RetrofitInstance.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateFailedException(R.string.update_error_download)

            val totalBytes = response.body.contentLength()
            if (totalBytes > MAX_APK_BYTES) throw UpdateFailedException(R.string.update_error_download)

            var writtenBytes = 0L
            var lastPercent = -1
            target.outputStream().use { output ->
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break

                        writtenBytes += count
                        if (writtenBytes > MAX_APK_BYTES) {
                            throw UpdateFailedException(R.string.update_error_download)
                        }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)

                        if (totalBytes > 0) {
                            val percent = (writtenBytes * 100 / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        }
        return digest.digest().toHex()
    }

    private fun verifyPackage(context: Context, apk: File) {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val update = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: throw UpdateFailedException(R.string.update_error_invalid_apk)
        val installed = packageManager.getPackageInfo(context.packageName, flags)

        if (update.packageName != context.packageName) {
            throw UpdateFailedException(R.string.update_error_invalid_apk)
        }
        if (PackageInfoCompat.getLongVersionCode(update) <=
            PackageInfoCompat.getLongVersionCode(installed)
        ) {
            throw UpdateFailedException(R.string.update_error_not_newer)
        }

        val updateSigners = signerHashes(update)
        val installedSigners = signerHashes(installed)
        if (updateSigners.isEmpty() || updateSigners != installedSigners) {
            throw UpdateFailedException(R.string.update_error_signature)
        }
    }

    private fun signerHashes(info: PackageInfo): Set<String> {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let {
                if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map {
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toHex()
        }.toSet()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
