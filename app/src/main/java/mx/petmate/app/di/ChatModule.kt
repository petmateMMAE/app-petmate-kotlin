package mx.petmate.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.data.local.dao.MessageDao
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.ChatApiService
import mx.petmate.app.data.repository.ChatRepositoryImpl
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.ChatRepository
import retrofit2.Retrofit
import javax.inject.Singleton

/** Chat de seguimiento (spec 016, T-005). */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)

    /**
     * Offline-first: la pantalla observa Room y la red solo la refresca.
     *
     * `currentUserId` sale de la sesion, igual que en los demas repositorios: toda consulta de
     * Room filtra por la cuenta y sin sesion no se lee nada (C-14).
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        api: ChatApiService,
        caller: AuthorizedCaller,
        messageDao: MessageDao,
        authRepository: AuthRepository,
        dispatchers: DispatcherProvider,
    ): ChatRepository {
        val session = authRepository.sessionState.stateIn(
            scope = CoroutineScope(SupervisorJob() + dispatchers.default),
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Checking,
        )
        return ChatRepositoryImpl(
            api = api,
            caller = caller,
            messageDao = messageDao,
            currentUserId = { (session.value as? SessionState.SignedIn)?.user?.id },
            dispatchers = dispatchers,
        )
    }
}
