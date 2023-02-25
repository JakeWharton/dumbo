package com.jakewharton.dumbo

import com.jakewharton.dumbo.Tweet.MentionEntity
import com.jakewharton.dumbo.Tweet.UrlEntity
import java.time.Instant

data class Toot(
	val text: String,
	val posted: Instant,
	val language: String,
	val inReplyToId: String? = null,
	val attachments: List<Attachment>,
) {
	companion object {
		fun fromTweet(
			tweet: Tweet,
			dumboDb: DumboDb,
			identityMapping: IdentityMapping,
		): Toot {
			val text = buildString {
				var index = 0
				for (entity in tweet.entities.sortedBy { it.indices.first }) {
					if (entity.indices.first > index) {
						append(tweet.text.substring(index, entity.indices.first))
					}
					when (entity) {
						is UrlEntity -> {
							append(entity.url)
						}

						is MentionEntity -> {
							append(identityMapping.map(entity.id, entity.username))
						}

						is Tweet.MediaEntity -> {
							// media will be added as attachments
						}
					}
					index = entity.indices.last
				}
				if (index < tweet.text.length) {
					append(tweet.text.substring(index))
				}
			}.trim()

			val attachments = tweet.entities.filterIsInstance<Tweet.MediaEntity>().map {
				// media filename is build from media id and a suffes from url
				Attachment(
					fileId = it.url.substringAfterLast("/").substringBeforeLast("."),
					description = it.description,
				)
			}

			val inReplyToId = if (tweet.inReplyToId == null) {
				null
			} else {
				checkNotNull(dumboDb[tweet.inReplyToId]) {
					"Unable to map tweet ${tweet.id} replying to ${tweet.inReplyToId} without tootMap entry"
				}
			}
			return Toot(
				text = text,
				posted = tweet.createdAt,
				language = tweet.language,
				inReplyToId = inReplyToId,
				attachments = attachments,
			)
		}
	}

	data class Attachment(
		val fileId: String,
		val description: String?,
	)
}
