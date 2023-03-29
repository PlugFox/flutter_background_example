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

  /// Check if the background service is open.
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
