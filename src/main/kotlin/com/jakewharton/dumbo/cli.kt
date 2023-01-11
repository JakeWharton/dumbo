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
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
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
		.convert { HttpUrl.get(it) }
		.required()
	private val archiveDir by argument(name = "ARCHIVE")
		.help("Directory of extracted Twitter archive")
		.path(fileSystem = fs, mustExist = true, canBeFile = false)

	@OptIn(ExperimentalSerializationApi::class)
	override fun run() {
		val okhttp = OkHttpClient.Builder()
			.build()
		val json = Json {
			ignoreUnknownKeys = true
		}
		val converterFactory = json.asConverterFactory(MediaType.parse("application/json")!!)

		val retrofit = Retrofit.Builder()
			.client(okhttp)
			.baseUrl(host)
			.addConverterFactory(converterFactory)
			.build()
		val api = retrofit.create<MastodonApi>()

		try {
			runBlocking {
				DumboApp(api).run(host, archiveDir, debug = debug)
			}
		} finally {
			okhttp.connectionPool().evictAll()
			okhttp.dispatcher().executorService().shutdown()
		}
	}
}
