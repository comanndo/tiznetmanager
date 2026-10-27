package com.tiznet.manager.ui.users

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.UserRow
import com.tiznet.manager.databinding.FragmentUsersBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.show
import com.tiznet.manager.util.snack
import com.tiznet.manager.util.visible

class UsersFragment : BaseFragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UsersViewModel by viewModels()
    private lateinit var stateView: StateView
    private lateinit var adapter: UsersAdapter

    private val filters = listOf(
        "all" to R.string.users_filter_all,
        "active" to R.string.users_filter_active,
        "disabled" to R.string.users_filter_disabled,
        "expired" to R.string.users_filter_expired,
        "no_data" to R.string.users_filter_no_data
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        adapter = UsersAdapter(
            onClick = { openDetail(it) },
            onLongClick = { viewModel.toggleSelection(it.uuid) },
            selection = { viewModel.selection.value }
        )
        binding.usersList.layoutManager = LinearLayoutManager(requireContext())
        binding.usersList.adapter = adapter

        filters.forEach { (key, labelRes) ->
            val chip = Chip(requireContext()).apply {
                text = getString(labelRes)
                isCheckable = true
                id = View.generateViewId()
                tag = key
                setOnClickListener { viewModel.setFilter(key) }
            }
            binding.filterChips.addView(chip)
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = viewModel.setQuery(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.fabAdd.setOnClickListener { openEdit(null, false) }
        binding.fabBulk.setOnClickListener { openEdit(null, true) }

        binding.actionBulkEnable.setOnClickListener {
            viewModel.bulkAction("enable") { it?.let { msg -> binding.root.snack(msg) } }
        }
        binding.actionBulkDisable.setOnClickListener {
            viewModel.bulkAction("disable") { it?.let { msg -> binding.root.snack(msg) } }
        }
        binding.actionBulkReset.setOnClickListener { showResetDialog() }
        binding.actionBulkDelete.setOnClickListener { confirmBulkDelete() }

        viewModel.filter.collectOnStarted { filter ->
            for (i in 0 until binding.filterChips.childCount) {
                val chip = binding.filterChips.getChildAt(i) as Chip
                chip.isChecked = chip.tag == filter
            }
        }
        viewModel.visibleUsers.collectOnStarted { adapter.submitList(it) }
        viewModel.selection.collectOnStarted { selection ->
            binding.selectionBar.visible(selection.isNotEmpty())
            binding.selectionCount.text = getString(R.string.users_selected, selection.size)
            adapter.notifyDataSetChanged()
        }
        viewModel.state.collectOnStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                UiState.Loading -> stateView.loading()
                is UiState.Error -> stateView.error(state.message) { viewModel.load() }
                is UiState.Success -> {
                    stateView.content()
                    if (state.data.users.isEmpty()) stateView.empty()
                }
                UiState.Idle -> Unit
            }
        }
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.users_bulk_reset_usage)
            .setItems(
                arrayOf(
                    getString(R.string.users_bulk_reset_usage),
                    getString(R.string.users_bulk_reset_days),
                    getString(R.string.users_bulk_add_days),
                    getString(R.string.users_bulk_add_limit)
                )
            ) { _, which ->
                when (which) {
                    0 -> viewModel.bulkAction("reset_usage") { it?.let { m -> binding.root.snack(m) } }
                    1 -> viewModel.bulkAction("reset_days") { it?.let { m -> binding.root.snack(m) } }
                    2 -> askNumber(R.string.users_bulk_add_days) { value ->
                        viewModel.bulkAction("add_days", days = value) { it?.let { m -> binding.root.snack(m) } }
                    }
                    3 -> askNumber(R.string.users_bulk_add_limit) { value ->
                        viewModel.bulkAction("add_limits", gb = value.toDouble()) {
                            it?.let { m -> binding.root.snack(m) }
                        }
                    }
                }
            }.show()
    }

    private fun askNumber(titleRes: Int, onValue: (Int) -> Unit) {
        val input = com.google.android.material.textfield.TextInputEditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                input.text?.toString()?.toIntOrNull()?.let(onValue)
            }.show()
    }

    private fun confirmBulkDelete() {
        val count = viewModel.selection.value.size
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.users_bulk_delete)
            .setMessage(getString(R.string.users_confirm_bulk_delete, count))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.bulkAction("delete") { it?.let { m -> binding.root.snack(m) } }
            }.show()
    }

    private fun openDetail(user: UserRow) {
        findNavController().navigate(
            R.id.userDetailFragment,
            bundleOf("uuid" to user.uuid)
        )
    }

    private fun openEdit(uuid: String?, bulk: Boolean) {
        findNavController().navigate(
            R.id.userEditFragment,
            bundleOf("uuid" to uuid, "bulk" to bulk)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
