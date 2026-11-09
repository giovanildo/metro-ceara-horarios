package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StartChosenTripTest {
    @Test fun semEscolhaProcuraDesdeAgora() = assertEquals(478, seatedSearchFrom(478, 486, null))

    @Test fun oProximoMetroMantemABuscaDesdeAgora() = assertEquals(478, seatedSearchFrom(478, 486, 486))

    @Test fun horarioMaisAFrenteProcuraDesdeEle() = assertEquals(510, seatedSearchFrom(478, 486, 510))

    @Test fun semProximoMetroProcuraDesdeAgora() = assertEquals(478, seatedSearchFrom(478, null, 510))
}
