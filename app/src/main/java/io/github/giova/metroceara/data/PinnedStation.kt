package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit

/**
 * A estação que a pessoa fixou como sua.
 *
 * Existe para o app continuar útil quando a localização é negada ou não chega,
 * e porque quem pega metrô costuma sair sempre da mesma estação.
 */
class PinnedStation(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("metrofor_prefs", Context.MODE_PRIVATE)

    /** O par estação/linha fixado, ou `null` se não houver. */
    fun get(): Pair<String, String>? {
        val stopId = prefs.getString(KEY_STOP, null) ?: return null
        val routeId = prefs.getString(KEY_ROUTE, null) ?: return null
        return stopId to routeId
    }

    fun set(stopId: String, routeId: String) = prefs.edit {
        putString(KEY_STOP, stopId)
        putString(KEY_ROUTE, routeId)
        remove(KEY_WIDGET_NEARBY)
    }

    fun clear() = prefs.edit {
        remove(KEY_STOP)
        remove(KEY_ROUTE)
    }

    /** A última estação aberta: é a do widget quando não há estação fixada. */
    fun lastOpened(): Pair<String, String>? {
        val stopId = prefs.getString(KEY_LAST_STOP, null) ?: return null
        val routeId = prefs.getString(KEY_LAST_ROUTE, null) ?: return null
        return stopId to routeId
    }

    fun setLastOpened(stopId: String, routeId: String) = prefs.edit {
        putString(KEY_LAST_STOP, stopId)
        putString(KEY_LAST_ROUTE, routeId)
    }

    /**
     * Depois do ↻ do widget, ele mostra a estação mais perto (a do ↻ ou a que o
     * app achar pelo GPS depois), mesmo com uma fixada; fixar uma estação com a
     * estrela volta à ordem normal.
     */
    var widgetNearby: Boolean
        get() = prefs.getBoolean(KEY_WIDGET_NEARBY, false)
        set(value) = prefs.edit { putBoolean(KEY_WIDGET_NEARBY, value) }

    /** A estação mais perto da pessoa na última vez que o app achou a localização. */
    fun lastNearby(): Pair<String, String>? {
        val stopId = prefs.getString(KEY_NEAR_STOP, null) ?: return null
        val routeId = prefs.getString(KEY_NEAR_ROUTE, null) ?: return null
        return stopId to routeId
    }

    fun setLastNearby(stopId: String, routeId: String) = prefs.edit {
        putString(KEY_NEAR_STOP, stopId)
        putString(KEY_NEAR_ROUTE, routeId)
    }

    private companion object {
        const val KEY_NEAR_STOP = "near_stop_id"
        const val KEY_NEAR_ROUTE = "near_route_id"
        const val KEY_STOP = "pinned_stop_id"
        const val KEY_ROUTE = "pinned_route_id"
        const val KEY_LAST_STOP = "last_stop_id"
        const val KEY_LAST_ROUTE = "last_route_id"
        const val KEY_WIDGET_NEARBY = "widget_nearby"
    }
}
