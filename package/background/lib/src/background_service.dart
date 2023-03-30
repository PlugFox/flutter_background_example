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

class _$BackgroundBinding = BindingBase with SchedulerBinding, ServicesBinding;
