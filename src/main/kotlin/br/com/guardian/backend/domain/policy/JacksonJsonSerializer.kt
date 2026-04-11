package br.com.guardian.backend.domain.policy

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class JacksonJsonSerializer(
    private val objectMapper: ObjectMapper
) : JsonSerializer {

    override fun <T> serialize(obj: T): String {
        return objectMapper.writeValueAsString(obj)
    }

    override fun <T> deserialize(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    override fun <T> deserializeList(json: String, elementType: Class<T>): List<T> {
        val type = objectMapper.typeFactory.constructCollectionType(List::class.java, elementType)
        return objectMapper.readValue(json, type)
    }
}
