import 'dart:async';
import 'dart:developer' as developer;

/// Background entry point
@pragma('vm:entry-point')
void main() => runZonedGuarded<void>(
      () async {
        /* Your code */
        for (var i = 0;; i++) {
          await Future<void>.delayed(const Duration(seconds: 1));
          developer.log('Hello from background #$i', name: 'background');
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
