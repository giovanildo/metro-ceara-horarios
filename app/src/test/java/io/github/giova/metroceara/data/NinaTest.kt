package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NinaTest {
    private val nina = Nina("5585933007001", setOf("linha sul", "linha nordeste", "linha oeste", "vlt aeroporto"))

    @Test
    fun `abre a conversa da Super NINA no WhatsApp`() {
        assertEquals("https://wa.me/5585933007001", nina.link)
    }

    @Test
    fun `aparece nas linhas de Fortaleza e região`() {
        assertTrue(nina.serves("Linha Sul"))
        assertTrue(nina.serves(" VLT Aeroporto "))
    }

    @Test
    fun `some em Sobral e no Cariri`() {
        assertFalse(nina.serves("VLT Sobral - Linha Norte"))
        assertFalse(nina.serves("VLT Cariri"))
    }

    @Test
    fun `sem estação conhecida, aparece`() {
        assertTrue(nina.serves(null))
    }
}
