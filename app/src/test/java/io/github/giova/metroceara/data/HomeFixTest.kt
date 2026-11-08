package io.github.giova.metroceara.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFixTest {

    private val hold = 120_000L
    private val shown = ShownFix(lat = -3.9000, lon = -38.6000, accuracyMeters = 5.6f, atMillis = 0L)

    /** Cerca de 750 m ao norte da posição mostrada. */
    private val nearbyLat = -3.8933

    /** Cerca de 5,5 km ao norte da posição mostrada. */
    private val farLat = -3.8500

    @Test
    fun `sem posicao na tela, a primeira entra`() {
        val none: ShownFix? = null
        assertFalse(none.keepsOver(nearbyLat, -38.6, 600f, 1_000L, hold))
    }

    @Test
    fun `rede imprecisa logo depois da precisa fica de fora`() {
        assertTrue(shown.keepsOver(nearbyLat, -38.6, 600f, 4_000L, hold))
    }

    @Test
    fun `posicao tao precisa quanto a atual entra`() {
        assertFalse(shown.keepsOver(nearbyLat, -38.6, 5f, 4_000L, hold))
    }

    @Test
    fun `pequena perda de precisao nao segura a posicao`() {
        assertFalse(shown.keepsOver(nearbyLat, -38.6, 25f, 4_000L, hold))
    }

    @Test
    fun `parado, a imprecisa continua de fora depois do tempo`() {
        assertTrue(shown.keepsOver(nearbyLat, -38.6, 600f, 300_000L, hold))
    }

    @Test
    fun `depois do tempo, a imprecisa entra se mostrar outro lugar`() {
        assertFalse(shown.keepsOver(farLat, -38.6, 600f, 300_000L, hold))
    }

    @Test
    fun `dentro do tempo, ate outro lugar espera`() {
        assertTrue(shown.keepsOver(farLat, -38.6, 600f, 60_000L, hold))
    }
}
