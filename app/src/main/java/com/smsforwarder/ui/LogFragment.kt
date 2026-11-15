package com.smsforwarder.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smsforwarder.R
import com.smsforwarder.data.DeliveryStatus
import com.smsforwarder.data.LogEntry
import com.smsforwarder.data.RuleRepository
import com.smsforwarder.databinding.FragmentLogBinding
import com.smsforwarder.databinding.ItemLogBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LogViewModel by viewModels {
        LogViewModelFactory(requireContext())
    }

    private lateinit var adapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        adapter = LogAdapter()
        binding.recyclerLog.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLog.adapter = adapter

        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.textEmptyLog.visibility =
                if (logs.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.buttonClearLog.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_log_title)
                .setMessage(R.string.clear_log_message)
                .setPositiveButton(R.string.clear) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.clearLogs()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ---- Adapter ----

class LogAdapter : ListAdapter<LogEntry, LogAdapter.LogViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.textLogTime.text = dateFormat.format(Date(entry.timestamp))
            binding.textLogSender.text = entry.sender
            binding.textLogSim.text = "SIM${entry.simSlot}"
            binding.textLogBody.text = entry.body.take(80)

            val rules = entry.matchedRules.joinToString(", ").ifEmpty { "—" }
            binding.textLogRules.text = rules

            val statusIcon = when (entry.status) {
                DeliveryStatus.SUCCESS -> "✓"
                DeliveryStatus.FAILED -> "✗"
                DeliveryStatus.PENDING -> "⏳"
            }
            binding.textLogStatus.text = statusIcon
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LogEntry>() {
            override fun areItemsTheSame(o: LogEntry, n: LogEntry) = o.id == n.id
            override fun areContentsTheSame(o: LogEntry, n: LogEntry) = o == n
        }
    }
}

// ---- ViewModel ----

class LogViewModel(private val repository: RuleRepository) : ViewModel() {
    val logs = repository.getRecentLogs()

    fun clearLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }
}

class LogViewModelFactory(private val context: android.content.Context) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LogViewModel(RuleRepository(context)) as T
    }
}
