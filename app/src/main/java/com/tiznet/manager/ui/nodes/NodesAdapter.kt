package com.tiznet.manager.ui.nodes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.PanelNode
import com.tiznet.manager.databinding.ItemNodeBinding
import com.tiznet.manager.util.relativeTime

class NodesAdapter(
    private val onPing: (PanelNode) -> Unit,
    private val onSync: (PanelNode) -> Unit,
    private val onDelete: (PanelNode) -> Unit
) : ListAdapter<PanelNode, NodesAdapter.VH>(DIFF) {

    inner class VH(val b: ItemNodeBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val node = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            nodeName.text = node.name.ifBlank { node.host }
            nodeHost.text = node.host
            val (statusText, dotRes) = when (node.status) {
                "online" -> ctx.getString(R.string.status_online) to R.drawable.bg_dot_online
                "late" -> ctx.getString(R.string.status_late) to R.drawable.bg_dot_late
                "offline" -> ctx.getString(R.string.status_offline) to R.drawable.bg_dot_offline
                else -> ctx.getString(R.string.status_never) to R.drawable.bg_dot_never
            }
            nodeStatus.text = statusText
            statusDot.setBackgroundResource(dotRes)
            nodeMeta.text = buildString {
                append("${ctx.getString(R.string.nodes_last_seen)}: ")
                append(node.lastSeen?.let { relativeTime(it) } ?: "—")
                node.details.lastUsageReport?.let {
                    append(" · ${it.users} ${ctx.getString(R.string.dash_users_online)}")
                }
            }
            btnPing.setOnClickListener { onPing(node) }
            btnSync.setOnClickListener { onSync(node) }
            btnDelete.setOnClickListener { onDelete(node) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PanelNode>() {
            override fun areItemsTheSame(o: PanelNode, n: PanelNode) = o.id == n.id
            override fun areContentsTheSame(o: PanelNode, n: PanelNode) = o == n
        }
    }
}
