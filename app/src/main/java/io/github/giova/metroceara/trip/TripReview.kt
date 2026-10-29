package io.github.giova.metroceara.trip

import kotlin.math.abs
import kotlin.math.max

/**
 * Acompanha uma viagem para dizer, no fim, se ela teve algum problema — só aí o
 * app convida a pessoa a mandar o registro ([io.github.giova.metroceara.data.TripLog]).
 * Perguntar em toda viagem cansa; estas são as viagens que ajudam a corrigir o app.
 *
 * Problemas: ficou [limitMinutes] ou mais sem GPS; a chegada errou mais que
 * [limitMinutes] em relação à prevista na hora do aviso de descida; a viagem foi
 * encerrada pela pessoa depois de embarcar e antes do destino; ou o app desistiu
 * de esperar.
 */
class TripReview(private val limitMinutes: Double = 3.0) {

    private var boarded = false
    private var reached = -1
    private var destination = 0
    private var etaAtAlert: Double? = null

    var maxGpsSilentMinutes = 0.0
        private set

    /** Chegada de verdade menos a prevista no aviso de descida (positivo = atrasou). */
    var arrivalErrorMinutes: Double? = null
        private set

    fun onUpdate(trip: ActiveTrip) {
        val progress = trip.progress
        reached = progress.index
        destination = trip.plan.stops.lastIndex
        if (progress.index >= 0) boarded = true
        progress.gpsSilentMinutes?.let { maxGpsSilentMinutes = max(maxGpsSilentMinutes, it) }
        if (trip.alerted && etaAtAlert == null) etaAtAlert = progress.arrivalMinutes
    }

    /** Trecho novo de um roteiro com integração: o aviso de descida dele é outro. */
    fun onLeg() {
        etaAtAlert = null
    }

    fun onArrived(nowMinutes: Double) {
        arrivalErrorMinutes = etaAtAlert?.let { nowMinutes - it }
    }

    /** Os problemas, com o motivo do fim ("arrived", "user", "give_up"). Vazio = viagem normal. */
    fun problems(endReason: String): List<String> = buildList {
        if (endReason == "give_up") add("give_up")
        if (endReason == "user" && boarded && reached < destination) add("ended_early")
        if (maxGpsSilentMinutes >= limitMinutes) add("gps_lost")
        if (arrivalErrorMinutes?.let { abs(it) > limitMinutes } == true) add("arrival_off")
    }
}
