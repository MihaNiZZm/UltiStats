package com.github.mihanizzm.ultistats.repository.converter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mihanizzm.ultistats.model.events.Event
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class EventListJsonConverter : AttributeConverter<MutableList<Event>, String> {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val eventListType = objectMapper.typeFactory
        .constructCollectionType(MutableList::class.java, Event::class.java)

    override fun convertToDatabaseColumn(attribute: MutableList<Event>?): String =
        objectMapper.writerFor(eventListType).writeValueAsString(attribute ?: mutableListOf<Event>())

    override fun convertToEntityAttribute(dbData: String?): MutableList<Event> {
        val json = dbData?.asJsonContent() ?: return mutableListOf()
        return objectMapper.readValue(json, eventListType)
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
