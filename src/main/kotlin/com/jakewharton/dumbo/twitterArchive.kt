package com.jakewharton.dumbo

import com.jakewharton.dumbo.Tweet.MentionEntity
import com.jakewharton.dumbo.Tweet.UrlEntity
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.buffer
import okio.source

class TwitterArchive(
	directory: Path,
) {
	private val tweets = directory.resolve("data/tweets.js")

	@OptIn(ExperimentalSerializationApi::class)
	fun loadTweets(): List<Tweet> {
		val entries = tweets.source().buffer().use { source ->
			check(source.rangeEquals(0, tweetsPrefix)) {
				"$tweets did not start with $tweetsPrefix"
			}
			source.skip(tweetsPrefix.size.toLong())

			json.decodeFromBufferedSource(ListSerializer(ArchiveTweetEntry.serializer()), source)
		}
		return entries.map {
			Tweet(
				id = it.tweet.id,
				inReplyToId = it.tweet.in_reply_to_status_id,
				createdAt = it.tweet.created_at,
				language = it.tweet.lang,
				text = it.tweet.full_text,
				entities = buildList {
					this += it.tweet.entities.urls.map { entity ->
						UrlEntity(
							url = entity.expanded_url,
							indices = entity.indices,
						)
					}
					this += it.tweet.entities.user_mentions.map { entity ->
						MentionEntity(
							id = entity.id,
							username = entity.screen_name,
							indices = entity.indices,
						)
					}
				}
			)
		}.sorted()
	}

	private companion object {
		val tweetsPrefix = "window.YTD.tweets.part0 = ".encodeUtf8()
		val json = Json {
			ignoreUnknownKeys = false
		}
	}
}

/** A user-friendly model of a Tweet massaged from the raw JSON of the archive. */
data class Tweet(
	val id: String,
	val inReplyToId: String? = null,
	val createdAt: Instant,
	val language: String,
	val text: String,
	val entities: List<Entity> = emptyList(),
) : Comparable<Tweet> {
	/** A clickable URL. */
	val url get() = "https://twitter.com/twitter/status/$id"
	val isRetweet get() = text.startsWith("RT @")
	val isMention get() = text.startsWith("@")

	override fun compareTo(other: Tweet) = comparator.compare(this, other)

	private companion object {
		private val comparator = compareBy(Tweet::createdAt)
			.thenByDescending(Tweet::id)
	}

	sealed interface Entity {
		val indices: IntRange
	}
	data class UrlEntity(
		val url: String,
		override val indices: IntRange,
	) : Entity
	data class MentionEntity(
		val id: String,
		val username: String,
		override val indices: IntRange,
	) : Entity
}

/**
 * A full modeling of the Twitter archive's JSON with as few defaults as possible
 * and deserialized without ignoring keys to ensure nothing is missed.
 */
@Serializable
private data class ArchiveTweetEntry(
	val tweet: Tweet,
) {
	@Serializable
	data class Tweet(
		val edit_info: JsonElement,
		/** Always false, even if true. */
		val retweeted: Boolean,
		val source: String,
		val entities: Entities,
		val extended_entities: Entities? = null,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val display_text_range: IntRange,
		val favorite_count: UInt,
		val id_str: String,
		val truncated: Boolean,
		val retweet_count: UInt,
		val id: String,
		val possibly_sensitive: Boolean = false,
		@Serializable(TwitterTimestampSerializer::class)
		val created_at: Instant,
		val favorited: Boolean,
		val full_text: String,
		val lang: String,
		val in_reply_to_status_id: String? = null,
		val in_reply_to_status_id_str: String? = null,
		val in_reply_to_user_id: String? = null,
		val in_reply_to_user_id_str: String? = null,
		val in_reply_to_screen_name: String? = null,
		val coordinates: Coordinates? = null,
		val geo: Coordinates? = null,
	)

	@Serializable
	data class Coordinates(
		val type: CoordinateType,
		val coordinates: List<String>, // TODO parse
	)

	@Serializable
	enum class CoordinateType {
		Point,
	}

	@Serializable
	data class Entities(
		val hashtags: List<HashtagEntity> = emptyList(),
		val media: List<MediaEntity> = emptyList(),
		val symbols: List<SymbolEntity> = emptyList(),
		val user_mentions: List<UserMentionEntity> = emptyList(),
		val urls: List<UrlEntity> = emptyList(),
	)

	@Serializable
	data class SymbolEntity(
		val text: String,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val indices: IntRange,
	)

	@Serializable
	data class HashtagEntity(
		val text: String,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val indices: IntRange,
	)

	@Serializable
	data class MediaEntity(
		val expanded_url: String,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val indices: IntRange,
		val url: String,
		val media_url: String,
		val id_str: String,
		val id: String,
		val media_url_https: String,
		val sizes: MediaSizes,
		val type: MediaType,
		val display_url: String,
		val video_info: MediaVideoInfo? = null,
		val source_status_id: String? = null,
		val source_status_id_str: String? = null,
		val source_user_id: String? = null,
		val source_user_id_str: String? = null,
		val additional_media_info: AdditionalMediaInfo? = null,
	)

	@Serializable
	data class AdditionalMediaInfo(
		val monetizable: Boolean,
		val title: String? = null,
		val description: String? = null,
		val embeddable: Boolean = false, // TODO correct default?
	)

	@Serializable
	data class MediaVideoInfo(
		val aspect_ratio: List<String>, // TODO Pair<String, String>?
		val variants: List<MediaVideoVariant>,
		val duration_millis: UInt? = null,
	)

	@Serializable
	data class MediaVideoVariant(
		val bitrate: UInt? = null,
		val content_type: String,
		val url: String,
	)

	@Serializable
	data class MediaSizes(
		val small: MediaSize,
		val medium: MediaSize,
		val large: MediaSize,
		val thumb: MediaSize,
	)

	@Serializable
	data class MediaSize(
		val w: UInt,
		val h: UInt,
		val resize: MediaResize,
	)

	@Serializable
	enum class MediaResize {
		@SerialName("fit") Fit,
		@SerialName("crop") Crop,
	}

	@Serializable
	enum class MediaType {
		@SerialName("photo") Photo,
		@SerialName("video") Video,
		@SerialName("animated_gif") AnimatedGif,
	}

	@Serializable
	data class UserMentionEntity(
		val name: String,
		val screen_name: String,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val indices: IntRange,
		val id_str: String,
		val id: String,
	)

	@Serializable
	data class UrlEntity(
		val url: String,
		val expanded_url: String,
		val display_url: String,
		@Serializable(TwoStringArrayIntRangeSerializer::class)
		val indices: IntRange,
	)
}

private object TwitterTimestampSerializer : KSerializer<Instant> {
	private val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss ZZ yyyy")
	override val descriptor = PrimitiveSerialDescriptor("twitter_timestamp", STRING)

	override fun deserialize(decoder: Decoder): Instant {
		val string = decoder.decodeString()
		return OffsetDateTime.parse(string, formatter).toInstant()
	}

	override fun serialize(encoder: Encoder, value: Instant) {
		throw AssertionError()
	}
}

private object TwoStringArrayIntRangeSerializer : KSerializer<IntRange> {
	private val delegate = ListSerializer(String.serializer())
	override val descriptor get() = delegate.descriptor

	override fun deserialize(decoder: Decoder): IntRange {
		val items = delegate.deserialize(decoder)
		check(items.size == 2) { items.toString() }
		return items[0].toInt()..items[1].toInt()
	}

	override fun serialize(encoder: Encoder, value: IntRange) {
		throw AssertionError()
	}
}
