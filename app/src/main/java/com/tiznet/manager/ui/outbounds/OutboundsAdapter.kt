package com.tiznet.manager.ui.outbounds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.Outbound
import com.tiznet.manager.databinding.ItemOutboundBinding

class OutboundsAdapter(
    private val onToggle: (Outbound) -> Unit,
    private val onMove: (Outbound, Int) -> Unit,
    private val onLongPress: (Outbound) -> Unit
) : ListAdapter<Outbound, OutboundsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemOutboundBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemOutboundBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            outboundName.text = item.name
            outboundMode.text = buildString {
                append(item.mode)
                if (item.active) append(" · ").append(ctx.getString(R.string.status_active))
                if (item.host.isNotBlank()) append(" · ").append(item.host)
            }
            modeIcon.setImageResource(iconForMode(item.mode))
            defaultBadge.visibility = if (item.isDefault) android.view.View.VISIBLE else android.view.View.GONE
            enableSwitch.setOnCheckedChangeListener(null)
            enableSwitch.isChecked = item.enabled
            enableSwitch.setOnCheckedChangeListener { _, _ -> onToggle(item) }
            btnUp.setOnClickListener { onMove(item, -1) }
            root.setOnLongClickListener { onLongPress(item); true }
            root.setOnClickListener { onLongPress(item) }
        }
    }

    private fun iconForMode(mode: String) = when (mode) {
        "warp" -> R.drawable.ic_cloud
        "direct" -> R.drawable.ic_swap
        "block" -> R.drawable.ic_block
        "socks" -> R.drawable.ic_router
        "tor" -> R.drawable.ic_visibility_off
        "psiphon" -> R.drawable.ic_globe
        "core_outbound", "core_endpoint", "xray_outbound" -> R.drawable.ic_code
        else -> R.drawable.ic_swap
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Outbound>() {
            override fun areItemsTheSame(o: Outbound, n: Outbound) = o.id == n.id
            override fun areContentsTheSame(o: Outbound, n: Outbound) = o == n
        }
    }
}
