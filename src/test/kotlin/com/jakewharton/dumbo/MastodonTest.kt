package com.jakewharton.dumbo

import java.time.Instant
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Test

class MastodonTest {
	@Test fun fromTweet1() {
		val tweet = Tweet(
			edit_info = JsonNull,
			retweeted = false,
			source = """<a href="http://twitter.com" rel="nofollow">Twitter Web Client</a>""",
			entities = Entities(
				hashtags = listOf(),
				media = listOf(),
				symbols = listOf(),
				user_mentions = listOf(),
				urls = listOf(
					UrlEntity(
						url = "http://t.co/Ysy68q4",
						expanded_url = "https://market.android.com/search?q=seriesguide",
						display_url = "market.android.com/search?q=serie…",
						indices = 18..37,
					),
					UrlEntity(
						url = "http://t.co/CxvKWoE",
						expanded_url = "https://github.com/UweTrottmann/SeriesGuide",
						display_url = "github.com/UweTrottmann/S…",
						indices = 97..116,
					),
				),
			),
			extended_entities = null,
			display_text_range = 0..116,
			favorite_count = 0U,
			id_str = "87764348256272384",
			truncated = false,
			retweet_count = 0U,
			id = "87764348256272384",
			possibly_sensitive = false,
			created_at = Instant.parse("2011-07-04T06:07:05Z"),
			favorited = false,
			full_text = "SeriesGuide beta (http://t.co/Ysy68q4) is now using ActionBarSherlock. Please support and fork!! http://t.co/CxvKWoE",
			lang = "en",
			in_reply_to_status_id = null,
			in_reply_to_status_id_str = null,
			in_reply_to_user_id = null,
			in_reply_to_user_id_str = null,
			in_reply_to_screen_name = null,
			coordinates = null,
			geo = null,
		)
		val expected = Toot(
			text = "SeriesGuide beta (https://market.android.com/search?q=seriesguide) is now using ActionBarSherlock. Please support and fork!! https://github.com/UweTrottmann/SeriesGuide",
			posted = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			favoriteCount = 0U,
			reblogCount = 0U,
		)
		val actual = Toot.fromTweet(tweet)
		assertEquals(expected, actual)
	}
}
