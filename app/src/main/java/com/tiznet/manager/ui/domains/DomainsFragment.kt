package com.tiznet.manager.ui.domains

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.DomainRow
import com.tiznet.manager.databinding.DialogDomainBinding
import com.tiznet.manager.databinding.FragmentDomainsBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.snack

class DomainsFragment : BaseFragment() {

    private var _binding: FragmentDomainsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DomainsViewModel by viewModels()
    private lateinit var stateView: StateView
    private val adapter = DomainsAdapter { showDialog(it) }

    private val modes = listOf("direct", "cdn", "relay", "worker", "sub_link_only")
    private val tlsModes = listOf("valid", "fake", "reality", "dns", "telegram", "shadowtls", "ssfaketls")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDomainsBinding.inflate(inflater, container, false)
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
                    adapter.submitList(state.data)
                    if (state.data.isEmpty()) stateView.empty()
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun showDialog(existing: DomainRow?) {
        val db = DialogDomainBinding.inflate(layoutInflater)
        db.dropdownMode.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modes)
        )
        db.dropdownTls.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tlsModes)
        )
        db.dropdownMode.setText(existing?.mode ?: modes.first(), false)
        db.dropdownTls.setText(existing?.fakeMode ?: tlsModes.first(), false)
        existing?.let {
            db.inputDomain.setText(it.domain)
            db.inputAlias.setText(it.alias)
            db.inputCdnIp.setText(it.cdnIp)
            db.inputServernames.setText(it.servernames)
            db.inputTlsPort.setText(it.tlsPort?.toString().orEmpty())
            db.inputHttpPort.setText(it.httpPort?.toString().orEmpty())
            db.switchResolve.isChecked = it.resolveIp
            db.switchEch.isChecked = it.ech
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.domains_add else R.string.action_edit)
            .setView(db.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val payload = viewModel.buildPayload(
                    domain = db.inputDomain.text?.toString().orEmpty(),
                    alias = db.inputAlias.text?.toString().orEmpty(),
                    mode = modes[db.dropdownMode.listSelection.coerceAtLeast(0)],
                    tls = tlsModes[db.dropdownTls.listSelection.coerceAtLeast(0)],
                    cdnIp = db.inputCdnIp.text?.toString().orEmpty(),
                    servernames = db.inputServernames.text?.toString().orEmpty(),
                    tlsPort = db.inputTlsPort.text?.toString()?.toIntOrNull(),
                    httpPort = db.inputHttpPort.text?.toString()?.toIntOrNull(),
                    resolve = db.switchResolve.isChecked,
                    ech = db.switchEch.isChecked
                )
                val callback: (String?) -> Unit = { error ->
                    binding.root.snack(error ?: getString(R.string.saved))
                }
                if (existing == null) viewModel.create(payload, callback)
                else viewModel.update(existing, payload, callback)
            }
            .setNeutralButton(if (existing != null) getString(R.string.action_delete) else null) { _, _ ->
                existing?.let {
                    viewModel.delete(it) { e -> binding.root.snack(e ?: getString(R.string.deleted)) }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
