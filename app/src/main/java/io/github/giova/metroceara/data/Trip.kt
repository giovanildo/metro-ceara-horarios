package io.github.giova.metroceara.data

import org.json.JSONArray
import org.json.JSONObject

/** Uma estação no caminho da viagem, com o horário programado nela. */
data class TripStop(
    val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    /** Minutos desde a meia-noite, pela grade do dia. */
    val scheduled: Int,
    /** Debaixo da terra (regras.json "subterranea"): ali o GPS some. */
    val underground: Boolean = false,
)

/**
 * A integração no fim de um trecho de roteiro: na chegada, a pessoa desce e vai
 * a pé ([walkMinutes]) para [nextRoute], que sai às [nextDeparture] rumo a
 * [nextHeadsign]; o roteiro termina em [finalDestination].
 */
data class TripTransfer(
    val nextRoute: String,
    val nextHeadsign: String,
    val nextDeparture: Int,
    val walkMinutes: Int,
    val finalDestination: String,
    /** `true` grátis, `false` paga, `null` não informada. */
    val free: Boolean? = null,
)

/** Uma estação de um trecho do roteiro, para o trilho da viagem. */
data class LegStation(val name: String, val scheduled: Int)

/** Um trecho do roteiro, para o trilho da viagem. */
data class JourneyLeg(
    val routeName: String,
    val colorHex: String,
    val headsign: String,
    val fromName: String,
    val toName: String,
    val departure: Int,
    val arrival: Int,
    /** As estações do trecho; vazio numa viagem salva antes de existir o trilho. */
    val stations: List<LegStation> = emptyList(),
)

/**
 * O roteiro com integração inteiro: o destino final, os trechos (os feitos e
 * os que faltam) e qual está em andamento ([current]). O serviço do modo
 * viagem segue de um trecho para o outro sozinho.
 */
data class TripJourney(
    val destRouteId: String,
    val destStopId: String,
    val finalName: String,
    val legs: List<JourneyLeg>,
    val current: Int,
) {
    val originName get() = legs.first().fromName
    val arrival get() = legs.last().arrival
}

/**
 * O que a pessoa escolheu: uma viagem de [stops].first() até [stops].last(),
 * na linha [routeName] rumo a [headsign].
 */
data class TripPlan(
    val routeId: String,
    val routeName: String,
    val headsign: String,
    val stops: List<TripStop>,
    /** Quantas estações antes do destino vem o aviso de descida. */
    val alertStationsBefore: Int = 2,
    /**
     * Viagem "sentado pelo terminal": índice da chegada ao terminal, onde o
     * trem volta (a próxima parada é o mesmo terminal, já como partida).
     * -1 = viagem direta.
     */
    val turnaroundIndex: Int = -1,
    /** Trecho de um roteiro com integração: o destino é a estação de integração. */
    val transfer: TripTransfer? = null,
    /**
     * Simulação: a pessoa está longe da estação e quis ver a viagem mesmo assim.
     * Segue só a tabela de horários, sem GPS, e tudo aparece marcado "Simulação".
     */
    val simulated: Boolean = false,
    /** Num roteiro com integração, o roteiro inteiro; este plano é o trecho [TripJourney.current]. */
    val journey: TripJourney? = null,
    /** O lugar que a pessoa buscou ("Buscar lugares"), quando o destino veio dele. */
    val placeName: String? = null,
) {
    val viaTerminal get() = turnaroundIndex in 0 until stops.lastIndex
    val terminal get() = stops.getOrNull(turnaroundIndex)

    val origin get() = stops.first()
    val destination get() = stops.last()

    /** A estação em que o aviso dispara: N antes do destino, ou a origem se a viagem é curta. */
    val alertIndex get() = (stops.lastIndex - alertStationsBefore).coerceAtLeast(0)

    fun toJson(): String = JSONObject()
        .put("routeId", routeId)
        .put("routeName", routeName)
        .put("headsign", headsign)
        .put("alert", alertStationsBefore)
        .put("turn", turnaroundIndex)
        .put("sim", simulated)
        .put("place", placeName ?: JSONObject.NULL)
        .apply {
            journey?.let { j ->
                put("journey", JSONObject().put("route", j.destRouteId).put("stop", j.destStopId)
                    .put("final", j.finalName).put("current", j.current)
                    .put("legs", JSONArray().apply {
                        j.legs.forEach {
                            put(JSONObject().put("route", it.routeName).put("color", it.colorHex).put("headsign", it.headsign)
                                .put("from", it.fromName).put("to", it.toName).put("dep", it.departure).put("arr", it.arrival)
                                .put("st", JSONArray().apply {
                                    it.stations.forEach { s -> put(JSONObject().put("n", s.name).put("t", s.scheduled)) }
                                }))
                        }
                    }))
            }
        }
        .apply {
            transfer?.let {
                put("transfer", JSONObject().put("route", it.nextRoute).put("headsign", it.nextHeadsign)
                    .put("t", it.nextDeparture).put("walk", it.walkMinutes).put("final", it.finalDestination)
                    .put("free", it.free ?: JSONObject.NULL))
            }
        }
        .put("stops", JSONArray().apply {
            stops.forEach {
                put(JSONObject().put("id", it.stopId).put("name", it.name)
                    .put("lat", it.lat).put("lon", it.lon).put("t", it.scheduled).put("under", it.underground))
            }
        })
        .toString()

    companion object {
        fun fromJson(json: String): TripPlan? = runCatching {
            val o = JSONObject(json)
            val array = o.getJSONArray("stops")
            TripPlan(
                routeId = o.getString("routeId"),
                routeName = o.getString("routeName"),
                headsign = o.getString("headsign"),
                stops = (0 until array.length()).map { i ->
                    val s = array.getJSONObject(i)
                    TripStop(
                        s.getString("id"), s.getString("name"), s.getDouble("lat"), s.getDouble("lon"), s.getInt("t"),
                        underground = s.optBoolean("under"),
                    )
                },
                alertStationsBefore = o.optInt("alert", 2),
                turnaroundIndex = o.optInt("turn", -1),
                simulated = o.optBoolean("sim"),
                placeName = if (o.isNull("place")) null else o.getString("place"),
                journey = o.optJSONObject("journey")?.let { j ->
                    val legs = j.getJSONArray("legs")
                    TripJourney(
                        destRouteId = j.getString("route"),
                        destStopId = j.getString("stop"),
                        finalName = j.getString("final"),
                        current = j.getInt("current"),
                        legs = (0 until legs.length()).map { i ->
                            val l = legs.getJSONObject(i)
                            val st = l.optJSONArray("st")
                            JourneyLeg(l.getString("route"), l.getString("color"), l.getString("headsign"),
                                l.getString("from"), l.getString("to"), l.getInt("dep"), l.getInt("arr"),
                                stations = (0 until (st?.length() ?: 0)).map { k ->
                                    st!!.getJSONObject(k).let { LegStation(it.getString("n"), it.getInt("t")) }
                                })
                        },
                    )
                },
                transfer = o.optJSONObject("transfer")?.let {
                    TripTransfer(
                        nextRoute = it.getString("route"),
                        nextHeadsign = it.getString("headsign"),
                        nextDeparture = it.getInt("t"),
                        walkMinutes = it.getInt("walk"),
                        finalDestination = it.getString("final"),
                        free = if (it.isNull("free")) null else it.getBoolean("free"),
                    )
                },
            )
        }.getOrNull()
    }
}

/** Como a posição atual foi descoberta. [SIMULATED]: viagem de simulação, só pela tabela. */
enum class TripFix { GPS, SCHEDULE, SIMULATED }

/** Onde a viagem está agora. */
data class TripProgress(
    /** Índice em [TripPlan.stops] da última estação alcançada (-1 = ainda não partiu). */
    val index: Int,
    val fix: TripFix,
    /** Atraso medido pelo GPS em relação à grade, em minutos (positivo = atrasado). */
    val delayMinutes: Double,
    /** Previsão de chegada ao destino, em minutos desde a meia-noite. */
    val arrivalMinutes: Double,
    /**
     * Há quantos minutos não chega posição de GPS boa, quando passou de
     * [TripEstimator.GPS_LOST_MINUTES]; `null` = GPS respondendo.
     */
    val gpsSilentMinutes: Double? = null,
) {
    /** A posição é presumida pela tabela de horários, não vista pelo GPS. */
    val presumed: Boolean get() = fix == TripFix.SCHEDULE
    fun stationsLeft(plan: TripPlan): Int = (plan.stops.lastIndex - index.coerceAtLeast(0)).coerceAtLeast(0)

    /**
     * A próxima parada, já no metrô. `null` antes de embarcar e quando a próxima
     * é o destino: "destino em 1 estação" já diz isso.
     */
    fun nextStop(plan: TripPlan): TripStop? =
        plan.stops.getOrNull(index + 1)?.takeIf { index >= 0 && stationsLeft(plan) >= 2 }
}

/**
 * Acompanha a viagem. O GPS manda quando é confiável e está perto de uma
 * estação; quando ele some (dentro do trem, no trecho subterrâneo), a posição
 * sai da grade de horários, corrigida pelo atraso que o GPS mediu da última vez.
 * A posição nunca anda para trás.
 *
 * Lições do campo (04/11, Jereissati → Chico da Silva):
 * - com o satélite respondendo, a grade não anda sozinha: o metrô passou da
 *   origem, ficou 6 min parado em Carlito (fora da viagem) e a grade já o punha
 *   em Maracanaú e Virgílio Távora;
 * - o atraso vale pela chegada à estação, não pela saída: o tempo parado na
 *   plataforma não é atraso;
 * - no túnel o metrô recupera tempo (a grade folga entre Padre Cícero e Chico
 *   da Silva) e a estimativa ficava duas estações atrás; sem satélite, a
 *   chegada às estações subterrâneas adianta [TUNNEL_EARLY_MINUTES]: o aviso de
 *   descida tem de vir cedo, nunca tarde;
 * - no túnel, a rede de celular com até [LOOSE_ACCURACY_METERS] já serve para
 *   andar para a frente (só para estação subterrânea);
 * - uma posição de satélite solta, depois de um tempo sem sinal, pode ser velha
 *   (veio uma de Padre Cícero com o metrô já perto de Chico): só conta a partir
 *   da segunda seguida.
 */
class TripEstimator(
    private val plan: TripPlan,
    /** Sem posição boa por este tempo, o GPS é dado como perdido (configurável). */
    private val gpsLostMinutes: Double = GPS_LOST_MINUTES,
) {

    private var index = -1
    private var delay = 0.0
    private var lastFix = if (plan.simulated) TripFix.SIMULATED else TripFix.SCHEDULE
    private var startedAt: Double? = null
    private var lastGoodGps: Double? = null
    private var lastSatellite: Double? = null
    /** Posições de satélite seguidas, sem um buraco de [gpsLostMinutes]. */
    private var satelliteRun = 0
    /** O GPS já pôs a pessoa numa estação da viagem pelo menos uma vez. */
    private var seenAtStation = false
    /** A última estação em que o GPS viu a pessoa, e quando. */
    private var lastStationFix: Pair<Int, Double>? = null

    /** Satélite respondendo agora, com pelo menos duas posições seguidas. */
    private fun satelliteAlive(nowMinutes: Double): Boolean {
        val last = lastSatellite ?: return false
        return satelliteRun >= 2 && nowMinutes - last < gpsLostMinutes
    }

    /**
     * Uma posição. Só conta se for precisa e estiver perto de uma estação da
     * viagem. [fromNetwork]: veio da rede de celular/Wi-Fi, não do satélite.
     */
    fun onLocation(lat: Double, lon: Double, accuracyMeters: Float, nowMinutes: Double, fromNetwork: Boolean = false) {
        if (plan.simulated) return
        if (accuracyMeters > MAX_ACCURACY_METERS) {
            // Sem satélite (túnel), a rede de celular imprecisa ainda serve para
            // andar para a frente: em Benfica ela acertou a estação com 200 m.
            if (fromNetwork && accuracyMeters <= LOOSE_ACCURACY_METERS) onLooseNetwork(lat, lon, nowMinutes)
            return
        }
        // A posição pela rede erra até ~150 m e já pôs a pessoa na estação
        // errada; com satélite recente, ela é descartada. Sem satélite (túnel),
        // ainda serve.
        if (fromNetwork) {
            val satellite = lastSatellite
            if (satellite != null && nowMinutes - satellite <= PREFER_SATELLITE_MINUTES) return
            lastGoodGps = nowMinutes
        } else {
            val previous = lastSatellite
            satelliteRun = if (previous != null && nowMinutes - previous < gpsLostMinutes) satelliteRun + 1 else 1
            lastSatellite = nowMinutes
            // Posição boa, mesmo longe de estação: o GPS está alcançável. Uma
            // posição solta depois de um buraco ainda não prova isso.
            if (satelliteRun >= 2 || seenAtStation.not()) lastGoodGps = nowMinutes
        }
        // Numa viagem pelo terminal a mesma estação aparece duas vezes (ida e
        // volta): das que estão no raio, vale a próxima à frente; só sem
        // nenhuma à frente olhamos para trás.
        val inRange = plan.stops.withIndex()
            .map { (i, stop) -> i to distanceMeters(lat, lon, stop.lat, stop.lon) }
            .filter { it.second <= STATION_RADIUS_METERS }
        if (inRange.isEmpty()) return
        val (nearest, distance) = inRange.filter { it.first >= index }.minByOrNull { it.first }
            ?: inRange.maxBy { it.first }
        // Voltar só com GPS muito bom e firme: é o caso de quem ainda espera na
        // estação um trem atrasado, enquanto a grade já "andou" sozinha.
        val confident = accuracyMeters <= CONFIDENT_ACCURACY_METERS && distance <= CONFIDENT_RADIUS_METERS &&
            (fromNetwork || satelliteRun >= 2)
        if (nearest < index && !confident) return
        val arriving = nearest != lastStationFix?.first
        index = nearest
        lastFix = TripFix.GPS
        seenAtStation = true
        lastStationFix = nearest to nowMinutes
        // Estar na estação i agora quer dizer atraso de (agora − horário em i).
        // Vale a chegada: o tempo parado na plataforma não é atraso. Na origem é
        // o contrário: chegar cedo não é adiantamento (o trem ainda vai passar) e
        // continuar lá depois do horário é atraso da partida.
        if (nearest == 0) {
            updateDelay((nowMinutes - plan.stops[0].scheduled).coerceAtLeast(0.0))
        } else if (arriving) {
            updateDelay(nowMinutes - plan.stops[nearest].scheduled)
        }
    }

    private fun onLooseNetwork(lat: Double, lon: Double, nowMinutes: Double) {
        if (!seenAtStation) return
        val satellite = lastSatellite
        if (satellite != null && nowMinutes - satellite < gpsLostMinutes) return
        val ahead = plan.stops.withIndex()
            .filter { (i, stop) ->
                // Só debaixo da terra: na superfície a antena perto da estação já
                // a deu 1 min antes do metrô chegar (Conjunto Esperança, 04/11).
                i > index && stop.underground && distanceMeters(lat, lon, stop.lat, stop.lon) <= STATION_RADIUS_METERS
            }
            .minByOrNull { it.index } ?: return
        index = ahead.index
        lastFix = TripFix.SCHEDULE
        lastStationFix = ahead.index to nowMinutes
        updateDelay(nowMinutes - ahead.value.scheduled)
    }

    private fun updateDelay(measured: Double) {
        if (kotlin.math.abs(measured) <= MAX_DELAY_MINUTES) delay = measured
    }

    /**
     * O GPS ainda vê a pessoa na estação de partida (há no máximo 1 minuto),
     * quando o trem já devia ter saído há [afterMinutes]: ela perdeu o trem.
     */
    fun missedDeparture(nowMinutes: Double, afterMinutes: Double): Boolean {
        val (station, at) = lastStationFix ?: return false
        return !plan.simulated && station == 0 && nowMinutes - at <= 1.0 &&
            nowMinutes >= plan.origin.scheduled + afterMinutes
    }

    /** O estado agora, avançando pela grade quando o GPS não diz nada novo. */
    fun progress(nowMinutes: Double): TripProgress {
        // No terminal o trem espera o horário de saída: passado dele, um
        // adiantamento medido na ida não vale mais.
        if (plan.viaTerminal && index >= plan.turnaroundIndex && delay < 0) delay = 0.0
        val started = startedAt ?: nowMinutes.also { startedAt = it }
        val since = lastGoodGps ?: started
        // Antes do primeiro sinal numa estação da viagem, a grade não sabe se a
        // pessoa já embarcou: não passa da partida. Com o GPS respondendo longe
        // da linha (ela ainda vai até a estação), espera; só se o GPS sumir
        // (começou já no túnel) a grade volta a andar. Na simulação não há GPS.
        val gpsAlive = nowMinutes - since < gpsLostMinutes
        val waitingFirstFix = !plan.simulated && !seenAtStation && gpsAlive
        // Já embarcado e com o satélite respondendo, só o GPS faz a viagem andar:
        // cada estação aparece no raio dele. Enquanto o metrô não chega à
        // próxima, o atraso é no mínimo o que já passou do horário dela.
        val holding = !plan.simulated && seenAtStation && satelliteAlive(nowMinutes)
        if (holding) {
            plan.stops.getOrNull(index + 1)?.let { next ->
                val late = nowMinutes - next.scheduled
                if (late > delay && late <= MAX_DELAY_MINUTES) delay = late
            }
        }
        // No túnel, sem satélite, depois de o GPS ter visto o metrô andando (já
        // fora da origem), adianta: o aviso tem de vir cedo, nunca tarde. Na
        // superfície o GPS falha pouco e o metrô não recupera tempo assim.
        val moving = (lastStationFix?.first ?: 0) >= 1
        val early = if (!plan.simulated && moving && !holding) TUNNEL_EARLY_MINUTES else 0.0
        val bySchedule = when {
            waitingFirstFix || holding -> -1
            else -> plan.stops.indexOfLast { it.scheduled + delay - (if (it.underground) early else 0.0) <= nowMinutes }
        }
        if (bySchedule > index) {
            index = bySchedule
            lastFix = if (plan.simulated) TripFix.SIMULATED else TripFix.SCHEDULE
        }
        val silent = if (plan.simulated) 0.0 else nowMinutes - since
        val lost = silent >= gpsLostMinutes
        return TripProgress(
            index = index,
            // Sem GPS, a posição é presumida, mesmo que a estação tenha vindo dele.
            fix = if (lost && lastFix == TripFix.GPS) TripFix.SCHEDULE else lastFix,
            delayMinutes = delay,
            arrivalMinutes = plan.destination.scheduled + delay,
            gpsSilentMinutes = silent.takeIf { lost },
        )
    }

    companion object {
        /**
         * Padrão: sem posição boa por 1 minuto (12 tentativas, uma a cada 5 s),
         * o GPS é dado como inalcançável. Ajustável nas configurações. É menos que o tempo entre duas estações
         * (~2 min), então o aviso aparece antes de a estimativa errar de estação.
         */
        const val GPS_LOST_MINUTES = 1.0

        private const val MAX_ACCURACY_METERS = 150f
        /** Rede de celular no túnel: até isto ainda serve para andar para a frente. */
        const val LOOSE_ACCURACY_METERS = 300f
        /** Sem satélite, a estimativa adianta isto (no túnel o metrô recupera tempo). */
        const val TUNNEL_EARLY_MINUTES = 1.5
        private const val STATION_RADIUS_METERS = 300.0
        private const val CONFIDENT_ACCURACY_METERS = 40f
        private const val CONFIDENT_RADIUS_METERS = 150.0
        private const val MAX_DELAY_MINUTES = 30.0
        /** Posição de satélite com menos que isso faz a da rede ser ignorada. */
        private const val PREFER_SATELLITE_MINUTES = 0.25
    }
}
