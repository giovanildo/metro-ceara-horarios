package io.github.giova.metroceara.data

import io.github.giova.metroceara.data.ReviewInvite.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewInviteTest {
    private val day = 24L * 60 * 60 * 1000

    /** Chegadas reais, um dia depois da outra; devolve se cada uma mostrou o convite. */
    private fun trips(start: State, count: Int, from: Long = day): Pair<State, List<Boolean>> {
        var s = start
        val shown = (1..count).map { i ->
            val (next, show) = s.onArrival(from + i * day, simulated = false)
            s = next
            show
        }
        return s to shown
    }

    @Test
    fun `aparece na terceira viagem`() {
        val (s, shown) = trips(State(), 3)
        assertEquals(listOf(false, false, true), shown)
        assertEquals(1, s.shown)
    }

    @Test
    fun `viagem simulada não conta`() {
        val (s, show) = State(trips = 2).onArrival(day, simulated = true)
        assertFalse(show)
        assertEquals(2, s.trips)
    }

    @Test
    fun `a mesma chegada não conta duas vezes`() {
        val (s, _) = State(trips = 1).onArrival(day, simulated = false)
        val (again, show) = s.onArrival(day, simulated = false)
        assertFalse(show)
        assertEquals(2, again.trips)
    }

    @Test
    fun `agora não volta depois de 21 dias, e só uma vez`() {
        val (first, _) = trips(State(), 3)
        // Viagens nos 20 dias seguintes: nada.
        val (mid, quiet) = trips(first, 20, from = first.lastShown)
        assertTrue(quiet.none { it })
        // No 21º dia depois do primeiro convite: o segundo.
        val (second, show) = mid.onArrival(first.lastShown + ReviewInvite.AGAIN_AFTER_MILLIS, simulated = false)
        assertTrue(show)
        // Depois do segundo, nunca mais.
        val (_, later) = trips(second, 60, from = 100 * day)
        assertTrue(later.none { it })
    }

    @Test
    fun `avaliou, nunca mais`() {
        val (first, _) = trips(State(), 3)
        val (_, later) = trips(first.onRate(), 60, from = 10 * day)
        assertTrue(later.none { it })
    }
}
