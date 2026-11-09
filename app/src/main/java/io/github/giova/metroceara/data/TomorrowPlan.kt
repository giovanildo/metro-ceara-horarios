package io.github.giova.metroceara.data

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
