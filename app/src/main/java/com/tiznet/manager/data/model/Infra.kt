package com.tiznet.manager.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------- Nodes

@Serializable
data class NodeDetails(
    @SerialName("last_usage_report") val lastUsageReport: NodeUsageReport? = null,
    @SerialName("today_usage") val todayUsage: Long = 0,
    @SerialName("today_online") val todayOnline: Int = 0,
    @SerialName("last_from_node") val lastFromNode: String? = null,
    @SerialName("last_to_node") val lastToNode: String? = null
)

@Serializable
data class NodeUsageReport(
    val time: String = "",
    val users: Int = 0,
    val bytes: Long = 0
)

@Serializable
data class PanelNode(
    val id: Int = 0,
    val name: String = "",
    val host: String = "",
    val country: String = "",
    @SerialName("admin_url") val adminUrl: String = "",
    @SerialName("last_seen") val lastSeen: String? = null,
    val status: String = "never",
    val domains: List<String> = emptyList(),
    val details: NodeDetails = NodeDetails()
)

@Serializable
data class NodesResponse(
    val nodes: List<PanelNode> = emptyList(),
    @SerialName("panel_version") val panelVersion: String = ""
)

@Serializable
data class NodePing(
    val online: Boolean = false,
    val version: String = "",
    val error: String = ""
)

@Serializable
data class NodeIcmpPing(
    val ip: String = "",
    val ok: Boolean = false,
    val ms: Double? = null,
    val method: String = "",
    val error: String = ""
)

@Serializable
data class NodeJobResponse(val job: String = "")

// ---------------------------------------------------------------- Domains

@Serializable
data class CertJob(
    val state: String = "",
    val error: String? = null,
    @SerialName("rate_limited") val rateLimited: Boolean = false,
    val ca: String? = null
)

@Serializable
data class DomainTls(
    val status: String = "missing",
    @SerialName("needs_valid") val needsValid: Boolean = false,
    val issuer: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val error: String = "",
    val job: CertJob? = null
)

@Serializable
data class DomainRow(
    val id: Int = 0,
    val domain: String = "",
    val alias: String = "",
    val mode: String = "direct",
    @SerialName("fake_mode") val fakeMode: String = "valid",
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("custom_proxy_ids") val customProxyIds: List<Int> = emptyList(),
    @SerialName("show_domain_ids") val showDomainIds: List<Int> = emptyList(),
    @SerialName("server_domain_id") val serverDomainId: Int? = null,
    @SerialName("download_domain_id") val downloadDomainId: Int? = null,
    @SerialName("cdn_ip") val cdnIp: String = "",
    @SerialName("resolve_ip") val resolveIp: Boolean = false,
    val ech: Boolean = false,
    val servernames: String = "",
    @SerialName("extra_params") val extraParams: String = "",
    @SerialName("tls_port") val tlsPort: Int? = null,
    @SerialName("http_port") val httpPort: Int? = null,
    val tls: DomainTls = DomainTls(),
    @SerialName("panel_link") val panelLink: String? = null
)

@Serializable
data class DomainsResponse(
    val domains: List<DomainRow> = emptyList()
)

@Serializable
data class DomainDetection(
    val domain: String = "",
    val ok: Boolean = false,
    val ip: String = "",
    val error: String = ""
)

@Serializable
data class PortCheck(
    val port: Int = 0,
    val open: Boolean = false,
    val error: String = ""
)

// ---------------------------------------------------------------- Outbounds

@Serializable
data class Outbound(
    val id: Int = 0,
    val name: String = "",
    val mode: String = "direct",
    val position: Int = 0,
    val enabled: Boolean = false,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("has_rules") val hasRules: Boolean = false,
    val active: Boolean = false,
    val domestic: Boolean = false,
    val host: String = "",
    val port: Int? = null,
    val username: String = "",
    @SerialName("has_password") val hasPassword: Boolean = false,
    val config: String? = null,
    val tag: String? = null,
    @SerialName("bridge_port") val bridgePort: Int? = null,
    @SerialName("runs_in") val runsIn: String? = null,
    @SerialName("is_builtin") val isBuiltin: Boolean = false,
    @SerialName("lists_override") val listsOverride: Boolean = false,
    val sites: List<String> = emptyList(),
    val geosites: List<String> = emptyList(),
    @SerialName("rule_sets") val ruleSets: List<String> = emptyList()
)

@Serializable
data class OutboundsResponse(val outbounds: List<Outbound> = emptyList())

// ---------------------------------------------------------------- Admins

@Serializable
data class AdminLimits(
    @SerialName("max_users") val maxUsers: Int = 0,
    @SerialName("max_active_users") val maxActiveUsers: Int = 0,
    @SerialName("max_online_users") val maxOnlineUsers: Int? = null,
    @SerialName("max_total_usage_GB") val maxTotalUsageGb: Double? = null
)

@Serializable
data class AdminStats(
    val total: Int = 0,
    val active: Int = 0,
    val online: Int = 0,
    @SerialName("usage_GB") val usageGb: Double = 0.0
)

@Serializable
data class AdminRow(
    val uuid: String = "",
    val name: String = "",
    val comment: String = "",
    val mode: String = "admin",
    @SerialName("can_add_admin") val canAddAdmin: Boolean = false,
    val capabilities: Map<String, Boolean> = emptyMap(),
    @SerialName("parent_uuid") val parentUuid: String? = null,
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("is_me") val isMe: Boolean = false,
    @SerialName("has_password") val hasPassword: Boolean = false,
    val alias: String = "",
    @SerialName("admin_link") val adminLink: String = "",
    val limits: AdminLimits? = null,
    val stats: AdminStats = AdminStats(),
    @SerialName("sub_admins") val subAdmins: Int = 0,
    @SerialName("default_outbound") val defaultOutbound: Int? = null,
    @SerialName("inherited_outbound") val inheritedOutbound: Int? = null,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false
)

@Serializable
data class AdminsResponse(val admins: List<AdminRow> = emptyList())
