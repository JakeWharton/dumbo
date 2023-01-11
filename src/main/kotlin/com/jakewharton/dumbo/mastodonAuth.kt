package com.jakewharton.dumbo

import java.nio.file.Path
import java.util.Scanner
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl

class MastodonAuthenticator(
	directory: Path,
	private val host: HttpUrl,
	private val api: MastodonApi,
	private val scanner: Scanner,
) {
	private val dumboAuthPath = directory.resolve("dumbo_auth.json")

	suspend fun obtain(): String {
		var auth = if (dumboAuthPath.exists()) {
			val jsonObject = json.decodeFromString(JsonObject.serializer(), dumboAuthPath.readText())
			val serializer = if ("access_token" in jsonObject) {
				MastodonAuthStage2.serializer()
			} else {
				MastodonAuthStage1.serializer()
			}
			json.decodeFromJsonElement(serializer, jsonObject)
		} else {
			val createApplicationEntity = api.createApplication(
				clientName = "Dumbo Tweet Importer",
				redirectUris = "urn:ietf:wg:oauth:2.0:oob",
				scopes = "read write",
				website = "https://github.com/JakeWharton/dumbo",
			)
			val auth = MastodonAuthStage1(
				client_id = createApplicationEntity.client_id,
				client_secret = createApplicationEntity.client_secret,
			)
			dumboAuthPath.writeText(json.encodeToString(auth))
			auth
		}

		if (auth is MastodonAuthStage1) {
			val authUrl = host.newBuilder("oauth/authorize")!!
				.addQueryParameter("client_id", auth.client_id)
				.addQueryParameter("scope", "read write")
				.addQueryParameter("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
				.addQueryParameter("response_type", "code")
				.build()
			println()
			println("Visit $authUrl in your browser")
			print("Paste resulting code and press enter: ")
			val code = scanner.next()!!
			println()

			val createTokenEntity = api.createOauthToken(
				clientId = auth.client_id,
				clientSecret = auth.client_secret,
				redirectUri = "urn:ietf:wg:oauth:2.0:oob",
				grantType = "authorization_code",
				code = code,
				scope = "read write"
			)
			check(createTokenEntity.token_type == "Bearer")
			check("write" in createTokenEntity.scope.split(" "))
			auth = MastodonAuthStage2(
				client_id = auth.client_id,
				client_secret = auth.client_secret,
				access_token = createTokenEntity.access_token,
			)
			dumboAuthPath.writeText(json.encodeToString(auth))
		}

		check(auth is MastodonAuthStage2)
		return "Bearer ${auth.access_token}"
	}

	private companion object {
		val json = Json { prettyPrint = true }
	}
}

private sealed interface MastodonAuthStage

@Serializable
private data class MastodonAuthStage1(
	val client_id: String,
	val client_secret: String,
) : MastodonAuthStage

@Serializable
private data class MastodonAuthStage2(
	val client_id: String,
	val client_secret: String,
	val access_token: String,
) : MastodonAuthStage
