import 'package:background/background_scope.dart';
import 'package:flutter/material.dart';

/// {@template home_screen}
/// HomeScreen widget.
/// {@endtemplate}
class HomeScreen extends StatelessWidget {
  /// {@macro home_screen}
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('Foreground Service Demo'),
        ),
        body: SafeArea(
          child: Center(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: BackgroundScope.statusOf(context).isClosed
                      ? () => BackgroundScope.openOf(context)
                      : null,
                  child: const Text('Start Background Service'),
                ),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: BackgroundScope.statusOf(context).isOpened
                      ? () => BackgroundScope.closeOf(context)
                      : null,
                  child: const Text('Stop Background Service'),
                ),
              ],
            ),
          ),
        ),
      );
}
