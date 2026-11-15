package com.smsforwarder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import com.smsforwarder.receiver.SmsReceiver
import com.smsforwarder.util.NotificationHelper

/**
 * Persistent foreground service that keeps the app alive and registers a
 * dynamic SMS receiver as a belt-and-suspenders addition to the manifest receiver.
 */
class SmsForwarderService : Service() {

    private var dynamicSmsReceiver: SmsReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        NotificationHelper.createChannels(this)

        // Must call startForeground within 5 seconds (Android requirement)
        val notification = NotificationHelper.buildServiceNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        registerDynamicReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        // Ensure we are still in foreground even if re-started
        val notification = NotificationHelper.buildServiceNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed – unregistering dynamic receiver")
        unregisterDynamicReceiver()
    }

    // ---- Dynamic receiver ----

    private fun registerDynamicReceiver() {
        if (dynamicSmsReceiver != null) return
        dynamicSmsReceiver = SmsReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            priority = 999
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(dynamicSmsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(dynamicSmsReceiver, filter)
            }
            Log.d(TAG, "Dynamic SMS receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register dynamic SMS receiver", e)
        }
    }

    private fun unregisterDynamicReceiver() {
        dynamicSmsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered
            }
            dynamicSmsReceiver = null
        }
    }

    companion object {
        private const val TAG = "SmsForwarderService"
    }
}
