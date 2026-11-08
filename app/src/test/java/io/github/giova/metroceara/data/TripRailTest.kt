package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRailTest {

    private fun stop(name: String, t: Int) = TripStop(name, name, 0.0, 0.0, t)
    private fun progress(index: Int, delay: Double = 0.0) = TripProgress(index, TripFix.GPS, delay, 0.0)

    private val sul = listOf(stop("A", 600), stop("B", 602), stop("C", 604), stop("D", 606), stop("E", 608))

    @Test
    fun `viagem simples vai da origem ao destino e aplica o atraso`() {
        val rail = TripRail.build(TripPlan("1", "Linha Sul", "X", sul), progress(1, delay = 2.0), "ce0f2e")
        assertEquals(listOf("A", "B", "C", "D", "E"), rail.rows.map { it.name })
        assertEquals(1, rail.position)
        assertEquals(3, rail.stationsLeft)
        assertEquals(606, rail.rows[2].minutes)
        assertTrue(rail.rows.last().isDestination)
    }

    @Test
    fun `antes de embarcar fica na origem`() {
        val rail = TripRail.build(TripPlan("1", "Linha Sul", "X", sul), progress(-1), "ce0f2e")
        assertEquals(0, rail.position)
        assertEquals(4, rail.stationsLeft)
    }

    @Test
    fun `sino na estacao do aviso, nunca na origem nem no destino`() {
        val normal = TripRail.build(TripPlan("1", "Linha Sul", "X", sul), progress(0), "c")
        assertEquals(listOf("C"), normal.rows.filter { it.isAlert }.map { it.name })
        val curta = TripRail.build(TripPlan("1", "Linha Sul", "X", sul.take(2)), progress(0), "c")
        assertTrue(curta.rows.none { it.isAlert })
    }

    @Test
    fun `sentado pelo terminal nao conta o terminal duas vezes`() {
        val stops = listOf(stop("B", 600), stop("T", 602), stop("T", 610), stop("B", 612), stop("C", 614))
        val rail = TripRail.build(TripPlan("1", "Linha Sul", "X", stops, turnaroundIndex = 1), progress(0), "c")
        assertTrue(rail.rows[1].isTurnaround)
        assertTrue(rail.rows[2].isRepeat)
        assertEquals(3, rail.stationsLeft)
    }

    private val legs = listOf(
        JourneyLeg("Linha Sul", "red", "Chico", "A", "P", 600, 606,
            listOf(LegStation("A", 600), LegStation("B", 603), LegStation("P", 606))),
        JourneyLeg("Linha Nordeste", "blue", "Iate", "P", "Z", 620, 626,
            listOf(LegStation("P", 620), LegStation("M", 623), LegStation("Z", 626))),
    )

    private fun journey(current: Int) = TripJourney("3", "z", "Z", legs, current)

    @Test
    fun `integracao mostra o proximo trecho, a caminhada e nao repete a plataforma`() {
        val stops = listOf(stop("A", 600), stop("B", 603), stop("P", 606))
        val plan = TripPlan("1", "Linha Sul", "Chico", stops, alertStationsBefore = 1,
            transfer = TripTransfer("Linha Nordeste", "Iate", 620, 2, "Z", free = true), journey = journey(0))
        val rail = TripRail.build(plan, progress(1, delay = 1.0), "ignorada")

        assertEquals(listOf("A", "B", "P", "P", "P", "M", "Z"), rail.rows.map { it.name })
        assertEquals(1, rail.position)
        assertEquals(listOf("red", "red", "red", "blue", "blue", "blue", "blue"), rail.rows.map { it.colorHex })
        val walk = rail.rows[3]
        assertTrue(walk.isWalk)
        assertEquals(2, walk.walkMinutes)
        assertEquals(true, walk.walkFree)
        assertTrue(rail.rows[2].isTransfer)
        assertFalse(rail.rows[2].isDestination)
        assertEquals("Linha Nordeste", rail.rows[4].boardsLine)
        assertEquals(620, rail.rows[4].boardsAt)
        assertEquals(623, rail.rows[5].minutes)
        // P, M e Z: a outra plataforma de P não conta.
        assertEquals(3, rail.stationsLeft)
        assertEquals(4, rail.stationsRidden)
    }

    @Test
    fun `no segundo trecho a posicao continua depois dos trechos feitos`() {
        val stops = listOf(stop("P", 620), stop("M", 623), stop("Z", 626))
        val plan = TripPlan("3", "Linha Nordeste", "Iate", stops, journey = journey(1))
        val rail = TripRail.build(plan, progress(1), "ignorada")
        assertEquals(5, rail.position)
        assertEquals("M", rail.rows[rail.position].name)
    }

    @Test
    fun `viagem salva sem as estacoes do trecho ainda monta o trilho`() {
        val velhos = legs.map { it.copy(stations = emptyList()) }
        val stops = listOf(stop("A", 600), stop("B", 603), stop("P", 606))
        val plan = TripPlan("1", "Linha Sul", "Chico", stops, journey = TripJourney("3", "z", "Z", velhos, 0))
        val rail = TripRail.build(plan, progress(0), "x")
        assertEquals(listOf("A", "B", "P", "P", "P", "Z"), rail.rows.map { it.name })
    }
}
