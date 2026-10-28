package com.tiznet.manager.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------- Panel info / capabilities

/**
 * Response of `/api/v2/panel/info/`, available on every Hiddify version
 * (v11-v14) and used to detect the panel generation before feature calls.
 */
@Serializable
data class PanelInfo(val version: String? = null)

/**
 * Hiddify API generation boundary.
 *
 * The rich Admin V2 REST surface (`users/`, `nodes/`, `settings/`, `apply/`,
 * `backup/`, ...) and the `__admin_v2_bootstrap` endpoint exist from Hiddify
 * v13. v11/v12 only expose the smaller commercial REST API v2 (`user/`,
 * `admin_user/`, `server_status/`, `log/`, ...) under the same base prefix.
 */
object PanelCapabilities {
    const val MIN_MODERN_VERSION = 13

    fun majorVersion(version: String?): Int? =
        version?.trim()?.substringBefore('.')?.toIntOrNull()

    /** v11/v12 = limited "legacy" API; v13+ = full Admin V2. Unknown versions default to modern. */
    fun isLegacy(version: String?): Boolean =
        (majorVersion(version) ?: MIN_MODERN_VERSION) < MIN_MODERN_VERSION
}

// ---------------------------------------------------------------- Bootstrap

@Serializable
data class BootstrapResponse(
    @SerialName("proxy_path") val proxyPath: String = "",
    @SerialName("api_base") val apiBase: String = "",
    @SerialName("router_base") val routerBase: String = "",
    val locale: String = "en",
    @SerialName("panel_version") val panelVersion: String? = null,
    @SerialName("panel_logo_url") val panelLogoUrl: String? = null,
    @SerialName("panel_mode") val panelMode: String = "",
    @SerialName("account_mode") val accountMode: String = "",
    val capabilities: Map<String, Boolean> = emptyMap(),
    @SerialName("needs_quick_setup") val needsQuickSetup: Boolean = false,
    val menu: JsonElement? = null,
    val notices: JsonElement? = null,
    val telegram: JsonObject? = null
)

// ---------------------------------------------------------------- Tags

@Serializable
data class Tag(
    val id: Int = 0,
    val name: String = "",
    val color: String = "blue",
    @SerialName("is_default") val isDefault: Boolean = false,
    val count: Int = 0
)

@Serializable
data class TagList(
    val tags: List<Tag> = emptyList(),
    val colors: List<String> = emptyList()
)

@Serializable
data class TagCreated(
    @SerialName("created_id") val createdId: Int = 0
)

// ---------------------------------------------------------------- Protocols

@Serializable
data class ProtocolSwitch(
    val key: String = "",
    val label: String = "",
    val description: String = "",
    val category: String = "",
    val enabled: Boolean = false,
    @SerialName("apply_mode") val applyMode: String = "nothing"
)

@Serializable
data class ProtocolSwitches(
    val items: List<ProtocolSwitch> = emptyList(),
    @SerialName("restart_mode") val restartMode: String = "nothing"
)

// ---------------------------------------------------------------- Settings

@Serializable
data class SettingChoice(val value: String = "", val label: String = "")

@Serializable
data class SettingField(
    val key: String = "",
    val label: String = "",
    val description: String = "",
    val type: String = "text",
    val value: JsonElement? = null,
    @SerialName("apply_mode") val applyMode: String = "nothing",
    val essential: Boolean = false,
    @SerialName("protocol_switch") val protocolSwitch: Boolean = false,
    val required: Boolean = false,
    val choices: List<SettingChoice>? = null,
    val pattern: String? = null,
    @SerialName("pattern_message") val patternMessage: String? = null,
    val maxlength: Int? = null
)

@Serializable
data class SettingCategory(
    val id: String = "",
    val label: String = "",
    val description: String = "",
    val fields: List<SettingField> = emptyList()
)

@Serializable
data class SettingsResponse(val categories: List<SettingCategory> = emptyList())

@Serializable
data class SettingsSaveOut(
    val categories: List<SettingCategory> = emptyList(),
    @SerialName("restart_mode") val restartMode: String = "nothing",
    val warnings: List<String> = emptyList(),
    val changed: List<String> = emptyList(),
    val reload: Boolean = false,
    @SerialName("new_admin_path") val newAdminPath: String? = null,
    val telegram: JsonObject? = null
)

// ---------------------------------------------------------------- Apply

@Serializable
data class LogFile(
    val name: String = "",
    val size: Long = 0,
    val mtime: Long = 0
)

@Serializable
data class ApplyState(
    val running: List<String> = emptyList(),
    val last: JsonObject = JsonObject(emptyMap()),
    val files: List<LogFile> = emptyList(),
    val node: Boolean = false,
    val telegram: JsonObject? = null,
    val now: Long = 0
)

@Serializable
data class StartedRun(
    val action: String = "",
    val log: String = "",
    val started: Long = 0,
    val running: List<String> = emptyList(),
    val files: List<LogFile> = emptyList()
)

@Serializable
data class LogChunk(
    val text: String = "",
    val offset: Long = 0,
    val size: Long = 0
)

// ---------------------------------------------------------------- Backup

@Serializable
data class PanelCounts(
    val users: Int = 0,
    val admins: Int = 0,
    val domains: Int = 0,
    @SerialName("custom_proxies") val customProxies: Int = 0,
    val nodes: Int = 0,
    val settings: Int = 0,
    @SerialName("domain_names") val domainNames: List<String> = emptyList(),
    @SerialName("admin_path_changes") val adminPathChanges: Boolean = false
)

@Serializable
data class BackupFile(
    val name: String = "",
    val size: Long = 0,
    val created: String = ""
)

@Serializable
data class BackupState(
    val current: PanelCounts = PanelCounts(),
    val files: List<BackupFile> = emptyList(),
    val telegram: JsonObject? = null,
    val directory: String = "",
    val created: String? = null
)

// ---------------------------------------------------------------- Broadcast

@Serializable
data class BroadcastState(
    val bot: Boolean = false,
    @SerialName("bot_username") val botUsername: String = "",
    val counts: Map<String, Int> = emptyMap(),
    val limits: JsonObject? = null
)

@Serializable
data class BroadcastJob(
    val id: String = "",
    val total: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    val state: String = "",
    val started: Long = 0,
    val finished: Long? = null
)

// ---------------------------------------------------------------- Clean IP

@Serializable
data class RealityState(
    val enabled: Boolean = false,
    val sni: String = "",
    val dest: String = "",
    val pbk: String = "",
    val sid: String = ""
)

@Serializable
data class CleanIpNetwork(
    val cidr: String = "",
    val country: String = "",
    val ok: Boolean = false
)
