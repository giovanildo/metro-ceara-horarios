package io.github.giova.metroceara.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.ScheduleStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * A cada ~3 h, com internet, confere o regras.json (só baixa se mudou: ETag) e
 * notifica operação especial nova. O sistema pode atrasar em economia de bateria.
 */
class OperationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching { ScheduleStore.syncRules(applicationContext) }
            .onFailure { DevLog.log("operacoes_worker_fail", "reason" to (it.message ?: it.javaClass.simpleName)) }
        // Mesmo sem rede, as regras guardadas podem ter dia ainda não avisado.
        OperationAlerts.notifyNew(applicationContext)
        Result.success()
    }

    companion object {
        private const val NAME = "operacoes_metrofor"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<OperationWorker>(3, TimeUnit.HOURS)
                // Quem acabou de instalar está com o app aberto e já vê o aviso na tela.
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
