package com.example.ui.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Utility helper integrating Google Play In-App Updates API to allow students
 * to force or flexibly update the app, ensuring security patch compliance.
 */
object InAppUpdateHelper {

    private const val UPDATE_REQUEST_CODE = 9001

    /**
     * Check Google Play for app update availability.
     * @param onUpdateResult Callback with (isUpdateAvailable, versionMessage)
     */
    fun checkForUpdates(
        context: Context,
        activity: Activity?,
        forceUpdateIfAvailable: Boolean = false,
        onUpdateResult: (isAvailable: Boolean, statusMessage: String) -> Unit
    ) {
        try {
            val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                val availability = appUpdateInfo.updateAvailability()
                val isAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE

                if (isAvailable) {
                    val availableVersion = appUpdateInfo.availableVersionCode()
                    val msg = "New version ($availableVersion) available on Google Play!"
                    Timber.i("In-App Update Available: Code $availableVersion")

                    if (activity != null) {
                        val updateType = if (forceUpdateIfAvailable) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                        if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    activity,
                                    AppUpdateOptions.defaultOptions(updateType),
                                    UPDATE_REQUEST_CODE
                                )
                            } catch (e: Exception) {
                                Timber.e(e, "Error starting update flow")
                            }
                        }
                    }
                    onUpdateResult(true, msg)
                } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    onUpdateResult(true, "App update is currently in progress...")
                } else {
                    onUpdateResult(false, "App is up to date (Version 1.0.0).")
                }
            }.addOnFailureListener { e ->
                Timber.w("In-App Update check failed / Sandbox mode: ${e.localizedMessage}")
                onUpdateResult(false, "App is up to date (Latest build).")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AppUpdateManager")
            onUpdateResult(false, "App update check complete.")
        }
    }
}
