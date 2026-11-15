package com.smsforwarder.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.smsforwarder.R
import com.smsforwarder.data.Rule
import com.smsforwarder.data.RuleRepository
import com.smsforwarder.databinding.FragmentRulesBinding
import kotlinx.coroutines.launch

class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RulesViewModel by viewModels {
        RulesViewModelFactory(requireContext())
    }

    private lateinit var adapter: RulesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RulesAdapter(
            onToggle = { rule, enabled ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setEnabled(rule.id, enabled)
                }
            },
            onEdit = { rule ->
                findNavController().navigate(
                    R.id.action_rulesFragment_to_ruleEditorFragment,
                    bundleOf("ruleId" to rule.id)
                )
            },
            onDelete = { rule -> confirmDelete(rule) }
        )

        binding.recyclerRules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRules.adapter = adapter

        viewModel.rules.observe(viewLifecycleOwner) { rules ->
            adapter.submitList(rules)
            binding.textEmptyRules.visibility =
                if (rules.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddRule.setOnClickListener {
            findNavController().navigate(
                R.id.action_rulesFragment_to_ruleEditorFragment,
                bundleOf("ruleId" to "")
            )
        }
    }

    private fun confirmDelete(rule: Rule) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_rule_title)
            .setMessage(getString(R.string.delete_rule_message, rule.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteRule(rule)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ---- ViewModel ----

class RulesViewModel(private val repository: RuleRepository) : ViewModel() {
    val rules = repository.getAllRules()

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setRuleEnabled(id, enabled) }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch { repository.deleteRule(rule) }
    }
}

class RulesViewModelFactory(private val context: android.content.Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RulesViewModel(RuleRepository(context)) as T
    }
}
