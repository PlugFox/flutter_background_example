package tld.domain.background

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import tld.domain.background_controller.BackgroundController
import tld.domain.background_controller.IAttachableBackgroundController

/// BackgroundPlugin - the main plugin class that will be registered with the FlutterEngine
/// when the FlutterEngine is attached to the Activity.
class BackgroundPlugin: FlutterPlugin {
  companion object {
    private const val TAG: String = "BackgroundPlugin"
  }

  /// The Controller that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var controller: IAttachableBackgroundController? = null

  /// Called when the FlutterEngine is attached to the Activity
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine")
    controller = BackgroundController(flutterPluginBinding).apply { attach() }
  }

  /// Called when the FlutterEngine is detached from the Activity
  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
    controller?.detach()
  }
}
