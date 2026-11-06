package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.BuildConfig
import com.github.libretube.R
import com.github.libretube.obj.update.UpdateInfo
import com.github.libretube.util.UpdateFailedException
import com.github.libretube.util.UpdateInstaller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateAvailableDialog : DialogFragment() {
    private lateinit var release: UpdateInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        release = BundleCompat.getParcelable(
            requireArguments(), ARG_RELEASE, UpdateInfo::class.java
        )!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.update_title, release.tagName))
            .setMessage(buildMessage())
            .setPositiveButton(R.string.update_install, null)
            .setNegativeButton(R.string.update_later, null)
            .setNeutralButton(R.string.update_open_page) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, release.htmlUrl.toUri()))
            }
            .create()

        dialog.setOnShowListener {
            // set here so that the dialog stays open while downloading
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                downloadAndInstall(dialog)
            }
        }
        isCancelable = false
        return dialog
    }

    private fun buildMessage(): String {
        val versions = getString(
            R.string.update_versions, BuildConfig.VERSION_NAME, release.tagName
        )
        val changelog = release.body.orEmpty().trim()
        return if (changelog.isEmpty()) versions else "$versions\n\n$changelog"
    }

    private fun downloadAndInstall(dialog: AlertDialog) {
        val context = requireContext().applicationContext
        val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positive.isEnabled = false
        dialog.setMessage(getString(R.string.update_downloading, 0))

        lifecycleScope.launch {
            try {
                val apk = UpdateInstaller.downloadAndVerify(context, release) { percent ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        dialog.setMessage(getString(R.string.update_downloading, percent))
                    }
                }

                withContext(Dispatchers.Main) {
                    if (UpdateInstaller.startInstall(requireActivity(), apk)) {
                        dismissAllowingStateLoss()
                    } else {
                        dialog.setMessage(getString(R.string.update_allow_install))
                        positive.isEnabled = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UpdateFailedException) {
                showFailure(dialog, getString(e.messageRes))
            } catch (e: Exception) {
                Log.w(TAG, "update failed: $e")
                showFailure(dialog, getString(R.string.update_error_download))
            }
        }
    }

    private fun showFailure(dialog: AlertDialog, message: String) {
        dialog.setMessage(message)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ARG_RELEASE = "update_release"
        private const val TAG = "UpdateAvailableDialog"
    }
}
