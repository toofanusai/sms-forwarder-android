package com.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.smsforwarder.forwarder.ForwarderEngine
import com.smsforwarder.util.SimUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS_RECEIVED intent received")

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SMS messages", e)
            return
        }

        if (messages.isEmpty()) {
            Log.w(TAG, "No messages in intent")
            return
        }

        // Subscription ID is in the intent extras on Android 7+ (API 24+)
        val subscriptionId = intent.getIntExtra("subscription", -1)
            .takeIf { it != -1 }
            ?: intent.getIntExtra("android.telephony.extra.SLOT_INDEX", 0)

        val simSlot = if (subscriptionId >= 0) {
            SimUtil.getSimSlot(context, subscriptionId)
        } else 1

        // Group multi-part messages by sender
        val grouped = mutableMapOf<String, StringBuilder>()
        for (msg in messages) {
            val sender = msg.originatingAddress ?: "Unknown"
            grouped.getOrPut(sender) { StringBuilder() }.append(msg.messageBody)
        }

        val dateMillis = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val engine = ForwarderEngine(context)

        // Use goAsync() so we can launch coroutines safely from a BroadcastReceiver
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, bodyBuilder) in grouped) {
                    engine.process(
                        sender = sender,
                        body = bodyBuilder.toString(),
                        simSlot = simSlot,
                        dateMillis = dateMillis
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
