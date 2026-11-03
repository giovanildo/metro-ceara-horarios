package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Os blocos da tela inicial, que a pessoa pode pôr em outra ordem ou esconder. */
enum class HomeBlock(val key: String) {
    /** Trabalho e Casa. */
    PLACES("lugares"),

    /** As duas estações mais próximas: sempre aparecem (é o motivo do app). */
    STATIONS("estacoes"),

    /** Avisos do Metrofor (operação especial, feriado, ENEM). */
    NOTICES("aviso"),

    /** "Para onde você vai?". */
    SEARCH("busca"),
    ROUTINES("rotinas"),

    /** Bicicletar mais perto: só com "Levo bike" ligado. */
    BIKE("bike"),
    LINES("linhas"),
    ;

    /** Este bloco não pode ser escondido. */
    val fixed: Boolean get() = this == STATIONS
}

/**
 * A tela inicial de cada pessoa: a ordem dos blocos, os escondidos e os ajustes
 * de "Ajustar para mim". Fica só no aparelho.
 */
data class HomeLayout(
    val order: List<HomeBlock> = DEFAULT_ORDER,
    val hidden: Set<HomeBlock> = emptySet(),
    /** Levo bike: regra da bike no cartão e o Bicicletar mais perto. */
    val bike: Boolean = false,
    /** Pago meia ou tenho gratuidade: passagem e guia da meia no cartão. */
    val halfFare: Boolean = false,
    /** Preciso de elevador ou rampa: acessibilidade da estação no cartão. */
    val access: Boolean = false,
) {
    /** O bloco aparece na tela inicial. */
    fun shows(block: HomeBlock): Boolean = when {
        block.fixed -> true
        block == HomeBlock.BIKE && !bike -> false
        else -> block !in hidden
    }

    /** "Vou ao trabalho ou à escola": Trabalho e Casa aparecem. */
    val commute: Boolean get() = HomeBlock.PLACES !in hidden

    /** "Ainda não conheço o metrô": a busca vem primeiro. */
    val newcomer: Boolean get() = order.firstOrNull() == HomeBlock.SEARCH

    fun withVisible(block: HomeBlock, visible: Boolean): HomeLayout =
        if (block.fixed) this else copy(hidden = if (visible) hidden - block else hidden + block)

    /** Sobe ([delta] = -1) ou desce (+1) um bloco. */
    fun moved(block: HomeBlock, delta: Int): HomeLayout {
        val i = order.indexOf(block)
        val j = i + delta
        if (i < 0 || j !in order.indices) return this
        return copy(order = order.toMutableList().apply { add(j, removeAt(i)) })
    }

    /** Liga: a busca vai para o topo. Desliga: volta ao lugar dela no padrão. */
    fun withNewcomer(on: Boolean): HomeLayout {
        val rest = order - HomeBlock.SEARCH
        if (on) return copy(order = listOf(HomeBlock.SEARCH) + rest, hidden = hidden - HomeBlock.SEARCH)
        if (!newcomer) return this
        // Volta para depois do bloco que vem antes dela na ordem padrão.
        val before = DEFAULT_ORDER.subList(0, DEFAULT_ORDER.indexOf(HomeBlock.SEARCH)).lastOrNull { it in rest }
        val at = before?.let { rest.indexOf(it) + 1 } ?: rest.size
        return copy(order = rest.toMutableList().apply { add(at, HomeBlock.SEARCH) })
    }

    /** Só a ordem e o que aparece: os ajustes de bike, meia e acessibilidade ficam. */
    fun resetOrder(): HomeLayout = copy(order = DEFAULT_ORDER, hidden = emptySet())

    val isDefaultOrder: Boolean get() = order == DEFAULT_ORDER && hidden.isEmpty()

    companion object {
        /** O perfil da maioria: ver logo o próximo metrô das estações perto. */
        val DEFAULT_ORDER = listOf(
            HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.SEARCH,
            HomeBlock.ROUTINES, HomeBlock.BIKE, HomeBlock.LINES,
        )
    }
}

/** Guarda o [HomeLayout] no aparelho e avisa a interface quando muda. */
object HomeLayoutStore {

    private val state = MutableStateFlow<HomeLayout?>(null)

    fun flow(context: Context): StateFlow<HomeLayout?> {
        if (state.value == null) state.value = load(context)
        return state.asStateFlow()
    }

    fun save(context: Context, layout: HomeLayout) {
        prefs(context).edit {
            putString("order", layout.order.joinToString(",") { it.key })
            putString("hidden", layout.hidden.joinToString(",") { it.key })
            putBoolean("bike", layout.bike)
            putBoolean("half_fare", layout.halfFare)
            putBoolean("access", layout.access)
        }
        state.value = layout
    }

    private fun load(context: Context): HomeLayout {
        val p = prefs(context)
        fun blocks(text: String?) = text.orEmpty().split(",").mapNotNull { k -> HomeBlock.entries.firstOrNull { it.key == k } }
        // Bloco novo numa versão futura entra no lugar dele no padrão.
        val saved = blocks(p.getString("order", null)).distinct()
        val order = if (saved.isEmpty()) HomeLayout.DEFAULT_ORDER else {
            HomeLayout.DEFAULT_ORDER.filter { it !in saved }.fold(saved) { acc, missing ->
                val at = HomeLayout.DEFAULT_ORDER.indexOf(missing).coerceAtMost(acc.size)
                acc.toMutableList().apply { add(at, missing) }
            }
        }
        return HomeLayout(
            order = order,
            hidden = blocks(p.getString("hidden", null)).filterNot { it.fixed }.toSet(),
            bike = p.getBoolean("bike", false),
            halfFare = p.getBoolean("half_fare", false),
            access = p.getBoolean("access", false),
        )
    }

    // Arquivo próprio: "Restaurar padrões" das configurações não mexe aqui.
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_home_layout", Context.MODE_PRIVATE)
}
