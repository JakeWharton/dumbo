package com.jakewharton.dumbo

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun Any?.toQuickPrettyString(): String {
	fun Any?.recurse() = toQuickPrettyString().prependIndent("  ").substring(2)
	return when {
		this is Collection<*> -> joinToString(
			separator = ",\n  ",
			prefix = "[\n  ",
			postfix = "\n]",
			transform = Any?::recurse,
		)
		this == null || !this::class.isData -> toString()
		else -> this::class.declaredMemberProperties.joinToString(
			separator = ",\n",
			prefix = "${this::class.simpleName}(\n",
			postfix = "\n)",
			transform = {
				"  ${it.name} = ${(it as KProperty1<Any, Any?>).get(this).recurse()}"
			}
		)
	}
}
