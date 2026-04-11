package br.com.guardian.backend.domain.policy

interface JsonSerializer {
    fun <T> serialize(obj: T): String
    fun <T> deserialize(json: String, clazz: Class<T>): T
    fun <T> deserializeList(json: String, elementType: Class<T>): List<T>
}
