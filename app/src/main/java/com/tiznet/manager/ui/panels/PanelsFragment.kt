package com.tiznet.manager.ui.panels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiznet.manager.R
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.databinding.DialogPanelBinding
import com.tiznet.manager.databinding.FragmentPanelsBinding
import com.tiznet.manager.databinding.ItemPanelBinding
import com.tiznet.manager.ui.common.BaseFragment
import com.tiznet.manager.ui.common.StateView
import com.tiznet.manager.util.hide
import com.tiznet.manager.util.snack
import com.tiznet.manager.util.visible

class PanelsFragment : BaseFragment() {

    private var _binding: FragmentPanelsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PanelsViewModel by viewModels()
    private lateinit var stateView: StateView

    private val adapter = PanelAdapter(
        onClick = { panel ->
            viewModel.switchTo(panel) {
                findNavController().navigate(R.id.dashboardFragment)
            }
        },
        onMenu = { panel -> showMenu(panel) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPanelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stateView = StateView(binding.stateView.root)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.fabAdd.setOnClickListener { showDialog(null) }

        viewModel.panels.collectOnStarted { panels ->
            adapter.submitList(panels) {
                if (panels.isEmpty()) stateView.empty(getString(R.string.login_no_panels))
                else stateView.content()
            }
        }
        viewModel.session.activePanel.collectOnStarted { active ->
            adapter.activeId = active?.id
            adapter.notifyDataSetChanged()
        }
        viewModel.error.collectOnStarted { error ->
            if (!error.isNullOrBlank()) {
                binding.root.snack(error)
                viewModel.consumeError()
            }
        }
        viewModel.busy.collectOnStarted { busy ->
            if (busy) stateView.loading() else stateView.content().also {
                if (viewModel.panels.value.isEmpty()) stateView.empty(getString(R.string.login_no_panels))
            }
        }
    }

    private fun showMenu(panel: PanelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(panel.name)
            .setItems(
                arrayOf(
                    getString(R.string.panels_switch),
                    getString(R.string.panels_edit),
                    getString(R.string.panels_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> viewModel.switchTo(panel) {
                        findNavController().navigate(R.id.dashboardFragment)
                    }
                    1 -> showDialog(panel)
                    2 -> confirmDelete(panel)
                }
            }.show()
    }

    private fun confirmDelete(panel: PanelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.panels_delete)
            .setMessage(R.string.panels_confirm_delete)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(panel) }
            .show()
    }

    private fun showDialog(existing: PanelEntity?) {
        val db = DialogPanelBinding.inflate(layoutInflater)
        existing?.let {
            db.inputName.setText(it.name)
            db.inputUrl.setText(it.baseUrl)
            db.inputProxyPath.setText(it.proxyPath)
            db.inputUsername.setText(it.username)
            db.inputPassword.setHint(R.string.optional)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) R.string.panels_add else R.string.panels_edit)
            .setView(db.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.save(
                    existing = existing,
                    name = db.inputName.text?.toString().orEmpty(),
                    url = db.inputUrl.text?.toString().orEmpty(),
                    proxyPath = db.inputProxyPath.text?.toString().orEmpty(),
                    username = db.inputUsername.text?.toString().orEmpty(),
                    password = db.inputPassword.text?.toString()
                        ?: if (existing != null) repository.decryptPassword(existing) else ""
                ) { error ->
                    if (error == null) {
                        binding.root.snack(getString(R.string.login_success))
                        findNavController().navigate(R.id.dashboardFragment)
                    } else binding.root.snack(error)
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class PanelAdapter(
    private val onClick: (PanelEntity) -> Unit,
    private val onMenu: (PanelEntity) -> Unit
) : ListAdapter<PanelEntity, PanelAdapter.VH>(DIFF) {

    var activeId: Long? = null

    inner class VH(val b: ItemPanelBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemPanelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val panel = getItem(position)
        with(holder.b) {
            panelName.text = panel.name
            panelUrl.text = panel.baseUrl
            panelMeta.text = panel.panelVersion?.let { "v$it" } ?: ""
            activeCheck.visible(panel.id == activeId)
            root.setOnClickListener { onClick(panel) }
            root.setOnLongClickListener { onMenu(panel); true }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PanelEntity>() {
            override fun areItemsTheSame(o: PanelEntity, n: PanelEntity) = o.id == n.id
            override fun areContentsTheSame(o: PanelEntity, n: PanelEntity) = o == n
        }
    }
}
