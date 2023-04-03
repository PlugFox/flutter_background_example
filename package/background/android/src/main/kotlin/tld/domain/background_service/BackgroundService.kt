package tld.domain.background_service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.annotation.Keep
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.view.FlutterCallbackInformation
import tld.domain.background.BackgroundPlugin
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
/// 1. startBackgroundService() ->
/// 2. onCreate() ->
/// 3. createNotificationChannel() ->
/// 4. onStartCommand() ->
/// 5. initService() ->
/// 6. startForeground() ->
/// 7. startDartIsolate()
///
/// Termination:
/// 1. stopBackgroundService()
/// 2. onStartCommand()
/// 3. onDestroy()
/// 4. closeService()
/// 5. deleteNotificationChannel()
class BackgroundService : Service() {
    companion object {
        /// Health check of FlutterEngine and ForegroundService
        fun healthCheck(context: Context) : Boolean {
            val dart = isExecutingDart
            val service = isServiceRunning
            fun log(message: String) {
                Log.d(TAG, "[healthCheck] $message")
                showToast(context, message)
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

        /// Show Toast message
        private fun showToast(context: Context, message: String) =
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

        /// Check if service is running
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        var isExecutingDart: Boolean
            get() = flutterEngine?.dartExecutor?.isExecutingDart == true
            private set(_) {}

        /// Start Background Service by last callback information from SharedPreferences
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context) : Boolean {
            with(BackgroundSharedPreferencesHelper.getLastCallbackInformation(context)) {
                // e.g. Pair("package:background/src/main.dart", "main")
                if (this == null) {
                    Log.d(TAG, "[startBackgroundService] No last callback information found")
                    return false
                }
                return startBackgroundService(context, first, second)
            }
        }

        /// Start Background Service by entryPointRawHandler
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, entryPointRawHandler: Long) : Boolean {
            Log.d(TAG, "[startBackgroundService] Will try to start BackgroundService " +
                    "by entryPointRawHandler: $entryPointRawHandler")
            return FlutterCallbackInformation.lookupCallbackInformation(entryPointRawHandler).let {
                startBackgroundService(context, it.callbackLibraryPath, it.callbackName)
            }
        }

        /// Start Background Service by callbackLibraryPath and callbackName
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, callbackLibraryPath: String, callbackName: String) : Boolean {
            if (callbackLibraryPath.isEmpty() || callbackName.isEmpty()) return false
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
            BackgroundSharedPreferencesHelper.putLastCallbackInformation(context, callbackLibraryPath, callbackName)
            return true
        }

        /// Stop the Background Service
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun stopBackgroundService(context: Context) {
            if (!isExecutingDart) return
            Log.d(TAG, "[stopBackgroundService] Will try to stop BackgroundService")
            Intent(context, BackgroundService::class.java).apply {
                action = ACTION_STOP_FOREGROUND_SERVICE
                context.startService(this)
            }
            // Remove last callback information if foreground service
            // should not be restarted after reboot:
            BackgroundSharedPreferencesHelper.removeLastCallbackInformation(context)
        }

        private var flutterEngine: FlutterEngine? = null
        private var isServiceRunning: Boolean = false
        private const val TAG: String = "BackgroundService"
        private const val FOREGROUND_SERVICE_ID: Int = 1
        private const val NOTIFICATION_CHANNEL_ID: String = "tld.domain.background_service.BackgroundServiceChannel"
        private const val NOTIFICATION_CHANNEL_NAME: String = "Foreground Service Channel"
        private const val ACTION_START_FOREGROUND_SERVICE: String = "ACTION_START_FOREGROUND_SERVICE"
        private const val ACTION_STOP_FOREGROUND_SERVICE: String = "ACTION_STOP_FOREGROUND_SERVICE"
        private const val CALLBACK_LIBRARY_PATH_KEY: String = "CALLBACK_LIBRARY_PATH"
        private const val CALLBACK_NAME_KEY: String = "CALLBACK_NAME"
    }

    /// On create service
    override fun onCreate() {
        isServiceRunning = true
        super.onCreate()
        Log.d(TAG, "[onCreate] BackgroundService created")
        try {
            createNotificationChannel()
        } catch (exception: Throwable) {
            Log.e(TAG, "[onCreate] Error: ${exception.message}", exception)
        }
    }

    /// Handling incoming intents and start foreground service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: String? = intent?.action
        Log.d(TAG, String.format("[onStartCommand] %s", action))
        if (action != null) when (action) {
            // For start foreground service
            ACTION_START_FOREGROUND_SERVICE -> {
                initService(intent)
                Log.d(TAG, String.format("[onStartCommand] Background service is started."))
                Toast.makeText(applicationContext, "Background service is started.", Toast.LENGTH_LONG).show()
                return START_STICKY // START_NOT_STICKY
            }
            // For stop foreground service
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

                //val packageName = applicationContext.packageName
                //val i = packageManager.getLaunchIntentForPackage(packageName)
                //var flags = PendingIntent.FLAG_CANCEL_CURRENT
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //    flags = flags or PendingIntent.FLAG_MUTABLE
                //}
                //val pi = PendingIntent.getActivity(this@BackgroundService, 11, i, flags)

                fun getNotification(): Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(it.getStringExtra("ContentTitle") ?: "Background service enabled")
                    .setContentText(it.getStringExtra("ContentText") ?: "Background service has been enabled")
                    .setSmallIcon(R.drawable.ic_background) // android.R.drawable.ic_dialog_info
                    .setColor(Color.GREEN)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setChannelId(NOTIFICATION_CHANNEL_ID)
                    .setAutoCancel(false) // Remove notification on click
                    .setOngoing(true) // Prevent swipe to remove
                    //.setContentIntent(pi)
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
            NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH or IMPORTANCE_DEFAULT or IMPORTANCE_LOW
        ).apply {
            description = "Executing process in background"
            enableLights(true) // Notifications posted to this channel should display notification lights
            lightColor = Color.GREEN // Sets the notification light color for notifications posted to this channel
        }
        val manager: NotificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        manager.createNotificationChannel(channel)
        //showToast(this, "Notification channel created")
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
            if (isExecutingDart) {
                val exception = AssertionError(
                    "BackgroundService already running!"
                )
                Log.e(TAG, "[startDartIsolate] BackgroundService already running!", exception)
                throw exception
            }
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

/// FlutterEngine lifecycle listener
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

/// Helper class for storing and retrieving callback information
/// of last started Background Service's FlutterEngine
private object BackgroundSharedPreferencesHelper {
    private const val TAG : String = "BackgroundSPHelper"
    private const val SHARED_PREFERENCES_NAME : String = "tld.domain.background_service"
    private const val CALLBACK_LIBRARY_PATH_KEY : String = "CALLBACK_LIBRARY_PATH"
    private const val CALLBACK_NAME_KEY : String = "CALLBACK_NAME"

    /// Get entry point of last started Background Service's FlutterEngine
    /// Returning <CallbackLibraryPath, CallbackName>
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun getLastCallbackInformation(context: Context): Pair<String, String>?  =
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).let {
            val callbackLibraryPath: String = it.getString(CALLBACK_LIBRARY_PATH_KEY, null) ?: return null
            val callbackName: String = it.getString(CALLBACK_NAME_KEY, null) ?: return null
            Log.d(TAG, "Callback information loaded: $callbackLibraryPath, $callbackName")
            return Pair(callbackLibraryPath, callbackName)
        }

    /// Put entry point of last started Background Service's FlutterEngine
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun putLastCallbackInformation(context: Context, callbackLibraryPath: String?, callbackName: String?) =
        with(context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()) {
            if (callbackLibraryPath?.isBlank() != false || callbackName?.isBlank() != false) {
                remove(CALLBACK_LIBRARY_PATH_KEY)
                remove(CALLBACK_NAME_KEY)
                apply()
                Log.d(TAG, "Callback information removed")
            } else {
                putString(CALLBACK_LIBRARY_PATH_KEY, callbackLibraryPath)
                putString(CALLBACK_NAME_KEY, callbackName)
                apply()
                Log.d(TAG, "Callback information saved: $callbackLibraryPath, $callbackName")
            }
        }

    /// Remove entry point of last started Background Service's FlutterEngine
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun removeLastCallbackInformation(context: Context) =
        putLastCallbackInformation(context, null, null)
}
