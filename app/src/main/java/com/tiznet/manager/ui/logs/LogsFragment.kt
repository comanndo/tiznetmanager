package com.tiznet.manager.ui.logs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.LogFile
import com.tiznet.manager.databinding.FragmentLogsBinding
import com.tiznet.manager.databinding.ItemLogfileBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show

class LogsFragment : BaseFragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogsViewModel by viewModels()
    private lateinit var stateView: StateView
    private val adapter = FilesAdapter { viewModel.open(it) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.viewerCard.hide()
        binding.btnShare.setOnClickListener { shareCurrent() }

        viewModel.state.collectOnStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    adapter.submitList(state.data)
                    if (state.data.isEmpty()) stateView.empty(getString(R.string.logs_empty))
                }
                UiState.Idle -> Unit
            }
        }
        viewModel.content.collectOnStarted { pair ->
            if (pair != null) {
                binding.viewerCard.show()
                binding.viewerTitle.text = pair.first
                binding.logContent.text = pair.second
            }
        }
    }

    private fun shareCurrent() {
        val pair = viewModel.content.value ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, pair.first)
            putExtra(Intent.EXTRA_TEXT, pair.second)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class FilesAdapter(
    private val onClick: (LogFile) -> Unit
) : ListAdapter<LogFile, FilesAdapter.VH>(DIFF) {

    inner class VH(val b: ItemLogfileBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemLogfileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = getItem(position)
        with(holder.b) {
            logName.text = file.name
            logMeta.text = com.tiznet.manager.util.formatBytes(file.size)
            root.setOnClickListener { onClick(file) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LogFile>() {
            override fun areItemsTheSame(o: LogFile, n: LogFile) = o.name == n.name
            override fun areContentsTheSame(o: LogFile, n: LogFile) = o == n
        }
    }
}
