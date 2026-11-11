package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conte sobre você: a frequência mostra ou esconde Casa, Trabalho e rotinas. */
class ProfileTest {

    @Test
    fun `todo dia mostra Casa e rotinas, conhecendo a cidade esconde`() {
        val hidden = HomeLayout(hidden = setOf(HomeBlock.PLACES, HomeBlock.ROUTINES))
        val daily = hidden.withFrequency(RideFrequency.DAILY)
        assertTrue(daily.shows(HomeBlock.PLACES) && daily.shows(HomeBlock.ROUTINES))
        assertEquals(RideFrequency.DAILY, daily.frequency)
        val visitor = HomeLayout().withFrequency(RideFrequency.VISITOR)
        assertFalse(visitor.shows(HomeBlock.PLACES) || visitor.shows(HomeBlock.ROUTINES))
        assertEquals(hidden.hidden, hidden.withFrequency(RideFrequency.SOMETIMES).hidden)
    }
}
