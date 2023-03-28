// Autogenerated from Pigeon (v9.1.1), do not edit directly.
// See also: https://pub.dev/packages/pigeon
// ignore_for_file: public_member_api_docs, non_constant_identifier_names, avoid_as, unused_import, unnecessary_parenthesis, prefer_null_aware_operators, omit_local_variable_types, unused_shown_name, unnecessary_import

import 'dart:async';
import 'dart:typed_data' show Float64List, Int32List, Int64List, Uint8List;

import 'package:flutter/foundation.dart' show ReadBuffer, WriteBuffer;
import 'package:flutter/services.dart';

/// Contains a boolean value.
class BooleanValue {
  BooleanValue({
    this.value,
  });

  bool? value;

  Object encode() {
    return <Object?>[
      value,
    ];
  }

  static BooleanValue decode(Object result) {
    result as List<Object?>;
    return BooleanValue(
      value: result[0] as bool?,
    );
  }
}

/// Payload for the message that will be sent to the native code
/// and spawn the background service.
class OpenMessage {
  OpenMessage({
    this.entryPointRawHandler,
  });

  int? entryPointRawHandler;

  Object encode() {
    return <Object?>[
      entryPointRawHandler,
    ];
  }

  static OpenMessage decode(Object result) {
    result as List<Object?>;
    return OpenMessage(
      entryPointRawHandler: result[0] as int?,
    );
  }
}

class _ApiFromDartCodec extends StandardMessageCodec {
  const _ApiFromDartCodec();
  @override
  void writeValue(WriteBuffer buffer, Object? value) {
    if (value is BooleanValue) {
      buffer.putUint8(128);
      writeValue(buffer, value.encode());
    } else if (value is OpenMessage) {
      buffer.putUint8(129);
      writeValue(buffer, value.encode());
    } else {
      super.writeValue(buffer, value);
    }
  }

  @override
  Object? readValueOfType(int type, ReadBuffer buffer) {
    switch (type) {
      case 128: 
        return BooleanValue.decode(readValue(buffer)!);
      case 129: 
        return OpenMessage.decode(readValue(buffer)!);
      default:
        return super.readValueOfType(type, buffer);
    }
  }
}

/// Message class for communication with the native code
/// and the background service.
///
/// This class is used by the Dart code to send messages to the native code.
class ApiFromDart {
  /// Constructor for [ApiFromDart].  The [binaryMessenger] named argument is
  /// available for dependency injection.  If it is left null, the default
  /// BinaryMessenger will be used which routes to the host platform.
  ApiFromDart({BinaryMessenger? binaryMessenger})
      : _binaryMessenger = binaryMessenger;
  final BinaryMessenger? _binaryMessenger;

  static const MessageCodec<Object?> codec = _ApiFromDartCodec();

  /// Open the background service.
  Future<void> open(OpenMessage arg_openMessage) async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.ApiFromDart.open', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(<Object?>[arg_openMessage]) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else {
      return;
    }
  }

  Future<BooleanValue> isOpen() async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.ApiFromDart.isOpen', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(null) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else if (replyList[0] == null) {
      throw PlatformException(
        code: 'null-error',
        message: 'Host platform returned null value for non-null return value.',
      );
    } else {
      return (replyList[0] as BooleanValue?)!;
    }
  }

  /// Close the background service.
  Future<void> close() async {
    final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
        'dev.flutter.pigeon.ApiFromDart.close', codec,
        binaryMessenger: _binaryMessenger);
    final List<Object?>? replyList =
        await channel.send(null) as List<Object?>?;
    if (replyList == null) {
      throw PlatformException(
        code: 'channel-error',
        message: 'Unable to establish connection on channel.',
      );
    } else if (replyList.length > 1) {
      throw PlatformException(
        code: replyList[0]! as String,
        message: replyList[1] as String?,
        details: replyList[2],
      );
    } else {
      return;
    }
  }
}

/// Message class for communication with the native code
/// and the background service.
///
/// This class is used by the native code to send messages to the Dart code.
abstract class ApiToDart {
  static const MessageCodec<Object?> codec = StandardMessageCodec();

  /// Called when the background service is opened.
  void afterOpening();

  /// Called when the background service is closed.
  void afterClosing();

  static void setup(ApiToDart? api, {BinaryMessenger? binaryMessenger}) {
    {
      final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
          'dev.flutter.pigeon.ApiToDart.afterOpening', codec,
          binaryMessenger: binaryMessenger);
      if (api == null) {
        channel.setMessageHandler(null);
      } else {
        channel.setMessageHandler((Object? message) async {
          // ignore message
          api.afterOpening();
          return;
        });
      }
    }
    {
      final BasicMessageChannel<Object?> channel = BasicMessageChannel<Object?>(
          'dev.flutter.pigeon.ApiToDart.afterClosing', codec,
          binaryMessenger: binaryMessenger);
      if (api == null) {
        channel.setMessageHandler(null);
      } else {
        channel.setMessageHandler((Object? message) async {
          // ignore message
          api.afterClosing();
          return;
        });
      }
    }
  }
}
