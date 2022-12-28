package com.jakewharton.dumbo

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager

data class PostgresConnection(
	val host: String,
	val port: Int,
	val database: String,
	val user: String,
	val password: String,
) {
	companion object {
		const val DefaultHost = "localhost"
		const val DefaultPort = 5432
	}
}

fun withDatabase(db: PostgresConnection, block: (db: MastodonDb) -> Unit) {
	val connection = DriverManager.getConnection(
		"jdbc:postgresql://${db.host}:${db.port}/${db.database}",
		db.user,
		db.password
	)
	val driver = object : JdbcDriver() {
		override fun getConnection() = connection
		override fun closeConnection(connection: Connection) = Unit
		override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
		override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
		override fun notifyListeners(queryKeys: Array<String>) = Unit
	}

	connection.use {
		block(MastodonDb(driver))
	}
}
