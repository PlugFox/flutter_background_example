package tld.domain.background_controller

import android.content.Context
import android.util.Log
import tld.domain.background_service.BackgroundService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import tld.domain.background_controller.api.*

/// A Controller that will be attached to the FlutterEngine and will be responsible for the
/// communication between Flutter and native Android.
interface IAttachableBackgroundController {
    /// Attach to the FlutterEngine.
    fun attach()

    /// Detach from the FlutterEngine.
    fun detach()
}

/// Communication between Flutter and Android side through method channels.
class BackgroundController(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) :
        IAttachableBackgroundController, ApiFromDart {

    internal companion object {
        private const val TAG: String = "BackgroundController"

        /// This sink should be registered by the [open] method and all the messages should be sent
        /// through it to the Flutter side which opened the background service.
        internal var sink: ApiToDart? = null
    }

    private val applicationContext: Context
    private val binaryMessenger: BinaryMessenger

    init {
        flutterPluginBinding.let {
            applicationContext = it.applicationContext
            binaryMessenger = it.binaryMessenger
        }
    }

    override fun attach() {
        Log.d(TAG, "attach")
        ApiFromDart.setUp(binaryMessenger, this)
    }

    override fun detach() {
        Log.d(TAG, "detach")
        ApiFromDart.setUp(binaryMessenger, null)
    }

    override fun open(openMessage: OpenMessage, callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "open")
        val entryPointRawHandler: Long = openMessage.entryPointRawHandler ?: throw Exception("entryPointRawHandler is null")
        try {
            sink = ApiToDart(binaryMessenger)
            Log.d(TAG, "startBackgroundService")
            BackgroundService.startBackgroundService(applicationContext, entryPointRawHandler)
            callback(Result.success(Unit))
            sink?.afterOpening { }
        } catch (exception: Throwable) {
            Log.e(TAG, "Error while starting BackgroundService", exception)
            callback(Result.failure(exception))
        }
    }

    override fun isOpen(): BooleanValue = BooleanValue(BackgroundService.healthCheck(applicationContext))

    override fun close(callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "close")
        try {
            BackgroundService.stopBackgroundService(applicationContext)
            callback(Result.success(Unit))
            sink?.afterClosing { }
            sink = null
        } catch (exception: Throwable) {
            Log.e(TAG, "Error while closing BackgroundService", exception)
            callback(Result.failure(exception))
        }
    }
}