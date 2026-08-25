package com.github.mihanizzm.ultistats.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.sql.DriverManager

class EventTypeMigrationTest {
    @Test
    fun `legacy event types are renamed without changing other rows`() {
        DriverManager.getConnection("jdbc:h2:mem:event_type_migration;MODE=PostgreSQL").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE events (event_type VARCHAR(64) NOT NULL)")
                statement.execute("INSERT INTO events (event_type) VALUES ('DROP'), ('TURNOVER'), ('PASS')")
            }

            ScriptUtils.executeSqlScript(
                connection,
                ClassPathResource("db/migration/V2__rename_event_types.sql"),
            )

            val eventTypes = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT event_type FROM events ORDER BY event_type").use { rows ->
                    buildList {
                        while (rows.next()) add(rows.getString("event_type"))
                    }
                }
            }
            assertThat(eventTypes).containsExactly("INCOMPLETE_PASS", "PASS", "PICKUP")
        }
    }
}
