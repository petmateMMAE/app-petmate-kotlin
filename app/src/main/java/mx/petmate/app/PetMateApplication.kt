package mx.petmate.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.sync.SyncOutbox
import javax.inject.Inject

@HiltAndroidApp
class PetMateApplication : Application() {

    @Inject lateinit var outbox: SyncOutbox

    @Inject lateinit var dispatchers: DispatcherProvider

    override fun onCreate() {
        super.onCreate()
        // Abre la base al arrancar (spec 001, T-011) y devuelve a pendiente lo que el proceso dejo a medio
        // enviar (spec 005, T-002). Fuera del hilo principal.
        CoroutineScope(SupervisorJob() + dispatchers.io).launch { outbox.recoverInterrupted() }
    }
}
