import 'package:background/background_scope.dart';
import 'package:flutter/material.dart';

import 'home_screen.dart';

/// {@template app}
/// App widget.
/// {@endtemplate}
class App extends StatelessWidget {
  /// {@macro app}
  const App({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Foreground Service Demo',
        theme: ThemeData.from(
          colorScheme: ColorScheme.fromSwatch(),
          useMaterial3: true,
        ),
        home: const HomeScreen(),
        builder: (context, child) => BackgroundScope(
          child: child!,
        ),
      );
}
