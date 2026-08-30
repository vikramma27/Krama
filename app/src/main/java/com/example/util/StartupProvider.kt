package com.example.util

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Custom ContentProvider that initializes essential app services asynchronously and in parallel
 * during application startup, eliminating main-thread blocking before Activity launch.
 */
class StartupProvider : ContentProvider() {

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(): Boolean {
        val ctx = context?.applicationContext ?: return true

        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.tag(TAG).i("[STARTUP PROVIDER] ContentProvider initialized. Executing parallel startup initialization...")

        bgScope.launch {
            try {
                // 1. Parallel Firebase SDK initialization
                if (FirebaseApp.getApps(ctx).isEmpty()) {
                    FirebaseApp.initializeApp(ctx)
                    Timber.tag(TAG).i("[STARTUP PROVIDER] FirebaseApp initialized successfully in parallel background task.")
                }

                // 2. Parallel Notification Channels registration
                com.example.service.KramaNotificationChannelManager.createNotificationChannels(ctx)
                Timber.tag(TAG).i("[STARTUP PROVIDER] Notification channels registered in parallel.")

                // 3. Parallel Crashlytics init
                KramaCrashlytics.init(ctx)
                Timber.tag(TAG).i("[STARTUP PROVIDER] KramaCrashlytics initialized in parallel.")

            } catch (e: Throwable) {
                Timber.tag(TAG).w(e, "[STARTUP PROVIDER] Non-fatal exception during parallel startup tasks: ${e.message}")
            }
        }

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        private const val TAG = "StartupProvider"
    }
}
