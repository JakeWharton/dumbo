@file:JvmName("Main")

package com.jakewharton.dumbo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import retrofit2.Retrofit
import retrofit2.create

fun main(vararg args: String) {
	DumboCommand(FileSystems.getDefault()).main(args)
}

private class DumboCommand(
	fs: FileSystem,
) : CliktCommand(name = "dumbo") {
	private val debug by option(hidden = true).flag()
	private val host by option("--host", metavar = "URL")
		.help("Mastodon server host")
		.convert { it.toHttpUrl() }
		.required()
	private val edits by option()
		.help("Edit Mastodon posts if Tweet or mapping changed")
		.flag()
	private val identityMapping by option("--identities", metavar = "TOML")
		.help("""
			|A TOML file mapping Twitter IDs or usernames to Mastodon handles
			|
			|Format:
			|```
			|[by-id]
			|123="@foo@example.com"
			|
			|[by-name]
			|bar="@bar@example.com"
			|```
			|""".trimMargin())
		.path(fileSystem = fs, canBeDir = false)
	private val archiveDir by argument(name = "ARCHIVE")
		.help("Directory of extracted Twitter archive")
		.path(fileSystem = fs, mustExist = true, canBeFile = false)

	@OptIn(ExperimentalSerializationApi::class)
	override fun run() {
		val okhttp = OkHttpClient.Builder()
			.apply {
				if (debug) {
					addInterceptor(HttpLoggingInterceptor(::println).setLevel(BASIC))
				}
			}
			.build()
		val json = Json {
			ignoreUnknownKeys = true
		}
		val converterFactory = json.asConverterFactory("application/json".toMediaType())

		val retrofit = Retrofit.Builder()
			.client(okhttp)
			.baseUrl(host)
			.addConverterFactory(converterFactory)
			.build()
		val api = retrofit.create<MastodonApi>()

		// Parse outside of Clikt converter so exceptions propagate.
		val identityMapping = identityMapping?.let(IdentityMapping::loadToml) ?: IdentityMapping.Empty

		try {
			runBlocking {
				DumboApp(api).run(host, archiveDir, identityMapping, edits, debug)
			}
		} finally {
			okhttp.connectionPool.evictAll()
			okhttp.dispatcher.executorService.shutdown()
		}
	}
}
