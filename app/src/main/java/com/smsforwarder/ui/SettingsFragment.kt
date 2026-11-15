package com.smsforwarder.ui

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.smsforwarder.R
import com.smsforwarder.data.DestinationType
import com.smsforwarder.data.RuleRepository
import com.smsforwarder.databinding.FragmentSettingsBinding
import com.smsforwarder.forwarder.TelegramForwarder
import com.smsforwarder.forwarder.WebhookForwarder
import com.smsforwarder.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start on boot toggle
        binding.switchStartOnBoot.isChecked = prefs.getBoolean("start_on_boot", true)
        binding.switchStartOnBoot.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("start_on_boot", isChecked).apply()
        }

        // Battery optimization status
        updateBatteryOptStatus()

        binding.buttonBatteryOpt.setOnClickListener {
            PermissionHelper.requestBatteryOptimizationIgnore(requireContext())
        }

        // Test Telegram
        binding.buttonTestTelegram.setOnClickListener {
            lifecycleScope.launch {
                testTelegram()
            }
        }

        // Test Webhook
        binding.buttonTestWebhook.setOnClickListener {
            lifecycleScope.launch {
                testWebhook()
            }
        }

        // App version
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName, 0
            )
            binding.textAppVersion.text = getString(R.string.version_format, pInfo.versionName)
        } catch (e: Exception) {
            binding.textAppVersion.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        updateBatteryOptStatus()
    }

    private fun updateBatteryOptStatus() {
        val ignored = PermissionHelper.isIgnoringBatteryOptimizations(requireContext())
        binding.textBatteryOptStatus.text = if (ignored)
            getString(R.string.battery_opt_ok)
        else
            getString(R.string.battery_opt_warning)
    }

    private suspend fun testTelegram() {
        val repo = RuleRepository(requireContext())
        val rule = withContext(Dispatchers.IO) {
            repo.getEnabledRules().firstOrNull {
                it.destinations.contains(DestinationType.TELEGRAM)
            }
        }

        if (rule == null) {
            Toast.makeText(requireContext(), R.string.no_telegram_rule, Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonTestTelegram.isEnabled = false
        val ok = TelegramForwarder().send(
            botToken = rule.telegramBotToken,
            chatId = rule.telegramChatId,
            simSlot = 1,
            sender = "Test",
            body = getString(R.string.test_message_body)
        )
        binding.buttonTestTelegram.isEnabled = true

        val msgRes = if (ok) R.string.test_success else R.string.test_failed
        Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
    }

    private suspend fun testWebhook() {
        val repo = RuleRepository(requireContext())
        val rule = withContext(Dispatchers.IO) {
            repo.getEnabledRules().firstOrNull {
                it.destinations.contains(DestinationType.WEBHOOK)
            }
        }

        if (rule == null) {
            Toast.makeText(requireContext(), R.string.no_webhook_rule, Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonTestWebhook.isEnabled = false
        val ok = WebhookForwarder().send(
            webhookUrl = rule.webhookUrl,
            sender = "Test",
            body = getString(R.string.test_message_body),
            simSlot = 1
        )
        binding.buttonTestWebhook.isEnabled = true

        val msgRes = if (ok) R.string.test_success else R.string.test_failed
        Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
