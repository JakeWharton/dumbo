package com.jakewharton.dumbo

import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml

interface IdentityMapping {
	fun map(userId: String, userHandle: String): String

	companion object {
		val Empty: IdentityMapping = TomlIdentityMapping()

		fun of(
			byId: Map<String, String> = emptyMap(),
			byName: Map<String, String> = emptyMap(),
		): IdentityMapping {
			return TomlIdentityMapping(byId, byName)
		}

		fun loadToml(toml: Path): IdentityMapping {
			val parsed = Toml.decodeFromString(TomlIdentityMapping.serializer(), toml.readText())
			return TomlIdentityMapping(
				byId = parsed.byId,
				byName = parsed.byName,
			)
		}
	}
}

@Serializable
private class TomlIdentityMapping(
	@SerialName("by-id")
	val byId: Map<String, String> = emptyMap(),
	@SerialName("by-name")
	val byName: Map<String, String> = emptyMap(),
): IdentityMapping {

	override fun map(userId: String, userHandle: String): String {
		return byId[userId] ?: byName[userHandle] ?: "@${userHandle}@twitter.com"
	}
}
