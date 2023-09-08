package com.jakewharton.dumbo

import com.jakewharton.dumbo.Tweet.MediaEntity
import com.jakewharton.dumbo.Tweet.MentionEntity
import com.jakewharton.dumbo.Tweet.UrlEntity
import java.time.Instant

data class Toot(
	val text: String,
	val posted: Instant,
	val language: String,
	val inReplyToId: String? = null,
	val media: List<Media> = emptyList(),
) {
	data class Media(
		val id: String,
		val filename: String,
	)

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
						is MediaEntity -> {
							// If the text is already non-empty then it will have contained a space
							// between the existing text and the URL to the media. Remove that space.
							if (isNotEmpty()) {
								check(this[lastIndex] == ' ')
								deleteCharAt(lastIndex)
							}
							// Nothing to append as text!
						}
					}
					index = entity.indices.last
				}
				if (index < tweet.text.length) {
					append(tweet.text.substring(index))
				}
			}
			val media = buildList {
				for (entity in tweet.entities.filterIsInstance<MediaEntity>()) {
					this += Media(
						id = entity.id,
						filename = entity.filename,
					)
				}
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
				media = media,
			)
		}
	}
}
