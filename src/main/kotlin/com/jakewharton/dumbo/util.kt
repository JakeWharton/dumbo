package com.jakewharton.dumbo

import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

fun Any?.toQuickPrettyString(): String {
	fun Any?.recurse() = toQuickPrettyString().prependIndent("  ").substring(2)
	return when {
		this is Collection<*> -> joinToString(
			separator = ",",
			prefix = "[",
			postfix = "\n]",
			transform = { "\n  ${it.recurse()}"},
		)
		this is CharSequence -> "\"$this\""
		this == null || !this::class.isData -> toString()
		else -> this::class.declaredMemberProperties.joinToString(
			separator = ",",
			prefix = "${this::class.simpleName}(",
			postfix = "\n)",
			transform = {
				"\n  ${it.name} = ${(it as KProperty1<Any, Any?>).get(this).recurse()}"
			}
		)
	}
}
