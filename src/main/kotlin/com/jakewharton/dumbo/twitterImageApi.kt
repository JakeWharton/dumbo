package com.jakewharton.dumbo

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface TwimgApi {
	@Streaming
	@GET("/media/{filename}:{quality}")
	suspend fun downloadImage(
		@Path("filename") filename: String,
		@Path("quality") quality: String,
	): ResponseBody
}
