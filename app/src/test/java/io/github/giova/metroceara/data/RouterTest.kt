package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Benfica → Papicu com integração em Parangaba, com os horários da grade de dia
 * útil do Metrofor de 23/10/2026 (Linha Sul de 07:17 a 23:03, Linha Nordeste toda).
 */
class RouterTest {

    private val sul = "1"
    private val nordeste = "3"
    private val benfica = "2"
    private val parangaba = "5"
    private val parangabaNe = "31"
    private val papicu = "23"

    private val sulTrips = listOf(
        437 to 445, 460 to 468, 473 to 481, 499 to 507, 514 to 522, 539 to 547, 549 to 557, 571 to 579,
        593 to 601, 613 to 621, 635 to 643, 657 to 665, 679 to 687, 701 to 709, 723 to 731, 745 to 753,
        1295 to 1303, 1317 to 1325, 1339 to 1347, 1361 to 1369, 1383 to 1391,
    )
    private val nordesteTrips = (330..1370 step 40).map { it to it + 30 }

    private val timetable = object : Timetable {
        override fun trips(routeId: String, fromStop: String, toStop: String): List<LegTrip> = when {
            routeId == sul && fromStop == benfica && toStop == parangaba ->
                sulTrips.map { (d, a) -> LegTrip("s$d", 0, d, a) }
            routeId == nordeste && fromStop == parangabaNe && toStop == papicu ->
                nordesteTrips.map { (d, a) -> LegTrip("n$d", 1, d, a) }
            else -> emptyList()
        }
    }

    private val links = listOf(
        TransferLink(sul, parangaba, nordeste, parangabaNe, 2),
        TransferLink(nordeste, parangabaNe, sul, parangaba, 2),
    )

    private fun router(factor: Double = 1.0) =
        Router(timetable, links) { link, _ -> kotlin.math.ceil(link.baseMinutes * factor).toInt() }

    private val origin = RoutePoint(sul, benfica)
    private val destination = RoutePoint(nordeste, papicu)

    @Test
    fun `sugere o ultimo metro que pega a mesma chegada`() {
        val best = router().plan(origin, destination, from = 450).first()
        assertEquals(473, best.departure) // 07:53
        assertEquals(520, best.arrival) // 08:40
        assertEquals(460, best.sameArrivalFrom) // o das 07:40 chega igual
        assertEquals(490, best.legs[1].departure) // VLT das 08:10
        assertEquals(7, best.transfers.single().slackMinutes)
    }

    @Test
    fun `outros horarios vem um por chegada`() {
        val all = router().plan(origin, destination, from = 450)
        assertEquals(listOf(520, 560, 600, 640), all.map { it.arrival })
        assertEquals(514, all[1].departure) // 08:34, e nao o das 08:19
    }

    @Test
    fun `cadeira de rodas perde o VLT apertado`() {
        val walking = router().plan(origin, destination, from = 675).first()
        assertEquals(690, walking.legs[1].departure) // 11:30
        assertTrue(walking.transfers.single().tight)
        assertEquals(730, walking.transfers.single().nextDeparture)

        val wheelchair = router(factor = 2.0).plan(origin, destination, from = 675).first()
        assertEquals(730, wheelchair.legs[1].departure) // 12:10
        assertFalse(wheelchair.transfers.single().tight)
    }

    @Test
    fun `ultima integracao do dia`() {
        assertEquals(1339, router().lastDeparture(origin, destination)) // 22:19
        assertTrue(router().plan(origin, destination, from = 1340).isEmpty())
    }

    @Test
    fun `ja na estacao de integracao, so o segundo trecho`() {
        val best = router().plan(RoutePoint(sul, parangaba), destination, from = 600).first()
        assertEquals(1, best.legs.size)
        assertEquals(610, best.departure)
        assertTrue(best.transfers.isEmpty())
    }

    @Test
    fun `linha parada nao entra no roteiro`() {
        assertTrue(router().plan(origin, destination, from = 450, avoid = setOf(nordeste)).isEmpty())
        assertNull(router().lastDeparture(origin, destination, avoid = setOf(nordeste)))
    }

    @Test
    fun `Caucaia ao Aeroporto passa por tres integracoes`() {
        val net = listOf(
            TransferLink("2", "33", "1", "1", 5), TransferLink("1", "1", "2", "33", 5),
            TransferLink("1", "5", "3", "31", 2), TransferLink("3", "31", "1", "5", 2),
            TransferLink("3", "29", "7", "66", 3), TransferLink("7", "66", "3", "29", 3),
        )
        val reach = Router(timetable, net) { l, _ -> l.baseMinutes }.reachable("2")
        assertEquals(setOf("2", "1", "3", "7"), reach)
    }
}
