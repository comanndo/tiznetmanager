package com.tiznet.manager.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DailyPoint(
    val date: String = "",
    val usage: Long = 0,
    val online: Int = 0,
    @SerialName("by_child") val byChild: JsonObject? = null
)

@Serializable
data class UsageTotals(
    val today: Long = 0,
    val yesterday: Long = 0,
    val week: Long = 0,
    val month: Long = 0,
    val total: Long = 0
)

@Serializable
data class UsageAverages(
    @SerialName("daily_week") val dailyWeek: Long = 0,
    @SerialName("daily_month") val dailyMonth: Long = 0
)

@Serializable
data class UsagePrevious(val week: Long = 0, val month: Long = 0)

@Serializable
data class UsageTrends(
    val day: Double? = null,
    val week: Double? = null,
    val month: Double? = null
)

@Serializable
data class UsagePeak(val date: String = "", val usage: Long = 0)

@Serializable
data class DashboardUsage(
    val totals: UsageTotals = UsageTotals(),
    val averages: UsageAverages = UsageAverages(),
    val previous: UsagePrevious = UsagePrevious(),
    val trends: UsageTrends = UsageTrends(),
    val peak: UsagePeak? = null
)

@Serializable
data class UsersOnline(
    val m5: Int = 0,
    val h24: Int = 0,
    val today: Int = 0,
    val yesterday: Int = 0,
    val week: Int = 0,
    val month: Int = 0
)

@Serializable
data class UsersAverages(
    @SerialName("daily_week") val dailyWeek: Int = 0,
    @SerialName("daily_month") val dailyMonth: Int = 0
)

@Serializable
data class DashboardUsers(
    val total: Int = 0,
    val enabled: Int = 0,
    val active: Int = 0,
    val online: UsersOnline = UsersOnline(),
    val averages: UsersAverages = UsersAverages()
)

@Serializable
data class DashboardCpu(
    val percent: Double = 0.0,
    @SerialName("per_core") val perCore: List<Double> = emptyList(),
    val cores: Int = 0,
    @SerialName("load_avg") val loadAvg: List<Double> = emptyList(),
    @SerialName("load_percent") val loadPercent: List<Double> = emptyList()
)

@Serializable
data class DashboardMemory(
    @SerialName("used_gb") val usedGb: Double = 0.0,
    @SerialName("total_gb") val totalGb: Double = 0.0,
    @SerialName("available_gb") val availableGb: Double = 0.0,
    @SerialName("cached_gb") val cachedGb: Double = 0.0,
    val percent: Double = 0.0,
    @SerialName("swap_used_gb") val swapUsedGb: Double = 0.0,
    @SerialName("swap_total_gb") val swapTotalGb: Double = 0.0,
    @SerialName("swap_percent") val swapPercent: Double = 0.0
)

@Serializable
data class DashboardDisk(
    @SerialName("used_gb") val usedGb: Double = 0.0,
    @SerialName("total_gb") val totalGb: Double = 0.0,
    @SerialName("free_gb") val freeGb: Double = 0.0,
    val percent: Double = 0.0,
    @SerialName("hiddify_gb") val hiddifyGb: Double? = null
)

@Serializable
data class DashboardNetwork(
    @SerialName("bytes_sent") val bytesSent: Long = 0,
    @SerialName("bytes_recv") val bytesRecv: Long = 0,
    @SerialName("sent_gb") val sentGb: Double = 0.0,
    @SerialName("recv_gb") val recvGb: Double = 0.0,
    @SerialName("total_gb") val totalGb: Double = 0.0,
    @SerialName("sampled_at") val sampledAt: Double = 0.0,
    val connections: Int = 0,
    @SerialName("unique_ips") val uniqueIps: Int = 0
)

@Serializable
data class DashboardHost(
    val hostname: String = "",
    @SerialName("boot_time") val bootTime: Double = 0.0,
    @SerialName("uptime_s") val uptimeS: Long = 0,
    @SerialName("panel_version") val panelVersion: String? = null
)

@Serializable
data class DashboardProcess(
    val name: String = "",
    val percent: Double = 0.0,
    @SerialName("memory_gb") val memoryGb: Double? = null,
    @SerialName("cpu_percent") val cpuPercent: Double? = null,
    val path: String? = null
)

@Serializable
data class DashboardProcesses(
    val count: Int = 0,
    val cpu: List<DashboardProcess> = emptyList(),
    val memory: List<DashboardProcess> = emptyList()
)

@Serializable
data class DashboardNode(
    val id: Int = 0,
    val name: String = "",
    val mode: String = ""
)

@Serializable
data class DashboardNodeStats(
    val id: Int = 0,
    val name: String = "",
    val mode: String = "",
    @SerialName("panel_url") val panelUrl: String? = null,
    val ok: Boolean = true,
    val error: String? = null,
    val cpu: DashboardCpu? = null,
    val memory: DashboardMemory? = null,
    val disk: DashboardDisk? = null,
    val network: DashboardNetwork? = null,
    val host: DashboardHost? = null
)

@Serializable
data class DashboardResponse(
    @SerialName("generated_at") val generatedAt: String = "",
    @SerialName("range_days") val rangeDays: Int = 30,
    val series: List<DailyPoint> = emptyList(),
    val usage: DashboardUsage = DashboardUsage(),
    val users: DashboardUsers = DashboardUsers(),
    val system: DashboardSystem = DashboardSystem(),
    val processes: DashboardProcesses = DashboardProcesses(),
    val nodes: List<DashboardNode> = emptyList(),
    @SerialName("node_stats") val nodeStats: List<DashboardNodeStats> = emptyList(),
    @SerialName("child_id") val childId: Int? = null
)

@Serializable
data class DashboardSystem(
    val cpu: DashboardCpu = DashboardCpu(),
    val memory: DashboardMemory = DashboardMemory(),
    val disk: DashboardDisk = DashboardDisk(),
    val network: DashboardNetwork = DashboardNetwork(),
    val host: DashboardHost = DashboardHost()
)

@Serializable
data class ServerStatusResponse(
    val stats: JsonObject = JsonObject(emptyMap()),
    @SerialName("usage_history") val usageHistory: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class AdminInfo(
    val name: String = "",
    val comment: String = "",
    val uuid: String = "",
    val mode: String = "",
    @SerialName("can_add_admin") val canAddAdmin: Boolean = false,
    val capabilities: Map<String, Boolean> = emptyMap(),
    @SerialName("parent_admin_uuid") val parentAdminUuid: String? = null,
    @SerialName("telegram_id") val telegramId: Long = 0,
    val lang: String = "en"
)
