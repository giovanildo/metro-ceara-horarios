package io.github.giova.metroceara

import android.app.Application
import io.github.giova.metroceara.alerts.OperationWorker
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.TripLog

/** Carrega as regras do Metrofor, agenda a conferência das operações especiais, inicia os registros (de testes e das viagens) e grava travamentos nele antes de o app fechar. */
class MetroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DevLog.init(this)
        TripLog.init(this)
        Regras.init(this)
        DevLog.log("app_start", *DevLog.deviceInfo(this))
        // Operação especial (ENEM, eleição) notificada mesmo com o app fechado.
        runCatching { OperationWorker.schedule(this) }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            DevLog.crash(error)
            previous?.uncaughtException(thread, error)
        }
    }
}
