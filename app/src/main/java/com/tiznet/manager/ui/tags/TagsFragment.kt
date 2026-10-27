package com.tiznet.manager.ui.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.model.Tag
import com.tiznet.manager.databinding.DialogTagBinding
import com.tiznet.manager.databinding.FragmentTagsBinding
import com.tiznet.manager.databinding.ItemTagBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.ui.common.UiState
import com.tiznet.manager.util.snack

class TagsFragment : BaseFragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TagsViewModel by viewModels()
    private lateinit var stateView: StateView
    private val colors = listOf("blue", "green", "orange", "red", "black")
    private var selectedColor = "blue"

    private val adapter = TagsAdapter(
        onClick = { showDialog(it) },
        onLongClick = { confirmDelete(it) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTagsBinding.inflate(inflater, container, false)
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

    private fun colorRes(name: String) = when (name) {
        "red" -> R.color.status_expired
        "orange" -> R.color.status_no_data
        "green" -> R.color.status_active
        "black" -> R.color.black
        else -> R.color.brand_500
    }

    private fun showDialog(existing: Tag?) {
        val db = DialogTagBinding.inflate(layoutInflater)
        db.inputName.setText(existing?.name.orEmpty())
        selectedColor = existing?.color ?: "blue"
        colors.forEach { color ->
            val chip = Chip(requireContext()).apply {
                text = color
                isCheckable = true
                isChecked = color == selectedColor
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), colorRes(color))
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setOnClickListener { selectedColor = color }
            }
            db.colorChips.addView(chip)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.tags_add else R.string.action_edit)
            .setView(db.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = db.inputName.text?.toString().orEmpty()
                if (name.isBlank()) {
                    binding.root.snack(getString(R.string.enter_value))
                    return@setPositiveButton
                }
                val callback: (String?) -> Unit = { error ->
                    binding.root.snack(error ?: getString(R.string.saved))
                }
                if (existing == null) viewModel.create(name, selectedColor, callback)
                else viewModel.update(existing, name, selectedColor, callback)
            }.show()
    }

    private fun confirmDelete(tag: Tag) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tags_confirm_delete)
            .setMessage(tag.name)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.delete(tag) { error ->
                    binding.root.snack(error ?: getString(R.string.deleted))
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class TagsAdapter(
    private val onClick: (Tag) -> Unit,
    private val onLongClick: (Tag) -> Unit
) : ListAdapter<Tag, TagsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemTagBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tag = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            tagName.text = tag.name
            tagCount.text = ctx.getString(R.string.tags_count, tag.count)
            val color = when (tag.color) {
                "red" -> R.color.status_expired
                "orange" -> R.color.status_no_data
                "green" -> R.color.status_active
                "black" -> R.color.black
                else -> R.color.brand_500
            }
            colorDot.setBackgroundResource(
                if (tag.color == "black") R.drawable.bg_dot_offline else when (tag.color) {
                    "red" -> R.drawable.bg_dot_expired
                    "orange" -> R.drawable.bg_dot_no_data
                    "green" -> R.drawable.bg_dot_active
                    else -> R.drawable.bg_dot_online
                }
            )
            colorDot.contentDescription = tag.color
            root.setOnClickListener { onClick(tag) }
            root.setOnLongClickListener { onLongClick(tag); true }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Tag>() {
            override fun areItemsTheSame(o: Tag, n: Tag) = o.id == n.id
            override fun areContentsTheSame(o: Tag, n: Tag) = o == n
        }
    }
}
