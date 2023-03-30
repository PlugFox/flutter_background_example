package tld.domain.controller

import android.content.Context
import android.util.Log
import tld.domain.background_service.BackgroundService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import tld.domain.controller.api.*

interface IAttachableController {
    /// Attach to the FlutterEngine
    fun attach()

    /// Detach from the FlutterEngine
    fun detach()
}

class Controller(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) : IAttachableController, ApiFromDart {
    internal companion object {
        private const val TAG: String = "Controller"
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
        sink = ApiToDart(binaryMessenger)
    }

    override fun detach() {
        Log.d(TAG, "detach")
        ApiFromDart.setUp(binaryMessenger, null)
        sink = null
    }

    override fun open(openMessage: OpenMessage, callback: (Result<Unit>) -> Unit) {
        Log.d(TAG, "open")
        val entryPointRawHandler: Long = openMessage.entryPointRawHandler ?: throw Exception("entryPointRawHandler is null")
        try {
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
        } catch (exception: Throwable) {
            Log.e(TAG, "Error while closing BackgroundService", exception)
            callback(Result.failure(exception))
        }
    }
}