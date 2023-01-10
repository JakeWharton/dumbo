package com.jakewharton.dumbo

import java.time.Instant

data class Toot(
	val text: String,
	val posted: Instant,
	val language: String,
	val favoriteCount: UInt,
	val reblogCount: UInt,
) {
	companion object {
		fun fromTweet(tweet: Tweet): Toot {
			val text = buildString {
				var index = 0
				for (url in tweet.entities.urls) {
					if (url.indices.first > index) {
						append(tweet.full_text.substring(index, url.indices.first))
					}
					append(url.expanded_url)
					index = url.indices.last
				}
				if (index < tweet.full_text.length) {
					append(tweet.full_text.substring(index))
				}
			}
			return Toot(
				text = text,
				posted = tweet.created_at,
				language = tweet.lang,
				favoriteCount = tweet.favorite_count,
				reblogCount = tweet.retweet_count,
			)
		}
	}
}
