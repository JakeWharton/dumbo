package com.jakewharton.dumbo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.MultipartBody
import org.jsoup.Jsoup
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface MastodonApi {
	@FormUrlEncoded
	@POST("api/v1/apps")
	suspend fun createApplication(
		@Field("client_name") clientName: String,
		@Field("redirect_uris") redirectUris: String,
		@Field("scopes") scopes: String,
		@Field("website") website: String,
	): CreateApplicationEntity

	@FormUrlEncoded
	@POST("oauth/token")
	suspend fun createOauthToken(
		@Field("client_id") clientId: String,
		@Field("client_secret") clientSecret: String,
		@Field("redirect_uri") redirectUri: String,
		@Field("grant_type") grantType: String,
		@Field("code") code: String,
		@Field("scope") scope: String,
	): CreateTokenEntity

	@GET("api/v1/accounts/verify_credentials")
	suspend fun verifyCredentials(
		@Header("Authorization") authorization: String,
	): AccountEntity

	@FormUrlEncoded
	@POST("api/v1/statuses")
	suspend fun createStatus(
		@Header("Authorization") authorization: String,
		@Header("Idempotency-Key") idempotency: String,
		@Field("status") content: String,
		@Field("language") language: String?,
		@Field("created_at") createdAt: String,
		@Field("in_reply_to_id") inReplyToId: String?,
		@Field("media_ids[]") mediaIds: List<String>?,
	): StatusEntity

	@GET("api/v1/statuses/{id}")
	suspend fun getStatus(
		@Path("id") id: String,
	): StatusEntity

	@FormUrlEncoded
	@POST("api/v1/statuses/{id}")
	suspend fun editStatus(
		@Header("Authorization") authorization: String,
		@Header("Idempotency-Key") idempotency: String,
		@Path("id") id: String,
		@Field("status") content: String,
	): StatusEntity

	@Multipart
	@POST("api/v2/media")
	suspend fun uploadMedia(
		@Header("Authorization") authorization: String,
		@Part file: MultipartBody.Part,
		@Part("description") description: String?,
		@Part("focus") focus: String?,
	): Response<MediaAttachment>

	@GET("/api/v1/media/{id}")
	suspend fun getMedia(
		@Path("id") id: String,
	): Response<MediaAttachment>
}

@Serializable
data class CreateApplicationEntity(
	val client_id: String,
	val client_secret: String,
)

@Serializable
data class CreateTokenEntity(
	val access_token: String,
	val token_type: String,
	val scope: String,
)

@Serializable
data class AccountEntity(
	val id: String,
)

@Serializable
data class StatusEntity(
	val id: String,
	@SerialName("content") val rawContent: String,
) {
	@Transient
	val content: String = run {
		val parsed = Jsoup.parseBodyFragment(rawContent).body()
		if (parsed.childrenSize() == 0) {
			parsed.text()
		} else {
			parsed.children().joinToString("\n\n") { it.text() }
		}
	}
}

@Serializable
data class MediaAttachment(
	val id: String,
)
