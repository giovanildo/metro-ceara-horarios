package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeStyleTest {
    @Test
    fun `sem escolha guardada, ou com chave desconhecida, vale o trilho`() {
        assertEquals(HomeStyle.RAIL, HomeStyle.of(null))
        assertEquals(HomeStyle.RAIL, HomeStyle.of("outra"))
        assertEquals(HomeStyle.PHRASES, HomeStyle.of("frases"))
    }

    @Test
    fun `o via é a próxima estação do sentido`() {
        assertEquals("Padre Cícero", viaStation(listOf("Padre Cícero", "Porangabussu", "Carlito Benevides")))
    }

    @Test
    fun `se a próxima é o terminal, sem via`() {
        assertNull(viaStation(listOf("Carlito Benevides")))
    }

    @Test
    fun `no terminal, sem via`() {
        assertNull(viaStation(emptyList()))
    }
}
