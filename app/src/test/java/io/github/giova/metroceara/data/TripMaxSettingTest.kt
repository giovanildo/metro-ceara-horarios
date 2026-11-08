package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TripMaxSettingTest {

    @Test
    fun `quem ainda tinha o padrao antigo passa para 1 km`() {
        assertEquals(1_000, AppSettings.loadedTripMax(3_000, 1_000, alreadyMigrated = false))
    }

    @Test
    fun `outro valor escolhido fica como esta`() {
        assertEquals(5_000, AppSettings.loadedTripMax(5_000, 1_000, alreadyMigrated = false))
    }

    @Test
    fun `depois de salvar, 3 km escolhido de novo vale`() {
        assertEquals(3_000, AppSettings.loadedTripMax(3_000, 1_000, alreadyMigrated = true))
    }

    @Test
    fun `nao deixa o limite abaixo do aviso de longe`() {
        assertEquals(3_000, AppSettings.loadedTripMax(3_000, 2_000, alreadyMigrated = false))
    }
}
