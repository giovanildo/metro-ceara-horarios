package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentTripsTest {
    private fun t(dest: String, landmark: String? = null) = RecentTrip("SBE", "sul", dest, "sul", landmark)

    @Test
    fun `a mais nova vai para o topo e guarda só três`() {
        val l = emptyList<RecentTrip>().withTrip(t("A")).withTrip(t("B")).withTrip(t("C")).withTrip(t("D"))
        assertEquals(listOf("D", "C", "B"), l.map { it.destStop })
    }

    @Test
    fun `o mesmo trajeto não repete, sobe`() {
        val l = listOf(t("A"), t("B"), t("C")).withTrip(t("C"))
        assertEquals(listOf("C", "A", "B"), l.map { it.destStop })
    }

    @Test
    fun `lugar é outro trajeto que a estação dele`() {
        val l = listOf(t("A")).withTrip(t("A", "Rodoviária"))
        assertEquals(2, l.size)
    }

    @Test
    fun `codifica e lê de volta, e ignora linha quebrada`() {
        val trip = t("A", "Rodoviária")
        assertEquals(trip, RecentTrip.decode(trip.encode()))
        assertEquals(t("A"), RecentTrip.decode(t("A").encode()))
        assertNull(RecentTrip.decode("SBE\tsul"))
    }

    @Test
    fun `viagem guardada sem a hora continua valendo`() {
        assertEquals(t("A"), RecentTrip.decode("SBE\tsul\tA\tsul\t"))
    }

    @Test
    fun `a data de quando foi vista sobrevive e não muda o trajeto`() {
        val trip = t("A").copy(at = 123L)
        assertEquals(trip, RecentTrip.decode(trip.encode()))
        assertTrue(trip.sameAs(t("A")))
    }

    @Test
    fun `o caminho de volta troca as estações e esquece o lugar`() {
        val back = RecentTrip("SBE", "sul", "PAP", "ne", "Rodoviária", 9L).reversed()
        assertEquals(RecentTrip("PAP", "ne", "SBE", "sul", null, 9L), back)
    }

    // 09/11/2026, 07:05 em Fortaleza (UTC-3).
    private val morning = 1_794_218_700_000L
    private val hour = 3_600_000L

    @Test
    fun `perto do destino inverte, perto da origem não`() {
        assertTrue(shouldReverseRecent(9000.0, 100.0, 3000.0, morning, morning + hour, 3))
        assertFalse(shouldReverseRecent(100.0, 9000.0, 3000.0, morning, morning + 8 * hour, 3))
    }

    @Test
    fun `sem localização, inverte só horas depois e no mesmo dia`() {
        assertFalse(shouldReverseRecent(null, null, 3000.0, morning, morning + 2 * hour, 3))
        assertTrue(shouldReverseRecent(null, null, 3000.0, morning, morning + 10 * hour, 3))
        assertFalse(shouldReverseRecent(null, null, 3000.0, morning, morning + 24 * hour, 3))
    }

    @Test
    fun `longe das duas estações, vale o tempo`() {
        assertTrue(shouldReverseRecent(9000.0, 8000.0, 3000.0, morning, morning + 10 * hour, 3))
        assertFalse(shouldReverseRecent(8000.0, 9000.0, 3000.0, morning, morning + hour, 3))
    }

    @Test
    fun `viagem sem data não inverte pelo tempo`() {
        assertFalse(shouldReverseRecent(null, null, 3000.0, 0, morning, 3))
    }
}
