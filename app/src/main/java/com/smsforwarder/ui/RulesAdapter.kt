package com.smsforwarder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smsforwarder.data.DestinationType
import com.smsforwarder.data.Rule
import com.smsforwarder.databinding.ItemRuleBinding

class RulesAdapter(
    private val onToggle: (Rule, Boolean) -> Unit,
    private val onEdit: (Rule) -> Unit,
    private val onDelete: (Rule) -> Unit
) : ListAdapter<Rule, RulesAdapter.RuleViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: Rule) {
            binding.textRuleName.text = rule.name
            binding.textRuleSim.text = "SIM: ${rule.sim.name}"

            val destList = rule.destinations.joinToString(" + ") { dest ->
                when (dest) {
                    DestinationType.TELEGRAM -> "Telegram"
                    DestinationType.WEBHOOK -> "Webhook"
                }
            }.ifEmpty { "بدون مقصد" }
            binding.textRuleDestinations.text = destList

            binding.switchRuleEnabled.setOnCheckedChangeListener(null)
            binding.switchRuleEnabled.isChecked = rule.enabled
            binding.switchRuleEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(rule, isChecked)
            }

            binding.root.setOnClickListener { onEdit(rule) }
            binding.buttonDeleteRule.setOnClickListener { onDelete(rule) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Rule>() {
            override fun areItemsTheSame(oldItem: Rule, newItem: Rule) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Rule, newItem: Rule) = oldItem == newItem
        }
    }
}
