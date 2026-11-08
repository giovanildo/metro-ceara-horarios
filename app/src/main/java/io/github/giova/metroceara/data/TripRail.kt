package io.github.giova.metroceara.data

import kotlin.math.roundToInt

/** Uma linha do trilho do modo viagem: uma estação ou a caminhada de uma integração. */
data class RailRow(
    val name: String,
    /** Horário previsto; só faz sentido nas que ainda vêm pela frente. */
    val minutes: Int?,
    val colorHex: String,
    val isWalk: Boolean = false,
    /** Onde a viagem inteira termina. */
    val isDestination: Boolean = false,
    /** Onde a pessoa desce para fazer a integração. */
    val isTransfer: Boolean = false,
    /** Onde sai o aviso de descida. */
    val isAlert: Boolean = false,
    /** O terminal de uma viagem "sentado": o metrô volta dali. */
    val isTurnaround: Boolean = false,
    /** Primeira estação de um trecho depois de integração: a linha e o horário de saída. */
    val boardsLine: String? = null,
    val boardsAt: Int? = null,
    val walkMinutes: Int? = null,
    val walkFree: Boolean? = null,
    /** A mesma estação da anterior (a outra plataforma, ou o terminal na volta): não conta. */
    val isRepeat: Boolean = false,
)

/** O trajeto inteiro, da origem ([rows] primeiro) ao destino, e onde a pessoa está. */
data class Rail(val rows: List<RailRow>, val position: Int) {
    val stationsLeft: Int get() = rows.drop(position + 1).count { !it.isWalk && !it.isRepeat }
    val stationsRidden: Int get() = (rows.count { !it.isWalk && !it.isRepeat } - 1).coerceAtLeast(0)
}

/**
 * Monta o trilho a partir do plano e do andamento. O trecho em curso vem de
 * [TripPlan.stops], com o atraso medido; os outros, da grade guardada no roteiro.
 * Só lê o que o estimador já decidiu: a posição nunca é calculada aqui.
 */
object TripRail {

    fun build(plan: TripPlan, progress: TripProgress, lineHex: String): Rail {
        val rows = mutableListOf<RailRow>()
        val journey = plan.journey
        var position = 0

        fun addCurrent(hex: String) {
            position = rows.size + progress.index.coerceAtLeast(0)
            plan.stops.forEachIndexed { i, stop ->
                rows += RailRow(
                    name = JourneyPlanner.plainName(stop.name),
                    minutes = (stop.scheduled + progress.delayMinutes).roundToInt(),
                    colorHex = hex,
                    isAlert = i == plan.alertIndex && i in 1 until plan.stops.lastIndex,
                    isTurnaround = plan.viaTerminal && i == plan.turnaroundIndex,
                )
            }
        }

        if (journey == null) {
            addCurrent(lineHex)
        } else {
            journey.legs.forEachIndexed { j, leg ->
                val first = rows.size
                if (j == journey.current) {
                    addCurrent(leg.colorHex)
                } else {
                    leg.stations.ifEmpty { listOf(LegStation(leg.fromName, leg.departure), LegStation(leg.toName, leg.arrival)) }
                        .forEach { rows += RailRow(it.name, it.scheduled, leg.colorHex) }
                }
                if (j > 0) rows[first] = rows[first].copy(boardsLine = leg.routeName, boardsAt = leg.departure)
                if (j < journey.legs.lastIndex) {
                    rows[rows.lastIndex] = rows.last().copy(isTransfer = true, isAlert = false)
                    val here = j == journey.current
                    rows += RailRow(
                        name = leg.toName, minutes = null, colorHex = journey.legs[j + 1].colorHex, isWalk = true,
                        walkMinutes = plan.transfer?.walkMinutes?.takeIf { here },
                        walkFree = plan.transfer?.free?.takeIf { here },
                    )
                }
            }
        }
        rows[rows.lastIndex] = rows.last().copy(isDestination = true, isAlert = false)

        var previous: String? = null
        return Rail(
            rows.map { row ->
                if (row.isWalk) return@map row
                val repeated = row.name == previous
                previous = row.name
                row.copy(isRepeat = repeated)
            },
            position,
        )
    }
}
