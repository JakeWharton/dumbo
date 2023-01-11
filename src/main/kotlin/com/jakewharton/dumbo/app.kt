package com.jakewharton.dumbo

import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.util.Scanner
import java.util.UUID
import kotlin.system.exitProcess
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

		val authenticator = MastodonAuthenticator(archiveDir, host, api, scanner)
		val authorization = authenticator.obtain()

		val id = api.verifyCredentials(authorization).id
		debug { "Mastodon user ID: $id" }

		val opLogPath = archiveDir.resolve("dumbo_log.txt")

		val twitterArchive = TwitterArchive(archiveDir)
		val tweets = twitterArchive.loadTweets()
		debug { "Loaded ${tweets.size} tweets" }

			for (tweet in tweets) {
				val opMap = opLogPath.toOpMap()
				debug { "Op map: $opMap" }

				val postedTweetIds = opMap.filterValues { it != null }.keys

				if (tweet.isRetweet) {
					debug { "[${tweet.id}] Do not keep retweets of tweets from other authors" }
					continue
				}
				if (tweet.isMention) {
					debug { "[${tweet.id}] Do not keep @mentions to individual accounts" }
					continue
				}
				if (tweet.inReplyToId != null && tweet.inReplyToId !in postedTweetIds) {
					debug { "[${tweet.id}] Do not keep replies to tweets which are not my own or which we explicitly skipped" }
					continue
				}
				if (tweet.id in opMap) {
					debug { "[${tweet.id}] We have already processed this Tweet" }
					continue
				}

				val toot = Toot.fromTweet(tweet, opMap)

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
							createdAt = toot.posted.atOffset(UTC).toString(),
							inReplyToId = toot.inReplyToId,
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
