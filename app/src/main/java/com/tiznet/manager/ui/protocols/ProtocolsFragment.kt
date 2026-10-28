package com.tiznet.manager.ui.protocols

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
import com.tiznet.manager.data.model.ProtocolSwitch
import com.tiznet.manager.databinding.FragmentProtocolsBinding
import com.tiznet.manager.databinding.ItemProtocolBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.snack

class ProtocolsFragment : BaseFragment() {

    private var _binding: FragmentProtocolsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProtocolsViewModel by viewModels()
    private lateinit var stateView: StateView
    private val adapter = ProtocolAdapter { item ->
        viewModel.toggle(item) { error ->
            error?.let { binding.root.snack(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProtocolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }

        viewModel.state.collectOnStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(
                    state.message,
                    onRetry = { if (session.isLegacyApi) openWebAdmin() else viewModel.load() },
                    retryText = getString(
                        if (session.isLegacyApi) R.string.unsupported_legacy_open
                        else R.string.action_retry
                    )
                )
                is UiState.Success -> {
                    stateView.content()
                    adapter.submitList(state.data)
                    if (state.data.isEmpty()) stateView.empty()
                }
                UiState.Idle -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class ProtocolAdapter(
    private val onToggle: (ProtocolSwitch) -> Unit
) : ListAdapter<ProtocolSwitch, ProtocolAdapter.VH>(DIFF) {

    inner class VH(val b: ItemProtocolBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemProtocolBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            label.text = item.label.ifBlank { item.key }
            category.text = item.category
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = item.enabled
            switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(item) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ProtocolSwitch>() {
            override fun areItemsTheSame(o: ProtocolSwitch, n: ProtocolSwitch) = o.key == n.key
            override fun areContentsTheSame(o: ProtocolSwitch, n: ProtocolSwitch) = o == n
        }
    }
}
