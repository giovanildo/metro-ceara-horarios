package io.github.giova.metrofortaleza.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Cópia do registro de testes numa pasta escolhida pela pessoa (de preferência
 * no Google Drive), como o backup do gioBank: o seletor de pasta do Android dá
 * ao app permissão para aquela pasta, e o app do Drive cuida de mandar para a
 * nuvem. Sem login nem biblioteca da Google.
 *
 * Um arquivo por dia, `metro-fortaleza-log-AAAA-MM-DD.log`, regravado com o
 * conteúdo do dia (o arquivo local continua sendo a fonte). Se não der para
 * gravar no arquivo do dia, cria outro com o mesmo nome acrescido de "-2"
 * (depois "-3"…) e passa a usar esse.
 *
 * Tudo roda na thread do [DevLog], para não disputar os arquivos com ele.
 */
object DevLogCloud {

    data class State(val folder: String?, val lastOkMillis: Long, val error: String?)

    /** Regravar no máximo uma vez por minuto: durante a viagem chegam eventos a cada segundo. */
    private const val MIN_INTERVAL_MILLIS = 60_000L
    private const val MAX_SUFFIX = 9
    private const val MIME = "application/octet-stream" // com text/plain alguns provedores trocam .log por .txt

    private val _state = MutableStateFlow(State(null, 0L, null))
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile private var lastAttempt = 0L

    fun init(context: Context) {
        val p = prefs(context)
        _state.value = State(p.getString(KEY_FOLDER_NAME, null)?.takeIf { p.contains(KEY_TREE) }, p.getLong(KEY_OK, 0L), null)
    }

    val enabled: Boolean get() = _state.value.folder != null

    /** Guarda a pasta escolhida (resultado do seletor de pasta) e a permissão de gravar nela. */
    fun setFolder(context: Context, tree: Uri): Boolean {
        val resolver = context.contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (runCatching { resolver.takePersistableUriPermission(tree, flags) }.isFailure) {
            _state.value = _state.value.copy(error = "Essa pasta não permite gravação automática.")
            return false
        }
        val name = runCatching {
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            resolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "pasta escolhida"
        val p = prefs(context)
        p.getString(KEY_TREE, null)?.let(Uri::parse)?.takeIf { it != tree }?.let { old ->
            runCatching { resolver.releasePersistableUriPermission(old, flags) }
        }
        // Pasta nova: esquece os arquivos e tamanhos da pasta anterior.
        p.edit {
            p.all.keys.filter { it.startsWith(KEY_DOC) || it.startsWith(KEY_SIZE) }.forEach { remove(it) }
            putString(KEY_TREE, tree.toString())
            putString(KEY_FOLDER_NAME, name)
        }
        _state.value = State(name, p.getLong(KEY_OK, 0L), null)
        return true
    }

    fun clearFolder(context: Context) {
        val p = prefs(context)
        p.getString(KEY_TREE, null)?.let(Uri::parse)?.let { tree ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        p.edit { clear() }
        _state.value = State(null, 0L, null)
    }

    /**
     * Copia para a pasta os dias que mudaram desde a última cópia. Com [force]
     * falso, só se passou [MIN_INTERVAL_MILLIS] da última tentativa. Chamar
     * só na thread do [DevLog].
     */
    fun sync(context: Context, localDir: File, force: Boolean) {
        val p = prefs(context)
        val tree = p.getString(KEY_TREE, null)?.let(Uri::parse) ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastAttempt < MIN_INTERVAL_MILLIS) return
        lastAttempt = now
        val days = localDir.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty().sortedBy { it.name }
        var error: String? = null
        for (file in days) {
            val day = file.nameWithoutExtension
            val size = file.length()
            if (p.getLong(KEY_SIZE + day, -1L) == size) continue
            val bytes = runCatching { file.readBytes() }.getOrElse { error = it.message; continue }
            val written = writeDay(context, tree, day, bytes)
            if (written) p.edit { putLong(KEY_SIZE + day, size) } else error = "não deu para gravar o dia $day na pasta"
        }
        if (error == null) {
            p.edit { putLong(KEY_OK, now) }
            _state.value = _state.value.copy(lastOkMillis = now, error = null)
        } else {
            _state.value = _state.value.copy(error = error)
        }
    }

    /** Grava o dia no arquivo dele; se falhar, tenta "-2", "-3"… e passa a usar o que deu certo. */
    private fun writeDay(context: Context, tree: Uri, day: String, bytes: ByteArray): Boolean {
        val p = prefs(context)
        val current = p.getInt(KEY_DOC + day, 1)
        for (n in current..MAX_SUFFIX) {
            val name = if (n == 1) "metro-fortaleza-log-$day.log" else "metro-fortaleza-log-$day-$n.log"
            val ok = runCatching {
                val doc = findOrCreate(context, tree, name)
                write(context, doc, bytes)
            }.isSuccess
            if (ok) {
                if (n != current) p.edit { putInt(KEY_DOC + day, n) }
                return true
            }
        }
        return false
    }

    private fun findOrCreate(context: Context, tree: Uri, name: String): Uri {
        val resolver = context.contentResolver
        val parentId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, parentId)
        return DocumentsContract.createDocument(resolver, parent, MIME, name) ?: error("não deu para criar $name")
    }

    private fun write(context: Context, doc: Uri, bytes: ByteArray) {
        val resolver = context.contentResolver
        // "wt" trunca; alguns provedores só aceitam "rwt" ou "w" (como no gioBank).
        val out = listOf("wt", "rwt", "w").firstNotNullOfOrNull { mode ->
            runCatching { resolver.openOutputStream(doc, mode) }.getOrNull()
        } ?: error("não deu para abrir o arquivo")
        out.use { it.write(bytes) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_devlog_cloud", Context.MODE_PRIVATE)

    private const val KEY_TREE = "tree"
    private const val KEY_FOLDER_NAME = "folder_name"
    private const val KEY_OK = "last_ok"
    private const val KEY_DOC = "doc_"
    private const val KEY_SIZE = "size_"
}
