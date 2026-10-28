package com.tiznet.manager.data.remote

import com.tiznet.manager.data.model.AdminInfo
import com.tiznet.manager.data.model.AdminRow
import com.tiznet.manager.data.model.DailyPoint
import com.tiznet.manager.data.model.DashboardResponse
import com.tiznet.manager.data.model.ServerStatusResponse
import com.tiznet.manager.data.model.UserDetail
import com.tiznet.manager.data.model.UserRow
import com.tiznet.manager.data.model.UsersState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Adapters for Hiddify v11/v12 ("legacy" commercial REST API v2), whose route
 * table and payloads predate Admin V2 (v13+). Everything here is defensive:
 * unknown fields are ignored and missing values fall back to safe defaults.
 */
object LegacyMappers {

    /** v11 has no status field; derive the same vocabulary the UI already uses. */
    fun List<UserRow>.withDerivedStatus(): List<UserRow> = map {
        it.copy(status = if (it.enable) "active" else "disabled")
    }

    fun usersState(rows: List<UserRow>, meUuid: String?): UsersState =
        UsersState(
            users = rows.withDerivedStatus(),
            meUuid = meUuid.orEmpty(),
            canAdd = true
        )

    fun UserRow.toDetail(): UserDetail = UserDetail(
        id = id,
        uuid = uuid,
        name = name,
        comment = comment,
        enable = enable,
        status = status,
        currentUsageGb = currentUsageGb,
        usageLimitGb = usageLimitGb,
        packageDays = packageDays,
        remainingDays = remainingDays,
        startDate = startDate,
        expireDate = expireDate,
        mode = mode,
        daysToReset = daysToReset,
        lastResetTime = lastResetTime,
        lastOnline = lastOnline,
        ownerUuid = ownerUuid,
        ownerName = ownerName,
        preferredOutbound = preferredOutbound,
        telegramId = telegramId,
        tags = tags
    )

    fun AdminInfo.toRow(isMe: Boolean): AdminRow = AdminRow(
        uuid = uuid,
        name = name,
        comment = comment,
        mode = mode,
        canAddAdmin = canAddAdmin,
        capabilities = capabilities,
        parentUuid = parentAdminUuid,
        isMe = isMe,
        canEdit = !isMe,
        canDelete = !isMe && mode != "super_admin"
    )

    /**
     * v11's /server_status/ returns untyped dictionaries. Extract the daily
     * usage series when the shape is recognizable; otherwise an empty response
     * renders the dashboard's empty state instead of crashing.
     */
    fun dashboard(status: ServerStatusResponse): DashboardResponse {
        val series = mutableListOf<DailyPoint>()
        val history = status.usageHistory
        // Common shapes: {"YYYY-MM-DD": <bytes>} or {"YYYY-MM-DD": {"usage": <n>, ...}}
        history.entries.sortedBy { it.key }.forEach { (date, element) ->
            val usage = when {
                element is JsonPrimitive -> element.longOrNull
                    ?: element.doubleOrNull?.toLong()
                element is JsonObject -> element["usage"]?.jsonPrimitive?.longOrNull
                    ?: element["usage_bytes"]?.jsonPrimitive?.longOrNull
                    ?: element["bytes"]?.jsonPrimitive?.longOrNull
                else -> null
            }
            val online = (element as? JsonObject)
                ?.get("online")?.jsonPrimitive?.intOrNull ?: 0
            series.add(DailyPoint(date = date, usage = usage ?: 0L, online = online))
        }
        return DashboardResponse(series = series)
    }

    /**
     * Strip v13-only fields so v11/v12 marshmallow schemas (which reject
     * unknown keys on input) accept the write.
     */
    fun sanitizeUserBody(body: JsonObject): JsonObject {
        val allowed = setOf(
            "uuid", "name", "comment", "enable", "is_active",
            "usage_limit_GB", "package_days", "mode", "telegram_id",
            "start_date", "current_usage_GB", "last_reset_time", "lang",
            "added_by_uuid"
        )
        return JsonObject(body.filterKeys { it in allowed })
    }

    fun sanitizeAdminBody(body: JsonObject): JsonObject {
        val allowed = setOf(
            "uuid", "name", "comment", "mode", "can_add_admin",
            "parent_admin_uuid", "telegram_id", "lang", "max_users", "password"
        )
        return JsonObject(body.filterKeys { it in allowed })
    }

    @Suppress("unused")
    private fun JsonObject.text(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()
}
