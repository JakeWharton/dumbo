package com.jakewharton.dumbo

import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

fun Path.toOpMap(): Map<String, String?> {
	if (!exists()) return emptyMap()
	return readLines().associate { line ->
		val split = line.split(" ", limit = 2)
		split[0] to split.getOrNull(1)
	}
}

fun Path.appendId(tweetId: String, statusId: String?) {
	val output = buildString {
		append(tweetId)
		if (statusId != null) {
			append(' ')
			append(statusId)
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
