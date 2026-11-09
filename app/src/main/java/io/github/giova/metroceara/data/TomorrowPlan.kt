package io.github.giova.metroceara.data

/** Como o metrô volta na segunda, para os avisos de domingo. */
sealed interface MondayReturn {
    /** Primeiro metrô da segunda, em minutos do dia. */
    data class At(val minutes: Int) : MondayReturn

    /** O Metrofor avisou que a linha não circula na segunda. */
    data object Stopped : MondayReturn

    /** Operação especial: a grade da segunda só sai depois da meia-noite. */
    data object Special : MondayReturn

    /** Não há grade para saber. */
    data object Unknown : MondayReturn
}

/** A linha parada vem antes: é ela que decide se a pessoa tem metrô na segunda. */
fun mondayReturn(stopped: Boolean, special: Boolean, first: Int?): MondayReturn = when {
    stopped -> MondayReturn.Stopped
    special -> MondayReturn.Special
    first != null -> MondayReturn.At(first)
    else -> MondayReturn.Unknown
}

/** O que o roteiro pode mostrar de amanhã. */
enum class TomorrowStatus {
    /** Dia comum: horários previstos pela última grade comum. */
    FORECAST,

    /** Operação especial anunciada: a grade dela só sai depois da meia-noite. */
    SPECIAL,

    /** Domingo sem operação anunciada: metrô e VLT não circulam. */
    SUNDAY,

    /** Não há grade para prever (sábado nunca baixado, por exemplo). */
    UNKNOWN,
}

/**
 * [special]: operação anunciada que muda o horário; [sunday]: amanhã é domingo;
 * [announced]: há qualquer operação anunciada amanhã; [hasForecast]: há grade prevista.
 */
fun tomorrowStatus(special: Boolean, sunday: Boolean, announced: Boolean, hasForecast: Boolean): TomorrowStatus = when {
    special -> TomorrowStatus.SPECIAL
    sunday && !announced -> TomorrowStatus.SUNDAY
    hasForecast -> TomorrowStatus.FORECAST
    else -> TomorrowStatus.UNKNOWN
}
