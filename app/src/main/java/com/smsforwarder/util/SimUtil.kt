package com.smsforwarder.util

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

object SimUtil {

    private const val TAG = "SimUtil"

    /**
     * Returns the 1-based SIM slot index for the given [subscriptionId].
     * Returns 1 as a safe default when detection fails.
     */
    fun getSimSlot(context: Context, subscriptionId: Int): Int {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as SubscriptionManager
            val info = subManager.getActiveSubscriptionInfo(subscriptionId)
            if (info != null) {
                info.simSlotIndex + 1   // convert 0-based to 1-based
            } else {
                Log.w(TAG, "No subscription info for id=$subscriptionId, defaulting to SIM 1")
                1
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE denied – cannot determine SIM slot", e)
            1
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error determining SIM slot", e)
            1
        }
    }

    /**
     * Returns how many active SIM cards are present, or -1 if permission is denied.
     */
    fun getActiveSIMCount(context: Context): Int {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as SubscriptionManager
            subManager.activeSubscriptionInfoCount
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE denied – cannot count SIMs", e)
            -1
        }
    }
}
