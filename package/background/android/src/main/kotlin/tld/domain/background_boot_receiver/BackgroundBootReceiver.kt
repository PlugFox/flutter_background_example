package tld.domain.background_boot_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import tld.domain.background_service.BackgroundService

/// Receives the BOOT_COMPLETED broadcast and starts the BackgroundService
class BackgroundBootReceiver : BroadcastReceiver() {
    internal companion object {
        private const val TAG: String = "BackgroundBootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || BackgroundService.isExecutingDart) {
            Toast.makeText(
                context,
                "Can't launch background service with BackgroundBootReceiver",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "[onReceive] Start BackgroundService on intent ${intent.action}")
                try {
                    if (!BackgroundService.startBackgroundService(context)) {
                        Toast.makeText(
                            context,
                            "Can't launch background service",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (exception: Throwable) {
                    Log.e(TAG, "Error while starting Background service", exception)
                    Toast.makeText(context, "Error while starting Background service: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}