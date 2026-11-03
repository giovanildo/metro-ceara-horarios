package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectionViaTest {
    private val sul = listOf(
        "Chico da Silva", "José de Alencar", "São Benedito", "Benfica", "Padre Cícero", "Porangabussu",
        "Couto Fernandes", "Juscelino Kubitscheck", "Parangaba", "Vila Pery", "Manoel Sátiro", "Mondubim",
        "Conjunto Esperança", "Aracapé", "Alto Alegre", "Raquel de Queiroz", "Virgílio Távora", "Maracanaú",
        "Jereissati", "Carlito Benevides",
    ).mapIndexed { i, name -> Stop("s$i", name, i) }

    @Test
    fun `de Benfica para Carlito mostra quatro e corta`() {
        val via = directionVia(sul, "s3")!!
        assertEquals(listOf("Padre Cícero", "Porangabussu", "Couto Fernandes", "Juscelino Kubitscheck"), via.shown)
        assertEquals(true, via.more)
        assertEquals("Carlito Benevides", via.end)
        assertEquals(16, via.count)
    }

    @Test
    fun `de Benfica para Chico mostra tudo`() {
        val via = directionVia(sul.reversed(), "s3")!!
        assertEquals(listOf("São Benedito", "José de Alencar"), via.shown)
        assertEquals(false, via.more)
        assertEquals(3, via.count)
    }

    @Test
    fun `penúltima estação só tem a última`() {
        val via = directionVia(sul, "s18")!!
        assertEquals(emptyList<String>(), via.shown)
        assertEquals("Carlito Benevides", via.end)
    }

    @Test
    fun `última estação ou fora do sentido não tem caminho`() {
        assertNull(directionVia(sul, "s19"))
        assertNull(directionVia(sul, "xx"))
    }
}
