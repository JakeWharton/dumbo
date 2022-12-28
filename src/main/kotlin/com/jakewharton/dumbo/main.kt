@file:JvmName("Main")

package com.jakewharton.dumbo

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.readText

fun main(vararg args: String) {
	DumboCommand().main(args)
}

private class DumboCommand : CliktCommand(name = "dumbo") {
	private val config by option("-c", "--config", metavar = "")
		.help("TOML file with configuration options")
		.path(mustExist = true, canBeDir = false)
		.convert { DumboConfig.parseFromToml(it.readText()) }
		.required()
	private val accountId by option("--account", metavar = "ID")
		.help("Target Mastodon account ID")
		.long()
		.required()
	private val applicationId by option("--application", metavar = "ID")
		.help("Application ID to use")
		.long()
	private val archiveDir by argument(name = "ARCHIVE")
		.help("Directory of extracted Twitter archive")
		.path(mustExist = true, canBeFile = false)

	override fun run() {
		DumboApp(config, accountId, applicationId).run(archiveDir)
	}
}
