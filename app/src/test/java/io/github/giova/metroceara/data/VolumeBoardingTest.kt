package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Horário das compras e volumes grandes (notícia do Metrofor de 26/11/2026). */
class VolumeBoardingTest {
    private val volumes = VolumeRule(
        BikeRule(
            weekday = listOf(BikeWindow("09:00", "15:00"), BikeWindow("20:00")),
            saturday = listOf(BikeWindow("15:00")),
            sourceId = "volumes",
        ),
        sizes = "Embalado, somando até 160 cm.",
    )

    private fun at(day: Int, h: Int, m: Int = 0, running: Boolean = true, special: Boolean = false) =
        volumeBoarding(volumes, running, special, day, h * 60 + m)

    @Test
    fun `dia útil - pode das 9h às 15h e das 20h até o fim`() {
        assertEquals(BikeBoarding(true, 15 * 60), at(Calendar.TUESDAY, 10))
        assertEquals(BikeBoarding(false, 20 * 60), at(Calendar.TUESDAY, 15))
        assertEquals(BikeBoarding(true, null), at(Calendar.TUESDAY, 21))
        assertEquals(BikeBoarding(false, 9 * 60), at(Calendar.TUESDAY, 6, 30))
    }

    @Test
    fun `sábado - só das 15h em diante`() {
        assertEquals(BikeBoarding(false, 15 * 60), at(Calendar.SATURDAY, 10))
        assertTrue(at(Calendar.SATURDAY, 16)!!.allowed)
    }

    @Test
    fun `sem aviso no domingo, em dia especial, sem metrô ou sem a regra`() {
        assertNull(at(Calendar.SUNDAY, 10))
        assertNull(at(Calendar.FRIDAY, 10, special = true))
        assertNull(at(Calendar.FRIDAY, 10, running = false))
        assertNull(volumeBoarding(null, true, false, Calendar.FRIDAY, 600))
        assertFalse(at(Calendar.FRIDAY, 16)!!.allowed)
    }
}
