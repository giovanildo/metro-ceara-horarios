package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Para onde a pessoa vai todo dia: só muda o nome do lugar e os textos. */
enum class CommuteKind(val key: String) { WORK("trabalho"), SCHOOL("faculdade") }

/**
 * Os horários de "Sua ida e volta". As estações ficam em [Places]; aqui, a hora
 * de chegar ([arriveBy]), a de sair de lá ([leaveAt]), em minutos do dia, e os
 * dias em que elas valem (como em Calendar.DAY_OF_WEEK).
 */
data class CommuteSchedule(
    val kind: CommuteKind = CommuteKind.WORK,
    val arriveBy: Int? = null,
    val leaveAt: Int? = null,
    val days: Set<Int> = Routine.WEEKDAYS,
)

private const val DAY_MINUTES = 1440

/**
 * Para onde a pessoa vai agora só pelo relógio. Entre a saída e a próxima
 * chegada, a primeira metade ainda é a volta e a segunda já é a ida; entre a
 * chegada e a saída, a próxima viagem é a volta. Serve de manhã, à noite e em
 * quem vira a noite no trabalho, sem supor o que é ida.
 */
fun commuteDestinationByClock(now: Int, arriveBy: Int, leaveAt: Int): Place {
    val gap = (arriveBy - leaveAt + DAY_MINUTES) % DAY_MINUTES
    val sinceLeave = (now - leaveAt + DAY_MINUTES) % DAY_MINUTES
    return if (sinceLeave in (gap / 2)..gap) Place.WORK else Place.HOME
}

/**
 * Para onde a pessoa vai agora: perto de uma das duas estações, para a outra;
 * sem localização, ou longe das duas, pelo relógio.
 */
fun commuteDestination(
    now: Int,
    arriveBy: Int,
    leaveAt: Int,
    distanceHome: Double?,
    distanceWork: Double?,
    nearMeters: Double,
): Place {
    if (distanceHome != null && distanceWork != null && minOf(distanceHome, distanceWork) <= nearMeters) {
        return if (distanceHome <= distanceWork) Place.WORK else Place.HOME
    }
    return commuteDestinationByClock(now, arriveBy, leaveAt)
}

/**
 * [a] e [b] são a mesma estação para quem sai dela: o mesmo ponto, ou os dois lados
 * de uma integração a pé (Parangaba). O nome não serve: há dois "Padre Cícero", um
 * em Fortaleza e outro no Cariri.
 */
fun isSameStation(a: String, b: String, links: List<TransferLink>): Boolean =
    a == b || links.any { it.fromStop == a && it.toStop == b }

/** Por que este é o metrô em destaque. */
enum class CommuteReason {
    /** O próximo, sem hora marcada para levar em conta. */
    NEXT,

    /** O último que chega até a hora marcada. */
    ARRIVE_BY,

    /** Nenhum chega mais até a hora marcada: o próximo. */
    TOO_LATE,

    /** O primeiro depois da hora em que a pessoa costuma sair. */
    LEAVE_AT,

    /** O dia acaba antes da hora de sair de sempre (operação especial): o último. */
    EARLY,
}

/** O metrô em destaque, as alternativas e, perto do fim do dia, a partida do último. */
data class CommutePick(
    val main: Itinerary,
    val others: List<Itinerary>,
    val reason: CommuteReason,
    val lastDeparture: Int?,
)

/** A partir de quantas partidas para o fim do dia o último aparece. */
private const val SHOW_LAST_WITHIN = 6

/**
 * Escolhe o metrô de [options] (os do resto do dia, em ordem). [target] é a hora
 * marcada (chegar, na ida; sair, na volta), ou `null` quando não vale agora:
 * fora dos dias da pessoa ou com o sentido invertido à mão.
 */
fun pickCommute(options: List<Itinerary>, toWork: Boolean, target: Int?, now: Int): CommutePick? {
    if (options.isEmpty()) return null
    fun around(i: Int) = listOfNotNull(options.getOrNull(i - 1), options.getOrNull(i + 1))
    fun next() = Triple(0, options.drop(1).take(2), CommuteReason.NEXT)
    val (index, others, reason) = when {
        target == null || now > target -> next()
        toWork -> {
            val i = options.indexOfLast { it.arrival <= target }
            if (i >= 0) Triple(i, around(i), CommuteReason.ARRIVE_BY)
            else Triple(0, options.drop(1).take(2), CommuteReason.TOO_LATE)
        }
        else -> {
            val i = options.indexOfFirst { it.departure >= target }
            if (i >= 0) Triple(i, around(i), CommuteReason.LEAVE_AT)
            else Triple(options.lastIndex, options.dropLast(1).takeLast(2), CommuteReason.EARLY)
        }
    }
    val nearEnd = reason != CommuteReason.EARLY && index >= options.size - SHOW_LAST_WITHIN
    return CommutePick(options[index], others, reason, options.last().departure.takeIf { nearEnd })
}

/** Guarda "Sua ida e volta", só neste celular. */
object CommuteStore {
    private val state = MutableStateFlow<CommuteSchedule?>(null)

    fun flow(context: Context): StateFlow<CommuteSchedule?> {
        if (state.value == null) state.value = load(context)
        return state.asStateFlow()
    }

    fun save(context: Context, schedule: CommuteSchedule) {
        prefs(context).edit {
            putString("kind", schedule.kind.key)
            putInt("arrive_by", schedule.arriveBy ?: -1)
            putInt("leave_at", schedule.leaveAt ?: -1)
            putString("days", schedule.days.joinToString(","))
        }
        state.value = schedule
    }

    private fun load(context: Context): CommuteSchedule {
        val p = prefs(context)
        return CommuteSchedule(
            kind = CommuteKind.entries.firstOrNull { it.key == p.getString("kind", null) } ?: CommuteKind.WORK,
            arriveBy = p.getInt("arrive_by", -1).takeIf { it >= 0 },
            leaveAt = p.getInt("leave_at", -1).takeIf { it >= 0 },
            days = p.getString("days", null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: Routine.WEEKDAYS,
        )
    }

    // Arquivo próprio: "Restaurar padrões" das configurações não apaga.
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_commute", Context.MODE_PRIVATE)
}
