package io.github.giova.metroceara.data

/**
 * Estações que mudaram de nome por lei depois do GTFS do Metrofor, que ainda usa
 * o nome antigo. O app mostra o nome oficial com o antigo entre parênteses, como
 * as pessoas ainda conhecem a estação; a busca acha pelos dois. A regras.json continua com o nome do GTFS (as
 * versões antigas do app conferem o id por ele); a comparação passa pelos dois.
 *
 * Jereissati → Francisco Alves Marinho: Lei estadual 19.272, de 05/06/2025
 * (agente da estação ferroviária de Maracanaú, morto em 2010).
 */
object StationNames {
    private val renamed = mapOf("Jereissati" to "Francisco Alves Marinho (Jereissati)")
    private val former = renamed.entries.associate { (old, new) -> new to old }

    /** O nome que a placa da estação deve ter. */
    fun official(name: String): String = renamed[name.trim()] ?: name

    /** O nome antigo de uma estação renomeada, ou `null`. */
    fun formerName(name: String): String? = former[JourneyPlanner.plainName(name).trim()]

    /** Nome e nome antigo, para a busca achar pelos dois. */
    fun searchText(name: String): String = formerName(name)?.takeUnless { name.contains(it) }?.let { "$name $it" } ?: name
}
