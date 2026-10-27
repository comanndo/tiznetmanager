package com.tiznet.manager.ui.admins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.AdminRow
import com.tiznet.manager.databinding.DialogAdminBinding
import com.tiznet.manager.databinding.FragmentAdminsBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.copyToClipboard
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.snack

class AdminsFragment : BaseFragment() {

    private var _binding: FragmentAdminsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminsViewModel by viewModels()
    private lateinit var stateView: StateView
    private val adapter = AdminsAdapter { admin -> showMenu(admin) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminsBinding.inflate(inflater, container, false)
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

    private fun showMenu(admin: AdminRow) {
        val options = mutableListOf(
            getString(R.string.action_edit),
            getString(R.string.action_copy) + " · " + getString(R.string.admins_link)
        )
        if (admin.canDelete) options += getString(R.string.action_delete)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(admin.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showDialog(admin)
                    1 -> {
                        requireContext().copyToClipboard("admin link", admin.adminLink)
                        binding.root.snack(getString(R.string.action_copied))
                    }
                    2 -> confirmDelete(admin)
                }
            }.show()
    }

    private fun confirmDelete(admin: AdminRow) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(admin.name)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.delete(admin.uuid) { error ->
                    binding.root.snack(error ?: getString(R.string.deleted))
                }
            }.show()
    }

    private fun showDialog(existing: AdminRow?) {
        val dialogBinding = DialogAdminBinding.inflate(layoutInflater)
        existing?.let {
            dialogBinding.inputName.setText(it.name)
            dialogBinding.inputComment.setText(it.comment)
            dialogBinding.inputAlias.setText(it.alias)
            dialogBinding.passwordLayout.hide()
            dialogBinding.capSubAdmins.isChecked = it.capabilities["add_sub_admins"] == true
            dialogBinding.capOutbound.isChecked = it.capabilities["override_preferred_outbound"] == true
            dialogBinding.capSublinks.isChecked = it.capabilities["add_extra_sublinks"] == true
            dialogBinding.capUserExtra.isChecked = it.capabilities["modify_user_extra"] == true
            dialogBinding.capDomains.isChecked = it.capabilities["modify_domains"] == true
            dialogBinding.capOutbounds.isChecked = it.capabilities["modify_outbounds"] == true
            it.limits?.let { limits ->
                dialogBinding.inputMaxUsers.setText(limits.maxUsers.toString())
                dialogBinding.inputMaxActive.setText(limits.maxActiveUsers.toString())
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.admins_add else R.string.action_edit)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val caps = mapOf(
                    "add_sub_admins" to dialogBinding.capSubAdmins.isChecked,
                    "override_preferred_outbound" to dialogBinding.capOutbound.isChecked,
                    "add_extra_sublinks" to dialogBinding.capSublinks.isChecked,
                    "modify_user_extra" to dialogBinding.capUserExtra.isChecked,
                    "modify_domains" to dialogBinding.capDomains.isChecked,
                    "modify_outbounds" to dialogBinding.capOutbounds.isChecked
                )
                val payload = viewModel.buildPayload(
                    name = dialogBinding.inputName.text?.toString().orEmpty(),
                    comment = dialogBinding.inputComment.text?.toString().orEmpty(),
                    alias = dialogBinding.inputAlias.text?.toString().orEmpty(),
                    password = dialogBinding.inputPassword.text?.toString().orEmpty(),
                    maxUsers = dialogBinding.inputMaxUsers.text?.toString()?.toIntOrNull(),
                    maxActive = dialogBinding.inputMaxActive.text?.toString()?.toIntOrNull(),
                    caps = caps
                )
                val callback: (String?) -> Unit = { error ->
                    binding.root.snack(error ?: getString(R.string.saved))
                }
                if (existing == null) viewModel.create(payload, callback)
                else viewModel.update(existing.uuid, payload, callback)
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
