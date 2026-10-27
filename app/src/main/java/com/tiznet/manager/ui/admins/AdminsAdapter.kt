package com.tiznet.manager.ui.admins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.AdminRow
import com.tiznet.manager.databinding.ItemAdminBinding

class AdminsAdapter(
    private val onMenu: (AdminRow) -> Unit
) : ListAdapter<AdminRow, AdminsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemAdminBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val admin = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            adminName.text = admin.name.ifBlank { admin.uuid.take(8) }
            adminMeta.text = buildString {
                append(modeText(ctx, admin.mode))
                append(" · ${admin.stats.total} ${ctx.getString(R.string.nav_users)}")
                if (admin.alias.isNotBlank()) append(" · @").append(admin.alias)
            }
            adminLimits.text = admin.limits?.let {
                "${ctx.getString(R.string.admins_max_users)}: ${it.maxUsers} · " +
                    "${ctx.getString(R.string.admins_sub_count, admin.subAdmins)}"
            } ?: ctx.getString(R.string.admins_sub_count, admin.subAdmins)
            btnMenu.setOnClickListener { onMenu(admin) }
        }
    }

    private fun modeText(ctx: android.content.Context, mode: String) = when (mode) {
        "super_admin" -> ctx.getString(R.string.admins_mode_super)
        "admin" -> ctx.getString(R.string.admins_mode_admin)
        "agent" -> ctx.getString(R.string.admins_mode_agent)
        else -> mode
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminRow>() {
            override fun areItemsTheSame(o: AdminRow, n: AdminRow) = o.uuid == n.uuid
            override fun areContentsTheSame(o: AdminRow, n: AdminRow) = o == n
        }
    }
}
