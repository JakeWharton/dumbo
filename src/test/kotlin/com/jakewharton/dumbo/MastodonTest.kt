package com.jakewharton.dumbo

import com.jakewharton.dumbo.Tweet.UrlEntity
import java.time.Instant
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Test

class MastodonTest {
	@Test fun fromTweet1() {
		val tweet = Tweet(
			id = "87764348256272384",
			createdAt = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			text = "SeriesGuide beta (http://t.co/Ysy68q4) is now using ActionBarSherlock. Please support and fork!! http://t.co/CxvKWoE",
			entities = listOf(
				UrlEntity(
					url = "https://market.android.com/search?q=seriesguide",
					indices = 18..37,
				),
				UrlEntity(
					url = "https://github.com/UweTrottmann/SeriesGuide",
					indices = 97..116,
				),
			),
		)
		val expected = Toot(
			text = "SeriesGuide beta (https://market.android.com/search?q=seriesguide) is now using ActionBarSherlock. Please support and fork!! https://github.com/UweTrottmann/SeriesGuide",
			posted = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
		)
		val actual = Toot.fromTweet(tweet)
		assertEquals(expected, actual)
	}
}
