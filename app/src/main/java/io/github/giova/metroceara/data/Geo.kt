package io.github.giova.metroceara.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Distância em metros entre duas coordenadas, pela fórmula de haversine. */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
}

/**
 * A estação mais próxima de ([lat], [lon]), com a distância em metros.
 *
 * Quando a mesma parada é servida por mais de uma linha, vence a primeira da
 * lista — que vem ordenada pela ordem oficial das linhas.
 */
fun List<Station>.nearestTo(lat: Double, lon: Double): Pair<Station, Double>? =
    map { it to distanceMeters(lat, lon, it.lat, it.lon) }
        .minByOrNull { (_, distance) -> distance }

/** A estação mais próxima de cada linha, da mais perto para a mais longe. */
fun List<Station>.nearestPerRoute(lat: Double, lon: Double): List<Pair<Station, Double>> =
    map { it to distanceMeters(lat, lon, it.lat, it.lon) }
        .groupBy { (station, _) -> station.routeId }
        .map { (_, candidates) -> candidates.minBy { it.second } }
        .sortedBy { it.second }

/**
 * As duas estações da tela inicial: a mais próxima e, até [maxMeters], a mais
 * próxima de **outra linha**; sem outra linha por perto, a segunda mais próxima
 * da mesma linha. A mesma parada em duas linhas (Parangaba na Sul e na
 * Nordeste) conta como duas estações.
 */
fun List<Station>.nearestTwo(lat: Double, lon: Double, maxMeters: Double): List<Pair<Station, Double>> {
    val sorted = map { it to distanceMeters(lat, lon, it.lat, it.lon) }
        .distinctBy { (station, _) -> station.stopId to station.routeId }
        .sortedBy { it.second }
    val first = sorted.firstOrNull() ?: return emptyList()
    val rest = sorted.drop(1).filter { it.second <= maxMeters }
    val second = rest.firstOrNull { it.first.routeId != first.first.routeId } ?: rest.firstOrNull()
    return listOfNotNull(first, second)
}

/**
 * Padrão da distância a pé que ainda vale a pena para pegar uma bicicleta perto
 * de uma estação. A pessoa ajusta nas configurações ([AppSettings]).
 */
const val BIKE_MAX_METERS = 600.0

/** A estação de bicicleta mais próxima de ([lat], [lon]), dentro de [maxMeters]. */
fun List<BikeStation>.nearestBikeTo(
    lat: Double,
    lon: Double,
    maxMeters: Double = BIKE_MAX_METERS,
): Pair<BikeStation, Double>? =
    map { it to distanceMeters(lat, lon, it.lat, it.lon) }
        .filter { (_, distance) -> distance <= maxMeters }
        .minByOrNull { (_, distance) -> distance }
