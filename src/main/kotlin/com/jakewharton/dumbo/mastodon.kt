package com.jakewharton.dumbo

import java.time.Instant
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8

data class Toot(
	val id: Statuses.Id,
	val text: String,
	val posted: Instant,
	val language: String,
	val favoriteCount: UInt,
	val reblogCount: UInt,
) {
	companion object {
		fun fromTweet(tweet: Tweet): Toot {
			return Toot(
				id = Statuses.Id(snowflakeId("statuses", tweet.created_at)),
				text = tweet.full_text,
				posted = tweet.created_at,
				language = tweet.lang,
				favoriteCount = tweet.favorite_count,
				reblogCount = tweet.retweet_count,
			)
		}
	}
}

// Equivalent to Ruby's `SecureRandom.hex(16)`.
private val randomHex = "64924ea1830cc0ea"
private var sequence = 0L
private fun snowflakeId(tableName: String, timestamp: Instant): Long {
	// From https://github.com/mastodon/mastodon/blob/28cda42af5983d2d450c2c0a9fa8cd38006d8089/lib/mastodon/snowflake.rb#L68-L105
	val epochMillis = timestamp.toEpochMilli() shl 16
	// md5(table_name || $randomHex || time_part::text)
	val md5data = (tableName + randomHex + epochMillis).encodeUtf8().md5().hex()
	// 'x' || substr($md5data, 1, 4)
	val hex = md5data.substring(0, 4).decodeHex()
	// $hex::bit(16)::bigint
	val sequenceBase = hex.asByteBuffer().short
	val tail = (sequenceBase + ++sequence) and 65535
	return epochMillis or tail
}
