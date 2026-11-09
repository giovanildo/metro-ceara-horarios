package io.github.giova.metroceara.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.alerts.OperationAlerts
import io.github.giova.metroceara.data.DayType
import io.github.giova.metroceara.data.Landmark
import io.github.giova.metroceara.data.TomorrowStatus
import io.github.giova.metroceara.data.near
import io.github.giova.metroceara.data.tomorrowStatus
import io.github.giova.metroceara.data.walkMinutes
import io.github.giova.metroceara.data.LeaveScheduler
import io.github.giova.metroceara.data.LeaveStore
import io.github.giova.metroceara.data.Routine
import androidx.compose.runtime.key
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.RecentTrip
import io.github.giova.metroceara.data.RecentTrips
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.AccessWay
import io.github.giova.metroceara.data.transferRule
import io.github.giova.metroceara.data.transferMinutes
import io.github.giova.metroceara.data.nowDayOfWeek
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.bikeRuleFor
import io.github.giova.metroceara.data.bikeBoarding
import io.github.giova.metroceara.data.Router
import io.github.giova.metroceara.data.RoutePoint
import io.github.giova.metroceara.data.Mobility
import io.github.giova.metroceara.data.Itinerary
import androidx.compose.runtime.produceState
import io.github.giova.metroceara.data.distanceMeters
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Direction
import io.github.giova.metroceara.data.Stop
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.TripStop
import io.github.giova.metroceara.trip.TripTracker
import io.github.giova.metroceara.widget.StationWidget
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.LocationSource
import io.github.giova.metroceara.data.ShownFix
import io.github.giova.metroceara.data.keepsOver
import io.github.giova.metroceara.data.logFields
import io.github.giova.metroceara.data.MetroforDatabase
import io.github.giova.metroceara.data.PinnedStation
import io.github.giova.metroceara.data.Place
import io.github.giova.metroceara.data.Places
import io.github.giova.metroceara.data.ScheduleRepository
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.ItineraryLeg
import io.github.giova.metroceara.data.MondayReturn
import io.github.giova.metroceara.data.isSameStation
import io.github.giova.metroceara.data.isSundayWithoutService
import io.github.giova.metroceara.data.seatedSearchFrom
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.nearestBikeTo
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.nearestStations
import io.github.giova.metroceara.data.CommuteKind
import io.github.giova.metroceara.data.CommuteSchedule
import io.github.giova.metroceara.data.CommuteStore
import io.github.giova.metroceara.data.commuteDestination
import io.github.giova.metroceara.data.pickCommute
import io.github.giova.metroceara.data.suggestionsFrom
import io.github.giova.metroceara.data.viaStation
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.data.HomeBlock
import io.github.giova.metroceara.data.HomeLayout
import io.github.giova.metroceara.data.HomeLayoutStore
import io.github.giova.metroceara.data.nextDepartures
import io.github.giova.metroceara.data.nowMinutes
import java.util.Calendar
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TICK_MILLIS = 15_000L

/** GPS da tela inicial: no intervalo das configurações, ou ao andar 30 m. */
private const val HOME_GPS_MIN_METERS = 30f

private const val USER_POSITION_MAX_AGE_MILLIS = 10L * 60 * 1000

/** Andando a uns 4,5 km/h, para contar a caminhada até a estação de partida do roteiro. */
private const val WALK_METERS_PER_MINUTE = 75.0

/** Até essa caminhada, a pessoa já está na estação. */
private const val ORIGIN_WALK_FREE_MINUTES = 3

/** Pedido de abrir uma estação vindo de fora da tela (toque no widget). */
val OpenStationRequest = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>(null)

/**
 * Pedido de abrir o roteiro vindo do aviso da hora de sair ("Iniciar modo
 * viagem"): estação e linha de origem, estação e linha de destino.
 */
val JourneyRequest = kotlinx.coroutines.flow.MutableStateFlow<List<String>?>(null)


private fun formatKm(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(meters / 1000).replace('.', ',')



@Composable
fun AppRoot() {
    val context = LocalContext.current
    val settings = SettingsStore.flow(context).collectAsState().value ?: SettingsStore.get(context)

    // O GTFS do Metrofor traz a grade do dia; baixamos uma vez por dia e o
    // repositório é recriado quando chega grade nova ou o dia vira.
    var today by remember { mutableStateOf(ScheduleStore.today()) }
    var scheduleVersion by remember { mutableIntStateOf(0) }
    var scheduleSyncing by remember { mutableStateOf(false) }
    // Cada vez que o app abre ou volta para a frente, baixa a grade do dia do
    // Metrofor (é pequena). Um download cortado ao fechar o app é refeito aqui.
    var opened by remember { mutableIntStateOf(0) }
    // Com o app aberto na virada das 00:40, baixa a grade do dia sem esperar reabrir.
    var feedReady by remember { mutableStateOf(!ScheduleStore.beforeFeedTime()) }
    var notices by remember { mutableStateOf(ScheduleStore.notices()) }
    LaunchedEffect(today, opened, feedReady) {
        scheduleSyncing = true
        DevLog.log("sync_start")
        // As regras vêm antes: a montagem da grade usa as integrações delas.
        withContext(Dispatchers.IO) {
            runCatching { ScheduleStore.syncRules(context) }
                .onFailure { DevLog.log("regras_sync_fail", "reason" to (it.message ?: it.javaClass.simpleName)) }
        }
        // Domingo sem operação anunciada não tem metrô: a grade que o Metrofor serve
        // nesses dias é cópia da de dia útil e não deve ser guardada como "domingo".
        // As regras acabaram de chegar, então uma operação anunciada já conta.
        val result = if (isSundayWithoutService()) {
            DevLog.log("sync_skip", "reason" to "domingo sem operação")
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { ScheduleStore.sync(context, MetroforDatabase.bundledFile(context)) }
            }
        }
        result?.exceptionOrNull()?.let { DevLog.log("sync_fail", "reason" to (it.message ?: it.javaClass.simpleName)) }
        scheduleSyncing = false
        if (result?.isSuccess == true) scheduleVersion++
        // As notícias de operação especial vêm no regras.json, já baixado acima.
        notices = ScheduleStore.notices()
        // Com o app aberto o aviso está na tela: não precisa de notificação depois.
        OperationAlerts.markSeen(context)
        StationWidget.update(context)
    }

    // A primeira abertura copia o banco de assets; isso sai da thread principal.
    var repository by remember { mutableStateOf<ScheduleRepository?>(null) }
    LaunchedEffect(context, today, scheduleVersion) {
        repository = withContext(Dispatchers.IO) { ScheduleRepository(context) }.also {
            DevLog.log("schedule_source", "kind" to it.source.kind.name, "date" to it.source.date)
        }
    }

    val repo = repository
    if (repo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Guardamos apenas os ids para a navegação sobreviver à rotação de tela.
    var routeId by rememberSaveable { mutableStateOf<String?>(null) }
    var stopId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var fareGuideOpen by rememberSaveable { mutableStateOf(false) }
    var sourcesOpen by rememberSaveable { mutableStateOf(false) }
    var accessOpen by rememberSaveable { mutableStateOf(false) }
    // "Ajustar para mim" e "Ordem da tela inicial".
    var adjustOpen by rememberSaveable { mutableStateOf(false) }
    var styleOpen by rememberSaveable { mutableStateOf(false) }
    var orderOpen by rememberSaveable { mutableStateOf(false) }
    // Aba da barra de baixo (Início, Rotas, Linhas): volta a ela depois de abrir uma linha.
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    // Na tela inicial, a estação aberta além da mais perto ("stopId+routeId"); null = a mais perto.
    var expandedStation by rememberSaveable { mutableStateOf<String?>(null) }
    val homeLayout = HomeLayoutStore.flow(context).collectAsState().value ?: HomeLayout()
    // Sentido em que a tela de horários deve abrir (vindo de uma baldeação).
    var openDirectionId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Toque no widget: abre a tela da estação dele.
    val widgetRequest by OpenStationRequest.collectAsState()

    val routes = remember(repo) { repo.routes() }
    val bikeStations = remember(repo) { repo.bikeStations() }

    val route = routeId?.let { id -> remember(id) { repo.route(id) } }
    val stop = stopId?.let { id -> remember(id) { repo.stop(id) } }

    // ---------------------------------------------------------- tela inicial --
    val scope = rememberCoroutineScope()
    val pinned = remember(context) { PinnedStation(context) }
    val locationSource = remember(context) { LocationSource(context) }
    var homeState by remember { mutableStateOf<HomeStation>(HomeStation.Idle) }
    var now by remember { mutableIntStateOf(nowMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            now = nowMinutes()
            today = ScheduleStore.today()
            feedReady = !ScheduleStore.beforeFeedTime()
        }
    }

    // As estações da tela inicial (até 3): a mais próxima, a de outra linha até a
    // distância das configurações (3 km no padrão) e a seguinte.
    fun nearbyState(stations: List<Station>, lat: Double, lon: Double): HomeStation.Nearby? {
        val nearest = stations.nearestStations(lat, lon, settings.nearbyLinesMeters.toDouble())
        val (main, distance) = nearest.firstOrNull() ?: return null
        return HomeStation.Nearby(main, distance, lat, lon, nearest.drop(1))
    }

    // A posição que está na tela, para uma menos precisa não tomar o lugar dela.
    var shownFix by remember { mutableStateOf<ShownFix?>(null) }

    suspend fun locate(trigger: String = "start") {
        homeState = HomeStation.Locating
        shownFix = null
        val started = android.os.SystemClock.elapsedRealtime()
        DevLog.log(
            "loc_request",
            "trigger" to trigger,
            "fine" to locationSource.hasFinePermission(),
            "providers" to locationSource.enabledProviders(),
        )
        val position = locationSource.current()
        val elapsed = android.os.SystemClock.elapsedRealtime() - started
        if (position == null) {
            DevLog.log("loc_fail", "ms" to elapsed)
            homeState = HomeStation.Unavailable(HomeStation.Reason.NO_FIX)
            return
        }
        DevLog.log("loc_ok", "ms" to elapsed, *position.logFields())
        shownFix = ShownFix(
            position.latitude, position.longitude, position.accuracy, android.os.SystemClock.elapsedRealtime(),
        )
        val stations = withContext(Dispatchers.IO) { repo.stations() }
        homeState = nearbyState(stations, position.latitude, position.longitude)
            ?: HomeStation.Unavailable(HomeStation.Reason.NO_FIX)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch { locate() }
        } else {
            homeState = HomeStation.Unavailable(HomeStation.Reason.PERMISSION_DENIED)
        }
    }

    fun refreshHome() {
        // Uma estação fixada é uma escolha explícita: ela ganha do GPS.
        val chosen = pinned.get()
        if (chosen != null) {
            val station = repo.station(chosen.first, chosen.second)
            if (station != null) {
                homeState = HomeStation.Pinned(station)
                return
            }
            // A estação fixada sumiu do feed; esquecemos e seguimos.
            pinned.clear()
        }
        if (locationSource.hasPermission()) {
            scope.launch { locate() }
        } else if (homeState is HomeStation.Pinned) {
            homeState = HomeStation.Idle
        }
    }

    LaunchedEffect(repo) { refreshHome() }

    // GPS contínuo enquanto a tela está visível e a estação veio do GPS: ao
    // descer em outra estação, o destaque acompanha. Estação fixada ganha do
    // GPS, então aí não seguimos nada.
    val lifecycleOwner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    visible = true
                    // Não reinicia um download em andamento: mudar a chave cancelaria o atual.
                    if (!scheduleSyncing) opened++
                }
                Lifecycle.Event.ON_STOP -> visible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Também depois de uma falha: o GPS frio pode achar satélites só depois de
    // um tempo, e aí a estação aparece sozinha, sem a pessoa tocar em nada.
    val gpsFailed = (homeState as? HomeStation.Unavailable)?.reason == HomeStation.Reason.NO_FIX
    val followGps = visible && (homeState is HomeStation.Nearby || gpsFailed) && locationSource.hasPermission()
    LaunchedEffect(followGps, repo, settings) {
        if (!followGps) return@LaunchedEffect
        val stations = withContext(Dispatchers.IO) { repo.stations() }
        locationSource.updates(settings.homeGpsSeconds * 1000L, HOME_GPS_MIN_METERS).collect { position ->
            val receivedAt = android.os.SystemClock.elapsedRealtime()
            val shown = shownFix
            if (shown.keepsOver(
                    position.latitude, position.longitude, position.accuracy,
                    receivedAt, settings.homeFixHoldSeconds * 1000L,
                )
            ) {
                DevLog.log(
                    "home_gps_skip", *position.logFields(),
                    "kept_acc" to shown?.accuracyMeters,
                    "kept_age_s" to shown?.let { (receivedAt - it.atMillis) / 1000 },
                )
                return@collect
            }
            nearbyState(stations, position.latitude, position.longitude)?.let {
                DevLog.log(
                    "home_gps", *position.logFields(),
                    "station" to it.station.stopName, "dist_m" to it.distanceMeters.toInt(),
                )
                shownFix = ShownFix(position.latitude, position.longitude, position.accuracy, receivedAt)
                homeState = it
            }
        }
    }

    // ------------------------------------------------------------ viagem ----
    // Onde a pessoa está: a posição do GPS da tela inicial, ou, com estação
    // fixada, a última guardada no aparelho. `null` = não sabemos.
    fun userDistanceTo(lat: Double, lon: Double): Double? {
        val (userLat, userLon) = when (val state = homeState) {
            is HomeStation.Nearby -> state.userLat to state.userLon
            else -> locationSource.lastKnown(USER_POSITION_MAX_AGE_MILLIS)
                ?.let { it.latitude to it.longitude }
        } ?: return null
        return distanceMeters(userLat, userLon, lat, lon)
    }

    /** Longe demais (ou posição desconhecida = deixa tentar). */
    fun tripAllowedFrom(lat: Double, lon: Double): Boolean =
        (userDistanceTo(lat, lon) ?: 0.0) <= settings.tripMaxMeters

    var farTrip by remember { mutableStateOf<Pair<TripPlan, Double>?>(null) }
    // Direto x sentado pelo terminal, esperando a escolha da pessoa.
    var tripChoice by remember { mutableStateOf<Pair<TripPlan, TripPlan>?>(null) }

    val activeTrip by TripTracker.active.collectAsState()
    val tripArrival by TripTracker.arrival.collectAsState()
    val tripSound by TripTracker.soundEnabled(context).collectAsState()
    val tripStationAlerts by TripTracker.stationAlerts(context).collectAsState()
    var pendingTrip by remember { mutableStateOf<TripPlan?>(null) }
    var tripMessage by remember { mutableStateOf<String?>(null) }
    val tripPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val plan = pendingTrip
        pendingTrip = null
        // Notificação negada não impede a viagem: o card na tela continua valendo.
        if (plan != null && locationSource.hasPermission()) {
            TripTracker.start(context, plan)
            stopId = null
            routeId = null
        } else if (plan != null) {
            tripMessage = context.getString(R.string.trip_needs_location)
        }
    }

    fun launchTrip(plan: TripPlan) {
        val needed = buildList {
            addAll(LocationSource.PERMISSIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        pendingTrip = plan
        tripPermissionLauncher.launch(needed.toTypedArray())
    }

    // Trecho de uma viagem: as estações de [fromId] até [toId] no sentido dado,
    // com o horário de um trem específico em cada uma.
    fun leg(routeId: String, directionId: Int, fromId: String, toId: String, times: Map<String, Int>): List<TripStop>? =
        repo.tripStops(routeId, directionId, fromId, toId, times)

    // Confere a distância até a partida e começa (ou pergunta antes).
    fun confirmAndLaunch(plan: TripPlan) {
        val distance = userDistanceTo(plan.origin.lat, plan.origin.lon)
        when {
            distance != null && distance > settings.tripMaxMeters -> tripMessage = context.getString(
                R.string.trip_too_far, formatKm(distance), plan.origin.name, formatKm(settings.tripMaxMeters.toDouble()),
            )
            distance != null && distance > settings.tripWarnMeters -> farTrip = plan to distance
            else -> launchTrip(plan)
        }
    }

    fun startTrip(routeId: String, direction: Direction, origin: Stop, destination: Stop, chosen: ItineraryLeg, placeName: String?) {
        val route = repo.route(routeId) ?: return
        val earliest = nowMinutes() - settings.lateBoardingMinutes
        val next = repo.nextTrip(origin.id, routeId, direction.id, earliest)?.second
        // O metrô que a pessoa escolheu; se já passou (tela aberta há tempo), o próximo.
        val times = if (chosen.departure >= earliest) repo.tripTimes(chosen.tripId, routeId) else next
        val direct = times
            ?.let { leg(routeId, direction.id, origin.id, destination.id, it) }
            ?.takeIf { it.size >= 2 }
            ?.let { TripPlan(routeId, route.name, direction.headsign, it, settings.alertStationsBefore, placeName = placeName) }
        if (direct == null) {
            tripMessage = context.getString(R.string.trip_no_trip_today)
            return
        }

        // "Sentado pelo terminal": se o terminal do outro sentido está perto,
        // vai até ele e volta no mesmo trem, que lá esvazia. Em qualquer horário
        // (até a 1.78 era só no pico, e em 04/11 ele quis às 15h e não teve a
        // opção); "Direto" vem primeiro na escolha. Ele não é o metrô escolhido (sai
        // no outro sentido): num horário mais à frente, parte dali em diante.
        val from = seatedSearchFrom(earliest, next?.get(origin.id), chosen.departure)
        val seated = run {
            if (settings.seatedMaxStations <= 0) return@run null
            val back = repo.directions(routeId).firstOrNull { it.id != direction.id } ?: return@run null
            val toTerminal = repo.stops(routeId, back.id).dropWhile { it.id != origin.id }
            val terminal = toTerminal.lastOrNull() ?: return@run null
            if (toTerminal.size - 1 !in 1..settings.seatedMaxStations) return@run null
            val (_, times1) = repo.nextTrip(origin.id, routeId, back.id, from) ?: return@run null
            val going = leg(routeId, back.id, origin.id, terminal.id, times1) ?: return@run null
            val (_, times2) = repo.nextTrip(terminal.id, routeId, direction.id, going.last().scheduled) ?: return@run null
            val returning = leg(routeId, direction.id, terminal.id, destination.id, times2) ?: return@run null
            TripPlan(
                routeId, route.name, direction.headsign, going + returning,
                settings.alertStationsBefore, turnaroundIndex = going.lastIndex, placeName = placeName,
            )
        }
        if (seated == null) confirmAndLaunch(direct) else tripChoice = direct to seated
    }

    // A estação mais perto fica guardada para o widget (quando não há estação
    // fixada nem aberta); grava e redesenha só quando ela muda.
    val nearbyStation = (homeState as? HomeStation.Nearby)?.station
    LaunchedEffect(nearbyStation?.stopId, nearbyStation?.routeId) {
        nearbyStation?.let {
            pinned.setLastNearby(it.stopId, it.routeId)
            StationWidget.update(context)
        }
    }

    // A segunda estação mais próxima, a até 3 km (ganha cartão próprio na tela inicial).
    val otherLines = (homeState as? HomeStation.Nearby)?.others.orEmpty()

    // ------------------------------------------------------ "Sua viagem" ----
    // A tela "Para onde você vai?", aberta da tela inicial (saindo da estação em
    // destaque) ou de uma estação (saindo dela).
    var journeyOpen by remember { mutableStateOf(false) }
    var journeyFrom by remember { mutableStateOf<Station?>(null) }
    // Rotinas e avisos da hora de sair: a lista, a rotina sendo criada/mudada
    // (com "nova?") e, ao abrir o app, os avisos remarcados (por garantia).
    val routinesList by LeaveStore.routines(context).collectAsState()
    val leaveAlerts by LeaveStore.alerts(context).collectAsState()
    var routinesOpen by remember { mutableStateOf(false) }
    var routineEditing by remember { mutableStateOf<Pair<Routine, Boolean>?>(null) }
    // Toque no widget ou no aviso de uma partida: abre a tela da estação, por cima do que estiver aberto.
    LaunchedEffect(widgetRequest) {
        widgetRequest?.let { (stop, route) ->
            openDirectionId = null
            journeyOpen = false
            routinesOpen = false
            routineEditing = null
            showSettings = false
            showAbout = false
            fareGuideOpen = false
            sourcesOpen = false
            accessOpen = false
            adjustOpen = false
            styleOpen = false
            orderOpen = false
            routeId = route
            stopId = stop
            OpenStationRequest.value = null
        }
    }
    // Destino já escolhido (linha, estação), quando vem do aviso da hora de sair.
    var journeyDest by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Vindo de uma viagem recente para um lugar ("Rodoviária"): o nome dele.
    var journeyLandmark by remember { mutableStateOf<String?>(null) }
    val journeyRequest by JourneyRequest.collectAsState()
    LaunchedEffect(journeyRequest) {
        journeyRequest?.let { (originStop, originRoute, destStop, destRoute) ->
            journeyFrom = repo.station(originStop, originRoute)
            journeyDest = destRoute to destStop
            stopId = null
            routeId = null
            // As telas que ficam na frente do roteiro saem do caminho.
            routinesOpen = false
            routineEditing = null
            showSettings = false
            showAbout = false
            fareGuideOpen = false
            sourcesOpen = false
            accessOpen = false
            adjustOpen = false
            styleOpen = false
            orderOpen = false
            journeyOpen = true
            JourneyRequest.value = null
        }
    }

    var askExactForRoutine by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { LeaveScheduler.rescheduleAll(context) } }

    fun saveRoutine(routine: Routine) {
        // Mudou a rotina: o aviso já calculado para hoje é refeito.
        LeaveScheduler.cancelRoutine(context, routine)
        LeaveStore.putRoutine(context, routine)
        LeaveScheduler.planNext(context, routine)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (routine.enabled && !LeaveScheduler.canExact(context)) askExactForRoutine = true
    }

    val router = remember(repo, settings) {
        Router(repo.timetable, repo.transferLinks()) { link, arrival -> transferMinutes(link.baseMinutes, arrival, settings) }
    }
    // O roteiro de amanhã: a grade prevista, ou null quando não há como prever.
    val routerTomorrow = remember(repo, settings) {
        repo.timetableTomorrow?.let { timetable ->
            Router(timetable, repo.transferLinks()) { link, arrival -> transferMinutes(link.baseMinutes, arrival, settings) }
        }
    }
    val stoppedRoutes = remember(repo, today) {
        repo.routes().filter { Regras.current.stopped(today, it.name) }.map { it.id }.toSet()
    }

    // Domingo sem operação anunciada: sem modo viagem e sem roteiro (nenhum horário
    // na tela), só o aviso com a hora em que o metrô volta na segunda.
    val sundayOff = remember(now, today) { isSundayWithoutService() }
    val stoppedTomorrow = remember(repo, today) {
        repo.routes().filter { Regras.current.stopped(ScheduleStore.tomorrow(), it.name) }.map { it.id }.toSet()
    }

    // Minutos a pé da pessoa até a estação (0 sem localização, ou já nela).
    // Longe da estação (além do limite de caminhada), a pessoa está planejando, não
    // indo agora a pé: a caminhada não conta (de casa, a 10 km, daria 2 h a pé).
    fun walkTo(station: Station): Int =
        userDistanceTo(station.lat, station.lon)?.takeIf { it <= settings.walkToStationMeters }
            ?.let { ceil(it / WALK_METERS_PER_MINUTE).toInt() }
            ?.takeIf { it > ORIGIN_WALK_FREE_MINUTES } ?: 0

    // Da estação até um lugar procurado, no passo da pessoa (Configurações > Como você se desloca).
    fun landmarkWalkMinutes(landmark: Landmark): Int =
        landmark.walkMinutes(Regras.current.mobilityFactors[settings.mobility.key] ?: settings.mobility.defaultFactor)

    // As linhas da lista de destinos: as que se alcançam da origem (com o caminho)
    // ou, sem origem, todas. A estação onde se chega a pé numa integração não é
    // destino: ali a pessoa ainda nem embarcou.
    fun journeyLines(origin: Station?): List<JourneyLine> {
        val stopped = if (sundayOff) stoppedTomorrow else stoppedRoutes
        return repo.routes().mapNotNull { route ->
            val via = origin?.let { router.via(it.routeId, route.id) ?: return@mapNotNull null }
            val arrivalStop = via?.lastOrNull()?.toStop
            val stops = repo.directions(route.id).maxByOrNull { repo.stops(route.id, it.id).size }
                ?.let { repo.stops(route.id, it.id) }.orEmpty()
                .filter { it.id != arrivalStop && !(route.id == origin?.routeId && it.id == origin.stopId) }
            if (stops.isEmpty()) return@mapNotNull null
            JourneyLine(route.id, route.name, route.colorHex,
                via?.map { JourneyPlanner.plainName(repo.stop(it.fromStop)?.name.orEmpty()) }, stops, route.id in stopped)
        }
    }

    // De onde a pessoa sai agora: com a caminhada até a estação, ou já nela.
    fun leaveNow(origin: Station): Int = walkTo(origin).let { if (it > 0) nowMinutes() + it else nowMinutes() - settings.lateBoardingMinutes }

    fun routeView(itinerary: Itinerary, walk: Int = 0, dayOfWeek: Int = nowDayOfWeek()): RouteView {
        fun stopName(id: String) = JourneyPlanner.plainName(repo.stop(id)?.name ?: id)
        fun headsign(routeId: String, direction: Int) =
            repo.directions(routeId).firstOrNull { it.id == direction }?.headsign.orEmpty()
        val legs = itinerary.legs.map { leg ->
            val route = repo.route(leg.routeId)
            LegView(route?.name.orEmpty(), route?.colorHex.orEmpty(), headsign(leg.routeId, leg.directionId),
                stopName(leg.fromStop), stopName(leg.toStop), leg.departure, leg.arrival)
        }
        val transfers = itinerary.transfers.map { t ->
            TransferView(stopName(t.link.fromStop), t.needMinutes, t.tight, t.nextDeparture,
                // A regra de passagem usa o nome da grade, com o sufixo.
                transferRule(repo.stop(t.link.fromStop)?.name.orEmpty(), repo.stop(t.link.toStop)?.name.orEmpty()).fare)
        }
        // Com bicicleta, confere a regra de bike no embarque de cada trecho.
        val warnings = if (settings.mobility != Mobility.BIKE) emptyList() else legs.mapNotNull { leg ->
            val rule = bikeRuleFor(leg.fromName, leg.routeName)
            when {
                rule == null -> context.getString(R.string.route_bike_no_line, leg.routeName)
                !bikeBoarding(rule, dayOfWeek, leg.departure).allowed ->
                    context.getString(R.string.route_bike_not_now, leg.routeName, formatTime(leg.departure))
                else -> null
            }
        }.distinct()
        val walkNote = if (walk > 0) listOf(context.getString(R.string.route_walk_to_origin, legs.first().fromName, walk)) else emptyList()
        // Com cadeira de rodas, como se chega à plataforma onde se embarca, troca de linha e desce.
        val access = if (settings.mobility != Mobility.WHEELCHAIR) emptyList() else
            (listOf(itinerary.legs.first().fromStop) + itinerary.transfers.flatMap { listOf(it.link.fromStop, it.link.toStop) } +
                itinerary.legs.last().toStop).distinct().mapNotNull { id ->
                repo.stop(id)?.let { stop -> JourneyPlanner.plainName(stop.name) to Regras.current.station(stop.id, stop.name)?.access }
            }
        val accessWarnings = access.mapNotNull { (name, a) ->
            when (a?.way) {
                null -> context.getString(R.string.route_access_unknown, name)
                AccessWay.LIFT -> context.getString(R.string.route_access_lift, name)
                AccessWay.STAIRS_ONLY -> context.getString(R.string.route_access_stairs, name)
                else -> null
            }
        }.distinct()
        // Uma linha por estação: na integração, vale a primeira das duas plataformas de mesmo nome.
        val accessNote = access.distinctBy { it.first }.mapNotNull { (name, a) ->
            a?.let { accessSummary(context, it) }?.let { "$name: " + it.replaceFirstChar { c -> c.lowercase() } }
        }.distinct().takeIf { it.isNotEmpty() }?.let { listOf("♿ " + it.joinToString(" · ")) }.orEmpty()
        return RouteView(itinerary, legs, transfers, walkNote + warnings + accessWarnings, accessNote)
    }

    // O que dá para mostrar de amanhã, sem calcular roteiro nenhum.
    fun tomorrowStatusNow(): TomorrowStatus {
        val date = ScheduleStore.tomorrow()
        return tomorrowStatus(ScheduleStore.isSpecial(date), ScheduleStore.tomorrowType() == DayType.SUNDAY,
            Regras.current.operations[date] != null, routerTomorrow != null)
    }

    // Os roteiros de [origin] até [target]: saindo agora ([from] null) ou no horário
    // escolhido; num domingo sem metrô, nenhum: só o aviso de quando o metrô volta.
    fun planJourney(origin: Station, target: RoutePoint, from: Int?, tomorrow: Boolean = false): JourneyResult {
        val start = RoutePoint(origin.routeId, origin.stopId)
        val name = JourneyPlanner.plainName(repo.stop(target.stopId)?.name.orEmpty())
        if (tomorrow) {
            val date = ScheduleStore.tomorrow()
            val operation = Regras.current.operations[date]
            fun notice(title: Int, text: String, link: String? = null) =
                JourneyResult(emptyList(), monday = false, message = null, notice = TomorrowNotice(context.getString(title), text, link))
            when (tomorrowStatusNow()) {
                TomorrowStatus.SPECIAL -> return notice(R.string.journey_tomorrow_special_title,
                    context.getString(R.string.journey_tomorrow_special_text), operation?.link)
                TomorrowStatus.SUNDAY -> return notice(R.string.journey_tomorrow_sunday_title,
                    when (val monday = repo.mondayReturn(origin.stopId, origin.routeId)) {
                        is MondayReturn.At -> context.getString(R.string.journey_tomorrow_sunday_back, formatTime(monday.minutes))
                        MondayReturn.Stopped -> context.getString(R.string.journey_tomorrow_sunday_stopped)
                        MondayReturn.Special -> context.getString(R.string.journey_tomorrow_sunday_special)
                        MondayReturn.Unknown -> context.getString(R.string.journey_tomorrow_sunday_unknown)
                    })
                TomorrowStatus.UNKNOWN -> return notice(R.string.journey_tomorrow_unknown_title,
                    context.getString(R.string.journey_tomorrow_unknown_text))
                TomorrowStatus.FORECAST -> Unit
            }
            val router = routerTomorrow ?: return JourneyResult(emptyList(), monday = false, message = null)
            val plans = router.plan(start, target, from ?: 0, 4, stoppedTomorrow)
            val message = if (plans.isNotEmpty()) null else router.lastDeparture(start, target, stoppedTomorrow)
                ?.let { context.getString(R.string.journey_none_tomorrow_after, name, formatTime(it)) }
                ?: context.getString(R.string.journey_none_tomorrow, name)
            // Calendar.DAY_OF_WEEK vai de 1 (domingo) a 7 (sábado).
            val day = nowDayOfWeek() % 7 + 1
            return JourneyResult(plans.map { routeView(it, dayOfWeek = day) }, monday = false, message, forecast = true)
        }
        if (sundayOff) {
            return JourneyResult(emptyList(), monday = true, sundayOffText(context, repo.mondayReturn(origin.stopId, origin.routeId)))
        }
        val walk = if (from == null) walkTo(origin) else 0
        val plans = router.plan(start, target, from ?: leaveNow(origin), 4, stoppedRoutes)
        val message = if (plans.isNotEmpty()) null else router.lastDeparture(start, target, stoppedRoutes)
            ?.let { context.getString(R.string.route_none_after, name, formatTime(it)) }
            ?: context.getString(R.string.route_none_today, name)
        return JourneyResult(plans.map { routeView(it, walk = walk) }, monday = false, message)
    }

    // A chegada de quem sai agora, em cada estação da lista ("linha:estação").
    fun journeyArrivals(origin: Station): Map<String, Int> {
        if (sundayOff) return emptyMap()
        val start = RoutePoint(origin.routeId, origin.stopId)
        val from = leaveNow(origin)
        return journeyLines(origin).filterNot { it.stopped }.flatMap { line ->
            line.stops.mapNotNull { stop ->
                router.plan(start, RoutePoint(line.routeId, stop.id), from, 1, stoppedRoutes).firstOrNull()
                    ?.let { "${line.routeId}:${stop.id}" to it.arrival }
            }
        }.toMap()
    }

    // ----------------------------------------------------- Casa e Trabalho ----
    // Atalhos debaixo de "Para onde você vai?": a estação guardada, a chegada de
    // quem sai agora da estação em destaque, ou "você está aqui".
    val places by Places.all(context).collectAsState()
    var placeEditing by remember { mutableStateOf<Place?>(null) }
    val commute by CommuteStore.flow(context).collectAsState()
    val commuteKind = commute?.kind ?: CommuteKind.WORK
    var commuteOpen by remember { mutableStateOf(false) }
    fun placeName(saved: Pair<String, String>) = repo.stop(saved.first)?.name?.let(JourneyPlanner::plainName)
    val placeOrigin = homeState.station
    val placeShortcuts by produceState(emptyList<PlaceShortcut>(), places, placeOrigin, now, sundayOff, stoppedRoutes) {
        value = withContext(Dispatchers.IO) {
            Place.entries.map { place ->
                val saved = places?.get(place)
                val name = saved?.let(::placeName)
                val here = name != null && placeOrigin != null && JourneyPlanner.plainName(placeOrigin.stopName) == name
                val arrival = if (saved == null || name == null || here || placeOrigin == null || sundayOff) null else
                    router.plan(RoutePoint(placeOrigin.routeId, placeOrigin.stopId), RoutePoint(saved.second, saved.first),
                        leaveNow(placeOrigin), 1, stoppedRoutes).firstOrNull()?.arrival
                PlaceShortcut(place, name,
                    when {
                        here -> context.getString(R.string.place_here)
                        arrival != null -> context.getString(R.string.dest_arrives, formatTime(arrival))
                        else -> null
                    },
                    here)
            }
        }
    }

    // Sua ida e volta: com as duas estações e os dois horários, o bloco do alto da
    // tela inicial mostra o sentido de agora e qual metrô pegar. ⇄ vale enquanto o
    // app continuar achando o mesmo sentido: quando ele muda, a inversão acaba.
    var commuteFlippedFrom by remember { mutableStateOf<Place?>(null) }
    val commuteReady = places?.get(Place.HOME) != null && places?.get(Place.WORK) != null &&
        commute?.arriveBy != null && commute?.leaveAt != null
    val commuteView by produceState<CommuteView?>(null, places, commute, commuteFlippedFrom, homeState, now, sundayOff, stoppedRoutes) {
        value = withContext(Dispatchers.IO) {
            val schedule = commute ?: return@withContext null
            val arriveBy = schedule.arriveBy ?: return@withContext null
            val leaveAt = schedule.leaveAt ?: return@withContext null
            val homeStation = places?.get(Place.HOME)?.let { repo.station(it.first, it.second) } ?: return@withContext null
            val workStation = places?.get(Place.WORK)?.let { repo.station(it.first, it.second) } ?: return@withContext null
            val nearMeters = settings.walkToStationMeters.toDouble()
            val toHome = userDistanceTo(homeStation.lat, homeStation.lon)
            val toWork = userDistanceTo(workStation.lat, workStation.lon)
            val automatic = commuteDestination(now, arriveBy, leaveAt, toHome, toWork, nearMeters)
            val flipped = commuteFlippedFrom == automatic
            val destination = if (flipped) { if (automatic == Place.HOME) Place.WORK else Place.HOME } else automatic
            val target = if (destination == Place.HOME) homeStation else workStation
            // Longe das duas, sai da estação mais perto, como o atalho sempre fez.
            val farFromBoth = toHome != null && toWork != null && minOf(toHome, toWork) > nearMeters
            val nearest = (homeState as? HomeStation.Nearby)?.station
            val origin = when {
                // A não ser que a mais perto seja o próprio destino: daria "Casa → Casa".
                !flipped && farFromBoth && nearest != null && !isSameStation(nearest.stopId, target.stopId, repo.transferLinks()) -> nearest
                destination == Place.HOME -> workStation
                else -> homeStation
            }
            val point = RoutePoint(target.routeId, target.stopId)
            val hour = when {
                flipped || nowDayOfWeek() !in schedule.days -> null
                destination == Place.WORK -> arriveBy
                else -> leaveAt
            }
            val options = if (sundayOff) emptyList() else
                router.plan(RoutePoint(origin.routeId, origin.stopId), point, leaveNow(origin), Int.MAX_VALUE, stoppedRoutes)
            val pick = pickCommute(options, destination == Place.WORK, hour, now)
            CommuteView(
                destination = destination,
                automatic = automatic,
                kind = schedule.kind,
                origin = origin,
                originName = JourneyPlanner.plainName(origin.stopName),
                destinationName = JourneyPlanner.plainName(target.stopName),
                destinationRoute = target.routeId,
                destinationStop = target.stopId,
                lineColors = pick?.main?.legs?.mapNotNull { repo.route(it.routeId)?.colorHex }?.distinct()
                    ?: listOf(origin.routeColor),
                pick = pick,
                route = pick?.let { routeView(it.main, walk = walkTo(origin)) },
                message = if (pick != null) null else planJourney(origin, point, null).message,
                target = hour,
                vehicle = repo.route(origin.routeId)?.let { vehicleName(it.name) }.orEmpty(),
            )
        }
    }

    // Estação guardada fora da grade do dia: sem o bloco, voltam os atalhos. Começa
    // falso para os atalhos não piscarem enquanto o bloco é calculado.
    val commuteMissing by produceState(false, repo, places) {
        value = withContext(Dispatchers.IO) {
            listOf(Place.HOME, Place.WORK).any { place -> places?.get(place)?.let { repo.station(it.first, it.second) } == null }
        }
    }

    // As últimas viagens vistas em "Planeje sua viagem" (até 3), sem repetir Casa ou
    // Trabalho saindo da estação de agora, que já têm atalho.
    val recentTrips by RecentTrips.all(context).collectAsState()
    fun recentRow(t: RecentTrip): RecentRow? {
        val from = repo.stop(t.originStop)?.name ?: return null
        val to = repo.stop(t.destStop)?.name ?: return null
        val a = repo.route(t.originRoute) ?: return null
        val b = repo.route(t.destRoute) ?: return null
        return RecentRow(
            trip = t,
            title = "${JourneyPlanner.plainName(from)} → ${t.landmark ?: JourneyPlanner.plainName(to)}",
            destination = t.landmark ?: JourneyPlanner.plainName(to),
            lines = listOf(a.name to a.colorHex) + if (b.id != a.id) listOf(b.name to b.colorHex) else emptyList(),
            walk = t.landmark != null,
        )
    }

    val recentRows = remember(recentTrips, places, homeState.station) {
        val here = homeState.station
        recentTrips.orEmpty().filterNot { t ->
            here != null && t.landmark == null && t.originStop == here.stopId && t.originRoute == here.routeId &&
                places?.values?.any { it.first == t.destStop && it.second == t.destRoute } == true
        }.mapNotNull(::recentRow)
    }

    // O roteiro de quem sai agora; sem metrô hoje, nada.
    fun recentPlanOf(t: RecentTrip, onlyAhead: Boolean = false): Itinerary? {
        val origin = repo.station(t.originStop, t.originRoute) ?: return null
        // A tela inicial não sugere metrô que já saiu; o planejamento dá uma tolerância.
        val from = if (onlyAhead) maxOf(leaveNow(origin), nowMinutes()) else null
        return planJourney(origin, RoutePoint(t.destRoute, t.destStop), from).options.firstOrNull()?.itinerary
    }

    // Saída e chegada de quem sai agora.
    fun recentTimesOf(t: RecentTrip, known: Itinerary? = recentPlanOf(t)): Pair<Int, Int>? {
        val plan = known ?: return null
        val walk = t.landmark?.let { name -> Regras.current.landmarks.firstOrNull { it.name == name } }
            ?.let(::landmarkWalkMinutes) ?: 0
        return plan.departure to plan.arrival + walk
    }
    // Saída e chegada de quem sai agora, em cada recente, calculadas fora da tela.
    val recentTimes by produceState(emptyMap<RecentTrip, Pair<Int, Int>>(), recentRows, now, sundayOff, stoppedRoutes) {
        value = withContext(Dispatchers.IO) {
            recentRows.mapNotNull { row -> recentTimesOf(row.trip)?.let { row.trip to it } }.toMap()
        }
    }

    // A última viagem no cartão da estação aberta, para quem não guardou Casa nem
    // Trabalho: dali até as estações dela, com o metrô a pegar e a chegada.
    val openStationNow = expandedStation?.let { key -> otherLines.firstOrNull { it.first.stopId + it.first.routeId == key }?.first }
        ?: homeState.station
    val stationTrips by produceState<List<StationTrip>?>(null, recentTrips, places, openStationNow, now, sundayOff, stoppedRoutes) {
        value = withContext(Dispatchers.IO) {
            val last = recentTrips.orEmpty().firstOrNull()?.takeIf { places.isNullOrEmpty() } ?: return@withContext null
            if (repo.station(last.originStop, last.originRoute) == null || repo.station(last.destStop, last.destRoute) == null) return@withContext null
            val links = repo.transferLinks()
            last.suggestionsFrom(openStationNow?.stopId, openStationNow?.routeId, links) { id -> repo.stop(id)?.name?.let(JourneyPlanner::plainName) }
                .mapNotNull { trip ->
                    recentRow(trip)?.let { row ->
                        val plan = recentPlanOf(trip, onlyAhead = true)
                        StationTrip(
                            row = row,
                            times = recentTimesOf(trip, plan),
                            fromStation = openStationNow != null,
                            directionId = plan?.legs?.firstOrNull()?.takeIf { it.routeId == trip.originRoute }?.directionId,
                        )
                    }
                }
                .takeIf { it.isNotEmpty() }
        }
    }
    fun openRecent(trip: RecentTrip) {
        journeyFrom = repo.station(trip.originStop, trip.originRoute) ?: return
        journeyDest = trip.destRoute to trip.destStop
        journeyLandmark = trip.landmark
        journeyOpen = true
    }

    fun openPlace(place: Place) {
        val saved = places?.get(place) ?: return
        journeyLandmark = null
        journeyFrom = homeState.station
        // Já na estação do lugar: abre a lista, para escolher outro destino.
        journeyDest = (saved.second to saved.first).takeUnless { placeShortcuts.firstOrNull { it.place == place }?.here == true }
        journeyOpen = true
    }

    // O roteiro vira viagem. Num trecho só, pelo caminho de sempre (que oferece ir
    // sentado pelo terminal); com integração, o roteiro inteiro, trecho a trecho.
    fun startJourney(origin: Station, view: RouteView, placeName: String?) {
        journeyOpen = false
        val legs = view.itinerary.legs
        val single = legs.singleOrNull()
        val direction = single?.let { leg -> repo.directions(leg.routeId).firstOrNull { it.id == leg.directionId } }
        val destination = single?.let { repo.stop(it.toStop) }
        if (single != null && direction != null && destination != null) {
            startTrip(single.routeId, direction, Stop(origin.stopId, origin.stopName, seq = 0), destination, single, placeName)
            return
        }
        val plan = JourneyPlanner.plan(repo, view.itinerary, settings, simulated = false)?.copy(placeName = placeName)
        if (plan == null) {
            tripMessage = context.getString(R.string.trip_no_trip_today)
            return
        }
        confirmAndLaunch(plan)
    }

    tripChoice?.let { (direct, seated) ->
        TripChoiceDialog(
            direct = direct,
            seated = seated,
            onPick = { plan ->
                tripChoice = null
                confirmAndLaunch(plan)
            },
            onDismiss = { tripChoice = null },
        )
    }

    farTrip?.let { (plan, distance) ->
        AlertDialog(
            onDismissRequest = { farTrip = null },
            title = { Text(stringResource(R.string.trip_far_title)) },
            text = { Text(stringResource(R.string.trip_far_warning, formatKm(distance), plan.origin.name)) },
            // Simular: só pela tabela, marcado "Simulação" (os trechos seguintes de um roteiro
            // também). Esperar o GPS: a viagem de verdade, que começa quando ele achar a pessoa.
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        farTrip = null
                        launchTrip(plan.copy(simulated = true))
                    }) { Text(stringResource(R.string.trip_far_simulate)) }
                    TextButton(onClick = {
                        farTrip = null
                        launchTrip(plan)
                    }) { Text(stringResource(R.string.trip_far_wait)) }
                    TextButton(onClick = { farTrip = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    tripMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { tripMessage = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { tripMessage = null }) { Text("OK") } },
        )
    }

    fun departuresAt(station: Station): List<HomeDeparture> =
        repo.boardingDirections(station.stopId, station.routeId).map { direction ->
            val departure = nextDepartures(
                repo.departures(station.stopId, station.routeId, direction.id),
                now,
                count = 1,
                tomorrow = repo.departuresTomorrow(station.stopId, station.routeId, direction.id),
            ).firstOrNull()
            val stops = repo.stops(station.routeId, direction.id)
            HomeDeparture(
                headsign = direction.headsign,
                directionId = direction.id,
                departure = departure,
                dayEnd = if (departure == null) repo.dayEnd(station.stopId, station.routeId, direction.id) else null,
                published = direction.published,
                via = viaStation(stops.drop(stops.indexOfFirst { it.id == station.stopId } + 1).map { JourneyPlanner.plainName(it.name) }),
            )
        }

    val homeDepartures = remember(repo, homeState, now) {
        homeState.station?.let(::departuresAt).orEmpty()
    }
    val otherDepartures = remember(repo, homeState, now) {
        otherLines.map { (station, _) -> departuresAt(station) }
    }

    // Com GPS, o Bicicletar mais perto da pessoa; com estação fixada, o da estação.
    val homeBike = remember(homeState, bikeStations, settings) {
        when (val state = homeState) {
            is HomeStation.Nearby -> bikeStations
                .nearestBikeTo(state.userLat, state.userLon, settings.bikeNearYouMeters.toDouble())
                ?.let { (bike, distance) -> NearbyBike(bike, distance) }
                ?.let { HomeBike(it, fromYou = true) }

            is HomeStation.Pinned -> bikeStations
                .nearestBikeTo(state.station.lat, state.station.lon, settings.bikeNearStationMeters.toDouble())
                ?.let { (bike, distance) -> NearbyBike(bike, distance) }
                ?.let { HomeBike(it, fromYou = false) }

            else -> null
        }
    }

    fun openStation(station: Station) {
        openDirectionId = null
        routeId = station.routeId
        stopId = station.stopId
    }

    if (askExactForRoutine) {
        AlertDialog(
            onDismissRequest = { askExactForRoutine = false },
            title = { Text(stringResource(R.string.leave_exact_title)) },
            text = { Text(stringResource(R.string.leave_exact_text)) },
            confirmButton = {
                TextButton(onClick = {
                    askExactForRoutine = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }
                    }
                }) { Text(stringResource(R.string.leave_exact_allow)) }
            },
            dismissButton = { TextButton(onClick = { askExactForRoutine = false }) { Text(stringResource(R.string.leave_exact_later)) } },
        )
    }

    placeEditing?.let { place ->
        StationPickerDialog(
            stations = remember(repo) { repo.stations() },
            title = stringResource(
                when {
                    place == Place.HOME -> R.string.place_pick_home
                    commuteKind == CommuteKind.SCHOOL -> R.string.place_pick_school
                    else -> R.string.place_pick_work
                },
            ),
            note = stringResource(R.string.place_pick_note),
            onPick = {
                Places.set(context, place, it.stopId to it.routeId)
                placeEditing = null
            },
            onDismiss = { placeEditing = null },
            onNone = if (places?.get(place) == null) null else ({
                Places.set(context, place, null)
                placeEditing = null
            }),
        )
    }

    // ------------------------------------------------- blocos da tela inicial --
    // Cada bloco na ordem que a pessoa escolheu (padrão: avisos, Trabalho e Casa,
    // as duas estações, rotinas, Bicicletar).
    val trip = activeTrip
    // Terminada a viagem, volta-se ao Início, seja de onde ela tenha começado.
    LaunchedEffect(trip != null) { if (trip != null) homeTab = HomeTab.HOME }
    // A última viagem entra no cartão da estação no lugar dos atalhos de Casa e Trabalho.
    fun tripInStation(layout: HomeLayout) =
        layout.shows(HomeBlock.PLACES) && layout.shows(HomeBlock.STATIONS) && !stationTrips.isNullOrEmpty()

    fun homeBlocks(layout: HomeLayout): List<HomeBlock> = layout.homeOrder.filter { b ->
        layout.shows(b) && when (b) {
            HomeBlock.PLACES -> !tripInStation(layout)
            HomeBlock.STATIONS, HomeBlock.SEARCH -> true
            HomeBlock.ROUTINES -> !routinesList.isNullOrEmpty()
            HomeBlock.NOTICES -> notices.isNotEmpty()
            HomeBlock.BIKE -> homeBike != null
            HomeBlock.LINES -> false
        }
    }

    // A aba "Planeje sua viagem": "Para onde você vai?" e, com "Vou ao trabalho",
    // Trabalho e Casa (que também ficam no topo da tela inicial).
    fun routeBlocks(layout: HomeLayout): List<HomeBlock> =
        listOf(HomeBlock.SEARCH, HomeBlock.PLACES).filter { layout.shows(it) }

    @Composable
    fun HomeBlockContent(block: HomeBlock, layout: HomeLayout, inRoutes: Boolean = false) {
        when (block) {
            HomeBlock.PLACES -> Column {
                val view = commuteView
                when {
                    !inRoutes && commuteReady && !commuteMissing -> view?.let {
                        CommuteCard(
                            view = it,
                            now = now,
                            canStart = !sundayOff && tripAllowedFrom(it.origin.lat, it.origin.lon),
                            onOpen = {
                                journeyLandmark = null
                                journeyFrom = it.origin
                                journeyDest = it.destinationRoute to it.destinationStop
                                journeyOpen = true
                            },
                            onFlip = { commuteFlippedFrom = if (commuteFlippedFrom == it.automatic) null else it.automatic },
                            onStart = { it.route?.let { route -> startJourney(it.origin, route, null) } },
                        )
                    }
                    else -> PlaceShortcuts(
                        shortcuts = placeShortcuts,
                        kind = commuteKind,
                        onOpen = ::openPlace,
                        onEdit = { placeEditing = it },
                    )
                }
            }

            HomeBlock.STATIONS -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Com os avisos escondidos, o de hoje continua: os horários do dia são outros.
                if (!layout.shows(HomeBlock.NOTICES)) MetroforNotices(notices.filter { it.isToday })
                val extras = HomeCardExtras(
                    bike = layout.bike,
                    halfFare = layout.halfFare,
                    access = layout.access,
                    onOpenFareGuide = { fareGuideOpen = true },
                    mondayReturn = { repo.mondayReturn(it.stopId, it.routeId) },
                    hereMeters = settings.tripMaxMeters,
                )
                // Como os cartões da viagem: uma estação aberta (no começo, a mais perto)
                // e as outras compactas; tocar numa compacta abre ela.
                val expandedKey = expandedStation?.takeIf { key -> otherLines.any { it.first.stopId + it.first.routeId == key } }
                val trips = stationTrips?.takeIf { tripInStation(layout) }
                val tripFooter: (@Composable () -> Unit)? = trips?.let { list ->
                    {
                        StationTrips(
                            trips = list,
                            onOpen = ::openRecent,
                            onOther = {
                                val first = list.first().row.trip
                                journeyFrom = repo.station(first.originStop, first.originRoute)
                                journeyDest = null
                                journeyLandmark = null
                                journeyOpen = true
                            },
                        )
                    }
                }
                val styled = StationStyle(
                    style = layout.style,
                    trips = trips.orEmpty(),
                    onOpen = ::openRecent,
                    onOther = { station ->
                        journeyFrom = station
                        journeyDest = null
                        journeyLandmark = null
                        journeyOpen = true
                    },
                )
                HomeStationCard(
                    styled = styled,
                    state = homeState,
                    departures = homeDepartures,
                    now = now,
                    compact = expandedKey != null,
                    onExpand = { expandedStation = null },
                    footer = tripFooter.takeIf { homeState.station == null },
                    onUseLocation = {
                        if (locationSource.hasPermission()) {
                            scope.launch { locate("tap") }
                        } else {
                            permissionLauncher.launch(LocationSource.PERMISSIONS)
                        }
                    },
                    onOpenStation = ::openStation,
                    extras = extras,
                )
                otherLines.forEachIndexed { i, (station, distance) ->
                    val nearby = homeState as HomeStation.Nearby
                    HomeStationCard(
                        state = HomeStation.Nearby(station, distance, nearby.userLat, nearby.userLon),
                        departures = otherDepartures.getOrElse(i) { emptyList() },
                        now = now,
                        onUseLocation = {},
                        onOpenStation = ::openStation,
                        showRefresh = false,
                        compact = expandedKey != station.stopId + station.routeId,
                        onExpand = { expandedStation = station.stopId + station.routeId },
                        styled = styled,
                        // A passagem é por linha: na mesma linha do primeiro cartão, não repete.
                        extras = extras.copy(halfFare = extras.halfFare && station.routeName != homeState.station?.routeName),
                    )
                }
            }

            HomeBlock.NOTICES -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetroforNotices(notices)
                OperationNotificationOffer()
            }

            // "Para onde você vai?", saindo da estação em destaque.
            HomeBlock.SEARCH -> JourneySearchBar(onClick = {
                journeyFrom = homeState.station
                journeyDest = null
                journeyLandmark = null
                journeyOpen = true
            })

            // As rotinas: um toque abre a lista.
            HomeBlock.ROUTINES -> routinesList?.takeIf { it.isNotEmpty() }?.let { list ->
                TextButton(onClick = { routinesOpen = true }) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.routines_home, list.joinToString(", ") { it.name }),
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HomeBlock.BIKE -> homeBike?.let { HomeBikeCard(it) }
            HomeBlock.LINES -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                routes.forEach { route ->
                    RouteCard(route = route, stationCount = repo.stationCount(route.id), onClick = { routeId = route.id })
                }
            }
        }
    }

    // Por cima dos blocos, só quando os horários não são os de hoje, o aviso disso
    // (no dia normal, a origem fica em "Fonte").
    val showStatus = scheduleStatusIsWarning(repo.source)
    val homeHeader: (@Composable () -> Unit)? = if (!showStatus) null else ({
        ScheduleStatus(source = repo.source, syncing = scheduleSyncing)
    })

    // O WhatsApp da Super NINA, onde ela atende. Nada de DevLog aqui: o toque na
    // Nina não fica registrado em lugar nenhum.
    fun ninaOpener(routeName: String?): (() -> Unit)? = Regras.current.nina
        ?.takeIf { nina -> nina.serves(routeName) }
        ?.let { nina ->
            {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(nina.link))) }
                Unit
            }
        }

    // A prévia de "Ordem da tela inicial": os mesmos blocos, com o cabeçalho do app.
    @Composable
    fun HomePreviewContent(layout: HomeLayout) {
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        homeBlocks(layout).forEach { HomeBlockContent(it, layout) }
    }

    // --------------------------------------------------------------- telas ----
    val arrived = tripArrival?.takeIf { it.isRecent(System.currentTimeMillis()) }
    when {
        // Chegou: o resumo, antes de voltar ao app.
        arrived != null -> TripArrivalScreen(
            arrival = arrived,
            lineColorHex = repo.route(arrived.routeId)?.colorHex.orEmpty(),
            onDone = { TripTracker.dismissArrival() },
        )

        // Em viagem, a tela é só o trilho; o resto do app volta quando ela termina.
        trip != null && !trip.arrived -> TripRailScreen(
            trip = trip,
            lineColorHex = repo.route(trip.plan.routeId)?.colorHex.orEmpty(),
            soundEnabled = tripSound ?: true,
            onToggleSound = { TripTracker.setSoundEnabled(context, it) },
            stationAlerts = tripStationAlerts ?: false,
            onToggleStationAlerts = { TripTracker.setStationAlerts(context, it) },
            onNina = ninaOpener(trip.plan.routeName),
            onStop = { TripTracker.stop(context) },
        )

        orderOpen -> {
            BackHandler { orderOpen = false }
            HomeOrderScreen(
                layout = homeLayout,
                onChange = { HomeLayoutStore.save(context, it) },
                onBack = { orderOpen = false },
                preview = { layout -> HomePreviewContent(layout) },
            )
        }

        styleOpen -> {
            BackHandler { styleOpen = false }
            HomeStyleScreen(
                layout = homeLayout,
                onChange = { HomeLayoutStore.save(context, it) },
                onBack = { styleOpen = false },
            )
        }

        adjustOpen -> {
            BackHandler { adjustOpen = false }
            AdjustScreen(
                layout = homeLayout,
                onChange = { HomeLayoutStore.save(context, it) },
                onOpenOrder = { orderOpen = true },
                onBack = { adjustOpen = false },
            )
        }

        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(
                settings = settings,
                onChange = { SettingsStore.save(context, it) },
                onReset = { SettingsStore.reset(context) },
                onBack = { showSettings = false },
                places = Place.entries.map { place ->
                    place to places?.get(place)?.let { saved ->
                        placeName(saved)?.let { name -> listOfNotNull(name, repo.route(saved.second)?.name).joinToString(" · ") }
                    }
                },
                onEditPlace = { placeEditing = it },
                commuteKind = commuteKind,
                recentCount = recentTrips.orEmpty().size,
                onClearRecent = { RecentTrips.clear(context) },
                onOpenAdjust = { adjustOpen = true },
            )
        }

        sourcesOpen -> {
            BackHandler { sourcesOpen = false }
            SourcesScreen(schedule = repo.source, onBack = { sourcesOpen = false })
        }

        showAbout -> {
            BackHandler { showAbout = false }
            AboutScreen(onBack = { showAbout = false }, onOpenSources = { sourcesOpen = true })
        }

        fareGuideOpen && Regras.current.fareGuide != null -> {
            BackHandler { fareGuideOpen = false }
            FareGuideScreen(
                guide = Regras.current.fareGuide!!,
                fares = Regras.current.fareLines,
                onBack = { fareGuideOpen = false },
            )
        }

        accessOpen && route != null && stop != null -> {
            BackHandler { accessOpen = false }
            AccessScreen(
                stationName = JourneyPlanner.plainName(stop.name),
                lineName = route.name,
                access = Regras.current.station(stop.id, stop.name)?.access,
                onBack = { accessOpen = false },
            )
        }

        routineEditing != null -> {
            val (routine, isNew) = routineEditing!!
            BackHandler { routineEditing = null }
            RoutineEditScreen(
                initial = routine,
                isNew = isNew,
                stations = remember(repo) { repo.stations() },
                onSave = {
                    saveRoutine(it)
                    routineEditing = null
                    journeyOpen = false
                    routinesOpen = true
                },
                onDelete = {
                    LeaveScheduler.cancelRoutine(context, routine)
                    LeaveStore.removeRoutine(context, routine.id)
                    routineEditing = null
                },
                onBack = { routineEditing = null },
            )
        }

        commuteOpen -> {
            BackHandler { commuteOpen = false }
            CommuteScreen(
                schedule = commute ?: CommuteSchedule(),
                homeName = places?.get(Place.HOME)?.let(::placeName),
                workName = places?.get(Place.WORK)?.let(::placeName),
                onChange = { CommuteStore.save(context, it) },
                onPickStation = { placeEditing = it },
                onBack = { commuteOpen = false },
            )
        }

        routinesOpen -> {
            BackHandler { routinesOpen = false }
            RoutinesScreen(
                routines = routinesList.orEmpty(),
                alerts = leaveAlerts.orEmpty(),
                onToggle = { r, on ->
                    if (on) saveRoutine(r.copy(enabled = true)) else {
                        LeaveScheduler.cancelRoutine(context, r)
                        LeaveStore.putRoutine(context, r.copy(enabled = false))
                    }
                },
                onEdit = { routineEditing = it to false },
                onNew = {
                    routineEditing = Routine(
                        id = Routine.newId(), name = "",
                        originStopId = homeState.station?.stopId.orEmpty(), originRouteId = homeState.station?.routeId.orEmpty(),
                        originName = homeState.station?.let { JourneyPlanner.plainName(it.stopName) }.orEmpty(),
                        destStopId = "", destRouteId = "", destName = "",
                        days = Routine.WEEKDAYS, arriveBy = true, minutes = 8 * 60, before = settings.leaveBeforeMinutes,
                    ) to true
                },
                onBack = { routinesOpen = false },
            )
        }

        journeyOpen -> {
            BackHandler {
                journeyOpen = false
                journeyDest = null
            }
            key(journeyFrom, journeyDest, journeyLandmark) {
                JourneyScreen(
                    initialOrigin = journeyFrom,
                    stations = remember(repo) { repo.stations() },
                    linesFor = ::journeyLines,
                    arrivalsFor = ::journeyArrivals,
                    plan = ::planJourney,
                    tomorrowBlocked = { tomorrowStatusNow() != TomorrowStatus.FORECAST },
                    // Modo viagem só com metrô hoje e perto o bastante da estação.
                    canStartTrip = { !sundayOff && tripAllowedFrom(it.lat, it.lon) },
                    onStartTrip = ::startJourney,
                    onRepeat = { origin, line, dest, view ->
                        // O trajeto vira rotina: chegar até a hora deste roteiro, de segunda a sexta.
                        routineEditing = Routine(
                            id = Routine.newId(), name = "",
                            originStopId = origin.stopId, originRouteId = origin.routeId,
                            originName = JourneyPlanner.plainName(origin.stopName),
                            destStopId = dest.id, destRouteId = line.routeId, destName = JourneyPlanner.plainName(dest.name),
                            days = Routine.WEEKDAYS, arriveBy = true,
                            minutes = view?.itinerary?.arrival?.let { (it + 4) / 5 * 5 } ?: (8 * 60),
                            before = settings.leaveBeforeMinutes,
                        ) to true
                    },
                    onBack = {
                        journeyOpen = false
                        journeyDest = null
                    },
                    initialDestination = journeyDest,
                    places = Place.entries.mapNotNull { place -> places?.get(place)?.let { place to it } },
                    landmarks = Regras.current.landmarks,
                    walkMinutes = ::landmarkWalkMinutes,
                    initialLandmark = journeyLandmark,
                    recents = recentTrips.orEmpty(),
                    onResult = { origin, line, dest, place ->
                        RecentTrips.add(context, RecentTrip(origin.stopId, origin.routeId, dest.id, line.routeId, place?.name))
                    },
                )
            }
        }

        route != null && stop != null -> {
            // Sem estação fixada, o widget mostra a última aberta.
            LaunchedEffect(stop.id, route.id) {
                pinned.setLastOpened(stop.id, route.id)
                StationWidget.update(context)
            }
            // No terminal, o sentido que acaba nele só tem chegadas: a aba some.
            val directions = remember(route.id, stop.id) { repo.boardingDirections(stop.id, route.id) }
            val isPinned = pinned.get() == (stop.id to route.id)
            // O Stop da navegação não carrega coordenadas; o Station sim.
            val bike = remember(stop.id, route.id, bikeStations, settings) {
                repo.station(stop.id, route.id)
                    ?.let { bikeStations.nearestBikeTo(it.lat, it.lon, settings.bikeNearStationMeters.toDouble()) }
                    ?.let { (station, distance) -> NearbyBike(station, distance) }
            }
            BackHandler { stopId = null }
            DeparturesScreen(
                route = route,
                stop = stop,
                directions = directions,
                departuresFor = { direction ->
                    repo.departures(stop.id, route.id, direction.id)
                },
                tomorrowFor = { direction ->
                    repo.departuresTomorrow(stop.id, route.id, direction.id)
                },
                dayEndFor = { direction -> repo.dayEnd(stop.id, route.id, direction.id) },
                mondayReturn = { repo.mondayReturn(stop.id, route.id) },
                viaFor = { direction -> repo.via(stop.id, route.id, direction) },
                onPlanJourney = {
                    journeyFrom = repo.station(stop.id, route.id)
                    journeyOpen = true
                },
                transfers = remember(stop.id) { repo.transfersFrom(stop.id) },
                transferDepartures = { target, fromMinutes ->
                    nextDepartures(
                        repo.departures(target.stopId, target.routeId, target.direction.id),
                        fromMinutes,
                        count = 1,
                        tomorrow = repo.departuresTomorrow(target.stopId, target.routeId, target.direction.id),
                    ).firstOrNull()
                },
                onOpenTransfer = { target ->
                    openDirectionId = target.direction.id
                    routeId = target.routeId
                    stopId = target.stopId
                },
                // Pedido por uma integração; senão, o primeiro sentido.
                initialDirectionId = openDirectionId,
                schedule = repo.source,
                bike = bike,
                nearby = remember(stop.id, route.id) {
                    repo.station(stop.id, route.id)?.let { Regras.current.landmarks.near(stop.name, it.lat, it.lon) }.orEmpty()
                },
                walkMinutes = ::landmarkWalkMinutes,
                isPinned = isPinned,
                onTogglePin = {
                    if (isPinned) pinned.clear() else pinned.set(stop.id, route.id)
                    StationWidget.update(context)
                    refreshHome()
                },
                onOpenFareGuide = { fareGuideOpen = true },
                onOpenAccess = { accessOpen = true },
                onBack = { stopId = null },
            )
        }

        route != null -> {
            // O sentido 0 define a ordem em que a lista de estações é mostrada.
            val stops = remember(route.id) { repo.stops(route.id, directionId = 0) }
            BackHandler { routeId = null }
            StopsScreen(
                route = route,
                stops = stops,
                onBack = { routeId = null },
                onStopClick = {
                    openDirectionId = null
                    stopId = it.id
                },
            )
        }

        else -> RoutesScreen(
            routes = routes,
            stationCount = { repo.stationCount(it.id) },
            onRouteClick = { routeId = it.id },
            tab = homeTab,
            onTab = { homeTab = it },
            routeBlocks = routeBlocks(homeLayout),
            routeBlock = { HomeBlockContent(it, homeLayout, inRoutes = true) },
            routesExtra = {
                RecentTripsBlock(recentRows, recentTimes, onOpen = ::openRecent, onRemove = { RecentTrips.remove(context, it) })
            },
            menu = HomeMenuActions(
                onFareGuide = Regras.current.fareGuide?.let { { fareGuideOpen = true } },

                onNina = ninaOpener(homeState.station?.routeName),
                onRoutines = { routinesOpen = true },
                onCommute = { commuteOpen = true },
                onAdjust = { adjustOpen = true },
                onStyle = { styleOpen = true },
                onOrder = { orderOpen = true },
                onSettings = { showSettings = true },
                onSources = { sourcesOpen = true },
                onAbout = { showAbout = true },
                onShare = { shareApp(context) },
            ),
            blocks = homeBlocks(homeLayout),
            block = { HomeBlockContent(it, homeLayout) },
            header = homeHeader,
        )
    }
}
