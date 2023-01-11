package com.jakewharton.dumbo

class InMemoryDumboDb(vararg pairs: Pair<String, String?>) : DumboDb {
	private val map = mutableMapOf(*pairs)

	override fun contains(tweetId: String) = tweetId in map

	override fun get(tweetId: String): String? {
		return map[tweetId]
	}

	override fun minusAssign(tweetId: String) {
		map -= tweetId
	}

	override fun set(tweetId: String, statusId: String?) {
		map[tweetId] = statusId
	}
}
