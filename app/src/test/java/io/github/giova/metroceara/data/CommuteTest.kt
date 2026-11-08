package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommuteTest {
    private fun min(h: Int, m: Int = 0) = h * 60 + m

    private fun trip(dep: Int, arr: Int) = Itinerary(
        legs = listOf(ItineraryLeg("sul", 0, "t$dep", "A", "B", dep, arr)),
        transfers = emptyList(),
    )

    // Conjunto Esperança → José de Alencar: 21 min, de 18 em 18.
    private val ida = (0..5).map { trip(min(7, 16) + it * 18, min(7, 37) + it * 18) }

    @Test
    fun `dia comum, ida de manhã e volta depois da hora de chegar`() {
        val a = min(8)
        val l = min(17, 30)
        assertEquals(Place.WORK, commuteDestinationByClock(min(7, 5), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(12), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(17, 32), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(23), a, l))
        assertEquals(Place.WORK, commuteDestinationByClock(min(3), a, l))
    }

    @Test
    fun `faculdade à noite, à tarde ainda é a ida`() {
        val a = min(19)
        val l = min(22)
        assertEquals(Place.WORK, commuteDestinationByClock(min(17), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(21, 40), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(23, 30), a, l))
    }

    @Test
    fun `quem vira a noite no trabalho`() {
        val a = min(22)
        val l = min(6)
        assertEquals(Place.HOME, commuteDestinationByClock(min(6, 30), a, l))
        assertEquals(Place.WORK, commuteDestinationByClock(min(20), a, l))
        assertEquals(Place.HOME, commuteDestinationByClock(min(2), a, l))
    }

    @Test
    fun `perto de uma estação, a localização manda mais que o relógio`() {
        assertEquals(Place.WORK, commuteDestination(min(20), min(8), min(17, 30), 100.0, 9000.0, 1500.0))
        assertEquals(Place.HOME, commuteDestination(min(7), min(8), min(17, 30), 9000.0, 100.0, 1500.0))
    }

    @Test
    fun `longe das duas ou sem localização, vale o relógio`() {
        assertEquals(Place.WORK, commuteDestination(min(7), min(8), min(17, 30), 9000.0, 8000.0, 1500.0))
        assertEquals(Place.HOME, commuteDestination(min(18), min(8), min(17, 30), null, null, 1500.0))
    }

    @Test
    fun `ida, o último que chega a tempo, com o anterior e o que atrasa`() {
        val p = pickCommute(ida, toWork = true, target = min(8), now = min(7, 5))!!
        assertEquals(CommuteReason.ARRIVE_BY, p.reason)
        assertEquals(min(7, 34), p.main.departure)
        assertEquals(listOf(min(7, 16), min(7, 52)), p.others.map { it.departure })
    }

    @Test
    fun `ida quando nenhum chega mais a tempo`() {
        val p = pickCommute(ida.drop(3), toWork = true, target = min(8), now = min(7, 55))!!
        assertEquals(CommuteReason.TOO_LATE, p.reason)
        assertEquals(ida[3].departure, p.main.departure)
    }

    @Test
    fun `volta antes da hora de sair, o primeiro depois dela`() {
        val p = pickCommute(ida, toWork = false, target = min(7, 40), now = min(7, 5))!!
        assertEquals(CommuteReason.LEAVE_AT, p.reason)
        assertEquals(min(7, 52), p.main.departure)
    }

    @Test
    fun `volta depois da hora de sair, o próximo`() {
        val p = pickCommute(ida, toWork = false, target = min(7), now = min(7, 5))!!
        assertEquals(CommuteReason.NEXT, p.reason)
        assertEquals(ida[0].departure, p.main.departure)
    }

    @Test
    fun `operação especial, o dia acaba antes da hora de sair`() {
        val p = pickCommute(ida, toWork = false, target = min(17, 30), now = min(7, 5))!!
        assertEquals(CommuteReason.EARLY, p.reason)
        assertEquals(ida.last().departure, p.main.departure)
        assertEquals(ida.dropLast(1).takeLast(2).map { it.departure }, p.others.map { it.departure })
    }

    @Test
    fun `sem hora marcada, o próximo`() {
        assertEquals(CommuteReason.NEXT, pickCommute(ida, toWork = true, target = null, now = min(7, 5))!!.reason)
    }

    @Test
    fun `o último só aparece perto do fim do dia`() {
        val muitos = (0..20).map { trip(min(7) + it * 18, min(7, 21) + it * 18) }
        assertNull(pickCommute(muitos, toWork = false, target = null, now = min(7))!!.lastDeparture)
        assertEquals(muitos.last().departure, pickCommute(muitos.takeLast(4), toWork = false, target = null, now = min(7))!!.lastDeparture)
    }

    @Test
    fun `sem metrô, nada`() {
        assertNull(pickCommute(emptyList(), toWork = true, target = min(8), now = min(7)))
    }
}
