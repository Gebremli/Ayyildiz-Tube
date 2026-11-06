package com.github.libretube.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.ui.dialogs.UpdateAvailableDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Looks for a newer Ayyıldız Tube version on GitHub Releases.
 * Nothing is downloaded or installed here; that only happens after the user agrees in the dialog.
 */
class UpdateChecker(private val context: Context) {
    suspend fun checkUpdate(isManualCheck: Boolean = false) {
        try {
            val release = RetrofitInstance.externalApi.getLatestRelease()

            if (UpdateVersion.isNewer(release.tagName, BuildConfig.VERSION_NAME)) {
                withContext(Dispatchers.Main) { showUpdateAvailableDialog(release) }
            } else if (isManualCheck) {
                context.toastFromMainDispatcher(R.string.app_uptodate)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.w(TAG(), "update check failed with HTTP ${e.code()}")
            if (isManualCheck) {
                // GitHub answers 404 as long as no release has been published
                val message = if (e.code() == 404) R.string.app_uptodate else R.string.update_check_failed
                context.toastFromMainDispatcher(message)
            }
        } catch (e: Exception) {
            Log.w(TAG(), "update check failed: $e")
            if (isManualCheck) context.toastFromMainDispatcher(R.string.update_check_failed)
        }
    }

    private fun showUpdateAvailableDialog(release: com.github.libretube.obj.update.UpdateInfo) {
        val fragmentManager = (context as? FragmentActivity)?.supportFragmentManager ?: return
        if (fragmentManager.isStateSaved) return

        val dialog = UpdateAvailableDialog()
        dialog.arguments = Bundle().apply {
            putParcelable(UpdateAvailableDialog.ARG_RELEASE, release)
        }
        dialog.show(fragmentManager, UpdateAvailableDialog::class.java.simpleName)
    }
}
