package com.jakewharton.dumbo

import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Scanner
import kotlin.system.exitProcess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.buffer
import okio.source

@OptIn(ExperimentalSerializationApi::class)
class DumboApp(
	private val config: DumboConfig,
	private val accountId: Long,
	private val applicationId: Long?,
) {
	fun run(archiveDir: Path, debug: Boolean = false) {
		fun debug(body: () -> Any) {
			if (debug) {
				println("DEBUG ${body()}")
			}
		}

		val tweets = archiveDir.resolve("data/tweets.js")
		val opLogPath = archiveDir.resolve("dumbo_log.txt")

		val source = tweets.source().buffer()
		check(source.rangeEquals(0, tweetsPrefix)) {
			"Tweets file did not start with $tweetsPrefix"
		}
		source.skip(tweetsPrefix.size.toLong())

		val scanner = Scanner(System.`in`)
		val entries = Json.decodeFromBufferedSource(ListSerializer(TweetEntry.serializer()), source)
			.sorted()
		debug { "Loaded ${entries.size} tweets" }

		val connection = PostgresConnection(
			host = config.database.host,
			port = config.database.port,
			database = config.database.name,
			user = config.database.username,
			password = config.database.password,
		)
		withDatabase(connection) { db ->
			for (entry in entries) {
				val opMap = opLogPath.toOpMap()
				debug { "Op map: $opMap" }

				val seenIdsForReplies = opMap.filterValues { it != null }.keys

				if (entry.tweet.full_text.startsWith("RT @")) {
					debug { "[${entry.tweet.id}] Do not keep retweets of tweets from other authors" }
					continue
				}
				if (entry.tweet.full_text.startsWith("@")) {
					debug { "[${entry.tweet.id}] Do not keep @mentions to individual accounts" }
					continue
				}
				if (entry.tweet.in_reply_to_status_id != null && entry.tweet.in_reply_to_status_id !in seenIdsForReplies) {
					debug { "[${entry.tweet.id}] Do not keep replies to tweets which are not my own or which we explicitly skipped" }
					continue
				}
				if (entry.tweet.id in config.tweets.ignoredIds) {
					debug { "[${entry.tweet.id}] Do not keep tweets explicitly ignored" }
					continue
				}
				if (entry.tweet.id in opMap) {
					debug { "[${entry.tweet.id}] We have already processed this Tweet" }
					continue
				}

				val toot = Toot.fromTweet(entry.tweet)

				println("TWEET: https://twitter.com/twitter/status/${entry.tweet.id}")
				println(entry)
				println()
				println("TOOT:")
				println(toot)
				println()
				print("Post? ($inputYes, $inputNo, $inputSkip): ")
				when (val input = scanner.next()) {
					inputYes -> {
						db.transaction {
							val now = LocalDateTime.now()

							val conversationId = db.conversationsQueries.create(now).executeAsOne()
							debug { "Conversation ID: $conversationId" }

							db.statusesQueries.insert(
								id = toot.id,
								uri = "https://mastodon.jakewharton.com/users/jw/statuses/${toot.id}",
								text = toot.text,
								timestamp = LocalDateTime.ofInstant(toot.posted, ZoneOffset.UTC),
								language = "en",
								conversation_id = conversationId,
								account_id = accountId,
								application_id = applicationId,
							)

							db.status_statsQueries.insert(
								status_id = toot.id,
								replies_count = 0L,
								reblogs_count = toot.reblogCount.toLong(),
								favourites_count = toot.favoriteCount.toLong(),
								timestamp = now,
							)
						}

						opLogPath.appendId(entry.tweet.id, toot.id)
					}

					inputNo -> {
						opLogPath.appendId(entry.tweet.id, null)
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
	}

	private companion object {
		val tweetsPrefix = "window.YTD.tweets.part0 = ".encodeUtf8()
		private const val inputYes = "yes"
		private const val inputNo = "no"
		private const val inputSkip = "skip"
	}
}
