package com.jakewharton.dumbo

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class TweetEntry(
	val tweet: Tweet,
) : Comparable<TweetEntry> {
	override fun compareTo(other: TweetEntry) = comparator.compare(tweet, other.tweet)

	companion object {
		private val comparator = compareBy(Tweet::created_at)
			.thenByDescending(Tweet::id)
	}
}

@Serializable
data class Tweet(
	val edit_info: JsonElement,
	/** Always false, even if true. */
	val retweeted: Boolean,
	val source: String,
	val entities: Entities,
	val extended_entities: Entities? = null,
	@Serializable(TwoStringArrayIntRangeSerializer::class) val display_text_range: IntRange,
	val favorite_count: UInt,
	val id_str: String,
	val truncated: Boolean,
	val retweet_count: UInt,
	val id: String,
	val possibly_sensitive: Boolean = false,
	@Serializable(TwitterTimestampSerializer::class) val created_at: Instant,
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
	@Serializable(TwoStringArrayIntRangeSerializer::class) val indices: IntRange,
)

@Serializable
data class HashtagEntity(
	val text: String,
	@Serializable(TwoStringArrayIntRangeSerializer::class) val indices: IntRange,
)

@Serializable
data class MediaEntity(
	val expanded_url: String,
	@Serializable(TwoStringArrayIntRangeSerializer::class) val indices: IntRange,
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
	@Serializable(TwoStringArrayIntRangeSerializer::class) val indices: IntRange,
	val id_str: String,
	val id: String,
)

@Serializable
data class UrlEntity(
	val url: String,
	val expanded_url: String,
	val display_url: String,
	@Serializable(TwoStringArrayIntRangeSerializer::class) val indices: IntRange,
)

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
