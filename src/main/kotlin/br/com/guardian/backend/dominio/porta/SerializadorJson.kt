package br.com.guardian.backend.dominio.porta

interface SerializadorJson {
    fun <T> serializar(obj: T): String
    fun <T> desserializar(json: String, clazz: Class<T>): T
    fun <T> desserializarLista(json: String, elementType: Class<T>): List<T>
}
