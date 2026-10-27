package com.tiznet.manager.ui.domains

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiznet.manager.R
import com.tiznet.manager.data.model.DomainRow
import com.tiznet.manager.databinding.ItemDomainBinding
import com.tiznet.manager.util.formatDate

class DomainsAdapter(
    private val onClick: (DomainRow) -> Unit
) : ListAdapter<DomainRow, DomainsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemDomainBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDomainBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = getItem(position)
        val ctx = holder.b.root.context
        with(holder.b) {
            domainName.text = d.domain
            domainAlias.text = "${d.alias.ifBlank { d.mode }} · ${d.fakeMode}"
            tlsBadge.text = tlsText(ctx, d.tls.status)
            domainMeta.text = buildString {
                d.tlsPort?.let { append("TLS:$it ") }
                d.httpPort?.let { append("HTTP:$it ") }
                d.tls.expiresAt?.let { append("· ${ctx.getString(R.string.domains_cert_expire)}: ${formatDate(it)}") }
            }
            root.setOnClickListener { onClick(d) }
        }
    }

    private fun tlsText(ctx: android.content.Context, status: String) = when (status) {
        "valid" -> ctx.getString(R.string.tls_valid)
        "self_signed" -> ctx.getString(R.string.tls_self_signed)
        "expired" -> ctx.getString(R.string.tls_expired)
        "invalid" -> ctx.getString(R.string.tls_invalid)
        "missing" -> ctx.getString(R.string.tls_missing)
        else -> status
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DomainRow>() {
            override fun areItemsTheSame(o: DomainRow, n: DomainRow) = o.id == n.id
            override fun areContentsTheSame(o: DomainRow, n: DomainRow) = o == n
        }
    }
}
