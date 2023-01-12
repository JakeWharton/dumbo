package com.jakewharton.dumbo

import java.nio.file.Path
import java.time.ZoneOffset.UTC
import java.util.Scanner
import java.util.UUID
import kotlin.system.exitProcess
import okhttp3.HttpUrl
import retrofit2.HttpException

class DumboApp(
	private val api: MastodonApi,
) {
	suspend fun run(
		host: HttpUrl,
		archiveDir: Path,
		identityMapping: IdentityMapping,
		performEdits: Boolean,
		debug: Boolean,
	) {
		fun debug(body: () -> Any) {
			if (debug) {
				println("DEBUG ${body()}")
			}
		}

		val scanner = Scanner(System.`in`)

		val authenticator = MastodonAuthenticator(archiveDir, host, api, scanner)
		val authorization = authenticator.obtain()

		val twitterArchive = TwitterArchive(archiveDir)
		val tweets = twitterArchive.loadTweets()
		debug { "Loaded ${tweets.size} tweets" }

		val dumboDb = NioPathDumboDb(archiveDir)

		for (tweet in tweets) {
			if (tweet.isRetweet) {
				debug { "[${tweet.id}] Do not keep retweets of tweets from other authors" }
				continue
			}
			if (tweet.isMention) {
				debug { "[${tweet.id}] Do not keep @mentions to individual accounts" }
				continue
			}

			if (tweet.inReplyToId != null && tweet.inReplyToId !in dumboDb) {
				debug { "[${tweet.id}] Do not keep replies to tweets which are not my own or which we explicitly skipped" }
				continue
			}

			val existingStatus = if (tweet.id in dumboDb) {
				val existingTootId = dumboDb[tweet.id]
				if (existingTootId == null) {
					debug { "[${tweet.id}] This Tweet was explicitly ignored" }
					continue
				}
				if (!performEdits) {
					debug { "[${tweet.id}] This Tweet was already posted and we are not performing edits" }
					continue
				}
				try {
					api.getStatus(existingTootId)
				} catch (e: HttpException) {
					if (e.code() == 404) {
						println("Cross-posted tweet (${tweet.url}) was deleted from Mastodon.")
						print("Remove from log ($inputYes, $inputNo, $inputSkip): ")
						when (val input = scanner.next()) {
							inputYes -> {
								dumboDb -= tweet.id
								println("-------")
								null
							}
							inputNo -> {
								return
							}
							inputSkip -> {
								println("-------")
								continue
							}
							else -> {
								System.err.println("Unknown input: $input")
								exitProcess(129)
							}
						}
					} else {
						throw e
					}
				}
			} else {
				null
			}

			val toot = Toot.fromTweet(tweet, dumboDb, identityMapping)

			if (existingStatus != null && toot.text == existingStatus.content) {
				debug { "[${tweet.id}] Existing post content unchanged" }
				continue
			}

			println(tweet.toQuickPrettyString())
			println()
			if (existingStatus != null) {
				println("OLD ${existingStatus.toQuickPrettyString()}")
				println()
				print("NEW ")
			}
			println(toot.toQuickPrettyString())
			println()
			print("Post? ($inputYes, $inputNo, $inputSkip): ")
			when (val input = scanner.next()) {
				inputYes -> {
					if (existingStatus != null) {
						api.editStatus(
							id = existingStatus.id,
							authorization = authorization,
							idempotency = UUID.randomUUID().toString(),
							content = toot.text,
						)
					} else {
						val statusEntity = api.createStatus(
							authorization = authorization,
							idempotency = UUID.randomUUID().toString(),
							content = toot.text,
							language = toot.language,
							createdAt = toot.posted.atOffset(UTC).toString(),
							inReplyToId = toot.inReplyToId,
						)

						dumboDb[tweet.id] = statusEntity.id
					}
				}

				inputNo -> {
					dumboDb[tweet.id] = null
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
