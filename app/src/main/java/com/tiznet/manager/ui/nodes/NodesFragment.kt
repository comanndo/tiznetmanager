package com.tiznet.manager.ui.nodes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.PanelNode
import com.tiznet.manager.databinding.DialogNodeBinding
import com.tiznet.manager.databinding.FragmentNodesBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.snack

class NodesFragment : BaseFragment() {

    private var _binding: FragmentNodesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NodesViewModel by viewModels()
    private lateinit var stateView: StateView
    private val adapter by lazy {
        NodesAdapter(
            onPing = { node -> viewModel.ping(node) { binding.root.snack(it) } },
            onSync = { node ->
                viewModel.sync(node) { error ->
                    binding.root.snack(error ?: getString(R.string.operation_success))
                }
            },
            onDelete = { confirmDelete(it) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.fabAdd.setOnClickListener { showAddDialog() }

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

    private fun showAddDialog() {
        val dialogBinding = DialogNodeBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.nodes_add)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_create) { _, _ ->
                viewModel.register(
                    name = dialogBinding.inputName.text?.toString().orEmpty(),
                    adminLink = dialogBinding.inputAdminLink.text?.toString().orEmpty()
                ) { error -> binding.root.snack(error ?: getString(R.string.operation_success)) }
            }.show()
    }

    private fun confirmDelete(node: PanelNode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(node.name)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.remove(node) { error ->
                    binding.root.snack(error ?: getString(R.string.deleted))
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
