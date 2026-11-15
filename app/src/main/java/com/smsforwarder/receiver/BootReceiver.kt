package com.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.smsforwarder.service.SmsForwarderService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver action=$action")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (action !in validActions) return

        val prefs: SharedPreferences =
            context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

        val startOnBoot = prefs.getBoolean("start_on_boot", true)
        if (!startOnBoot) {
            Log.d(TAG, "Start-on-boot disabled, skipping service start")
            return
        }

        Log.d(TAG, "Starting SmsForwarderService after boot")
        val serviceIntent = Intent(context, SmsForwarderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
