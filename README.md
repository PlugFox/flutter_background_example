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
mkdir -p "./android/src/main/java/tld/domain/controller/"
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
code ./android/app/src/main/AndroidManifest.xml
```

<<

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="...">

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
  void open(OpenMessage openMessage);

  BooleanValue isOpen();

  /// Close the background service.
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
mkdir -p ./android/src/main/kotlin/tld/domain/controller
```

And run code generation

```bash
flutter pub run pigeon \
    --input "pigeons/api.dart" \
    --dart_out "lib/src/controller/api.g.dart" \
    --kotlin_out "android/src/main/kotlin/tld/domain/controller/Api.kt" \
    --kotlin_package "tld.domain.controller.api"
```

---

7. `[background]` Add icon image for notification. (optional)
   [Notification Icon Generator](https://romannurik.github.io/AndroidAssetStudio/icons-notification.html#source.type=clipart&source.clipart=ac_unit&source.space.trim=1&source.space.pad=0&name=ic_stat_ac_unit) will be helpful.
   path: `android/src/main/res/drawable-*`

---

8. `[main]` Check the following in your "`gradle.properties`" file:

```bash
code ./android/gradle.properties
```

<<

```xml
android.useAndroidX=true
android.enableJetifier=true
```

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
echo 'export "src/controller/background_scope.dart";' >> ./lib/background_scope.dart
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
