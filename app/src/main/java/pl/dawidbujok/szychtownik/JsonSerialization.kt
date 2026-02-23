package pl.dawidbujok.szychtownik

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Niestandardowy serializator dla klasy [Color] z Jetpack Compose.
 * `kotlinx.serialization` nie wie domyślnie, jak przekształcić obiekt `Color` na format JSON.
 * Ten serializator konwertuje kolor na jego 32-bitową wartość ARGB (jako Int) i z powrotem.
 */
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Color) {
        // Konwertuj obiekt Color na liczbę całkowitą ARGB i zapisz ją.
        encoder.encodeInt(value.toArgb())
    }

    override fun deserialize(decoder: Decoder): Color {
        // Odczytaj liczbę całkowitą i utwórz z niej obiekt Color.
        return Color(decoder.decodeInt())
    }
}

/**
 * Niestandardowy serializator dla klasy [LocalDateTime] z Javy.
 * Konwertuje obiekt `LocalDateTime` na ciąg znaków w standardowym formacie ISO-8601 i z powrotem.
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        // Sformatuj datę i czas do stringa i zapisz go.
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        // Odczytaj string i sparsuj go z powrotem do obiektu LocalDateTime.
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

/**
 * Globalna, dostępna w całej aplikacji instancja parsera JSON.
 * Jest skonfigurowana tak, aby obsługiwać niestandardowe typy danych ([Color], [LocalDateTime])
 * oraz ignorować nieznane klucze podczas deserializacji, co zwiększa odporność na zmiany formatu danych.
 */
val AppJson = Json {
    // Ignoruj klucze w JSON, których nie ma w klasie danych. Zapobiega to błędom,
    // gdy nowsza wersja aplikacji zapisze dane, a starsza próbuje je odczytać.
    ignoreUnknownKeys = true
    // Zawsze zapisuj wartości domyślne pól w klasach danych.
    encodeDefaults = true
    // Rejestruj nasze niestandardowe serializatory w module, aby `Json` wiedział, jak ich używać.
    serializersModule = SerializersModule { 
        contextual(Color::class, ColorSerializer)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}