package mx.petmate.app.domain.moderation

/** Tipos de contenido que aporta el usuario y pasan por moderacion (spec 004, T-001, R-01). */
enum class ModerationContentType {
    Text,
    Image,
    Video,
}
