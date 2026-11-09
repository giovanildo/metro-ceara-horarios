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

    /**
     * Fica só nas abas "Planeje sua viagem" (a busca) e Linhas, não na tela inicial.
     * Trabalho e Casa ficam nas duas: no topo da tela inicial, com a hora de chegada,
     * e debaixo da busca.
     */
    val inRoutes: Boolean get() = this == SEARCH || this == LINES
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
        // Sem opção de esconder desde a reorganização: quem escondeu antes não ficava sem eles.
        block.inRoutes -> true
        block == HomeBlock.BIKE && !bike -> false
        else -> block !in hidden
    }

    /** "Vou ao trabalho ou à escola": Trabalho e Casa aparecem. */
    val commute: Boolean get() = HomeBlock.PLACES !in hidden

    /** Os blocos da tela inicial, na ordem da pessoa (sem os da aba Rotas). */
    val homeOrder: List<HomeBlock> get() = order.filterNot { it.inRoutes }

    fun withVisible(block: HomeBlock, visible: Boolean): HomeLayout =
        if (block.fixed) this else copy(hidden = if (visible) hidden - block else hidden + block)

    /** Sobe ([delta] = -1) ou desce (+1) um bloco da tela inicial. */
    fun moved(block: HomeBlock, delta: Int): HomeLayout {
        val home = homeOrder
        val i = home.indexOf(block)
        val j = i + delta
        if (i < 0 || j !in home.indices) return this
        return copy(order = home.toMutableList().apply { add(j, removeAt(i)) } + order.filter { it.inRoutes })
    }

    /**
     * Só a ordem e o que aparece na tela inicial: os ajustes de bike, meia e
     * acessibilidade ficam.
     */
    fun resetOrder(): HomeLayout = copy(order = DEFAULT_ORDER, hidden = emptySet())

    val isDefaultOrder: Boolean
        get() = homeOrder == DEFAULT_ORDER.filterNot { it.inRoutes } && hidden.none { !it.inRoutes }

    companion object {
        /** O perfil da maioria: ver logo o próximo metrô das estações perto. */
        val DEFAULT_ORDER = listOf(
            HomeBlock.NOTICES, HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.ROUTINES, HomeBlock.BIKE,
            HomeBlock.SEARCH, HomeBlock.LINES,
        )

        /** O padrão até a v1.86, com os avisos depois das estações. */
        private val OLD_HOME_ORDER = listOf(
            HomeBlock.PLACES, HomeBlock.STATIONS, HomeBlock.NOTICES, HomeBlock.ROUTINES, HomeBlock.BIKE,
        )

        /** Quem nunca mudou a ordem passa para o padrão novo; uma ordem escolhida fica como está. */
        fun withNoticesOnTop(order: List<HomeBlock>): List<HomeBlock> =
            if (order.filterNot { it.inRoutes } == OLD_HOME_ORDER) DEFAULT_ORDER else order
    }
}

/** Guarda o [HomeLayout] no aparelho e avisa a interface quando muda. */
object HomeLayoutStore {

    // A chave "notices_on_top" (1.87) marcava a mudança sem gravar a ordem: na abertura
    // seguinte os avisos voltavam para baixo. Esta refaz a mudança, agora gravada.
    private const val KEY_NOTICES_ON_TOP = "notices_on_top_2"

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
        // Uma vez só: depois disso, pôr os avisos de volta embaixo tem de ficar valendo.
        val migrate = !p.getBoolean(KEY_NOTICES_ON_TOP, false)
        val current = if (migrate) HomeLayout.withNoticesOnTop(order) else order
        if (migrate) {
            p.edit {
                putBoolean(KEY_NOTICES_ON_TOP, true)
                if (current != order) putString("order", current.joinToString(",") { it.key })
            }
        }
        return HomeLayout(
            order = current,
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
