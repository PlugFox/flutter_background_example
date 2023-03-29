package tld.domain.background

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import tld.domain.controller.Controller
import tld.domain.controller.IAttachableController

/** BackgroundPlugin */
class BackgroundPlugin: FlutterPlugin {
  companion object {
    private const val TAG: String = "BackgroundPlugin"
    /*
    /// Вызывается в момент создания отдельного FlutterEngine под Foreground Service
    /// Создает необходимые зависимости от других плагинов
    internal fun configureFlutterEngine(flutterEngine: FlutterEngine) {
      fun createDependencies(): Set<FlutterPlugin> = setOf(
        BackgroundPlugin(false),
        // Other plugins, e.g.:
        // com.baseflow.geolocator.GeolocatorPlugin()
      )
      flutterEngine.plugins.add(createDependencies())
    }
    */
  }

  /// The Controller that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var controller: IAttachableController? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onAttachedToEngine")
    controller = Controller(flutterPluginBinding).apply { attach() }
    /*
    controller = when (isFrontend) {
      true -> Controller.backgroundSetup(flutterPluginBinding)
      else -> Controller.frontendSetup(flutterPluginBinding)
    }.apply {
      attach()
    }
    */
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
    controller?.detach()
  }
}
