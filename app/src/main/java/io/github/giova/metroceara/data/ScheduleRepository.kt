package io.github.giova.metroceara.data

import android.content.Context
import android.database.Cursor

/** Consultas de leitura sobre o banco de horários em uso (baixado ou embarcado). */
class ScheduleRepository(context: Context) {

    private val opened = MetroforDatabase.open(context)
    private val db = opened.first

    /** De onde vêm os horários deste repositório. */
    val source: ScheduleSource = opened.second

    /** A grade provável de amanhã, ou `null` se não há como saber (ou amanhã é domingo). */
    private val tomorrowDb = MetroforDatabase.openTomorrow(context)

    /** Amanhã é domingo: o próximo dia com metrô é a segunda, um dia útil. */
    private val mondayDb = if (ScheduleStore.tomorrowType() == DayType.SUNDAY) {
        MetroforDatabase.openType(context, DayType.WEEKDAY)
    } else {
        null
    }

    fun routes(): List<Route> = db.rawQuery(
        """
        SELECT route_id, name, description, color
        FROM route
        ORDER BY sort_order
        """.trimIndent(),
        null,
    ).map { Route(it.getString(0), it.getString(1), it.getString(2), it.getString(3)) }

    fun route(id: String): Route? = db.rawQuery(
        "SELECT route_id, name, description, color FROM route WHERE route_id = ?",
        arrayOf(id),
    ).map { Route(it.getString(0), it.getString(1), it.getString(2), it.getString(3)) }
        .firstOrNull()

    fun stop(id: String): Stop? = db.rawQuery(
        "SELECT stop_id, name FROM stop WHERE stop_id = ?",
        arrayOf(id),
    ).map { Stop(it.getString(0), it.getString(1), seq = 0) }.firstOrNull()

    fun stationCount(routeId: String): Int = db.rawQuery(
        "SELECT COUNT(DISTINCT stop_id) FROM route_stop WHERE route_id = ?",
        arrayOf(routeId),
    ).map { it.getInt(0) }.firstOrNull() ?: 0

    fun directions(routeId: String): List<Direction> = db.rawQuery(
        """
        SELECT direction_id, headsign
        FROM direction
        WHERE route_id = ?
        ORDER BY direction_id
        """.trimIndent(),
        arrayOf(routeId),
    ).map { Direction(it.getInt(0), it.getString(1)) }

    /**
     * Os sentidos em que dá para embarcar em [stopId]: o que termina ali só tem
     * chegadas e fica de fora (a não ser que seja o único). Se a grade do dia só
     * traz um sentido (Linha Norte de Sobral, nos dias úteis), a volta entra sem
     * horários, para a tela dizer que o Metrofor não a publicou em vez de sumir com ela.
     */
    fun boardingDirections(stopId: String, routeId: String): List<Direction> {
        val listed = directions(routeId)
        val missing = listed.singleOrNull()?.let { only ->
            stops(routeId, only.id).takeIf { it.size >= 2 && it.first().id != stopId }
                ?.let { Direction(1 - only.id, it.first().name, published = false) }
        }
        return (listed.filterNot { endsAt(stopId, routeId, it.id) } + listOfNotNull(missing)).ifEmpty { listed }
    }

    /** Estações na ordem em que [routeId] as percorre no sentido [directionId]. */
    fun stops(routeId: String, directionId: Int): List<Stop> = db.rawQuery(
        """
        SELECT s.stop_id, s.name, rs.seq
        FROM route_stop rs
        JOIN stop s ON s.stop_id = rs.stop_id
        WHERE rs.route_id = ? AND rs.direction_id = ?
        ORDER BY rs.seq
        """.trimIndent(),
        arrayOf(routeId, directionId.toString()),
    ).map { Stop(it.getString(0), it.getString(1), it.getInt(2)) }

    /** Todas as partidas do dia, em minutos desde a meia-noite, já ordenadas. */
    fun departures(stopId: String, routeId: String, directionId: Int): List<Int> =
        departuresIn(db, stopId, routeId, directionId)

    /**
     * As partidas de amanhã, pela grade do tipo de dia de amanhã — e não a de
     * hoje repetida, que num dia especial (eleição) daria horário errado.
     * `null` quando não há grade para saber.
     */
    fun departuresTomorrow(stopId: String, routeId: String, directionId: Int): List<Int>? =
        tomorrowDb?.let { departuresIn(it, stopId, routeId, directionId) }

    /** O último horário de hoje e o primeiro do próximo dia com metrô, para quando hoje acabou. */
    fun dayEnd(stopId: String, routeId: String, directionId: Int): DayEnd {
        val last = departures(stopId, routeId, directionId).lastOrNull()
        val monday = tomorrowDb == null && mondayDb != null
        val next = (tomorrowDb ?: mondayDb)?.let { departuresIn(it, stopId, routeId, directionId).firstOrNull() }
        return DayEnd(last, next, monday)
    }

    private fun departuresIn(
        db: android.database.sqlite.SQLiteDatabase,
        stopId: String,
        routeId: String,
        directionId: Int,
    ): List<Int> = db.rawQuery(
        """
        SELECT minutes
        FROM departure
        WHERE stop_id = ? AND route_id = ? AND direction_id = ?
        ORDER BY minutes
        """.trimIndent(),
        arrayOf(stopId, routeId, directionId.toString()),
    ).map { it.getInt(0) }

    /**
     * Viagem que sai de [stopId] na primeira partida a partir de [fromMinutes]:
     * o id e o horário programado em cada estação. `null` se não há mais
     * viagem hoje.
     */
    fun nextTrip(stopId: String, routeId: String, directionId: Int, fromMinutes: Int): Pair<String, Map<String, Int>>? {
        val tripId = db.rawQuery(
            """
            SELECT trip_id FROM departure
            WHERE stop_id = ? AND route_id = ? AND direction_id = ? AND minutes >= ?
            ORDER BY minutes LIMIT 1
            """.trimIndent(),
            arrayOf(stopId, routeId, directionId.toString(), fromMinutes.toString()),
        ).map { it.getString(0) }.firstOrNull() ?: return null
        val times = db.rawQuery(
            "SELECT stop_id, minutes FROM departure WHERE trip_id = ? AND route_id = ?",
            arrayOf(tripId, routeId),
        ).map { it.getString(0) to it.getInt(1) }.toMap()
        return tripId to times
    }

    /**
     * As estações de [fromId] até [toId] no sentido dado, com o horário de um
     * trem ([times]) em cada uma: o trecho que o modo viagem acompanha.
     */
    fun tripStops(routeId: String, directionId: Int, fromId: String, toId: String, times: Map<String, Int>): List<TripStop>? {
        val coords = stations().filter { it.routeId == routeId }.associateBy { it.stopId }
        val path = stops(routeId, directionId).dropWhile { it.id != fromId }
        val end = path.indexOfFirst { it.id == toId }
        if (path.isEmpty() || end < 0) return null
        return path.take(end + 1).map { stop ->
            val station = coords[stop.id] ?: return null
            val time = times[stop.id] ?: return null
            TripStop(stop.id, stop.name, station.lat, station.lon, time)
        }
    }

    /** Os horários de uma viagem, por estação (para montar o trecho de um roteiro). */
    fun tripTimes(tripId: String, routeId: String): Map<String, Int> = db.rawQuery(
        "SELECT stop_id, minutes FROM departure WHERE trip_id = ? AND route_id = ?",
        arrayOf(tripId, routeId),
    ).map { it.getString(0) to it.getInt(1) }.toMap()

    /** A grade do dia vista pelo [Router]: viagens que passam por uma estação e depois por outra. */
    val timetable = object : Timetable {
        override fun trips(routeId: String, fromStop: String, toStop: String): List<LegTrip> = db.rawQuery(
            """
            SELECT a.trip_id, a.direction_id, a.minutes, b.minutes
            FROM departure a
            JOIN departure b ON b.trip_id = a.trip_id AND b.route_id = a.route_id AND b.stop_id = ?
            WHERE a.route_id = ? AND a.stop_id = ? AND b.minutes > a.minutes
            ORDER BY a.minutes
            """.trimIndent(),
            arrayOf(toStop, routeId, fromStop),
        ).map { LegTrip(it.getString(0), it.getInt(1), it.getInt(2), it.getInt(3)) }
    }

    /**
     * Onde dá para trocar de linha: as integrações do regras.json (tabela
     * transfer) e a estação que serve duas linhas (Campo dos Velhos, em Sobral),
     * onde a troca é na mesma plataforma.
     */
    fun transferLinks(): List<TransferLink> {
        val routesOf = db.rawQuery("SELECT DISTINCT stop_id, route_id FROM route_stop", null)
            .map { it.getString(0) to it.getString(1) }
            .groupBy({ it.first }, { it.second })
        val declared = db.rawQuery("SELECT from_stop_id, to_stop_id, walk_minutes FROM transfer", null)
            .map { Triple(it.getString(0), it.getString(1), it.getInt(2)) }
            .flatMap { (from, to, walk) ->
                routesOf[from].orEmpty().flatMap { a ->
                    routesOf[to].orEmpty().filter { it != a }.map { b -> TransferLink(a, from, b, to, walk) }
                }
            }
        val shared = routesOf.filterValues { it.size > 1 }.flatMap { (stop, routes) ->
            routes.flatMap { a -> routes.filter { it != a }.map { b -> TransferLink(a, stop, b, stop, SAME_PLATFORM_MINUTES) } }
        }
        return declared + shared
    }

    /**
     * Baldeações a partir de [stopId]: a estação da outra linha, o tempo a pé e
     * os sentidos em que dá para **partir** de lá — nos terminais, o sentido que
     * termina na própria estação é só chegada e fica de fora.
     */
    fun transfersFrom(stopId: String): List<TransferTarget> = db.rawQuery(
        """
        SELECT t.to_stop_id, s.name, t.walk_minutes, rs.route_id, r.name, r.color, rs.direction_id, d.headsign,
               f.name
        FROM transfer t
        JOIN stop s ON s.stop_id = t.to_stop_id
        JOIN stop f ON f.stop_id = t.from_stop_id
        JOIN route_stop rs ON rs.stop_id = t.to_stop_id
        JOIN route r ON r.route_id = rs.route_id
        JOIN direction d ON d.route_id = rs.route_id AND d.direction_id = rs.direction_id
        WHERE t.from_stop_id = ?
          AND rs.seq < (SELECT MAX(x.seq) FROM route_stop x
                        WHERE x.route_id = rs.route_id AND x.direction_id = rs.direction_id)
        ORDER BY r.sort_order, rs.direction_id
        """.trimIndent(),
        arrayOf(stopId),
    ).map {
        TransferTarget(
            stopId = it.getString(0),
            stopName = it.getString(1),
            walkMinutes = it.getInt(2),
            routeId = it.getString(3),
            routeName = it.getString(4),
            routeColor = it.getString(5),
            direction = Direction(it.getInt(6), it.getString(7)),
            fare = transferRule(it.getString(8), it.getString(1)),
        )
    }

    /**
     * Se o sentido termina nesta estação (ela é o terminal dele): ali o
     * "sentido" só tem chegadas, nenhuma partida que sirva a quem embarca.
     */
    fun endsAt(stopId: String, routeId: String, directionId: Int): Boolean = db.rawQuery(
        """
        SELECT stop_id FROM route_stop
        WHERE route_id = ? AND direction_id = ?
        ORDER BY seq DESC LIMIT 1
        """.trimIndent(),
        arrayOf(routeId, directionId.toString()),
    ).map { it.getString(0) }.firstOrNull() == stopId

    /**
     * Todas as combinações estação/linha, para a busca da mais próxima. A posição
     * vem do GTFS, a não ser que regras.json a corrija (Sobral e a Escola do Cariri).
     */
    fun stations(): List<Station> = db.rawQuery(
        """
        SELECT s.stop_id, s.name, s.lat, s.lon, r.route_id, r.name, r.color
        FROM route_stop rs
        JOIN stop s ON s.stop_id = rs.stop_id
        JOIN route r ON r.route_id = rs.route_id
        GROUP BY s.stop_id, r.route_id
        ORDER BY r.sort_order
        """.trimIndent(),
        null,
    ).map {
        val fix = Regras.current.station(it.getString(0), it.getString(1))
        Station(
            stopId = it.getString(0),
            stopName = it.getString(1),
            lat = fix?.lat ?: it.getDouble(2),
            lon = fix?.lon ?: it.getDouble(3),
            routeId = it.getString(4),
            routeName = it.getString(5),
            routeColor = it.getString(6),
        )
    }

    fun station(stopId: String, routeId: String): Station? =
        stations().firstOrNull { it.stopId == stopId && it.routeId == routeId }

    fun bikeStations(): List<BikeStation> = db.rawQuery(
        "SELECT number, name, district, slots, lat, lon FROM bike_station",
        null,
    ).map {
        BikeStation(
            number = it.getInt(0),
            name = it.getString(1),
            district = it.getString(2),
            slots = it.getInt(3),
            lat = it.getDouble(4),
            lon = it.getDouble(5),
        )
    }

    fun meta(key: String): String? = db.rawQuery(
        "SELECT value FROM meta WHERE key = ?",
        arrayOf(key),
    ).map { it.getString(0) }.firstOrNull()

    private companion object {
        /** Trocar de linha na mesma estação ainda leva um minuto. */
        const val SAME_PLATFORM_MINUTES = 1
    }

    private fun <T> Cursor.map(read: (Cursor) -> T): List<T> = use { cursor ->
        buildList(cursor.count) {
            while (cursor.moveToNext()) add(read(cursor))
        }
    }
}
