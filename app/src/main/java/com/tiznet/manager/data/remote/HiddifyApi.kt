package com.tiznet.manager.data.remote

import com.tiznet.manager.data.model.AdminInfo
import com.tiznet.manager.data.model.AdminsResponse
import com.tiznet.manager.data.model.ApplyState
import com.tiznet.manager.data.model.BackupState
import com.tiznet.manager.data.model.BootstrapResponse
import com.tiznet.manager.data.model.BroadcastState
import com.tiznet.manager.data.model.BulkCreateResult
import com.tiznet.manager.data.model.BulkResult
import com.tiznet.manager.data.model.CleanIpNetwork
import com.tiznet.manager.data.model.DashboardResponse
import com.tiznet.manager.data.model.DomainDetection
import com.tiznet.manager.data.model.DomainsResponse
import com.tiznet.manager.data.model.LogChunk
import com.tiznet.manager.data.model.NodesResponse
import com.tiznet.manager.data.model.OutboundsResponse
import com.tiznet.manager.data.model.PanelInfo
import com.tiznet.manager.data.model.ProtocolSwitches
import com.tiznet.manager.data.model.UserRow
import com.tiznet.manager.data.model.RealityState
import com.tiznet.manager.data.model.ServerStatusResponse
import com.tiznet.manager.data.model.SettingsResponse
import com.tiznet.manager.data.model.SettingsSaveOut
import com.tiznet.manager.data.model.StartedRun
import com.tiznet.manager.data.model.TagList
import com.tiznet.manager.data.model.UserWrapper
import com.tiznet.manager.data.model.UsersState
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Admin V2 REST API of Hiddify panel (v11-v14 share this surface).
 * All paths are relative to `/<proxy_path>/api/v2/admin/`.
 */
interface HiddifyApi {

    // ---- Bootstrap / account -------------------------------------------------

    @GET
    suspend fun bootstrap(@Url url: String): BootstrapResponse

    /**
     * Panel version. Lives under `/api/v2/panel/info/` (outside the admin
     * prefix), so the full absolute URL is built by RetrofitFactory and
     * passed in. Available on Hiddify v11 through v14.
     */
    @GET
    suspend fun panelInfo(@Url url: String): PanelInfo

    // ---- Legacy commercial REST API v2 (Hiddify v11/v12) --------------------
    // These panels predate Admin V2: no bootstrap endpoint and a smaller route
    // table. Same /api/v2/admin/ base, singular "user/" and "admin_user/".

    @GET("user/")
    suspend fun legacyUsers(): List<UserRow>

    @GET("user/{uuid}/")
    suspend fun legacyUser(@Path("uuid") uuid: String): UserRow

    @POST("user/")
    suspend fun legacyCreateUser(@Body body: JsonObject): UserRow

    @PATCH("user/{uuid}/")
    suspend fun legacyUpdateUser(
        @Path("uuid") uuid: String,
        @Body body: JsonObject
    ): UserRow

    @DELETE("user/{uuid}/")
    suspend fun legacyDeleteUser(@Path("uuid") uuid: String): Response<Unit>

    @GET("admin_user/")
    suspend fun legacyAdmins(): List<AdminInfo>

    @POST("admin_user/")
    suspend fun legacyCreateAdmin(@Body body: JsonObject): AdminInfo

    @PATCH("admin_user/{uuid}/")
    suspend fun legacyUpdateAdmin(
        @Path("uuid") uuid: String,
        @Body body: JsonObject
    ): AdminInfo

    @DELETE("admin_user/{uuid}/")
    suspend fun legacyDeleteAdmin(@Path("uuid") uuid: String): Response<Unit>

    @GET("me/")
    suspend fun me(): AdminInfo

    @GET("server_status/")
    suspend fun serverStatus(@Query("admin_id") adminId: Int? = null): ServerStatusResponse

    @GET("dashboard/")
    suspend fun dashboard(
        @Query("days") days: Int? = null,
        @Query("include") include: String? = null,
        @Query("child_id") childId: Int? = null
    ): DashboardResponse

    // ---- Users ----------------------------------------------------------------

    @GET("users/")
    suspend fun users(): UsersState

    @GET("users/{uuid}/")
    suspend fun user(@Path("uuid") uuid: String): UserWrapper

    @POST("users/")
    suspend fun createUser(@Body body: JsonObject): UserWrapper

    @PATCH("users/{uuid}/")
    suspend fun updateUser(@Path("uuid") uuid: String, @Body body: JsonObject): UserWrapper

    @DELETE("users/{uuid}/")
    suspend fun deleteUser(@Path("uuid") uuid: String): Response<Unit>

    @POST("users/bulk/")
    suspend fun bulkUserAction(@Body body: JsonObject): BulkResult

    @POST("users/bulk-create/")
    suspend fun bulkCreateUsers(@Body body: JsonObject): BulkCreateResult

    // ---- Admins ---------------------------------------------------------------

    @GET("admins/")
    suspend fun admins(): AdminsResponse

    @POST("admins/")
    suspend fun createAdmin(@Body body: JsonObject): AdminsResponse

    @GET("admins/{uuid}/")
    suspend fun admin(@Path("uuid") uuid: String): JsonObject

    @PATCH("admins/{uuid}/")
    suspend fun updateAdmin(@Path("uuid") uuid: String, @Body body: JsonObject): JsonObject

    @DELETE("admins/{uuid}/")
    suspend fun deleteAdmin(@Path("uuid") uuid: String): Response<Unit>

    @POST("admins/{uuid}/reset-password/")
    suspend fun resetAdminPassword(@Path("uuid") uuid: String, @Body body: JsonObject): JsonObject

    @GET("admins/me/")
    suspend fun myAccount(): AdminInfo

    @PUT("admins/me/password/")
    suspend fun changeMyPassword(@Body body: JsonObject): JsonObject

    @PUT("admins/me/alias/")
    suspend fun changeMyAlias(@Body body: JsonObject): JsonObject

    @PUT("admins/me/language/")
    suspend fun changeMyLanguage(@Body body: JsonObject): JsonObject

    @PUT("admins/me/default-outbound/")
    suspend fun changeMyOutbound(@Body body: JsonObject): JsonObject

    // ---- Nodes ----------------------------------------------------------------

    @GET("nodes/")
    suspend fun nodes(): NodesResponse

    @POST("nodes/")
    suspend fun addNode(@Body body: JsonObject): JsonObject

    @GET("nodes/{id}/")
    suspend fun node(@Path("id") id: Int): JsonObject

    @DELETE("nodes/{id}/")
    suspend fun deleteNode(@Path("id") id: Int): Response<Unit>

    @GET("nodes/{id}/ping/")
    suspend fun pingNode(@Path("id") id: Int): JsonObject

    @GET("nodes/{id}/icmp/")
    suspend fun icmpNode(@Path("id") id: Int): JsonObject

    @POST("nodes/{id}/sync/")
    suspend fun syncNode(@Path("id") id: Int): JsonObject

    @GET("nodes/{id}/sync-status/")
    suspend fun nodeSyncStatus(@Path("id") id: Int): JsonObject

    // ---- Tags -----------------------------------------------------------------

    @GET("tags/")
    suspend fun tags(): TagList

    @POST("tags/")
    suspend fun createTag(@Body body: JsonObject): JsonObject

    @PATCH("tags/{id}/")
    suspend fun updateTag(@Path("id") id: Int, @Body body: JsonObject): TagList

    @DELETE("tags/{id}/")
    suspend fun deleteTag(@Path("id") id: Int): TagList

    @PUT("tags/assign/")
    suspend fun assignTags(@Body body: JsonObject): JsonObject

    // ---- Protocols ------------------------------------------------------------

    @GET("protocols/")
    suspend fun protocols(): ProtocolSwitches

    @PUT("protocols/")
    suspend fun updateProtocols(@Body body: JsonObject): ProtocolSwitches

    // ---- Settings -------------------------------------------------------------

    @GET("settings/")
    suspend fun settings(): SettingsResponse

    @PUT("settings/")
    suspend fun saveSettings(@Body body: JsonObject): SettingsSaveOut

    // ---- Outbounds ------------------------------------------------------------

    @GET("outbounds/")
    suspend fun outbounds(): OutboundsResponse

    @POST("outbounds/")
    suspend fun createOutbound(@Body body: JsonObject): OutboundsResponse

    @PATCH("outbounds/{id}/")
    suspend fun updateOutbound(@Path("id") id: Int, @Body body: JsonObject): JsonObject

    @DELETE("outbounds/{id}/")
    suspend fun deleteOutbound(@Path("id") id: Int): Response<Unit>

    @PUT("outbounds/order/")
    suspend fun reorderOutbounds(@Body body: JsonObject): OutboundsResponse

    @POST("outbounds/{id}/default/")
    suspend fun setDefaultOutbound(@Path("id") id: Int): JsonObject

    @POST("outbounds/{id}/reset/")
    suspend fun resetOutbound(@Path("id") id: Int): JsonObject

    // ---- Domains --------------------------------------------------------------

    @GET("domains-page/")
    suspend fun domains(): DomainsResponse

    @POST("domains-page/")
    suspend fun saveDomains(@Body body: JsonObject): DomainsResponse

    @PATCH("domains-page/{id}/")
    suspend fun patchDomain(@Path("id") id: Int, @Body body: JsonObject): DomainsResponse

    @DELETE("domains-page/{id}/")
    suspend fun deleteDomain(@Path("id") id: Int): Response<Unit>

    @PUT("domains-page/order/")
    suspend fun reorderDomains(@Body body: JsonObject): DomainsResponse

    @POST("domains-page/detect/")
    suspend fun detectDomain(@Body body: JsonObject): DomainDetection

    @GET("domains-page/port-check/")
    suspend fun portCheck(@Query("domain") domain: String, @Query("port") port: Int): JsonObject

    @GET("domains/options/")
    suspend fun domainOptions(): JsonObject

    // ---- Apply / logs ---------------------------------------------------------

    @GET("apply/")
    suspend fun applyState(): ApplyState

    @POST("apply/{action}/")
    suspend fun runApply(@Path("action") action: String): StartedRun

    @GET("apply/log/")
    suspend fun applyLog(
        @Query("name") name: String? = null,
        @Query("offset") offset: Long? = null
    ): LogChunk

    @POST("apply/logs/clear/")
    suspend fun clearApplyLogs(): JsonObject

    @FormUrlEncoded
    @POST("log/")
    suspend fun readLogFile(@Field("file") file: String): Response<ResponseBody>

    // ---- Backup ---------------------------------------------------------------

    @GET("backup/")
    suspend fun backupState(): BackupState

    @POST("backup/")
    suspend fun createBackup(): BackupState

    @GET("backup/download/")
    suspend fun downloadBackup(): ResponseBody

    @Multipart
    @POST("backup/restore/")
    suspend fun restoreBackup(
        @Part file: MultipartBody.Part,
        @Part("settings") settings: okhttp3.RequestBody,
        @Part("users") users: okhttp3.RequestBody,
        @Part("domains") domains: okhttp3.RequestBody,
        @Part("replace_owner_admin") replaceOwner: okhttp3.RequestBody
    ): JsonObject

    @POST("backup/restore/run/")
    suspend fun runRestore(@Body body: JsonObject): JsonObject

    @POST("backup/files/")
    suspend fun deleteBackupFile(@Body body: JsonObject): BackupState

    @POST("backup/telegram/")
    suspend fun sendBackupTelegram(): JsonObject

    // ---- Broadcast ------------------------------------------------------------

    @GET("broadcast/")
    suspend fun broadcastState(@Query("ids") ids: String? = null): BroadcastState

    @GET("broadcast/recipients/")
    suspend fun broadcastRecipients(): JsonObject

    @POST("broadcast/preview/")
    suspend fun broadcastPreview(@Body body: JsonObject): JsonObject

    @POST("broadcast/")
    suspend fun sendBroadcast(@Body body: JsonObject): JsonObject

    // ---- Clean IP / Reality ---------------------------------------------------

    @GET("clean-ip/reality/")
    suspend fun realityState(): RealityState

    @PUT("clean-ip/reality/")
    suspend fun saveReality(@Body body: JsonObject): RealityState

    @GET("clean-ip/network/")
    suspend fun cleanIpNetwork(): List<CleanIpNetwork>

    @GET("clean-ip/sni-candidates/")
    suspend fun sniCandidates(): JsonObject

    @POST("clean-ip/token/")
    suspend fun runCleanIpScan(@Body body: JsonObject): JsonObject

    // ---- System actions -------------------------------------------------------

    @GET("all-configs/")
    suspend fun allConfigs(): JsonObject

    @POST("reset-cache/")
    suspend fun resetCache(): JsonObject

    @POST("update_user_usage/")
    suspend fun updateUserUsage(): JsonObject

    @GET("all-public-port/")
    suspend fun allPublicPorts(): JsonObject

    // ---- Config tester --------------------------------------------------------

    @POST("config-tester/")
    suspend fun startConfigTest(@Body body: JsonObject): JsonObject

    @GET("config-tester/{jobId}/")
    suspend fun configTestJob(@Path("jobId") jobId: String): JsonObject

    @DELETE("config-tester/{jobId}/")
    suspend fun deleteConfigTestJob(@Path("jobId") jobId: String): Response<Unit>
}
