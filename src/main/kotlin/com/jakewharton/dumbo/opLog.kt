package com.jakewharton.dumbo

import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

fun Path.toOpMap(): Map<String, Statuses.Id?> {
	if (!exists()) return emptyMap()
	return readLines().associate { line ->
		val split = line.split(" ", limit = 2)
		split[0] to split.getOrNull(1)?.let { Statuses.Id(it.toLong()) }
	}
}

fun Path.appendId(tweetId: String, tootId: Statuses.Id?) {
	val output = buildString {
		append(tweetId)
		if (tootId != null) {
			append(' ')
			append(tootId.id)
		}
		append('\n')
	}
	// TODO https://youtrack.jetbrains.com/issue/KT-55659
	if (exists()) {
		appendText(output)
	} else {
		writeText(output)
	}
}
