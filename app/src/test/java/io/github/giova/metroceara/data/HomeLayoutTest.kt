package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `tela inicial tem trabalho e casa e horário, busca e linhas ficam nas abas`() {
        assertEquals(
            listOf(HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.ROUTINES, HomeBlock.BIKE),
            HomeLayout().homeOrder,
        )
        assertTrue(HomeBlock.SEARCH.inRoutes && HomeBlock.LINES.inRoutes)
        assertFalse(HomeBlock.PLACES.inRoutes)
    }

    @Test
    fun `ordem antiga guardada perde a busca e as linhas na tela inicial`() {
        val old = HomeLayout(order = listOf(HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.SEARCH,
            HomeBlock.ROUTINES, HomeBlock.BIKE, HomeBlock.LINES))
        assertEquals(listOf(HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.ROUTINES, HomeBlock.BIKE), old.homeOrder)
        assertTrue(old.isDefaultOrder)
    }

    @Test
    fun `subir e descer não passam das pontas`() {
        val l = HomeLayout()
        assertEquals(l, l.moved(HomeBlock.PLACES, -1))
        assertEquals(l, l.moved(HomeBlock.BIKE, 1))
        assertEquals(l, l.moved(HomeBlock.LINES, -1))
        assertEquals(HomeBlock.STATIONS, l.moved(HomeBlock.STATIONS, -1).homeOrder.first())
    }

    @Test
    fun `estações não podem ser escondidas`() {
        val l = HomeLayout().withVisible(HomeBlock.STATIONS, false)
        assertTrue(l.shows(HomeBlock.STATIONS))
    }

    @Test
    fun `bicicletar só aparece com levo bike`() {
        assertFalse(HomeLayout().shows(HomeBlock.BIKE))
        assertTrue(HomeLayout(bike = true).shows(HomeBlock.BIKE))
        assertFalse(HomeLayout(bike = true).withVisible(HomeBlock.BIKE, false).shows(HomeBlock.BIKE))
    }

    @Test
    fun `voltar ao padrão mantém bike meia e acessibilidade`() {
        val l = HomeLayout(bike = true, halfFare = true, access = true)
            .moved(HomeBlock.BIKE, -3).withVisible(HomeBlock.NOTICES, false)
        assertFalse(l.isDefaultOrder)
        val r = l.resetOrder()
        assertTrue(r.isDefaultOrder)
        assertTrue(r.bike && r.halfFare && r.access)
    }
}
