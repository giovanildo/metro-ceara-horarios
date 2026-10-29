package io.github.giova.metroceara.trip

import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.TripProgress
import io.github.giova.metroceara.data.TripStop
import org.junit.Assert.assertEquals
import org.junit.Test

/** Quando o app convida a mandar o registro: só nas viagens com problema. */
class TripReviewTest {

    private val plan = TripPlan(
        "1", "Linha Sul", "Carlito Benevides",
        listOf(
            TripStop("2", "Benfica", -3.7391, -38.5396, 1250),
            TripStop("14", "Padre Cícero", -3.7457, -38.5459, 1252),
            TripStop("3", "Porangabussu", -3.7515, -38.5517, 1254),
            TripStop("4", "Couto Fernandes", -3.7573, -38.5575, 1256),
        ),
    )

    private fun at(index: Int, eta: Double = 1256.0, silent: Double? = null, alerted: Boolean = false) =
        ActiveTrip(plan, TripProgress(index, TripFix.GPS, 0.0, eta, silent), alerted = alerted, arrived = false)

    @Test
    fun `viagem normal nao convida`() {
        val review = TripReview()
        review.onUpdate(at(0))
        review.onUpdate(at(1, alerted = true))
        review.onArrived(1257.0) // 1 min depois do previsto no aviso
        review.onUpdate(at(3, alerted = true))
        assertEquals(emptyList<String>(), review.problems("arrived"))
    }

    @Test
    fun `chegada muito diferente da prevista no aviso`() {
        val review = TripReview()
        review.onUpdate(at(1, eta = 1256.0, alerted = true))
        review.onArrived(1262.0)
        assertEquals(listOf("arrival_off"), review.problems("arrived"))
        assertEquals(6.0, review.arrivalErrorMinutes!!, 0.01)
    }

    @Test
    fun `muito tempo sem GPS`() {
        val review = TripReview()
        review.onUpdate(at(1, silent = 1.5))
        review.onUpdate(at(2, silent = 4.0))
        review.onUpdate(at(3))
        assertEquals(listOf("gps_lost"), review.problems("arrived"))
    }

    @Test
    fun `encerrada no meio, depois de embarcar`() {
        val review = TripReview()
        review.onUpdate(at(1))
        assertEquals(listOf("ended_early"), review.problems("user"))
    }

    @Test
    fun `desistiu antes de embarcar nao conta`() {
        val review = TripReview()
        review.onUpdate(at(-1))
        assertEquals(emptyList<String>(), review.problems("user"))
    }
}
