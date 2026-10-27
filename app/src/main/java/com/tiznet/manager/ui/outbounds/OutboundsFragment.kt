package com.tiznet.manager.ui.outbounds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.Outbound
import com.tiznet.manager.databinding.DialogOutboundBinding
import com.tiznet.manager.databinding.FragmentOutboundsBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.visible
import com.tiznet.manager.util.snack

class OutboundsFragment : BaseFragment() {

    private var _binding: FragmentOutboundsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OutboundsViewModel by viewModels()
    private lateinit var stateView: StateView
    private var current: List<Outbound> = emptyList()

    private val modes = listOf(
        "warp", "psiphon", "direct", "socks", "tor", "block",
        "core_outbound", "core_endpoint", "xray_outbound"
    )

    private val adapter by lazy {
        OutboundsAdapter(
            onToggle = { viewModel.toggle(it) },
            onMove = { item, delta -> viewModel.move(item, delta, current) { e -> e?.let { m -> binding.root.snack(m) } } },
            onLongPress = { showActions(it) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOutboundsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.fabAdd.setOnClickListener { showDialog(null) }

        viewModel.state.collectOnStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    current = state.data
                    adapter.submitList(state.data)
                    if (state.data.isEmpty()) stateView.empty()
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun showActions(outbound: Outbound) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(outbound.name)
            .setItems(
                arrayOf(
                    getString(R.string.action_edit),
                    getString(R.string.outbounds_make_default),
                    getString(R.string.outbounds_reset),
                    getString(R.string.action_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> showDialog(outbound)
                    1 -> viewModel.makeDefault(outbound) { it?.let { e -> binding.root.snack(e) } }
                    2 -> viewModel.reset(outbound) { it?.let { e -> binding.root.snack(e) } }
                    3 -> viewModel.delete(outbound) { e ->
                        binding.root.snack(e ?: getString(R.string.deleted))
                    }
                }
            }.show()
    }

    private fun showDialog(existing: Outbound?) {
        val db = DialogOutboundBinding.inflate(layoutInflater)
        db.dropdownMode.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modes)
        )
        db.dropdownMode.setText(existing?.mode ?: modes.first(), false)
        existing?.let {
            db.inputName.setText(it.name)
            db.inputHost.setText(it.host)
            db.inputPort.setText(it.port?.toString().orEmpty())
            db.inputUsername.setText(it.username)
            it.config?.let { c -> db.inputConfig.setText(c) }
            db.switchEnabled.isChecked = it.enabled
        }
        val isCustom = existing?.mode in listOf("core_outbound", "core_endpoint", "xray_outbound")
        db.configLayout.visibility = if (isCustom) View.VISIBLE else View.GONE
        db.dropdownMode.setOnItemClickListener { _, _, position, _ ->
            db.configLayout.visible(position in 6..8)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.outbounds_add else R.string.action_edit)
            .setView(db.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val input = OutboundsViewModel.Input(
                    name = db.inputName.text?.toString().orEmpty(),
                    mode = modes[db.dropdownMode.listSelection.coerceAtLeast(0)],
                    enabled = db.switchEnabled.isChecked,
                    host = db.inputHost.text?.toString().orEmpty(),
                    port = db.inputPort.text?.toString()?.toIntOrNull(),
                    username = db.inputUsername.text?.toString().orEmpty(),
                    password = db.inputPassword.text?.toString().orEmpty(),
                    config = db.inputConfig.text?.toString().orEmpty()
                )
                viewModel.save(existing, input) { error ->
                    binding.root.snack(error ?: getString(R.string.saved))
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
