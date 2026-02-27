package com.github.mihanizzm.ultistats.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mihanizzm.ultistats.service.MatchRepository
import com.github.mihanizzm.ultistats.service.PlayerRepository
import com.github.mihanizzm.ultistats.service.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File

@Service
class JsonPersistenceService(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val playerRepository: PlayerRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${ultistats.persistence.path:./data/ultistats.json}")
    private val persistencePath: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${ultistats.persistence.interval:30000}")
    fun scheduledSave() {
        try {
            save()
        } catch (e: Exception) {
            logger.error("Failed to save data", e)
        }
    }

    fun save() {
        val data = PersistenceData(
            matches = matchRepository.getAll(),
            teams = teamRepository.getAll(),
            players = playerRepository.getAll(),
        )
        val file = File(persistencePath)
        file.parentFile?.mkdirs()

        val tempFile = File("${persistencePath}.tmp")
        objectMapper.writeValue(tempFile, data)
        if (!tempFile.renameTo(file)) {
            file.delete()
            tempFile.renameTo(file)
        }

        logger.debug("Data saved to {}", persistencePath)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        val file = File(persistencePath)
        if (!file.exists()) {
            logger.info("No persistence file found at {}, starting with empty data", persistencePath)
            return
        }

        try {
            val data = objectMapper.readValue(file, PersistenceData::class.java)
            data.players.forEach { playerRepository.save(it) }
            data.teams.forEach { teamRepository.save(it) }
            data.matches.forEach { matchRepository.save(it) }
            logger.info(
                "Loaded {} matches, {} teams, {} players from {}",
                data.matches.size,
                data.teams.size,
                data.players.size,
                persistencePath
            )
        } catch (e: Exception) {
            logger.error("Failed to load data from {}", persistencePath, e)
        }
    }
}
