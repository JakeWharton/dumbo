package com.jakewharton.dumbo

import org.tomlj.Toml
import org.tomlj.TomlTable

data class DumboConfig(
	val database: DatabaseConfig,
	val tweets: TweetsConfig,
) {
	companion object {
		fun parseFromToml(toml: String): DumboConfig {
			val parseResult = Toml.parse(toml)
			require(!parseResult.hasErrors()) {
				"Unable to parse TOML config:\n\n * " + parseResult.errors().joinToString("\n *")
			}

			var database: DatabaseConfig? = null
			var tweets: TweetsConfig? = null
			for (key in parseResult.keySet()) {
				when (key) {
					"database" -> database = parseResult.getTable(key)!!.parseDatabase(key)
					"tweets" -> tweets = parseResult.getTable(key)!!.parseTweets(key)
					else -> error("Config contains unknown '$key' key")
				}
			}

			return DumboConfig(
				database = checkNotNull(database) { "Config missing required 'database' key" },
				tweets = checkNotNull(tweets) { "Config missing required 'tweets' key" },
			)
		}

		private fun TomlTable.parseDatabase(self: String): DatabaseConfig {
			var host = PostgresConnection.DefaultHost
			var port = PostgresConnection.DefaultPort
			var name: String? = null
			var username: String? = null
			var password: String? = null
			for (key in keySet()) {
				when (key) {
					"host" -> host = getString(key)!!
					"port" -> port = getLong(key)!!.toInt()
					"name" -> name = getString(key)!!
					"username" -> username = getString(key)!!
					"password" -> password = getString(key)!!
					else -> error("'$self' contains unknown '$key' key")
				}
			}
			return DatabaseConfig(
				host = host,
				port = port,
				name = checkNotNull(name) { "'$self' missing required 'name' key" },
				username = checkNotNull(username) { "'$self' missing required 'username' key" },
				password = checkNotNull(password) { "'$self' missing required 'password' key" },
			)
		}

		private fun TomlTable.parseTweets(self: String): TweetsConfig {
			var ignoredIds = emptySet<String>()
			for (key in keySet()) {
				when (key) {
					"ignored_ids" -> ignoredIds = getStringSet(key)
					else -> error("'$self' contains unknown '$key' key")
				}
			}
			return TweetsConfig(
				ignoredIds = ignoredIds,
			)
		}

		private fun TomlTable.getStringSet(key: String): Set<String> {
			val array = getArray(key)!!
			return (0 until array.size())
				.mapTo(mutableSetOf(), array::getString)
		}
	}
}

data class DatabaseConfig(
	val host: String,
	val port: Int,
	val name: String,
	val username: String,
	val password: String,
)

data class TweetsConfig(
	val ignoredIds: Set<String>,
)
