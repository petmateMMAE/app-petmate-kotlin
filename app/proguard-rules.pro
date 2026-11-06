# Reglas de ProGuard para el artefacto de release.

# Modelos serializados con kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class mx.petmate.app.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class mx.petmate.app.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Tink (lo trae androidx.security:security-crypto) referencia anotaciones de
# Error Prone que solo existen al compilar. R8 las pide; se ignoran sin riesgo.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
