package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NearestTwoTest {

    private fun st(id: String, route: String, lat: Double, lon: Double) =
        Station(id, id, lat, lon, route, "Linha $route", "000000")

    // Perto de São Benedito (coordenadas do GTFS).
    private val stations = listOf(
        st("SBE", "Sul", -3.7317481, -38.5348996),
        st("JAL", "Sul", -3.7260303, -38.5324792),
        st("MBR", "Oeste", -3.7192908, -38.5369844),
        st("MON", "Sul", -3.8074449, -38.5777348),
        st("SAT", "Sul", -3.7985321, -38.5749936),
    )

    @Test
    fun `a segunda é de outra linha quando há uma por perto`() {
        val two = stations.nearestTwo(-3.7330, -38.5352, 3_000.0)
        assertEquals(listOf("SBE", "MBR"), two.map { it.first.stopId })
    }

    @Test
    fun `sem outra linha por perto, a segunda é da mesma linha`() {
        val two = stations.nearestTwo(-3.8080, -38.5780, 3_000.0)
        assertEquals(listOf("MON", "SAT"), two.map { it.first.stopId })
    }

    @Test
    fun `a segunda fica de fora acima da distância`() {
        val two = stations.nearestTwo(-3.8080, -38.5780, 500.0)
        assertEquals(listOf("MON"), two.map { it.first.stopId })
    }
}
