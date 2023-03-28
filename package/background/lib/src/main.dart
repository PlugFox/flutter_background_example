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
