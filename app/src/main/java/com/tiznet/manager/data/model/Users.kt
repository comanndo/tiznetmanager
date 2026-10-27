package com.tiznet.manager.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

typealias UserStatus = String
typealias UserMode = String

const val UNLIMITED_GB = 1_000_000.0
const val UNLIMITED_DAYS = 10_000

@Serializable
data class UserRow(
    val id: Long = 0,
    val uuid: String = "",
    val name: String = "",
    val comment: String = "",
    val enable: Boolean = false,
    val status: UserStatus = "disabled",
    @SerialName("current_usage_GB") val currentUsageGb: Double = 0.0,
    @SerialName("usage_limit_GB") val usageLimitGb: Double = 0.0,
    @SerialName("package_days") val packageDays: Int = 0,
    @SerialName("remaining_days") val remainingDays: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("expire_date") val expireDate: String? = null,
    val mode: UserMode = "no_reset",
    @SerialName("days_to_reset") val daysToReset: Int? = null,
    @SerialName("last_reset_time") val lastResetTime: String? = null,
    @SerialName("last_online") val lastOnline: String? = null,
    @SerialName("owner_uuid") val ownerUuid: String? = null,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("preferred_outbound") val preferredOutbound: Int? = null,
    @SerialName("additional_configs") val additionalConfigsCount: Int = 0,
    @SerialName("telegram_id") val telegramId: Long? = null,
    val tags: List<Int> = emptyList()
)

@Serializable
data class UserDetail(
    val id: Long = 0,
    val uuid: String = "",
    val name: String = "",
    val comment: String = "",
    val enable: Boolean = false,
    val status: UserStatus = "disabled",
    @SerialName("current_usage_GB") val currentUsageGb: Double = 0.0,
    @SerialName("usage_limit_GB") val usageLimitGb: Double = 0.0,
    @SerialName("package_days") val packageDays: Int = 0,
    @SerialName("remaining_days") val remainingDays: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("expire_date") val expireDate: String? = null,
    val mode: UserMode = "no_reset",
    @SerialName("days_to_reset") val daysToReset: Int? = null,
    @SerialName("last_reset_time") val lastResetTime: String? = null,
    @SerialName("last_online") val lastOnline: String? = null,
    @SerialName("owner_uuid") val ownerUuid: String? = null,
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("preferred_outbound") val preferredOutbound: Int? = null,
    @SerialName("additional_configs") val additionalConfigs: JsonElement = JsonNull,
    @SerialName("inherited_configs") val inheritedConfigs: Int = 0,
    @SerialName("extra_params") val extraParams: String = "",
    @SerialName("telegram_id") val telegramId: Long? = null,
    val tags: List<Int> = emptyList()
)

@Serializable
data class ShareDomain(
    val domain: String = "",
    val label: String = "",
    val kind: String = "direct",
    val base: String = ""
)

@Serializable
data class OutboundOption(
    val id: Int = 0,
    val name: String = "",
    val mode: String = "",
    val enabled: Boolean = false
)

@Serializable
data class AdminBrief(
    val uuid: String = "",
    val name: String = "",
    @SerialName("parent_uuid") val parentUuid: String? = null
)

@Serializable
data class UsersState(
    val users: List<UserRow> = emptyList(),
    @SerialName("me_uuid") val meUuid: String = "",
    @SerialName("link_domains") val linkDomains: List<ShareDomain> = emptyList(),
    val admins: List<AdminBrief> = emptyList(),
    val outbounds: List<OutboundOption> = emptyList(),
    @SerialName("can_add") val canAdd: Boolean = true,
    val capabilities: Map<String, Boolean> = emptyMap(),
    val readOnly: Boolean = false
)

@Serializable
data class UserWrapper(val user: UserDetail)

@Serializable
data class BulkResult(val count: Int = 0)

@Serializable
data class BulkCreateResult(val users: List<UserDetail> = emptyList(), val count: Int = 0)
