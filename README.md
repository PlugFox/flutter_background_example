# How to create background service

### Important notes:

- First of all, replace all "`domain`" and "`tld`" with your organization domain, and all "`projectname`" with your project name.
- Points started with `[main]` are related to the main app and should be done in the package root directory.
- Points started with `[background]` are related to background service and should be done in `./package/background` directory.
- Implementation of the background service on android is also known as [Foreground service](https://developer.android.com/guide/components/foreground-services).
- Our communication transport with native code through [Method Channels](https://docs.flutter.dev/development/platform-integration/platform-channels) is based on [pigeon](https://pub.dev/packages/pigeon) package.
- Good idea to split the terminal into two panes, one for the `main` app and one for the `background` service.

### Steps:

1. `[main]` Create a new Flutter project (if you don't have one already)

```bash
flutter create -t app --project-name "projectname" --org "tld.domain" --description "description" --platforms android projectname
cd projectname
```

---

2. `[main]` Create a subdirectory and package for your service

```bash
mkdir -p ./package/background
flutter create -t plugin --project-name "background" --org "tld.domain" --description "Background service" --platforms android ./package/background
rm -rf ./package/background/example ./package/background/test ./package/background/lib
mkdir -p ./package/background/lib/src
touch ./package/background/lib/background.dart
echo "library background;" > ./package/background/lib/background.dart
```

And open the second pane in the terminal with `cd ./package/background/`

---

3. `[background]` Add "pigeon" dependency to your background `pubspec.yaml`

```yaml
flutter pub add meta
flutter pub add --dev pigeon
```

---

4. `[background]` Create directories for [pigeon](https://pub.dev/packages/pigeon) codegen.
   The controller is our main class that will be used to communicate with the native code, and spawn and kill the background service.

```bash
mkdir -p "./android/src/main/kotlin/tld/domain/background_controller/"
mkdir -p "./lib/src/controller/"
```

---

5. `[background]` Add permissions and service for BackgroundService (also known as [Foreground service](https://developer.android.com/guide/components/foreground-services)) to `AndroidManifest.xml`.
   Also, set permissions are required for the background service to start, work and restart after a reboot.

Also, if you need additional permissions, you can add them here. [Read more](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE).

For example:

- `FOREGROUND_SERVICE_CAMERA`
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `FOREGROUND_SERVICE_HEALTH`
- `FOREGROUND_SERVICE_LOCATION`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`
- `FOREGROUND_SERVICE_MICROPHONE`
- `FOREGROUND_SERVICE_PHONE_CALL`
- `FOREGROUND_SERVICE_REMOTE_MESSAGING`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`

```bash
code ./android/src/main/AndroidManifest.xml
```

<<

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="tld.domain.background">

    <!-- Allows a regular application to use Service.startForeground. -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

    <!-- Allows an application to receive the Intent.ACTION_BOOT_COMPLETED
            that is broadcast after the system finishes booting. -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- Intent android.intent.action.BOOT_COMPLETED is received after a "cold" boot. -->
    <uses-permission android:name="android.permission.BOOT_COMPLETED" />

    <!-- Intent android.intent.action.QUICKBOOT_POWERON is received after a "restart" or a "reboot". -->
    <uses-permission android:name="android.permission.QUICKBOOT_POWERON" />

    <!-- Allows using PowerManager WakeLocks to keep processor from sleeping or screen from dimming. -->
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <!-- Allows applications to access information about networks. -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Allows applications to open network sockets. -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!--
          Foreground service
          - - - - - - - - - - - - - - - - - -
          Specify that the service is a foreground service that satisfies
          a particular use case with "foregroundServiceType". For example,
          a foreground service type of "location" indicates that an app
          is getting the device's current location, usually to continue
          a user-initiated action related to device location.

          The "dataSync" indicates that the service is performing a sync operation,
          such as downloading or uploading data from a remote server.

          You can assign multiple foreground service types to a particular service.
          - - - - - - - - - - - - - - - - - -
          https://developer.android.com/guide/topics/manifest/service-element
          -->
        <service android:name=".BackgroundService"
                 android:enabled="true"
                 android:exported="true"
                 android:stopWithTask="false"
                 android:foregroundServiceType="dataSync"
                 />

        <!-- Restart service after reboot
            https://developer.android.com/guide/topics/manifest/receiver-element
            -->
        <receiver
                android:name=".BootReceiver"
                android:enabled="true"
                android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
                <action android:name="android.intent.action.BOOT_COMPLETED"/>
                <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
                <action android:name="com.htc.intent.action.QUICKBOOT_POWERON"/>
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

---

6. `[background]` Create a pigeon's message class for communication with the native code.

```bash
mkdir -p ./pigeons
code ./pigeons/api.dart
```

<<

```dart
import 'package:pigeon/pigeon.dart';

/// Contains a boolean value.
class BooleanValue {
  bool? value;
}

/// Payload for the message that will be sent to the native code
/// and spawn the background service.
class OpenMessage {
  int? entryPointRawHandler;
}

/// Message class for communication with the native code
/// and the background service.
///
/// This class is used by the Dart code to send messages to the native code.
@HostApi()
abstract class ApiFromDart {
  /// Open the background service.
  @async
  void open(OpenMessage openMessage);

  BooleanValue isOpen();

  /// Close the background service.
  @async
  void close();
}

/// Message class for communication with the native code
/// and the background service.
///
/// This class is used by the native code to send messages to the Dart code.
@FlutterApi()
abstract class ApiToDart {
  /// Called when the background service is opened.
  void afterOpening();

  /// Called when the background service is closed.
  void afterClosing();
}
```

Create the following directories for pigeon code generation (if they do not exist)

```bash
mkdir -p ./lib/src/controller
mkdir -p ./android/src/main/kotlin/com/vexus/background_controller/api
```

And run code generation

```bash
flutter pub run pigeon \
    --input "pigeons/api.dart" \
    --dart_out "lib/src/controller/api.g.dart" \
    --kotlin_out "android/src/main/kotlin/com/vexus/background_controller/api/Api.kt" \
    --kotlin_package "com.vexus.background_controller.api"
```

---

7. `[background]` Add icon image for notification.
   [Notification Icon Generator](https://romannurik.github.io/AndroidAssetStudio/icons-notification.html#source.type=clipart&source.clipart=ac_unit&source.space.trim=1&source.space.pad=0&name=ic_stat_ac_unit) will be helpful.
   path: `android/src/main/res/drawable-*`

---

8. `[main]` Check the following in your "`build.gradle`" file:

```bash
code ./android/build.gradle
```

And add native android dependencies.

---

9. `[background]` Create the entry point of the background service.

```bash
code ./lib/src/main.dart
```

<<

```dart
import 'dart:async';
import 'dart:developer' as developer;

/// Background entry point
@pragma('vm:entry-point')
void main() => runZonedGuarded<void>(
      () {
        /* Your code */
      },
      (error, stackTrace) => developer.log(
        'A global error has occurred: $error',
        error: error,
        stackTrace: stackTrace,
        name: 'background',
        level: 900,
      ),
    );
```

---

10. `[background]` Create the controller and export it.

```bash
touch ./lib/src/controller/controller.dart
echo 'export "src/controller/controller.dart";' >> ./lib/background.dart
code ./lib/src/controller/controller.dart
```

<<

```dart
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart' show ChangeNotifier;
import 'package:meta/meta.dart';

import '../main.dart';
import 'api.g.dart' as g;

/// The status of the background service.
enum BackgroundStatus {
  /// The background service is opening but not yet running.
  opening('opening'),

  /// The background service is running.
  opened('opened'),

  /// The background service is closing but not yet closed.
  closing('closing'),

  /// The background service is not running.
  closed('closed');

  const BackgroundStatus(this.name);

  /// The name of the status.
  final String name;

  /// Is the background service opening?
  bool get isOpened => this == opened;

  /// Is the background service not opening?
  bool get isNotOpened => !isOpened;

  @override
  String toString() => name;
}

/// The controller of the background service.
class Controller with ChangeNotifier implements g.ApiToDart {
  /// The controller of the background service.
  factory Controller() => _internalSingleton;

  Controller._internal() : _sender = g.ApiFromDart() {
    g.ApiToDart.setup(this);
    isOpen().ignore();
  }

  static final Controller _internalSingleton = Controller._internal();

  /// The sender of messages to the native code.
  final g.ApiFromDart _sender;

  BackgroundStatus _$status = BackgroundStatus.closed;
  set _status(BackgroundStatus value) {
    if (_$status == value) return;
    _$status = value;
    notifyListeners();
  }

  /// The current status of the background service.
  @nonVirtual
  BackgroundStatus get status => _$status;

  /// Open the background service.
  @mustCallSuper
  Future<void> open() async {
    try {
      _status = BackgroundStatus.opening;
      final entryPointRawHandler = ui.PluginUtilities.getCallbackHandle(
        main,
      )?.toRawHandle();
      if (entryPointRawHandler == null) {
        throw UnsupportedError('Can not get the entry point callback handle.');
      }
      await _sender.open(
        g.OpenMessage(entryPointRawHandler: entryPointRawHandler),
      );
      _status = BackgroundStatus.opened;
      isOpen().ignore(); // Check if the background service is still running.
    } on Object {
      _status = BackgroundStatus.closed;
      rethrow;
    }
  }

  /// Close the background service.
  @mustCallSuper
  Future<void> close() async {
    try {
      _status = BackgroundStatus.closing;
      await _sender.close();
      _status = BackgroundStatus.closed;
      isOpen().ignore(); // Check if the background service is closed now.
    } on Object {
      _status = BackgroundStatus.closed;
      rethrow;
    }
  }

  /// Check if the background service is running.
  @mustCallSuper
  Future<bool> isOpen() async {
    try {
      final result = await _sender.isOpen().then<bool?>((v) => v.value);
      switch (result) {
        case true:
          _status = BackgroundStatus.opened;
          return false;
        case false:
          _status = BackgroundStatus.closed;
          return false;
        default:
          throw StateError('Service status is unknown.');
      }
    } on Object {
      _status = BackgroundStatus.closed;
      rethrow;
    }
  }

  /// Called when the background service is opened.
  @override
  @protected
  @mustCallSuper
  void afterOpening() => _status = BackgroundStatus.opened;

  /// Called when the background service is closed.
  @override
  @protected
  @mustCallSuper
  void afterClosing() => _status = BackgroundStatus.closed;
}
```

---

11. `[background]` Create the scope and export it.

```bash
touch ./lib/src/controller/background_scope.dart
echo 'export "src/controller/background_scope.dart";' >> ./lib/background.dart
code ./lib/src/controller/background_scope.dart
```

<<

```dart
import 'package:flutter/widgets.dart';

import 'controller.dart';

/// {@template background_scope}
/// BackgroundScope widget.
/// {@endtemplate}
class BackgroundScope extends StatefulWidget {
  /// {@macro background_scope}
  const BackgroundScope({
    required this.child,
    this.autoOpen = false,
    super.key,
  });

  /// Auto open the background service when the widget is mounted.
  final bool autoOpen;

  /// The widget below this widget in the tree.
  final Widget child;

  /// The state from the closest instance of this class
  static BackgroundStatus statusOf(BuildContext context,
          {bool listen = true}) =>
      _InhBackgroundScope.of(context, listen: listen)._status;

  /// Open the background service.
  static Future<void> openOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.open();

  /// Close the background service.
  static Future<void> closeOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.close();

  /// Check if the background service is running.
  static Future<bool> isOpenOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.isOpen();

  @override
  State<BackgroundScope> createState() => _BackgroundScopeState();
}

/// State for widget BackgroundScope.
class _BackgroundScopeState extends State<BackgroundScope> {
  final Controller _controller = Controller();
  late BackgroundStatus _status = _controller.status;

  @override
  void initState() {
    super.initState();
    if (widget.autoOpen) _controller.open().ignore();
    _controller.addListener(_onStatusChanged);
  }

  @override
  void dispose() {
    _controller.removeListener(_onStatusChanged);
    super.dispose();
  }

  void _onStatusChanged() {
    if (!mounted) return;
    setState(() => _status = _controller.status);
  }

  @override
  Widget build(BuildContext context) => _InhBackgroundScope(
        controller: _controller,
        status: _status,
        child: widget.child,
      );
}

/// Inherited widget for quick access in the element tree.
class _InhBackgroundScope extends InheritedWidget {
  const _InhBackgroundScope({
    required Controller controller,
    required BackgroundStatus status,
    required super.child,
  })  : _controller = controller,
        _status = status;

  final Controller _controller;
  final BackgroundStatus _status;

  /// The state from the closest instance of this class
  /// that encloses the given context, if any.
  /// e.g. `_InheritedBackgroundScope.maybeOf(context)`.
  static _InhBackgroundScope? maybeOf(BuildContext context,
          {bool listen = false}) =>
      listen
          ? context.dependOnInheritedWidgetOfExactType<_InhBackgroundScope>()
          : (context
              .getElementForInheritedWidgetOfExactType<_InhBackgroundScope>()
              ?.widget as _InhBackgroundScope?);

  static Never _notFoundInheritedWidgetOfExactType() => throw ArgumentError(
        'Out of scope, not found inherited widget '
            'a _InheritedBackgroundScope of the exact type',
        'out_of_scope',
      );

  /// The state from the closest instance of this class
  /// that encloses the given context.
  /// e.g. `_InheritedBackgroundScope.of(context)`
  static _InhBackgroundScope of(BuildContext context, {bool listen = false}) =>
      maybeOf(context, listen: listen) ?? _notFoundInheritedWidgetOfExactType();

  @override
  bool updateShouldNotify(covariant _InhBackgroundScope oldWidget) =>
      _status != oldWidget._status;
}
```

---

12. `[main]` Add the background package to the main `pubspec.yaml` file.

```bash
code ./pubspec.yaml
```

<<

```yaml
dependencies:
  # ...
  background:
    path: package/background
```

---

13. `[background]` Create background service.

```bash
mkdir -p ./android/src/main/kotlin/tld/domain/background_service
code     ./android/src/main/kotlin/tld/domain/background_service/BackgroundService.kt
```

<<

```kotlin
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
/// 1. startBackgroundService
/// 2. onCreate
/// 3. createNotificationChannel
/// 4. onStartCommand
/// 5. initService
/// 6. startForeground
/// 7. startDartIsolate
///
/// Termination:
/// 1. stopBackgroundService
/// 2. onStartCommand
/// 3. onDestroy
/// 4. closeService
/// 5. deleteNotificationChannel
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

/// Register the plugins that should be registered when the FlutterEngine is attached to
/// the BackgroundService.
@Keep
object BackgroundPluginRegistrant {
    private const val TAG = "GeneratedPluginRegistrant"
    fun registerWith(flutterEngine: FlutterEngine) {
        try {
            flutterEngine.plugins.add(BackgroundPlugin())
            //flutterEngine.plugins.add(Plugin1())
            //flutterEngine.plugins.add(Plugin2())
            //flutterEngine.plugins.add(Plugin3())
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error while registering plugin: ${BackgroundPlugin::class.java.canonicalName}" +
                " with ${flutterEngine::class.java.canonicalName}",
                e
            )
        }
    }
}
```

---

14. `[background]` Create a BackgroundController to handle the messages from the dart side.

```bash
code ./android/src/main/kotlin/tld/domain/background_controller/BackgroundController.kt
```

<<

```kotlin
package tld.domain.background_controller

import android.content.Context
import android.util.Log
import tld.domain.background_service.BackgroundService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import tld.domain.background_controller.api.*

/// A Controller that will be attached to the FlutterEngine and will be responsible for the
/// communication between Flutter and native Android.
interface IAttachableBackgroundController {
    /// Attach to the FlutterEngine.
    fun attach()

    /// Detach from the FlutterEngine.
    fun detach()
}

/// Communication between Flutter and Android side through method channels.
class BackgroundController(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) :
        IAttachableBackgroundController, ApiFromDart {

    internal companion object {
        private const val TAG: String = "BackgroundController"

        /// This sink should be registered by the [open] method and all the messages should be sent
        /// through it to the Flutter side which opened the background service.
        internal var sink: ApiToDart? = null
    }

    private val applicationContext: Context
    private val binaryMessenger: BinaryMessenger

    init {
        flutterPluginBinding.let {
            applicationContext = it.applicationContext
            binaryMessenger = it.binaryMessenger
        }
    }

    override fun attach() {
        Log.d(TAG, "attach")
        ApiFromDart.setUp(binaryMessenger, this)
    }

    override fun detach() {
        Log.d(TAG, "detach")
        ApiFromDart.setUp(binaryMessenger, null)
    }

    override fun open(openMessage: OpenMessage, callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "open")
        val entryPointRawHandler: Long = openMessage.entryPointRawHandler ?: throw Exception("entryPointRawHandler is null")
        try {
            sink = ApiToDart(binaryMessenger)
            Log.d(TAG, "startBackgroundService")
            BackgroundService.startBackgroundService(applicationContext, entryPointRawHandler)
            callback(Result.success(Unit))
            sink?.afterOpening { }
        } catch (exception: Throwable) {
            Log.e(TAG, "Error while starting BackgroundService", exception)
            callback(Result.failure(exception))
        }
    }

    override fun isOpen(): BooleanValue = BooleanValue(BackgroundService.healthCheck(applicationContext))

    override fun close(callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "close")
        try {
            BackgroundService.stopBackgroundService(applicationContext)
            callback(Result.success(Unit))
            sink?.afterClosing { }
            sink = null
        } catch (exception: Throwable) {
            Log.e(TAG, "Error while closing BackgroundService", exception)
            callback(Result.failure(exception))
        }
    }
}
```

---

15. `[background]` Configure the background plugin.

```bash
code ./android/src/main/kotlin/tld/domain/background/BackgroundPlugin.kt
```

<<

```kotlin
package tld.domain.background

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import tld.domain.background_controller.BackgroundController
import tld.domain.background_controller.IAttachableBackgroundController

/// BackgroundPlugin - the main plugin class that will be registered with the FlutterEngine
/// when the FlutterEngine is attached to the Activity.
class BackgroundPlugin: FlutterPlugin {
  companion object {
    private const val TAG: String = "BackgroundPlugin"
  }

  /// The Controller that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var controller: IAttachableBackgroundController? = null

  /// Called when the FlutterEngine is attached to the Activity
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine")
    controller = BackgroundController(flutterPluginBinding).apply { attach() }
  }

  /// Called when the FlutterEngine is detached from the Activity
  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
    controller?.detach()
  }
}
```

---

16. `[background]` Configure the auto start after reboot.
    This receiver should be registered in the `android\src\main\AndroidManifest.xml` file.

```bash
mkdir ./android/src/main/kotlin/tld/domain/background_boot_receiver
code ./android/src/main/kotlin/tld/domain/background_boot_receiver/BackgroundBootReceiver.kt
```

<<

```kotlin
package tld.domain.background_boot_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import tld.domain.background_service.BackgroundService

/// Receives the BOOT_COMPLETED broadcast and starts the BackgroundService
class BackgroundBootReceiver : BroadcastReceiver() {
    internal companion object {
        private const val TAG: String = "BackgroundBootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || BackgroundService.isExecutingDart) {
            Toast.makeText(
                context,
                "Can't launch background service with BackgroundBootReceiver",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "[onReceive] Start BackgroundService on intent ${intent.action}")
                try {
                    if (!BackgroundService.startBackgroundService(context)) {
                        Toast.makeText(
                            context,
                            "Can't launch background service",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (exception: Throwable) {
                    Log.e(TAG, "Error while starting Background service", exception)
                    Toast.makeText(context, "Error while starting Background service: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

---

17. `[background]` Allow the background to use method channels with plugins and close itself.

```bash
code lib/src/background_service.dart
```

<<

```dart
import 'package:flutter/foundation.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

import 'controller/api.g.dart';

/// Background service, should be initialized in the background entry point.
@internal
class BackgroundService {
  static final BackgroundService _internalSingleton =
      BackgroundService._internal();

  // Initialize the background service, bindings and method channels.
  static BackgroundService get instance => _internalSingleton;
  BackgroundService._internal() {
    _servicesBinding = _$BackgroundBinding();
  }

  // ignore: unused_field
  static ServicesBinding? _servicesBinding;

  Future<void> close() => ApiFromDart().close();
}

/// Binding for the background service,
/// allows to use the [SchedulerBinding] and
/// [ServicesBinding] in the background.
/// For MethodChannel and Plugins to work,
/// we need to initialize the [ServicesBinding].
class _$BackgroundBinding = BindingBase with SchedulerBinding, ServicesBinding;
```

and update background's `main.dart`

```bash
code lib/src/main.dart
```

<<

```dart
import 'dart:async';
import 'dart:developer' as developer;

import 'background_service.dart';

/// Background entry point
@pragma('vm:entry-point')
void main() => runZonedGuarded<void>(
      () async {
        // Initialize the background service, bindings,
        // method channels, and plugins.
        BackgroundService.instance;
        /* Your code goes here... */
      },
      (error, stackTrace) => developer.log(
        'A global error has occurred: $error',
        error: error,
        stackTrace: stackTrace,
        name: 'background',
        level: 900,
      ),
    );

```
