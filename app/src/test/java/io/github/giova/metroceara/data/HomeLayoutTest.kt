package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `padrão é o do perfil da maioria`() {
        assertEquals(
            listOf(HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.SEARCH,
                HomeBlock.ROUTINES, HomeBlock.BIKE, HomeBlock.LINES),
            HomeLayout().order,
        )
    }

    @Test
    fun `subir e descer não passam das pontas`() {
        val l = HomeLayout()
        assertEquals(l, l.moved(HomeBlock.PLACES, -1))
        assertEquals(l, l.moved(HomeBlock.LINES, 1))
        assertEquals(HomeBlock.STATIONS, l.moved(HomeBlock.STATIONS, -1).order.first())
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
    fun `não conheço o metrô leva a busca ao topo e desligar devolve`() {
        val on = HomeLayout().withNewcomer(true)
        assertEquals(HomeBlock.SEARCH, on.order.first())
        assertTrue(on.newcomer)
        assertEquals(HomeLayout.DEFAULT_ORDER, on.withNewcomer(false).order)
    }

    @Test
    fun `voltar ao padrão mantém bike meia e acessibilidade`() {
        val l = HomeLayout(bike = true, halfFare = true, access = true)
            .moved(HomeBlock.LINES, -3).withVisible(HomeBlock.NOTICES, false)
        val r = l.resetOrder()
        assertTrue(r.isDefaultOrder)
        assertTrue(r.bike && r.halfFare && r.access)
    }
}
