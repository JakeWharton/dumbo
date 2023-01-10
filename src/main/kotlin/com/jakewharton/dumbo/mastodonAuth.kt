package com.jakewharton.dumbo

import kotlinx.serialization.Serializable

sealed interface MastodonAuth

@Serializable
data class MastodonAuthStage1(
	val client_id: String,
	val client_secret: String,
) : MastodonAuth

@Serializable
data class MastodonAuthStage2(
	val client_id: String,
	val client_secret: String,
	val access_token: String,
) : MastodonAuth
