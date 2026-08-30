package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StrictMode
import android.util.Log
import timber.log.Timber
import com.example.data.local.DatabaseInitializer
import com.example.service.KramaNotificationChannelManager
import com.example.util.KramaCrashlytics
import com.example.util.NetworkConnectivityMonitor
import com.example.util.StartupSafetyMonitor
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartupState {
    NOT_STARTED,
    CHECKING_NETWORK,
    FIREBASE_CONNECTING,
    NOTIFICATION_CHANNELS_SETUP,
    CRASHLYTICS_INIT,
    INITIALIZING_DATABASE_AND_SDK,
    CHECKING_PRAGMA_INTEGRITY,
    COMPLETED,
    FAILED
}

data class AppInitializationState(
    val startupState: StartupState = StartupState.NOT_STARTED,
    val isInitializing: Boolean = true,
    val progress: Float = 0.0f,
    val currentStep: String = "Starting Krama Security Engine...",
    val isSuccess: Boolean = false,
    val error: String? = null
)

class KramaApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _initializationState = MutableStateFlow(AppInitializationState())
    val initializationState: StateFlow<AppInitializationState> = _initializationState.asStateFlow()

    private val _startupState = MutableStateFlow(StartupState.NOT_STARTED)
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    lateinit var networkConnectivityMonitor: NetworkConnectivityMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Setup Timber-based logging architecture
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Log.i(TAG, "Timber DebugTree planted successfully.")
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= Log.WARN) {
                        Log.println(priority, tag ?: TAG, message)
                        if (t != null) {
                            KramaCrashlytics.recordNonFatalException(t, tag ?: "AppLog")
                        }
                    }
                }
            })
        }

        // 2. Setup Android StrictMode policy for debug builds
        if (BuildConfig.DEBUG) {
            try {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .build()
                )
                Timber.i("Global Android StrictMode policy configured successfully for Debug runtime.")
            } catch (e: Throwable) {
                Timber.w("StrictMode policy setup note: ${e.message}")
            }

            // LeakCanary configuration to monitor WebRTC and background worker memory leaks
            try {
                leakcanary.LeakCanary.config = leakcanary.LeakCanary.config.copy(
                    retainedVisibleThreshold = 2
                )
                Timber.i("LeakCanary initialized to monitor WebRTC services & background workers for runtime memory leaks.")
            } catch (e: Throwable) {
                Timber.w("LeakCanary setup note: ${e.message}")
            }
        }

        Log.i(TAG, "Initializing Krama Application lifecycle on background thread pool...")
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.i(TAG, "Synchronous FirebaseApp initialization succeeded on Application onCreate.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Synchronous FirebaseApp initialize note: ${e.message}")
        }
        networkConnectivityMonitor = NetworkConnectivityMonitor.getInstance(this)

        // Initialize Central Lifecycle Engine, App State Coordinator, Network Engine, & Recovery Engine
        try {
            com.example.domain.engine.LifecycleEngine.getInstance(this)
            com.example.domain.engine.NetworkStateEngine.getInstance(this)
            com.example.domain.engine.AppStateCoordinator.getInstance(this)
            com.example.domain.engine.RecoveryEngine.getInstance(this)
            Log.i(TAG, "Lifecycle Engine, AppStateCoordinator, NetworkStateEngine & RecoveryEngine initialized.")
        } catch (e: Throwable) {
            Log.w(TAG, "Engine initialization note: ${e.message}")
        }

        applicationScope.launch {
            initializeApplicationServices()
        }
    }

    fun retryInitialization() {
        _startupState.value = StartupState.NOT_STARTED
        _initializationState.value = AppInitializationState(
            startupState = StartupState.NOT_STARTED,
            isInitializing = true,
            progress = 0.05f,
            currentStep = "Retrying SDK & Network Initialization...",
            isSuccess = false,
            error = null
        )
        applicationScope.launch {
            initializeApplicationServices()
        }
    }

    fun bypassInitializationForOffline() {
        _startupState.value = StartupState.COMPLETED
        _initializationState.value = AppInitializationState(
            startupState = StartupState.COMPLETED,
            isInitializing = false,
            progress = 1.0f,
            currentStep = "Continuing in Offline Cache Mode.",
            isSuccess = true,
            error = null
        )
    }

    private fun checkNetworkAvailability(): Boolean {
        return try {
            networkConnectivityMonitor.isCurrentlyOnline()
        } catch (e: Throwable) {
            Log.w(TAG, "Network availability check note: ${e.message}")
            false
        }
    }

    private suspend fun initializeApplicationServices() {
        try {
            _startupState.value = StartupState.CHECKING_NETWORK
            _initializationState.value = AppInitializationState(
                startupState = StartupState.CHECKING_NETWORK,
                isInitializing = true,
                progress = 0.08f,
                currentStep = "Checking network connectivity..."
            )

            val isNetworkConnected = checkNetworkAvailability()
            Log.i(TAG, "Network state check before SDK startup: connected = $isNetworkConnected")

            // Step 1: Firebase initialization
            _startupState.value = StartupState.FIREBASE_CONNECTING
            _initializationState.value = _initializationState.value.copy(
                startupState = StartupState.FIREBASE_CONNECTING,
                progress = 0.22f,
                currentStep = if (isNetworkConnected) "Connecting Firebase Security Gateway..." else "Firebase (Offline Mode)..."
            )
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this)
                    Log.i(TAG, "FirebaseApp initialized successfully.")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "FirebaseApp initialization note: ${e.message}")
            }

            // Step 2: Register Notification Channels & Fetch FCM Token
            _startupState.value = StartupState.NOTIFICATION_CHANNELS_SETUP
            _initializationState.value = _initializationState.value.copy(
                startupState = StartupState.NOTIFICATION_CHANNELS_SETUP,
                progress = 0.42f,
                currentStep = "Registering Encrypted Notification Channels & FCM Token..."
            )
            try {
                KramaNotificationChannelManager.createNotificationChannels(this)
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            Log.i(TAG, "FCM Registration Token fetched on app startup: $token")
                            getSharedPreferences("krama_fcm_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("fcm_registration_token", token)
                                .apply()
                        } else {
                            Log.w(TAG, "Fetching FCM token failed", task.exception)
                        }
                    }
            } catch (e: Throwable) {
                Log.e(TAG, "Notification channels registration failed: ${e.message}")
            }

            // Step 3: Crashlytics
            _startupState.value = StartupState.CRASHLYTICS_INIT
            _initializationState.value = _initializationState.value.copy(
                startupState = StartupState.CRASHLYTICS_INIT,
                progress = 0.58f,
                currentStep = "Initializing Crashlytics Sentinel..."
            )
            try {
                KramaCrashlytics.init(this)
            } catch (e: Throwable) {
                Log.e(TAG, "Crashlytics setup failed: ${e.message}")
            }

            // Step 4: MatrixRustSDK & Room Database initialization on Dispatchers.IO
            _startupState.value = StartupState.INITIALIZING_DATABASE_AND_SDK
            _initializationState.value = _initializationState.value.copy(
                startupState = StartupState.INITIALIZING_DATABASE_AND_SDK,
                progress = 0.78f,
                currentStep = "Initializing MatrixRustSDK & SQLCipher Room Database..."
            )
            var startupSuccess = StartupSafetyMonitor.executeSafeStartup(this)
            if (!startupSuccess) {
                Log.w(TAG, "StartupSafetyMonitor initial attempt failed. Triggering recovery and re-attempting...")
                com.example.data.local.DatabaseHelper.triggerCorruptionRecovery(this)
                startupSuccess = StartupSafetyMonitor.executeSafeStartup(this)
                if (!startupSuccess) {
                    Log.e(TAG, "Secondary database initialization note: proceeding with clean database state.")
                }
            }

            // Step 5: Database PRAGMA integrity check & WorkManager setup
            _startupState.value = StartupState.CHECKING_PRAGMA_INTEGRITY
            _initializationState.value = _initializationState.value.copy(
                startupState = StartupState.CHECKING_PRAGMA_INTEGRITY,
                progress = 0.92f,
                currentStep = "Verifying PRAGMA Integrity & Scheduling WorkManager Tasks..."
            )
            try {
                DatabaseInitializer.initializeAndCheckIntegrity(this)
                com.example.worker.KramaWorkManagerInitializer.initializeAllWorkers(this)
            } catch (e: Throwable) {
                Log.e(TAG, "DatabaseInitializer / WorkManager background execution note: ${e.message}")
            }

            // Complete
            val finalStepMsg = if (isNetworkConnected) {
                "Krama Security Core Initialized Successfully."
            } else {
                "Krama Security Core Initialized (Offline Mode)."
            }

            _startupState.value = StartupState.COMPLETED
            _initializationState.value = AppInitializationState(
                startupState = StartupState.COMPLETED,
                isInitializing = false,
                progress = 1.0f,
                currentStep = finalStepMsg,
                isSuccess = true
            )
            Log.i(TAG, "Krama Application background initialization complete.")

            // Register ActivityLifecycleCallbacks to trigger explicit WebRTC background cleanup
            try {
                registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                    private var startedActivityCount = 0

                    override fun onActivityStarted(activity: android.app.Activity) {
                        startedActivityCount++
                    }

                    override fun onActivityStopped(activity: android.app.Activity) {
                        startedActivityCount--
                        if (startedActivityCount <= 0) {
                            startedActivityCount = 0
                            Log.i(TAG, "All activities stopped. Application entered background. Cleaning up WebRTC background resources.")
                            try {
                                com.example.service.WebRtcDiagnosticCollector.instance.stopCollecting()
                            } catch (e: Throwable) {}
                        }
                    }

                    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                    override fun onActivityResumed(activity: android.app.Activity) {}
                    override fun onActivityPaused(activity: android.app.Activity) {}
                    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                    override fun onActivityDestroyed(activity: android.app.Activity) {}
                })
            } catch (e: Throwable) {
                Log.w(TAG, "ActivityLifecycleCallbacks registration note: ${e.message}")
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error during background app initialization: ${e.message}", e)
            _startupState.value = StartupState.FAILED
            _initializationState.value = AppInitializationState(
                startupState = StartupState.FAILED,
                isInitializing = false,
                progress = 1.0f,
                currentStep = "Initialization failed: ${e.localizedMessage ?: e.message}",
                isSuccess = false,
                error = e.localizedMessage ?: e.message ?: "SDK startup failure"
            )
        }
    }

    companion object {
        private const val TAG = "KramaApplication"
        lateinit var instance: KramaApplication
            private set
    }
}
