package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O caso do log de 25/10: a pessoa começou o modo viagem em casa, a 1,4 km da
 * Carlito Benevides, com o GPS bom. A viagem não pode "andar" pela tabela.
 */
class TripEstimatorTest {

    private val stops = listOf(
        TripStop("11", "Carlito Benevides", -3.8889, -38.6188, 930), // 15:30
        TripStop("10", "Jereissati", -3.8820, -38.6203, 931),
        TripStop("19", "Maracanaú", -3.8756, -38.6250, 933),
    )
    private fun plan(simulated: Boolean = false) = TripPlan("1", "Linha Sul", "Chico da Silva", stops, simulated = simulated)

    // Em casa, longe da linha (posição do log, arredondada).
    private val homeLat = -3.8943
    private val homeLon = -38.6072

    @Test
    fun `com GPS bom e longe da linha, espera na partida`() {
        val estimator = TripEstimator(plan())
        var minute = 925.0
        while (minute <= 950.0) {
            estimator.onLocation(homeLat, homeLon, 11f, minute)
            val progress = estimator.progress(minute)
            assertEquals("andou em $minute", -1, progress.index)
            minute += 0.5
        }
    }

    @Test
    fun `sem GPS (tunel), a tabela anda depois de dado como perdido`() {
        val estimator = TripEstimator(plan())
        estimator.progress(925.0)
        assertEquals(stops.lastIndex, estimator.progress(940.0).index)
        assertEquals(TripFix.SCHEDULE, estimator.progress(940.0).fix)
    }

    @Test
    fun `GPS na estacao poe a viagem em andamento`() {
        val estimator = TripEstimator(plan())
        estimator.onLocation(stops[1].lat, stops[1].lon, 10f, 931.0)
        assertEquals(1, estimator.progress(931.0).index)
        assertEquals(TripFix.GPS, estimator.progress(931.0).fix)
    }

    @Test
    fun `ainda na plataforma 3 min depois da partida, perdeu o trem`() {
        val estimator = TripEstimator(plan())
        val origin = stops[0]
        estimator.onLocation(origin.lat, origin.lon, 10f, 929.0) // chegou à plataforma
        estimator.onLocation(origin.lat, origin.lon, 10f, 932.0)
        assertEquals(false, estimator.missedDeparture(932.0, 3.0)) // 15:32: ainda pode estar atrasado
        estimator.onLocation(origin.lat, origin.lon, 10f, 933.5)
        assertEquals(true, estimator.missedDeparture(933.5, 3.0))
    }

    @Test
    fun `embarcou e o GPS o viu na estacao seguinte, nao perdeu`() {
        val estimator = TripEstimator(plan())
        estimator.onLocation(stops[0].lat, stops[0].lon, 10f, 929.0)
        estimator.onLocation(stops[1].lat, stops[1].lon, 10f, 931.5)
        assertEquals(false, estimator.missedDeparture(934.0, 3.0))
    }

    @Test
    fun `simulacao segue so a tabela, sem GPS`() {
        val estimator = TripEstimator(plan(simulated = true))
        estimator.onLocation(homeLat, homeLon, 11f, 925.0)
        assertEquals(-1, estimator.progress(925.0).index)
        val end = estimator.progress(934.0)
        assertEquals(stops.lastIndex, end.index)
        assertEquals(TripFix.SIMULATED, end.fix)
        assertEquals(null, end.gpsSilentMinutes)
    }
}
