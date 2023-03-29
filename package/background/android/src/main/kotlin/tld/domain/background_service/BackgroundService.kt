package tld.domain.background_service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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

/// Android background service (Foreground Service)
/// with FlutterEngine inside.
class BackgroundService : Service() {
    companion object {
        // Запущен ли сервис?
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun isRunning(): Boolean = flutterEngine != null

        /// Запустить Background Service по идентификатору точки входа
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, entryPointRawHandler: Long) {
            Log.d(TAG, "[startBackgroundService] Will try to start BackgroundService by entryPointRawHandler: $entryPointRawHandler")
            FlutterCallbackInformation.lookupCallbackInformation(entryPointRawHandler).apply {
                startBackgroundService(context, callbackLibraryPath, callbackName)
            }
        }

        /// Запустить Background Service по описанию пути к точке входа
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun startBackgroundService(context: Context, callbackLibraryPath: String, callbackName: String) {
            if (callbackLibraryPath.isEmpty() || callbackName.isEmpty()) return
            if (isRunning()) stopBackgroundService(context)
            Log.d(TAG, "[startBackgroundService] Will try to start BackgroundService by callbackLibraryPath: $callbackLibraryPath, callbackName: $callbackName")
            Intent(context, BackgroundService::class.java).apply {
                putExtra("ContentTitle", "Background service enabled")
                putExtra("ContentText", "Background service has been enabled.")
                putExtra(CALLBACK_LIBRARY_PATH_KEY, callbackLibraryPath)
                putExtra(CALLBACK_NAME_KEY, callbackName)
                action = ACTION_START_FOREGROUND_SERVICE
                ContextCompat.startForegroundService(context, this)
            }
            putLastCallbackInformation(context, callbackLibraryPath, callbackName)
        }

        /// Остановить Background Service
        @Suppress("MemberVisibilityCanBePrivate", "unused")
        fun stopBackgroundService(context: Context) {
            if (!isRunning()) return
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

    /// При создании инстанса
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[onCreate] BackgroundService created")
    }

    /// Called when the service is being started.
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
        return super.onStartCommand(intent, flags, startId)
    }

    /// Called when the service is no longer used and is being destroyed permanently.
    override fun onDestroy() {
        Log.d(TAG, String.format("[onDestroy] Service is destroyed permanently."))
        try {
            closeService()
            super.onDestroy()
        } catch (error: Throwable) {
            Log.d(TAG, "[onDestroy] Kill current process to avoid memory leak in other plugin.")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
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
        if (isRunning()) {
            val exception = Exception("BackgroundService already running!")
            Log.e(TAG, "[initService] BackgroundService already running!", exception)
            throw exception
        }
        try {
            intent.let {
                val callbackLibraryPath: String =
                    it.getStringExtra(CALLBACK_LIBRARY_PATH_KEY) ?: throw Exception("CallbackLibraryPath not passed for dart entry point")
                val callbackName: String =
                    it.getStringExtra(CALLBACK_NAME_KEY) ?: throw Exception("CallbackName not passed for dart entry point")

                fun getNotification(): Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(it.getStringExtra("ContentTitle") ?: "Background service enabled")
                    .setContentText(it.getStringExtra("ContentText") ?: "Background service has been enabled")
                    //.setSmallIcon(R.drawable.ic_time)
                    .setOngoing(true)
                    .build()

                createNotificationChannel()
                startForeground(FOREGROUND_SERVICE_ID, getNotification())
                runDartEntryPoint(callbackLibraryPath, callbackName)
            }
        } catch (exception: Throwable) {
            Log.e(TAG, "[initService] An error occurred while initializing the service ${exception.message}")
            closeService()
            throw exception
        }
    }

    /// Close background service:
    /// + Destroy and discard flutter engine
    /// + Delete notification channel
    private fun closeService() {
        try {
            Log.d(TAG, "[closeService] Freeing background service resources")
            // Destroy and discard flutter engine
            flutterEngine?.apply {
                try {
                    plugins.removeAll()
                } catch (err: Throwable) {
                    Log.w(TAG, "Can't remove plugins from flutter engine ${err.message}")
                }
                destroy()
            }
            flutterEngine = null
            // Delete notification channel
            deleteNotificationChannel()
        } catch (err: Throwable) {
            Log.e(TAG, "Error freeing background service resources ${err.message}")
            throw err
        }
    }

    /// Create a notification channel for foreground service
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        Log.d(TAG, "[createNotificationChannel] Creating notification channel")
        val channel: NotificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "BackgroundService notification channel"
            enableLights(true)
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

    /// Create a new FlutterEngine and execute dart entrypoint.
    private fun runDartEntryPoint(callbackLibraryPath: String, callbackName: String) {
        try {
            Log.v(TAG, "[runDartEntryPoint] Starting flutter engine for background service")

            val loader: FlutterLoader = FlutterInjector.instance().flutterLoader()

            /* for (asset in loader.getLookupKeyForAsset("flutter_assets")) {
                Log.v(TAG, "Asset: $asset")
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
                }
            }
        } catch (error: UnsatisfiedLinkError) {
            Log.w(TAG, "[runDartEntryPoint] UnsatisfiedLinkError: After a reboot this may happen for a " +
                "short period and it is ok to ignore then! ${error.message}")
        } catch (error: Throwable) {
            flutterEngine = null
            Log.w(TAG, "[runDartEntryPoint] Unexpected exception during FlutterEngine initialization: "+
                    "${error.message}")
            throw error
        }
    }
}