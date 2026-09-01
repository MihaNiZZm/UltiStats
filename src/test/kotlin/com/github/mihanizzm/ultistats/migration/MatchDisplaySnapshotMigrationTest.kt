package com.github.mihanizzm.ultistats.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.sql.DriverManager

class MatchDisplaySnapshotMigrationTest {
    @Test
    fun `V3 backfills display snapshots and keeps unknown names empty`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:match_snapshot_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        ).use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("CREATE TABLE teams (id UUID PRIMARY KEY, name VARCHAR(255) NOT NULL)")
                sql.execute("CREATE TABLE players (id UUID PRIMARY KEY, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL)")
                sql.execute("CREATE TABLE matches (id UUID PRIMARY KEY)")
                sql.execute("CREATE TABLE match_teams (match_id UUID NOT NULL, team_id UUID NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (match_id, team_id))")
                sql.execute("CREATE TABLE match_participants (match_id UUID NOT NULL, participant_id UUID NOT NULL, team_id UUID NOT NULL, kind VARCHAR(16) NOT NULL, unknown_slot INTEGER, number INTEGER, PRIMARY KEY (match_id, participant_id))")
                sql.execute("INSERT INTO teams (id,name) VALUES ('00000000-0000-0000-0000-000000000001','Original team')")
                sql.execute("INSERT INTO players (id,first_name,last_name) VALUES ('00000000-0000-0000-0000-000000000010','Ivan','Ivanov')")
                sql.execute("INSERT INTO matches (id) VALUES ('00000000-0000-0000-0000-000000000100')")
                sql.execute("INSERT INTO match_teams (match_id,team_id,position) VALUES ('00000000-0000-0000-0000-000000000100','00000000-0000-0000-0000-000000000001',1)")
                sql.execute("INSERT INTO match_participants (match_id,participant_id,team_id,kind,number) VALUES ('00000000-0000-0000-0000-000000000100','00000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000001','PLAYER',7)")
                sql.execute("INSERT INTO match_participants (match_id,participant_id,team_id,kind,unknown_slot) VALUES ('00000000-0000-0000-0000-000000000100','00000000-0000-0000-0000-000000000011','00000000-0000-0000-0000-000000000001','UNKNOWN',1)")
            }
            ScriptUtils.executeSqlScript(
                connection,
                ClassPathResource("db/migration/V3__add_match_display_snapshots.sql"),
            )
            connection.createStatement().use { sql ->
                sql.executeQuery("SELECT team_name FROM match_teams").use { row ->
                    assertThat(row.next()).isTrue()
                    assertThat(row.getString("team_name")).isEqualTo("Original team")
                }
                sql.executeQuery(
                    "SELECT kind,first_name,last_name FROM match_participants ORDER BY kind",
                ).use { rows ->
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getString("first_name")).isEqualTo("Ivan")
                    assertThat(rows.getString("last_name")).isEqualTo("Ivanov")
                    assertThat(rows.next()).isTrue()
                    assertThat(rows.getString("kind")).isEqualTo("UNKNOWN")
                    assertThat(rows.getString("first_name")).isNull()
                    assertThat(rows.getString("last_name")).isNull()
                }
            }
        }
    }
}
