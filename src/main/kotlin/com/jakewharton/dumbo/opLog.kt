package com.jakewharton.dumbo

import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

fun Path.containsId(id: String) = !exists() || readLines().any { it == id }

fun Path.appendId(id: String) {
	// TODO https://youtrack.jetbrains.com/issue/KT-55659
	if (exists()) {
		appendText("$id\n")
	} else {
		writeText("$id\n")
	}
}
