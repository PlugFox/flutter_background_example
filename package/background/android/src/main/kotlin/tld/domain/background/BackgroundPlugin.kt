package tld.domain.background

//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
//import android.os.StrictMode
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import tld.domain.background_boot_receiver.BackgroundBootReceiver
import tld.domain.background_controller.BackgroundController
import tld.domain.background_controller.IAttachableBackgroundController

/** BackgroundPlugin */
class BackgroundPlugin: FlutterPlugin {
  companion object {
    private const val TAG: String = "BackgroundPlugin"
    private var bootReceiver: BackgroundBootReceiver? = null
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
    //registerBackgroundBootReceiver(flutterPluginBinding.applicationContext)
  }

  /// Called when the FlutterEngine is detached from the Activity
  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(TAG, "onDetachedFromEngine")
    controller?.detach()
  }

  /*
  /// Register the [BackgroundBootReceiver] to receive the BOOT_COMPLETED event
  private fun registerBackgroundBootReceiver(context: Context) {
    try {
      Log.d(TAG, "registerBackgroundBootReceiver")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // https://developer.android.com/guide/components/intents-filters#DetectUnsafeIntentLaunches
        val policy = StrictMode.VmPolicy.Builder()
          .detectUnsafeIntentLaunch()
          .build()
        StrictMode.setVmPolicy(policy)
      }
      if (bootReceiver != null) {
        context.unregisterReceiver(bootReceiver)
        bootReceiver = null
      }

      bootReceiver = BackgroundBootReceiver().apply {
        context.registerReceiver(this, IntentFilter().apply {
          addAction(Intent.ACTION_BOOT_COMPLETED)
          addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
          addAction("android.intent.action.QUICKBOOT_POWERON")
          addAction("com.htc.intent.action.QUICKBOOT_POWERON")
        })
      }
    } catch (exception: Throwable) {
      Log.w(TAG, "Exception during register BackgroundBootReceiver", exception)
    }
  }
  */
}
