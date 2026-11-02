package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Os lugares de sempre: atalhos debaixo de "Para onde você vai?". */
enum class Place(val key: String) { HOME("home"), WORK("work") }

/**
 * A estação de Casa e a de Trabalho (estação e linha), só neste celular. Guarda
 * só a estação, nunca o endereço.
 */
object Places {
    private val state = MutableStateFlow<Map<Place, Pair<String, String>>?>(null)

    fun all(context: Context): StateFlow<Map<Place, Pair<String, String>>?> {
        if (state.value == null) state.value = load(context)
        return state.asStateFlow()
    }

    /** Guarda [stop] (estação, linha) em [place]; `null` apaga. */
    fun set(context: Context, place: Place, stop: Pair<String, String>?) {
        prefs(context).edit {
            if (stop == null) {
                remove("place_${place.key}_stop")
                remove("place_${place.key}_route")
            } else {
                putString("place_${place.key}_stop", stop.first)
                putString("place_${place.key}_route", stop.second)
            }
        }
        state.value = load(context)
    }

    private fun load(context: Context): Map<Place, Pair<String, String>> {
        val prefs = prefs(context)
        return Place.entries.mapNotNull { place ->
            val stop = prefs.getString("place_${place.key}_stop", null) ?: return@mapNotNull null
            val route = prefs.getString("place_${place.key}_route", null) ?: return@mapNotNull null
            place to (stop to route)
        }.toMap()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_prefs", Context.MODE_PRIVATE)
}
