package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LongGapTest {
    private fun hm(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    // Linha Oeste saindo de Moura Brasil, grade de 23/11/2026.
    private val oeste = listOf(
        "05:30", "06:15", "07:00", "07:45", "08:30", "09:15", "11:45", "13:30",
        "15:00", "16:00", "16:45", "17:30", "18:15", "19:15", "20:00",
    ).map(::hm)

    @Test
    fun oesteMarcaOsVazios() {
        val gaps = longGaps(oeste, 90)
        assertEquals(
            mapOf(hm("09:15") to hm("11:45"), hm("11:45") to hm("13:30"), hm("13:30") to hm("15:00")),
            gaps,
        )
    }

    @Test
    fun zeroDesliga() = assertEquals(emptyMap<Int, Int>(), longGaps(oeste, 0))

    @Test
    fun duracao() {
        assertEquals("45 min", formatGap(45))
        assertEquals("2h", formatGap(120))
        assertEquals("2h30", formatGap(150))
        assertEquals("1h45", formatGap(105))
    }
}
