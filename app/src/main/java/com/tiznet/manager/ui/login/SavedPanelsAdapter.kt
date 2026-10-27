package com.tiznet.manager.ui.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.data.db.PanelEntity
import com.tiznet.manager.databinding.ItemSavedPanelBinding

class SavedPanelsAdapter(
    private val onClick: (PanelEntity) -> Unit,
    private val onMenu: (PanelEntity) -> Unit
) : ListAdapter<PanelEntity, SavedPanelsAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemSavedPanelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSavedPanelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val panel = getItem(position)
        with(holder.binding) {
            panelName.text = panel.name
            panelUrl.text = panel.baseUrl
            panelVersion.text = panel.panelVersion?.let { "v$it" } ?: ""
            root.setOnClickListener { onClick(panel) }
            btnPanelMenu.setOnClickListener { onMenu(panel) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PanelEntity>() {
            override fun areItemsTheSame(old: PanelEntity, new: PanelEntity) = old.id == new.id
            override fun areContentsTheSame(old: PanelEntity, new: PanelEntity) = old == new
        }
    }
}
