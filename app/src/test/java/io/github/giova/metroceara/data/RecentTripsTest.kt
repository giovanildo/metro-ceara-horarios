package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
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

    private val last = RecentTrip("BEN", "sul", "PAR", "sul", null, 9L)
    private val links = listOf(
        TransferLink("sul", "PAR", "ne", "PAR-NE", 4),
        TransferLink("ne", "PAR-NE", "sul", "PAR", 4),
        TransferLink("sul", "CHI", "oeste", "MOU", 5),
    )
    private fun names(id: String) = mapOf("PAR-NE" to "Parangaba", "PAR" to "Parangaba", "CHI" to "Chico", "MOU" to "Moura")[id] ?: id
    private fun from(trip: RecentTrip, stop: String?, route: String?) = trip.suggestionsFrom(stop, route, links, ::names)

    @Test
    fun `na origem, sugere só o destino`() {
        assertEquals(listOf("PAR"), from(last, "BEN", "sul").map { it.destStop })
    }

    @Test
    fun `no destino, sugere só a volta`() {
        assertEquals(listOf("BEN"), from(last, "PAR", "sul").map { it.destStop })
    }

    @Test
    fun `no outro lado da integração, também é o destino`() {
        assertEquals(listOf("BEN"), from(last, "PAR-NE", "ne").map { it.destStop })
    }

    @Test
    fun `em outra estação, as duas, o destino primeiro, saindo dali`() {
        val l = from(last, "POR", "sul")
        assertEquals(listOf("PAR", "BEN"), l.map { it.destStop })
        assertTrue(l.all { it.originStop == "POR" && it.originRoute == "sul" })
    }

    @Test
    fun `o lugar acompanha o destino e não a volta`() {
        val l = from(last.copy(landmark = "Rodoviária"), "POR", "sul")
        assertEquals(listOf("Rodoviária", null), l.map { it.landmark })
    }

    @Test
    fun `sem estação conhecida, a viagem e a volta como foram feitas`() {
        assertEquals(listOf(last, last.reversed()), from(last, null, null))
    }

    @Test
    fun `de outra linha, vai ao lado da integração que está nela`() {
        val l = from(last, "PAP", "ne")
        assertEquals(listOf("PAR-NE" to "ne", "BEN" to "sul"), l.map { it.destStop to it.destRoute })
    }

    @Test
    fun `integração entre estações de nomes diferentes mantém o destino`() {
        val l = from(RecentTrip("BEN", "sul", "CHI", "sul"), "CAU", "oeste")
        assertEquals("CHI", l.first().destStop)
    }
}
