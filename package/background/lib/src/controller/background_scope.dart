import 'package:flutter/widgets.dart';

import 'controller.dart';

/// {@template background_scope}
/// BackgroundScope widget.
/// {@endtemplate}
class BackgroundScope extends StatefulWidget {
  /// {@macro background_scope}
  const BackgroundScope({
    required this.child,
    this.autoOpen = false,
    super.key,
  });

  /// Auto open the background service when the widget is mounted.
  final bool autoOpen;

  /// The widget below this widget in the tree.
  final Widget child;

  /// The state from the closest instance of this class
  static BackgroundStatus statusOf(BuildContext context,
          {bool listen = true}) =>
      _InhBackgroundScope.of(context, listen: listen)._status;

  /// Open the background service.
  static Future<void> openOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.open();

  /// Close the background service.
  static Future<void> closeOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.close();

  /// Check if the background service is running.
  static Future<bool> isOpenOf(BuildContext context) =>
      _InhBackgroundScope.of(context, listen: false)._controller.isOpen();

  @override
  State<BackgroundScope> createState() => _BackgroundScopeState();
}

/// State for widget BackgroundScope.
class _BackgroundScopeState extends State<BackgroundScope> {
  late final Controller _controller;
  late BackgroundStatus _status = _controller.status;

  @override
  void initState() {
    super.initState();
    _controller = Controller();
    if (widget.autoOpen) _controller.open().ignore();
    _controller.addListener(_onStatusChanged);
  }

  @override
  void dispose() {
    _controller.removeListener(_onStatusChanged);
    super.dispose();
  }

  void _onStatusChanged() {
    if (!mounted) return;
    setState(() => _status = _controller.status);
  }

  @override
  Widget build(BuildContext context) => _InhBackgroundScope(
        controller: _controller,
        status: _status,
        child: widget.child,
      );
}

/// Inherited widget for quick access in the element tree.
class _InhBackgroundScope extends InheritedWidget {
  const _InhBackgroundScope({
    required Controller controller,
    required BackgroundStatus status,
    required super.child,
  })  : _controller = controller,
        _status = status;

  final Controller _controller;
  final BackgroundStatus _status;

  /// The state from the closest instance of this class
  /// that encloses the given context, if any.
  /// e.g. `_InheritedBackgroundScope.maybeOf(context)`.
  static _InhBackgroundScope? maybeOf(BuildContext context,
          {bool listen = false}) =>
      listen
          ? context.dependOnInheritedWidgetOfExactType<_InhBackgroundScope>()
          : (context
              .getElementForInheritedWidgetOfExactType<_InhBackgroundScope>()
              ?.widget as _InhBackgroundScope?);

  static Never _notFoundInheritedWidgetOfExactType() => throw ArgumentError(
        'Out of scope, not found inherited widget '
            'a _InheritedBackgroundScope of the exact type',
        'out_of_scope',
      );

  /// The state from the closest instance of this class
  /// that encloses the given context.
  /// e.g. `_InheritedBackgroundScope.of(context)`
  static _InhBackgroundScope of(BuildContext context, {bool listen = false}) =>
      maybeOf(context, listen: listen) ?? _notFoundInheritedWidgetOfExactType();

  @override
  bool updateShouldNotify(covariant _InhBackgroundScope oldWidget) =>
      _status != oldWidget._status;
}
