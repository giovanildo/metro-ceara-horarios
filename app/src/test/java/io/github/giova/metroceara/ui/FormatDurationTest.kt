package io.github.giova.metroceara.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {
    @Test
    fun `menos de uma hora em minutos, depois em horas`() {
        assertEquals("11 min", formatDuration(11))
        assertEquals("59 min", formatDuration(59))
        assertEquals("1h00", formatDuration(60))
        assertEquals("1h05", formatDuration(65))
    }
}
