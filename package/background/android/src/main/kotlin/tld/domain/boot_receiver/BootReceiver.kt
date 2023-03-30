package tld.domain.boot_receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import tld.domain.background_service.BackgroundService

/// Receives the BOOT_COMPLETED broadcast and starts the BackgroundService
class BootReceiver : BroadcastReceiver() {
    internal companion object {
        private const val TAG: String = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || BackgroundService.isExecutingDart) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "onReceive")
                context.apply {
                    packageManager?.getLaunchIntentForPackage(packageName)?.let { intent -> startActivity(intent) }
                    //startActivity(
                    //    Intent(context, MainActivity::class.java).apply {
                    //        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    //    }
                    //)
                }
                //BackgroundService.getLastCallbackInformation(context)?.apply {
                //    BackgroundService.startBackgroundService(context, first, second)
                //}
            }
        }
    }
}