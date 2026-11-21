package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationNamesTest {
    @Test
    fun `Jereissati vira Francisco Alves Marinho e a busca acha pelos dois`() {
        assertEquals("Francisco Alves Marinho (Jereissati)", StationNames.official("Jereissati"))
        assertEquals("Benfica", StationNames.official("Benfica"))
        assertEquals("Jereissati", StationNames.formerName("Francisco Alves Marinho (Jereissati)"))
        assertNull(StationNames.formerName("Benfica"))
        assertEquals("Francisco Alves Marinho (Jereissati)", StationNames.searchText("Francisco Alves Marinho (Jereissati)"))
    }
}
