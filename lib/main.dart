import 'dart:async';

import 'package:flutter/material.dart';
import 'package:projectname/src/app.dart';

@pragma('vm:entry-point')
void main([List<String>? args]) => runZonedGuarded<Future<void>>(() async {
      WidgetsFlutterBinding.ensureInitialized();
      runApp(const App());
    }, (error, stackTrace) {
      print('$error\n$stackTrace');
    });
