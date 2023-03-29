package tld.domain.background_service

import androidx.annotation.Keep
import io.flutter.Log
import io.flutter.embedding.engine.FlutterEngine
import tld.domain.background.BackgroundPlugin

@Keep
object BackgroundPluginRegistrant {
    private const val TAG = "GeneratedPluginRegistrant"
    fun registerWith(flutterEngine: FlutterEngine) {
        try {
            //flutterEngine.plugins.add(Plugin1())
            //flutterEngine.plugins.add(Plugin2())
            //flutterEngine.plugins.add(Plugin3())
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error while registering plugin: ${BackgroundPlugin::class.java.canonicalName}" +
                " with ${flutterEngine::class.java.canonicalName}",
                e
            )
        }
    }
}