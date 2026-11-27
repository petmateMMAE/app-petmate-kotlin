package mx.petmate.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import mx.petmate.app.BuildConfig
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.core.log.Logger
import mx.petmate.app.data.remote.AuthApiService
import mx.petmate.app.data.remote.BackendHealth
import mx.petmate.app.data.remote.DeviceApiService
import mx.petmate.app.data.remote.HealthApiService
import mx.petmate.app.data.remote.IdempotencyKeyInterceptor
import mx.petmate.app.data.remote.RetryInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Cliente HTTP.
 *
 * Queda configurado aunque todavia no se consuma: mientras
 * BuildConfig.USE_FAKE_BACKEND sea true, el repositorio activo es el falso.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideIdempotencyKeyInterceptor(): IdempotencyKeyInterceptor = IdempotencyKeyInterceptor()

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        idempotencyKeyInterceptor: IdempotencyKeyInterceptor,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // El orden no es intercambiable (001/T-033, D-15): la clave de idempotencia se pone
        // primero para que se genere una sola vez, y el reintento ocurre por dentro reusando esa
        // misma peticion. Al contrario, cada intento llevaria una clave distinta y el servidor no
        // podria descartar el duplicado.
        .addInterceptor(idempotencyKeyInterceptor)
        .addInterceptor(retryInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    // Registro del dispositivo para push (001/T-039). Rutas devices/register y devices/unregister,
    // resueltas en Q-02 de la spec 006 el 27/11/2026.
    @Provides
    @Singleton
    fun provideDeviceApiService(retrofit: Retrofit): DeviceApiService =
        retrofit.create(DeviceApiService::class.java)

    // Salud del backend (001/T-033). Rutas health/live y health/ready, resueltas en Q-02 el
    // 27/11/2026. La comprobacion no bloquea el arranque: solo informa y despierta el contenedor
    // de Azure, que se apaga sin uso (G-09). El motivo largo esta en BackendHealth.
    @Provides
    @Singleton
    fun provideHealthApiService(retrofit: Retrofit): HealthApiService =
        retrofit.create(HealthApiService::class.java)

    @Provides
    @Singleton
    fun provideBackendHealth(
        api: HealthApiService,
        dispatchers: DispatcherProvider,
        logger: Logger,
    ): BackendHealth = BackendHealth(api, dispatchers, logger)

    private const val TIMEOUT_SECONDS = 30L
}
