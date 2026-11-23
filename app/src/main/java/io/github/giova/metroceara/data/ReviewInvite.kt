package io.github.giova.metroceara.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit

/**
 * O convite para avaliar o app na Play Store, na tela de chegada do modo viagem.
 *
 * Sem biblioteca do Google (não é livre e quebraria o F-Droid): o botão abre a
 * página do app na loja. Regras (decididas em 23/11/2026):
 * - só depois da [GOOD_TRIPS]ª viagem que chegou ao destino (simulada não conta);
 * - aparece no máximo [MAX_SHOWN] vezes, com [AGAIN_AFTER_MILLIS] entre elas;
 * - tocou em "Avaliar", nunca mais;
 * - sem perguntar antes se a pessoa gostou (a Play proíbe filtrar quem avalia);
 * - só em quem instalou pela Play Store (APK e F-Droid não têm para onde ir).
 */
object ReviewInvite {

    const val GOOD_TRIPS = 3
    const val MAX_SHOWN = 2
    const val AGAIN_AFTER_MILLIS = 21L * 24 * 60 * 60 * 1000

    private const val PREFS = "review_invite"
    private const val KEY_TRIPS = "trips"
    private const val KEY_LAST_ARRIVAL = "last_arrival"
    private const val KEY_SHOWN = "shown"
    private const val KEY_LAST_SHOWN = "last_shown"
    private const val KEY_DONE = "done"

    /** A conta do convite. Puro, para os testes. */
    data class State(
        val trips: Int = 0,
        /** A chegada já contada (o atMillis dela): a mesma tela não conta duas vezes. */
        val lastArrival: Long = 0L,
        val shown: Int = 0,
        val lastShown: Long = 0L,
        val done: Boolean = false,
    ) {
        /** Uma chegada ao destino: o novo estado e se o convite aparece nela. */
        fun onArrival(arrivalMillis: Long, simulated: Boolean): Pair<State, Boolean> {
            if (arrivalMillis == lastArrival) return this to false
            if (simulated) return copy(lastArrival = arrivalMillis) to false
            val counted = copy(trips = trips + 1, lastArrival = arrivalMillis)
            val show = !done && counted.trips >= GOOD_TRIPS && shown < MAX_SHOWN &&
                (shown == 0 || arrivalMillis - lastShown >= AGAIN_AFTER_MILLIS)
            return if (show) counted.copy(shown = shown + 1, lastShown = arrivalMillis) to true else counted to false
        }

        fun onRate() = copy(done = true)
    }

    /**
     * Conta a chegada e diz se mostra o convite. A resposta para a mesma chegada
     * fica guardada em [shownFor], para a tela não sumir ao girar o celular.
     */
    fun onArrival(context: Context, arrivalMillis: Long, simulated: Boolean): Boolean {
        val p = prefs(context)
        if (p.getLong(KEY_LAST_ARRIVAL, 0L) == arrivalMillis) {
            return p.getLong(KEY_LAST_SHOWN, 0L) == arrivalMillis && !p.getBoolean(KEY_DONE, false)
        }
        if (!fromPlayStore(context)) return false
        val (next, show) = load(context).onArrival(arrivalMillis, simulated)
        save(context, next)
        return show
    }

    /** "Avaliar": abre a página do app na Play Store e não convida mais. */
    fun rate(context: Context) {
        save(context, load(context).onRate())
        val id = context.packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /** Instalado pela Play Store? (APK do GitHub e F-Droid: não.) */
    private fun fromPlayStore(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer == "com.android.vending"
    }.getOrDefault(false)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun load(context: Context): State {
        val p = prefs(context)
        return State(
            trips = p.getInt(KEY_TRIPS, 0),
            lastArrival = p.getLong(KEY_LAST_ARRIVAL, 0L),
            shown = p.getInt(KEY_SHOWN, 0),
            lastShown = p.getLong(KEY_LAST_SHOWN, 0L),
            done = p.getBoolean(KEY_DONE, false),
        )
    }

    private fun save(context: Context, s: State) = prefs(context).edit {
        putInt(KEY_TRIPS, s.trips)
        putLong(KEY_LAST_ARRIVAL, s.lastArrival)
        putInt(KEY_SHOWN, s.shown)
        putLong(KEY_LAST_SHOWN, s.lastShown)
        putBoolean(KEY_DONE, s.done)
    }
}
