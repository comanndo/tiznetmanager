package com.tiznet.manager.ui.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.UNLIMITED_DAYS
import com.tiznet.manager.data.model.UNLIMITED_GB
import com.tiznet.manager.data.model.UserRow
import com.tiznet.manager.databinding.ItemUserBinding
import com.tiznet.manager.util.formatGb
import java.util.Locale

class UsersAdapter(
    private val onClick: (UserRow) -> Unit,
    private val onLongClick: (UserRow) -> Unit,
    private val selection: () -> Set<String>
) : ListAdapter<UserRow, UsersAdapter.VH>(DIFF) {

    inner class VH(val b: ItemUserBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            userName.text = user.name.ifBlank { user.uuid.take(8) }
            val limit = if (user.usageLimitGb >= UNLIMITED_GB)
                ctx.getString(R.string.user_unlimited)
            else formatGb(user.usageLimitGb)
            userUsage.text = "${formatGb(user.currentUsageGb)} / $limit"
            usageProgress.setProgressCompat(UsersViewModel.usagePercent(user), true)
            userMeta.text = buildString {
                if (user.packageDays >= UNLIMITED_DAYS) append(ctx.getString(R.string.user_unlimited))
                else append("${user.remainingDays} ${ctx.getString(R.string.days)}")
                append(" · ${modeLabel(ctx, user.mode)}")
                user.lastOnline?.takeIf { it.isNotBlank() }?.let {
                    append(" · ${com.tiznet.manager.util.relativeTime(it)}")
                }
            }
            statusBadge.text = statusLabel(ctx, user.status)
            statusBadge.setTextColor(ContextCompat.getColor(ctx, statusColor(user.status)))
            statusDot.setBackgroundResource(statusDot(user.status))
            root.isChecked = user.uuid in selection()
            root.strokeWidth = if (user.uuid in selection()) 2 else 0
            root.setOnClickListener {
                if (selection().isNotEmpty()) onLongClick(user) else onClick(user)
            }
            root.setOnLongClickListener {
                onLongClick(user)
                true
            }
        }
    }

    private fun statusLabel(ctx: android.content.Context, status: String) = when (status) {
        "active" -> ctx.getString(R.string.status_active)
        "disabled" -> ctx.getString(R.string.status_disabled)
        "expired" -> ctx.getString(R.string.status_expired)
        "no_data" -> ctx.getString(R.string.status_no_data)
        else -> status
    }

    private fun statusColor(status: String) = when (status) {
        "active" -> R.color.status_active
        "disabled" -> R.color.status_disabled
        "expired" -> R.color.status_expired
        "no_data" -> R.color.status_no_data
        else -> R.color.status_disabled
    }

    private fun statusDot(status: String) = when (status) {
        "active" -> R.drawable.bg_dot_active
        "disabled" -> R.drawable.bg_dot_disabled
        "expired" -> R.drawable.bg_dot_expired
        "no_data" -> R.drawable.bg_dot_no_data
        else -> R.drawable.bg_dot_disabled
    }

    private fun modeLabel(ctx: android.content.Context, mode: String) = when (mode) {
        "no_reset" -> ctx.getString(R.string.user_mode_no_reset)
        "monthly" -> ctx.getString(R.string.user_mode_monthly)
        "weekly" -> ctx.getString(R.string.user_mode_weekly)
        "daily" -> ctx.getString(R.string.user_mode_daily)
        else -> mode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<UserRow>() {
            override fun areItemsTheSame(old: UserRow, new: UserRow) = old.uuid == new.uuid
            override fun areContentsTheSame(old: UserRow, new: UserRow) = old == new
        }
    }
}
