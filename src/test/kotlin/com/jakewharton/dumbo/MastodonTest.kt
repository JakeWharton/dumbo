package com.jakewharton.dumbo

import com.jakewharton.dumbo.Tweet.MentionEntity
import com.jakewharton.dumbo.Tweet.UrlEntity
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Test

class MastodonTest {
	@Test fun urlsReplaced() {
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
		val actual = Toot.fromTweet(tweet, emptyMap())
		assertEquals(expected, actual)
	}

	@Test fun urlsOutOfOrderReplaced() {
		val tweet = Tweet(
			id = "87764348256272384",
			createdAt = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			text = "SeriesGuide beta (http://t.co/Ysy68q4) is now using ActionBarSherlock. Please support and fork!! http://t.co/CxvKWoE",
			entities = listOf(
				UrlEntity(
					url = "https://github.com/UweTrottmann/SeriesGuide",
					indices = 97..116,
				),
				UrlEntity(
					url = "https://market.android.com/search?q=seriesguide",
					indices = 18..37,
				),
			),
		)
		val expected = Toot(
			text = "SeriesGuide beta (https://market.android.com/search?q=seriesguide) is now using ActionBarSherlock. Please support and fork!! https://github.com/UweTrottmann/SeriesGuide",
			posted = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
		)
		val actual = Toot.fromTweet(tweet, emptyMap())
		assertEquals(expected, actual)
	}

	@Test fun replyMapHit() {
		val replyMap = mapOf(
			"1" to null,
			"2" to "1234",
		)
		val tweet = Tweet(
			id = "3",
			inReplyToId = "2",
			createdAt = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			text = "Just setting up my Dumbo",
		)
		val expected = Toot(
			text = "Just setting up my Dumbo",
			posted = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			inReplyToId = "1234",
		)
		val actual = Toot.fromTweet(tweet, replyMap)
		assertEquals(expected, actual)
	}

	@Test fun replyMapExplicitNullThrows() {
		val replyMap = mapOf(
			"1" to null,
			"2" to "1234",
		)
		val tweet = Tweet(
			id = "3",
			inReplyToId = "1",
			createdAt = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			text = "Just setting up my Dumbo",
		)
		val t = assertFailsWith<IllegalStateException> {
			Toot.fromTweet(tweet, replyMap)
		}
		assertEquals("Unable to map tweet 3 replying to 1 without tootMap entry", t.message)
	}

	@Test fun replyMapMissThrows() {
		val replyMap = mapOf(
			"1" to null,
			"2" to "1234",
		)
		val tweet = Tweet(
			id = "4",
			inReplyToId = "3",
			createdAt = Instant.parse("2011-07-04T06:07:05Z"),
			language = "en",
			text = "Just setting up my Dumbo",
		)
		val t = assertFailsWith<IllegalStateException> {
			Toot.fromTweet(tweet, replyMap)
		}
		assertEquals("Unable to map tweet 4 replying to 3 without tootMap entry", t.message)
	}

	@Test fun mentionsReplacedWithMastodonConvention() {
		val tweet = Tweet(
			id = "91268136095068160",
			createdAt = Instant.parse("2011-07-13T22:09:53Z"),
			language = "en",
			text = "Got psuedo-confirmation from @retomeier that the action bar will not be part of future compat library revs! Good news for ActionBarSherlock.",
			entities = listOf(
				MentionEntity(
					username = "retomeier",
					indices = 29..39,
				)
			),
		)
		val expected = Toot(
			text = "Got psuedo-confirmation from @retomeier@twitter.com that the action bar will not be part of future compat library revs! Good news for ActionBarSherlock.",
			posted = Instant.parse("2011-07-13T22:09:53Z"),
			language = "en",
		)
		val actual = Toot.fromTweet(tweet, emptyMap())
		assertEquals(expected, actual)
	}
}
