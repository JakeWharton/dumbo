package com.jakewharton.dumbo

import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

interface DumboDb {
	operator fun contains(tweetId: String): Boolean
	operator fun get(tweetId: String): String?
	operator fun minusAssign(tweetId: String)
	operator fun set(tweetId: String, statusId: String?)
}

class NioPathDumboDb(
	directory: Path,
) : DumboDb {
	private val opLogPath = directory.resolve("dumbo_log.txt")

	override operator fun contains(tweetId: String): Boolean {
		if (opLogPath.notExists()) return false
		val startString = "$tweetId "
		return opLogPath.readLines()
			.any { it == tweetId || it.startsWith(startString) }
	}

	override operator fun get(tweetId: String): String? {
		val startString = "$tweetId "
		val line = opLogPath.readLines()
			.first { it == tweetId || it.startsWith(startString) }
		return line.split(' ', limit = 2).getOrNull(1)
	}

	override operator fun minusAssign(tweetId: String) {
		opLogPath.writeLines(
			opLogPath.readLines()
				.filter { !it.startsWith("$tweetId ") },
		)
	}

	override operator fun set(tweetId: String, statusId: String?) {
		val output = buildString {
			append(tweetId)
			if (statusId != null) {
				append(' ')
				append(statusId)
			}
			append('\n')
		}
		// TODO https://youtrack.jetbrains.com/issue/KT-55659
		if (opLogPath.exists()) {
			opLogPath.appendText(output)
		} else {
			opLogPath.writeText(output)
		}
	}
}

fun Path.toOpMap(): Map<String, String?> {
	if (!exists()) return emptyMap()
	return readLines().associate { line ->
		val split = line.split(" ", limit = 2)
		split[0] to split.getOrNull(1)
	}
}
