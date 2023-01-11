package com.jakewharton.dumbo

import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.util.Scanner
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl

class DumboApp(
	private val api: MastodonApi,
) {
	suspend fun run(
		host: HttpUrl,
		archiveDir: Path,
		debug: Boolean = false,
	) {
		fun debug(body: () -> Any) {
			if (debug) {
				println("DEBUG ${body()}")
			}
		}

		val scanner = Scanner(System.`in`)

		val authJson = Json { prettyPrint = true }
		val dumboAuthPath = archiveDir.resolve("dumbo_auth.json")
		var auth: MastodonAuth
		if (dumboAuthPath.exists()) {
			val jsonObject = authJson.decodeFromString(JsonObject.serializer(), dumboAuthPath.readText())
			val serializer = if ("access_token" in jsonObject) {
				MastodonAuthStage2.serializer()
			} else {
				MastodonAuthStage1.serializer()
			}
			auth = authJson.decodeFromJsonElement(serializer, jsonObject)
		} else {
			val createApplicationEntity = api.createApplication(
				clientName = "Dumbo Tweet Importer",
				redirectUris = "urn:ietf:wg:oauth:2.0:oob",
				scopes = "read write",
				website = "https://github.com/JakeWharton/dumbo",
			)
			auth = MastodonAuthStage1(
				client_id = createApplicationEntity.client_id,
				client_secret = createApplicationEntity.client_secret,
			)
			dumboAuthPath.writeText(authJson.encodeToString(auth))
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
			print("Paste resulting code: ")
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
			dumboAuthPath.writeText(authJson.encodeToString(auth))
		}
		check(auth is MastodonAuthStage2)
		val authorization = "Bearer ${auth.access_token}"

		val id = api.verifyCredentials(authorization).id
		debug { "User ID: $id" }


		val opLogPath = archiveDir.resolve("dumbo_log.txt")

		val twitterArchive = TwitterArchive(archiveDir)
		val tweets = twitterArchive.loadTweets()
		debug { "Loaded ${tweets.size} tweets" }

			for (tweet in tweets) {
				val opMap = opLogPath.toOpMap()
				debug { "Op map: $opMap" }

				val seenIdsForReplies = opMap.filterValues { it != null }.keys

				if (tweet.isRetweet) {
					debug { "[${tweet.id}] Do not keep retweets of tweets from other authors" }
					continue
				}
				if (tweet.isMention) {
					debug { "[${tweet.id}] Do not keep @mentions to individual accounts" }
					continue
				}
				if (tweet.inReplyToId != null && tweet.inReplyToId !in seenIdsForReplies) {
					debug { "[${tweet.id}] Do not keep replies to tweets which are not my own or which we explicitly skipped" }
					continue
				}
				if (tweet.id in opMap) {
					debug { "[${tweet.id}] We have already processed this Tweet" }
					continue
				}

				val toot = Toot.fromTweet(tweet)

				println("TWEET: ${tweet.url}")
				println(tweet)
				println()
				println("TOOT:")
				println(toot)
				println()
				print("Post? ($inputYes, $inputNo, $inputSkip): ")
				when (val input = scanner.next()) {
					inputYes -> {
						val statusEntity = api.createStatus(
							authorization = authorization,
							idempotency = UUID.randomUUID().toString(),
							status = toot.text,
							language = toot.language,
							createdAt = toot.posted.atOffset(UTC).toString()
						)

						opLogPath.appendId(tweet.id, statusEntity.id)
					}

					inputNo -> {
						opLogPath.appendId(tweet.id, null)
					}

					inputSkip -> Unit // Nothing to do!
					else -> {
						System.err.println("Unknown input: $input")
						exitProcess(129)
					}
				}

				println("-------")
			}
	}

	private companion object {
		private const val inputYes = "yes"
		private const val inputNo = "no"
		private const val inputSkip = "skip"
	}
}
