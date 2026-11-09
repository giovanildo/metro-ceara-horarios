package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TomorrowPlanTest {
    @Test fun diaComumComGradeEhPrevisao() =
        assertEquals(TomorrowStatus.FORECAST, tomorrowStatus(special = false, sunday = false, announced = false, hasForecast = true))

    @Test fun operacaoEspecialNaoTemPrevisaoMesmoComGrade() =
        assertEquals(TomorrowStatus.SPECIAL, tomorrowStatus(special = true, sunday = false, announced = true, hasForecast = true))

    @Test fun domingoComOperacaoEspecialEhEspecial() =
        assertEquals(TomorrowStatus.SPECIAL, tomorrowStatus(special = true, sunday = true, announced = true, hasForecast = false))

    @Test fun domingoSemOperacaoNaoTemMetro() =
        assertEquals(TomorrowStatus.SUNDAY, tomorrowStatus(special = false, sunday = true, announced = false, hasForecast = false))

    @Test fun domingoAnunciadoComHorarioNormalSemGradeNaoSeSabe() =
        assertEquals(TomorrowStatus.UNKNOWN, tomorrowStatus(special = false, sunday = true, announced = true, hasForecast = false))

    @Test fun linhaParadaSegundaNaoTemHorarioMesmoComGrade() =
        assertEquals(MondayReturn.Stopped, mondayReturn(stopped = true, special = false, first = 330))

    @Test fun linhaParadaVemAntesDaOperacaoEspecial() =
        assertEquals(MondayReturn.Stopped, mondayReturn(stopped = true, special = true, first = null))

    @Test fun segundaEspecialNaoTemHorario() =
        assertEquals(MondayReturn.Special, mondayReturn(stopped = false, special = true, first = null))

    @Test fun segundaComumVoltaNoPrimeiroMetro() =
        assertEquals(MondayReturn.At(330), mondayReturn(stopped = false, special = false, first = 330))

    @Test fun segundaSemGradeNaoSeSabe() =
        assertEquals(MondayReturn.Unknown, mondayReturn(stopped = false, special = false, first = null))

    @Test fun sabadoNuncaBaixadoNaoSeSabe() =
        assertEquals(TomorrowStatus.UNKNOWN, tomorrowStatus(special = false, sunday = false, announced = false, hasForecast = false))
}
