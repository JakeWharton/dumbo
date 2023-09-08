package com.jakewharton.dumbo

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.sink
import okio.source
import org.apache.tika.Tika
import retrofit2.HttpException

class MediaDb(
	directory: Path,
	private val mastodonApi: MastodonApi,
	private val authentication: String,
	private val twimgApi: TwimgApi,
) {
	private val tika = Tika()
	private val originalDir = directory / "dumbo-media"
	private val archiveDir = directory / "data/tweets_media"

	suspend fun uploadMedia(id: String, path: String): String {
		val filename = "$id-$path"
		val original = originalDir / filename
		val archived = archiveDir / filename

		if (original.notExists()) {
			try {
				twimgApi.downloadImage(path, "orig").source().use { source ->
					originalDir.createDirectories()
					original.sink().use(source::readAll)
				}
			} catch (e: HttpException) {
				if (e.code() != 404) {
					throw e
				}
			}
		}

		val upload = original.takeIf { it.exists() }
			?: archived.takeIf { it.exists() }
			?: throw IllegalStateException("No media available for $id $path")

		val contentType = tika.detect(upload).toMediaType()

		val uploadResponse = mastodonApi.uploadMedia(
			authorization = authentication,
			file = MultipartBody.Part.createFormData("file", id, PathRequestBody(upload, contentType)),
			description = "",
		)
		return when (uploadResponse.code()) {
			200 -> {
				// Media was small enough to be processed synchronously.
				uploadResponse.body()!!.id
			}
			202 -> {
				val attachmentId = uploadResponse.body()!!.id
				// Media was enqueued to be processed. Wait for it to be processed...
				while (true) {
					delay(10.seconds)
					val getResponse = mastodonApi.getMedia(authentication, attachmentId)
					when (getResponse.code()) {
						200 -> break
						206 -> continue
						else -> throw HttpException(getResponse)
					}
				}
				attachmentId
			}
			else -> throw HttpException(uploadResponse)
		}
	}
}

private class PathRequestBody(
	private val path: Path,
	private val contentType: MediaType,
) : RequestBody() {
	override fun contentType() = contentType
	override fun contentLength() = Files.size(path)

	override fun writeTo(sink: BufferedSink) {
		path.source().use(sink::writeAll)
	}
}
