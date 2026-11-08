package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
