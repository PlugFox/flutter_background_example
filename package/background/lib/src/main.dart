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
        for (var i = 0;; i = (i + 1) % 100) {
          await Future<void>.delayed(const Duration(seconds: 1));
          developer.log('Hello from background #$i', name: 'background');
          /* if (i == 10) {
            developer.log('Let\' suicide!', name: 'background');
            BackgroundService.instance.close();
            break;
          } */
        }
      },
      (error, stackTrace) => developer.log(
        'A global error has occurred: $error',
        error: error,
        stackTrace: stackTrace,
        name: 'background',
        level: 900,
      ),
    );
