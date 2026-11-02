package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarksTest {

    private fun place(name: String, search: String = "", station: String = "Benfica", walk: Int = 300,
                      lat: Double = -3.7404, lon: Double = -38.5378) =
        Landmark(name, "hospital", search, "", "Fortaleza", lat, lon, station, walk)

    private val list = listOf(
        place("Hospital Geral César Cals (HGCC)", "hgcc cesar cals", "José de Alencar"),
        place("Hospital Geral de Fortaleza (HGF)", "hgf", "Papicu", 1130),
        place("Rodoviária de Fortaleza", "rodoviaria joao thome", "Borges de Melo"),
        place("Arena Castelão", "castelao estadio", "Aeroporto", 3700),
        place("FAC · Faculdade Cearense", "fac", "Padre Cícero", 260, -3.7457, -38.5458),
    )

    @Test
    fun searchIgnoresAccentsAndWordOrder() {
        assertEquals(listOf("Rodoviária de Fortaleza"), list.search("rodoviaria").map { it.name })
        assertEquals(2, list.search("geral hospital").size)
        assertEquals("Hospital Geral de Fortaleza (HGF)", list.search("HGF").single().name)
    }

    @Test
    fun shortQueryFindsNothing() {
        assertTrue(list.search("h").isEmpty())
    }

    @Test
    fun farPlaceIsMarked() {
        assertTrue(list.search("castelão").single().far)
        assertFalse(list.search("rodoviaria").single().far)
    }

    @Test
    fun nearUsesNameAndPosition() {
        // Padre Cícero em Fortaleza (-3.7457, -38.5458), não a do Cariri (-7.22, -39.39).
        assertEquals(1, list.near("Padre Cícero", -3.7457, -38.5458).size)
        assertTrue(list.near("Padre Cícero", -7.2249, -39.3940).isEmpty())
        // "Parangaba - NE" é a mesma estação que "Parangaba".
        val parangaba = listOf(place("Shopping Parangaba", station = "Parangaba", lat = -3.7760, lon = -38.5620))
        assertEquals(1, parangaba.near("Parangaba - NE", -3.7758, -38.5639).size)
    }

    @Test
    fun walkMinutesFollowsMobility() {
        val p = place("X", walk = 300)
        assertEquals(4, p.walkMinutes(1.0))
        assertEquals(8, p.walkMinutes(2.0))
    }
}
