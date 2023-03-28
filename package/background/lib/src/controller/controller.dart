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

  /// Dispose the controller and subscriptions.
  /// This method should not be called directly.
  @override
  @mustCallSuper
  @visibleForTesting
  void dispose() {
    g.ApiToDart.setup(null);
    super.dispose();
  }
}
