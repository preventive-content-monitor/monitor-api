package br.com.guardian.backend.adaptadores.saida.json

import br.com.guardian.backend.dominio.porta.SerializadorJson
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class JacksonSerializadorJson(
    private val objectMapper: ObjectMapper
) : SerializadorJson {

    override fun <T> serializar(obj: T): String {
        return objectMapper.writeValueAsString(obj)
    }

    override fun <T> desserializar(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    override fun <T> desserializarLista(json: String, elementType: Class<T>): List<T> {
        val type = objectMapper.typeFactory.constructCollectionType(List::class.java, elementType)
        return objectMapper.readValue(json, type)
    }
}
