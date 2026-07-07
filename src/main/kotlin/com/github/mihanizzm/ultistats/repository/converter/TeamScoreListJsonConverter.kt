package com.github.mihanizzm.ultistats.repository.converter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mihanizzm.ultistats.model.TeamScore
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class TeamScoreListJsonConverter : AttributeConverter<MutableList<TeamScore>, String> {
    private val objectMapper = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val teamScoreListType = objectMapper.typeFactory
        .constructCollectionType(MutableList::class.java, TeamScore::class.java)

    override fun convertToDatabaseColumn(attribute: MutableList<TeamScore>?): String =
        objectMapper.writerFor(teamScoreListType).writeValueAsString(attribute ?: mutableListOf<TeamScore>())

    override fun convertToEntityAttribute(dbData: String?): MutableList<TeamScore> {
        val json = dbData?.asJsonContent() ?: return mutableListOf()
        return objectMapper.readValue(json, teamScoreListType)
    }

    private fun String.asJsonContent(): String? {
        val trimmed = trim()
        if (trimmed.isBlank()) {
            return null
        }

        return if (trimmed.startsWith("\"")) {
            objectMapper.readValue(trimmed, String::class.java)
        } else {
            trimmed
        }
    }
}
