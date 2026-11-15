package com.smsforwarder.forwarder

import android.content.Context
import android.util.Log
import com.smsforwarder.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Core engine: takes an incoming SMS, evaluates all rules, and dispatches to matching destinations.
 */
class ForwarderEngine(private val context: Context) {

    private val repository = RuleRepository(context)
    private val telegramForwarder = TelegramForwarder()
    private val webhookForwarder = WebhookForwarder()

    /**
     * Process an incoming SMS.
     * @param sender   the originating phone number
     * @param body     the message body
     * @param simSlot  1-based SIM slot index
     * @param dateMillis  epoch millis of the SMS
     */
    suspend fun process(
        sender: String,
        body: String,
        simSlot: Int,
        dateMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Processing SMS from $sender on SIM$simSlot")

        val rules = repository.getEnabledRules()
        val matchedRules = rules.filter { rule -> matches(rule, sender, simSlot) }

        if (matchedRules.isEmpty()) {
            Log.d(TAG, "No rules matched for sender=$sender SIM$simSlot")
            repository.addLog(
                LogEntry(
                    timestamp = dateMillis,
                    sender = sender,
                    body = body,
                    simSlot = simSlot,
                    matchedRules = emptyList(),
                    destinations = emptyList(),
                    status = DeliveryStatus.SUCCESS,
                    errorMessage = "No matching rules"
                )
            )
            return@withContext
        }

        val matchedNames = matchedRules.map { it.name }
        val allDestinations = matchedRules.flatMap { it.destinations }.toSet()
        var overallSuccess = true
        val errors = mutableListOf<String>()

        for (rule in matchedRules) {
            if (rule.destinations.contains(DestinationType.TELEGRAM)) {
                val ok = telegramForwarder.send(
                    botToken = rule.telegramBotToken,
                    chatId = rule.telegramChatId,
                    simSlot = simSlot,
                    sender = sender,
                    body = body
                )
                if (!ok) {
                    overallSuccess = false
                    errors.add("Telegram failed for rule '${rule.name}'")
                }
            }

            if (rule.destinations.contains(DestinationType.WEBHOOK)) {
                val ok = webhookForwarder.send(
                    webhookUrl = rule.webhookUrl,
                    sender = sender,
                    body = body,
                    simSlot = simSlot,
                    dateMillis = dateMillis
                )
                if (!ok) {
                    overallSuccess = false
                    errors.add("Webhook failed for rule '${rule.name}'")
                }
            }
        }

        repository.addLog(
            LogEntry(
                timestamp = dateMillis,
                sender = sender,
                body = body,
                simSlot = simSlot,
                matchedRules = matchedNames,
                destinations = allDestinations.map { it.name },
                status = if (overallSuccess) DeliveryStatus.SUCCESS else DeliveryStatus.FAILED,
                errorMessage = errors.joinToString("; ")
            )
        )
    }

    private fun matches(rule: Rule, sender: String, simSlot: Int): Boolean {
        // Check SIM filter
        val simOk = when (rule.sim) {
            SimFilter.BOTH -> true
            SimFilter.SIM1 -> simSlot == 1
            SimFilter.SIM2 -> simSlot == 2
        }
        if (!simOk) return false

        // Check sender filter
        val senderOk = when (rule.senderFilter) {
            SenderFilter.ANY -> true
            SenderFilter.WHITELIST -> rule.senderList.any { allowed ->
                sender.startsWith(allowed) || sender == allowed
            }
        }
        if (!senderOk) return false

        // Must have at least one destination configured
        return rule.destinations.isNotEmpty()
    }

    companion object {
        private const val TAG = "ForwarderEngine"
    }
}
