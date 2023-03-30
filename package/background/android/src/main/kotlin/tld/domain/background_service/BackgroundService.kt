package tld.domain.background_service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.view.FlutterCallbackInformation
import tld.domain.background.R

/// Android background service (Foreground Service)
/// with FlutterEngine inside.
///
/// This service is started by [BackgroundService.startBackgroundService] and stopped by
/// [BackgroundService.stopBackgroundService].
///
/// -------------------------
/// Lifecycle
/// -------------------------
/// Initialization:
/// 1. startBackgroundService(...) ->
/// 2. onCreate(...) ->
/// 3. createNotificationChannel() ->
/// 4. onStartCommand(...) ->
/// 5. initService(...) ->
/// 6. startForeground(...) ->
/// 7. startDartIsolate(...)
///
/// Termination:
/// 1. stopBackgroundService(...) ->
/// 2. onDestroy(...)
class BackgroundService : Service() {
    companion object {
        /// Health check of FlutterEngine and ForegroundService
        fun healthCheck(context: Context) : Boolean {
            val dart = isExecutingDart
            val service = isServiceRunning
            fun log(message: String) {
                Log.d(TAG, "[healthCheck] $message")
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            return if (isExecutingDart && service) {
                log("Background service is running")
                true
            } else if (!isExecutingDart && !service) {
                log("Background service is not running")
                false
            } else if (!dart) {
                log("Background service is running w/0 Dart")
                stopBackgroundService(context)
                false
            } else {
                log("Dart is running w/0 Background service")
                stopBackgroundService(context)
                false
            }
        }

        // Check if service is running
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        var isExecutingDart: Boolean
            get() = flutterEngine?.dartExecutor?.isExecutingDart == true
            private set(_) {}

        /// Start Background Service by entryPointRawHandler
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, entryPointRawHandler: Long) {
            Log.d(TAG, "[startBackgroundService] Will try to start BackgroundService " +
                    "by entryPointRawHandler: $entryPointRawHandler")
            FlutterCallbackInformation.lookupCallbackInformation(entryPointRawHandler).apply {
                startBackgroundService(context, callbackLibraryPath, callbackName)
            }
        }

        /// Start Background Service by callbackLibraryPath and callbackName
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, callbackLibraryPath: String, callbackName: String) {
            if (callbackLibraryPath.isEmpty() || callbackName.isEmpty()) return
            if (isExecutingDart) stopBackgroundService(context)
            Log.d(TAG, "[startBackgroundService] Will try to start BackgroundService " +
                    "by callbackLibraryPath: $callbackLibraryPath, callbackName: $callbackName")
            Intent(context, BackgroundService::class.java).apply {
                putExtra("ContentTitle", "Background service enabled")
                putExtra("ContentText", "Background service has been enabled.")
                putExtra(CALLBACK_LIBRARY_PATH_KEY, callbackLibraryPath)
                putExtra(CALLBACK_NAME_KEY, callbackName)
                action = ACTION_START_FOREGROUND_SERVICE
            }.also {
                ContextCompat.startForegroundService(context, it)
            }
            putLastCallbackInformation(context, callbackLibraryPath, callbackName)
        }

        /// Остановить Background Service
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun stopBackgroundService(context: Context) {
            if (!isExecutingDart) return
            Log.d(TAG, "[stopBackgroundService] Will try to stop BackgroundService")
            Intent(context, BackgroundService::class.java).apply {
                action = ACTION_STOP_FOREGROUND_SERVICE
                context.startService(this)
            }
            removeLastCallbackInformation(context)
        }

        /// Получить описание точки входа (Flutter Framework) для BackgroundService
        /// Возвращает пару <CallbackLibraryPath, CallbackName>
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun getLastCallbackInformation(context: Context): Pair<String, String>? =
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)?.let {
                val callbackLibraryPath: String = it.getString(CALLBACK_LIBRARY_PATH_KEY, "") ?: return null
                val callbackName: String = it.getString(CALLBACK_NAME_KEY, "") ?: return null
                if (callbackLibraryPath.isEmpty() || callbackName.isEmpty()) return null
                return Pair(callbackLibraryPath, callbackName)
            }

        private fun removeLastCallbackInformation(context: Context) =
            putLastCallbackInformation(context, null, null)

        private fun putLastCallbackInformation(context: Context, callbackLibraryPath: String?, callbackName: String?) =
            context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
                if (callbackLibraryPath == null
                    || callbackLibraryPath == ""
                    || callbackName == null
                    || callbackName == ""
                ) {
                    remove(CALLBACK_LIBRARY_PATH_KEY)
                    remove(CALLBACK_NAME_KEY)
                } else {
                    putString(CALLBACK_LIBRARY_PATH_KEY, callbackLibraryPath)
                    putString(CALLBACK_NAME_KEY, callbackName)
                }
            }

        private var flutterEngine: FlutterEngine? = null
        private var isServiceRunning: Boolean = false
        private const val TAG: String = "BackgroundService"
        private const val FOREGROUND_SERVICE_ID: Int = 1
        private const val NOTIFICATION_CHANNEL_ID: String = "tld.domain.background_service.BackgroundServiceChannel"
        private const val NOTIFICATION_CHANNEL_NAME = "Foreground Service Channel"
        private const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        private const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
        private const val SHARED_PREFERENCES_NAME = "tld.domain.background_service.BackgroundService"
        private const val CALLBACK_LIBRARY_PATH_KEY = "CALLBACK_LIBRARY_PATH"
        private const val CALLBACK_NAME_KEY = "CALLBACK_NAME"
    }

    /// On create service
    override fun onCreate() {
        isServiceRunning = true
        //super.onCreate()
        Log.d(TAG, "[onCreate] BackgroundService created")
        createNotificationChannel()
    }

    /// Handling incoming intents and start foreground service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: String? = intent?.action
        Log.d(TAG, String.format("[onStartCommand] %s", action))
        if (action != null) when (action) {
            // Вызывается для запуска foreground service
            ACTION_START_FOREGROUND_SERVICE -> {
                initService(intent)
                Log.d(TAG, String.format("[onStartCommand] Background service is started."))
                Toast.makeText(applicationContext, "Background service is started.", Toast.LENGTH_LONG).show()
                return START_NOT_STICKY
            }
            // Вызывается для остановки foreground service
            ACTION_STOP_FOREGROUND_SERVICE -> {
                Log.d(TAG, "[onStartCommand] Stop foreground service and remove the notification.")

                // Stop foreground service and remove the notification.
                stopForeground(true)

                // Stop the foreground service.
                stopSelf()

                Log.d(TAG, String.format("[onStartCommand] Background service is stopped."))
                Toast.makeText(applicationContext, "Background service is stopped.", Toast.LENGTH_LONG).show()
            }
            else -> {
                Log.d(TAG, String.format("[onStartCommand] Unknown action: %s", action))
            }
        }
        return START_STICKY
    }

    /// Called when the service is no longer used and is being destroyed permanently.
    override fun onDestroy() {
        isServiceRunning = false
        Log.d(TAG, String.format("[onDestroy] Service is destroyed permanently."))
        try {
            closeService()
        } catch (exception: Throwable) {
            Log.d(TAG, "[onDestroy] Kill current process to avoid memory leak in other plugin.", exception)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        super.onDestroy()
    }

    /// Called by the system every time a client explicitly starts
    // the service by calling Context.startService(Intent),
    override fun onBind(intent: Intent?): IBinder? = null

    /// Initialize background service:
    /// + Notification channel [NOTIFICATION_CHANNEL_ID]
    /// + Message about started ForegroundService
    /// + Start ForegroundService
    /// + Create and start FlutterEngine
    private fun initService(intent: Intent) {
        Log.d(TAG, String.format("[initService] Initialize background service"))
        if (isExecutingDart) {
            val exception = Exception("BackgroundService already running!")
            Log.e(TAG, "[initService] BackgroundService already running!", exception)
            throw exception
        }
        try {
            intent.let {
                val callbackLibraryPath: String =
                    it.getStringExtra(CALLBACK_LIBRARY_PATH_KEY) ?:
                        throw Exception("CallbackLibraryPath not passed for dart entry point")
                val callbackName: String =
                    it.getStringExtra(CALLBACK_NAME_KEY) ?:
                        throw Exception("CallbackName not passed for dart entry point")

                fun getNotification(): Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(it.getStringExtra("ContentTitle") ?: "Background service enabled")
                    .setContentText(it.getStringExtra("ContentText") ?: "Background service has been enabled")
                    .setSmallIcon(R.drawable.ic_background)
                    .setAutoCancel(false) // Remove notification on click
                    .setOngoing(true) // Prevent swipe to remove
                    .build()

                startForeground(FOREGROUND_SERVICE_ID, getNotification())
                startDartIsolate(callbackLibraryPath, callbackName)
            }
        } catch (exception: Throwable) {
            Log.e(TAG, "[initService] An error occurred while initializing the service", exception)
            closeService()
            throw exception
        }
    }

    /// Close background service:
    /// + Stop foreground service and remove the notification
    /// + Destroy and discard flutter engine
    /// + Delete notification channel
    private fun closeService() {
        try {
            try {
                Log.d(TAG, "[closeService] Stop foreground service and remove the notification.")
                stopForeground(true)
            } catch (err: Throwable) {
                Log.w(TAG, "[closeService] Can't stop foreground service", err)
            }

            Log.d(TAG, "[closeService] Destroy and discard flutter engine.")
            flutterEngine?.apply {
                try {
                    serviceControlSurface.detachFromService()
                } catch (err: Throwable) {
                    Log.w(TAG, "[closeService] Can't detach service control surface from flutter engine", err)
                }
                try {
                    plugins.removeAll()
                } catch (err: Throwable) {
                    Log.w(TAG, "[closeService] Can't remove plugins from flutter engine", err)
                }
                destroy()
            }
            flutterEngine = null

            try {
                Log.d(TAG, "[closeService] Delete notification channel.")
                deleteNotificationChannel()
            } catch (err: Throwable) {
                Log.w(TAG, "[closeService] Can't delete notification channel", err)
            }
        } catch (exception: Throwable) {
            Log.e(TAG, "[closeService] Error freeing background service resources", exception)
            throw exception
        }
    }

    /// Create a notification channel for foreground service
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        Log.d(TAG, "[createNotificationChannel] Creating notification channel")
        val channel: NotificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_DEFAULT or IMPORTANCE_LOW
        ).apply {
            description = "Executing process in background"
            enableLights(true) // Notifications posted to this channel should display notification lights
        }
        val manager: NotificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        manager.createNotificationChannel(channel)
    }

    /// Delete notification channel
    private fun deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        Log.d(TAG, "[deleteNotificationChannel] Deleting notification channel")
        val manager: NotificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
    }

    /// Create a new FlutterEngine and execute Dart entrypoint.
    private fun startDartIsolate(callbackLibraryPath: String, callbackName: String) {
        try {
            Log.d(TAG, "[startDartIsolate] Starting flutter engine for background service")

            val loader: FlutterLoader = FlutterInjector.instance().flutterLoader()

            /* for (asset in loader.getLookupKeyForAsset("flutter_assets")) {
                Log.d(TAG, "Asset: $asset")
            } */

            if (!loader.initialized()) {
                loader.startInitialization(applicationContext)
                // throw AssertionError(
                //     "DartEntrypoint can only be created once a FlutterEngine is created."
                // )
            }

            applicationContext.let { ctx ->
                loader.apply {
                    startInitialization(ctx)
                    ensureInitializationComplete(ctx, emptyArray<String>())
                }
                // Set up the engine.
                flutterEngine = FlutterEngine(ctx, null, false, false).also {
                    it.serviceControlSurface.attachToService(this, null, true)

                    // You can pass arguments to the dart entrypoint here.
                    val args: MutableList<String> = MutableList(0) { "" }

                    // Start executing Dart entrypoint.
                    it.dartExecutor.executeDartEntrypoint(
                        DartExecutor.DartEntrypoint(
                            loader.findAppBundlePath(),
                            callbackLibraryPath,
                            callbackName
                        ),
                        args
                    )
                    BackgroundPluginRegistrant.registerWith(it)
                    // flutterEngine?.dartExecutor?.isolateServiceId
                    it.addEngineLifecycleListener(BackgroundFlutterEngineLifecycleListener {
                        Log.d(TAG, "FlutterEngine has shutdown and will be discarded now.")
                        flutterEngine = null
                    })
                }
            }
        } catch (exception: UnsatisfiedLinkError) {
            Log.w(TAG, "[runDartEntryPoint] UnsatisfiedLinkError: After a reboot this may happen for a " +
                "short period and it is ok to ignore then!", exception)
        } catch (exception: Throwable) {
            Log.e(TAG, "[runDartEntryPoint] Unexpected exception during FlutterEngine initialization", exception)
            throw exception
        }
    }
}

private class BackgroundFlutterEngineLifecycleListener(private val callback: () -> Unit) : FlutterEngine.EngineLifecycleListener {
    private companion object {
        private const val TAG = "BFELListener"
    }

    override fun onPreEngineRestart() {
        TODO("Not yet implemented")
    }

    override fun onEngineWillDestroy() {
        Log.d(TAG, "[onEngineWillDestroy] FlutterEngine has shutdown")
        this.callback()
    }
}