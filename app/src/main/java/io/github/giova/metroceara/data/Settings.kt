package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Distâncias e tempos que a pessoa pode ajustar na tela de configurações.
 * Os padrões são os valores que o app usava fixos até a v1.9.
 */
data class AppSettings(
    /** Outras linhas ganham horários na tela inicial até esta distância. */
    val nearbyLinesMeters: Int = 3_000,
    /** Bicicletar mais perto de você, pelo GPS. */
    val bikeNearYouMeters: Int = 3_000,
    /** Bicicletar perto da estação (fixada ou na tela de horários). */
    val bikeNearStationMeters: Int = 600,
    /** Acima disso, o modo viagem pergunta antes de começar. */
    val tripWarnMeters: Int = 1_000,
    /** Acima disso, o modo viagem não aparece. */
    val tripMaxMeters: Int = 1_000,
    /** Até esta distância, o "Sair agora" soma a caminhada até a estação de partida. */
    val walkToStationMeters: Int = 3_000,
    /** Quantas estações antes do destino vem o aviso de descida. */
    val alertStationsBefore: Int = 2,
    /** Sem posição boa por este tempo, o GPS é dado como perdido. */
    val gpsLostSeconds: Int = 60,
    /** De quanto em quanto tempo a tela inicial pede posição ao GPS. */
    val homeGpsSeconds: Int = 10,
    /** Por quanto tempo a tela inicial segura a posição precisa contra uma menos precisa. */
    val homeFixHoldSeconds: Int = 120,
    /** O modo viagem ainda pega o trem que saiu há até este tempo. */
    val lateBoardingMinutes: Int = 2,
    /**
     * Quem está na estação de partida vê, em "Planeje sua viagem", o metrô que
     * devia ter saído há até este tempo ("pode estar atrasado") e pode começar o
     * modo viagem nele (11/11: o das 15:14 em Carlito atrasou e sumiu). 0 = não mostrar.
     */
    val delayedTrainMinutes: Int = 10,
    /** A viagem encerra sozinha se passar tanto assim da chegada prevista. */
    val tripGiveUpMinutes: Int = 30,
    /**
     * Até quantas estações de um terminal o modo viagem oferece ir até ele e
     * voltar sentado. 0 = não oferecer.
     */
    val seatedMaxStations: Int = 3,
    /** Tema: [THEME_AUTO] (como o celular), [THEME_LIGHT] ou [THEME_DARK]. */
    val themeMode: Int = THEME_AUTO,
    /** Cores tiradas do papel de parede (Android 12+), em vez das cores do app. */
    val wallpaperColors: Boolean = true,
    /** No aviso de descida e na chegada, lembrar de não esquecer os pertences. */
    val remindBelongings: Boolean = true,
    /** Como a pessoa se desloca nas integrações: muda o tempo de trocar de linha. */
    val mobility: Mobility = Mobility.WALKING,
    /** Minutos a mais em toda integração, além do que o perfil e o pico já somam. */
    val transferExtraMinutes: Int = 0,
    /** Quanto tempo antes da partida toca o alarme da hora de sair (lembrado da última vez). */
    val leaveBeforeMinutes: Int = 15,
) {
    companion object {
        const val THEME_AUTO = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        /** O padrão até a v1.86. */
        private const val OLD_TRIP_MAX_METERS = 3_000

        /** O limite do modo viagem ao abrir: o padrão antigo guardado passa para o novo, uma vez só. */
        fun loadedTripMax(saved: Int, warnMeters: Int, alreadyMigrated: Boolean): Int {
            val newDefault = AppSettings().tripMaxMeters
            return if (!alreadyMigrated && saved == OLD_TRIP_MAX_METERS && warnMeters <= newDefault) newDefault else saved
        }
    }
}

/** Guarda as [AppSettings] no aparelho e avisa a interface quando mudam. */
object SettingsStore {

    private const val KEY_TRIP_MAX_MIGRATED = "trip_max_1km"

    private val state = MutableStateFlow<AppSettings?>(null)

    fun flow(context: Context): StateFlow<AppSettings?> {
        if (state.value == null) state.value = load(context)
        return state.asStateFlow()
    }

    fun get(context: Context): AppSettings = flow(context).value ?: AppSettings()

    fun save(context: Context, settings: AppSettings) {
        prefs(context).edit {
            putInt("nearby_lines_m", settings.nearbyLinesMeters)
            putInt("bike_near_you_m", settings.bikeNearYouMeters)
            putInt("bike_near_station_m", settings.bikeNearStationMeters)
            putInt("trip_warn_m", settings.tripWarnMeters)
            putInt("trip_max_m", settings.tripMaxMeters)
            putBoolean(KEY_TRIP_MAX_MIGRATED, true)
            putInt("walk_to_station_m", settings.walkToStationMeters)
            putInt("alert_stations_before", settings.alertStationsBefore)
            putInt("gps_lost_s", settings.gpsLostSeconds)
            putInt("home_gps_s", settings.homeGpsSeconds)
            putInt("home_fix_hold_s", settings.homeFixHoldSeconds)
            putInt("late_boarding_min", settings.lateBoardingMinutes)
            putInt("delayed_train_min", settings.delayedTrainMinutes)
            putInt("trip_give_up_min", settings.tripGiveUpMinutes)
            putInt("seated_max_stations", settings.seatedMaxStations)
            putInt("theme_mode", settings.themeMode)
            putBoolean("wallpaper_colors", settings.wallpaperColors)
            putBoolean("remind_belongings", settings.remindBelongings)
            putString("mobility", settings.mobility.key)
            putInt("transfer_extra_min", settings.transferExtraMinutes)
            putInt("leave_before_min", settings.leaveBeforeMinutes)
        }
        state.value = settings
    }

    fun reset(context: Context) {
        prefs(context).edit { clear() }
        state.value = AppSettings()
    }

    private fun load(context: Context): AppSettings {
        val p = prefs(context)
        val d = AppSettings()
        return AppSettings(
            nearbyLinesMeters = p.getInt("nearby_lines_m", d.nearbyLinesMeters),
            bikeNearYouMeters = p.getInt("bike_near_you_m", d.bikeNearYouMeters),
            bikeNearStationMeters = p.getInt("bike_near_station_m", d.bikeNearStationMeters),
            tripWarnMeters = p.getInt("trip_warn_m", d.tripWarnMeters),
            tripMaxMeters = AppSettings.loadedTripMax(
                saved = p.getInt("trip_max_m", d.tripMaxMeters),
                warnMeters = p.getInt("trip_warn_m", d.tripWarnMeters),
                alreadyMigrated = p.getBoolean(KEY_TRIP_MAX_MIGRATED, false),
            ),
            // Até a v1.86 era o mesmo limite do modo viagem: quem o ajustou continua igual.
            walkToStationMeters = p.getInt("walk_to_station_m", p.getInt("trip_max_m", d.walkToStationMeters)),
            alertStationsBefore = p.getInt("alert_stations_before", d.alertStationsBefore),
            gpsLostSeconds = p.getInt("gps_lost_s", d.gpsLostSeconds),
            homeGpsSeconds = p.getInt("home_gps_s", d.homeGpsSeconds),
            homeFixHoldSeconds = p.getInt("home_fix_hold_s", d.homeFixHoldSeconds),
            lateBoardingMinutes = p.getInt("late_boarding_min", d.lateBoardingMinutes),
            delayedTrainMinutes = p.getInt("delayed_train_min", d.delayedTrainMinutes),
            tripGiveUpMinutes = p.getInt("trip_give_up_min", d.tripGiveUpMinutes),
            seatedMaxStations = p.getInt("seated_max_stations", d.seatedMaxStations),
            themeMode = p.getInt("theme_mode", d.themeMode),
            wallpaperColors = p.getBoolean("wallpaper_colors", d.wallpaperColors),
            remindBelongings = p.getBoolean("remind_belongings", d.remindBelongings),
            mobility = Mobility.entries.firstOrNull { it.key == p.getString("mobility", null) } ?: d.mobility,
            transferExtraMinutes = p.getInt("transfer_extra_min", d.transferExtraMinutes),
            leaveBeforeMinutes = p.getInt("leave_before_min", d.leaveBeforeMinutes),
        )
    }

    // Arquivo próprio: "Restaurar padrões" apaga só as configurações.
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_settings", Context.MODE_PRIVATE)
}
