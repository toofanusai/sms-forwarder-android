package com.smsforwarder.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.smsforwarder.R
import com.smsforwarder.data.*
import com.smsforwarder.databinding.FragmentRuleEditorBinding
import kotlinx.coroutines.launch

class RuleEditorFragment : Fragment() {

    private var _binding: FragmentRuleEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RuleEditorViewModel by viewModels {
        RuleEditorViewModelFactory(requireContext())
    }

    private var ruleId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRuleEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ruleId = arguments?.getString("ruleId") ?: ""

        if (ruleId.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadRule(ruleId)
            }
        }

        viewModel.rule.observe(viewLifecycleOwner) { rule ->
            rule ?: return@observe
            populateUi(rule)
        }

        setupSenderFilterToggle()
        setupAddSenderButton()
        setupSaveButton()
    }

    private fun populateUi(rule: Rule) {
        binding.editRuleName.setText(rule.name)

        when (rule.sim) {
            SimFilter.SIM1 -> binding.radioSim1.isChecked = true
            SimFilter.SIM2 -> binding.radioSim2.isChecked = true
            SimFilter.BOTH -> binding.radioSimBoth.isChecked = true
        }

        binding.switchSenderFilter.isChecked = rule.senderFilter == SenderFilter.WHITELIST
        binding.groupSenderList.visibility =
            if (rule.senderFilter == SenderFilter.WHITELIST) View.VISIBLE else View.GONE

        binding.chipGroupSenders.removeAllViews()
        rule.senderList.forEach { addSenderChip(it) }

        binding.checkboxTelegram.isChecked = rule.destinations.contains(DestinationType.TELEGRAM)
        binding.checkboxWebhook.isChecked = rule.destinations.contains(DestinationType.WEBHOOK)

        binding.groupTelegram.visibility =
            if (binding.checkboxTelegram.isChecked) View.VISIBLE else View.GONE
        binding.groupWebhook.visibility =
            if (binding.checkboxWebhook.isChecked) View.VISIBLE else View.GONE

        binding.editTelegramToken.setText(rule.telegramBotToken)
        binding.editTelegramChatId.setText(rule.telegramChatId)
        binding.editWebhookUrl.setText(rule.webhookUrl)
    }

    private fun setupSenderFilterToggle() {
        binding.switchSenderFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.groupSenderList.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.checkboxTelegram.setOnCheckedChangeListener { _, isChecked ->
            binding.groupTelegram.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.checkboxWebhook.setOnCheckedChangeListener { _, isChecked ->
            binding.groupWebhook.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupAddSenderButton() {
        binding.buttonAddSender.setOnClickListener {
            val number = binding.editNewSender.text.toString().trim()
            if (number.isNotEmpty()) {
                addSenderChip(number)
                binding.editNewSender.text?.clear()
            }
        }
    }

    private fun addSenderChip(number: String) {
        val chip = Chip(requireContext()).apply {
            text = number
            isCloseIconVisible = true
            setOnCloseIconClickListener { binding.chipGroupSenders.removeView(this) }
        }
        binding.chipGroupSenders.addView(chip)
    }

    private fun collectSenderList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroupSenders.childCount) {
            val chip = binding.chipGroupSenders.getChildAt(i) as? Chip
            chip?.text?.toString()?.let { list.add(it) }
        }
        return list
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            val name = binding.editRuleName.text.toString().trim()
            if (name.isEmpty()) {
                binding.editRuleName.error = getString(R.string.error_name_required)
                return@setOnClickListener
            }

            val sim = when {
                binding.radioSim1.isChecked -> SimFilter.SIM1
                binding.radioSim2.isChecked -> SimFilter.SIM2
                else -> SimFilter.BOTH
            }

            val senderFilter = if (binding.switchSenderFilter.isChecked)
                SenderFilter.WHITELIST else SenderFilter.ANY
            val senderList = collectSenderList()

            val destinations = mutableSetOf<DestinationType>()
            if (binding.checkboxTelegram.isChecked) destinations.add(DestinationType.TELEGRAM)
            if (binding.checkboxWebhook.isChecked) destinations.add(DestinationType.WEBHOOK)

            val rule = Rule(
                id = ruleId.ifEmpty { viewModel.newId() },
                name = name,
                sim = sim,
                senderFilter = senderFilter,
                senderList = senderList,
                destinations = destinations,
                enabled = true,
                telegramBotToken = binding.editTelegramToken.text.toString().trim(),
                telegramChatId = binding.editTelegramChatId.text.toString().trim(),
                webhookUrl = binding.editWebhookUrl.text.toString().trim()
            )

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.saveRule(rule)
                Toast.makeText(requireContext(), R.string.rule_saved, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ---- ViewModel ----

class RuleEditorViewModel(private val repository: RuleRepository) : ViewModel() {
    private val _rule = MutableLiveData<Rule?>()
    val rule: LiveData<Rule?> = _rule

    suspend fun loadRule(id: String) {
        _rule.postValue(repository.getRuleById(id))
    }

    suspend fun saveRule(rule: Rule) {
        repository.saveRule(rule)
    }

    fun newId(): String = repository.newRuleId()
}

class RuleEditorViewModelFactory(private val context: android.content.Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RuleEditorViewModel(RuleRepository(context)) as T
    }
}
