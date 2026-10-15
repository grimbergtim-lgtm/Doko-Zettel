package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: Long = DEFAULT_COLOR_HEX,
    val avatarSymbol: String = "♠",
    val createdAt: Long = System.currentTimeMillis()
) {
    @get:Ignore
    val color: Color
        get() = parseColorHex(colorHex)

    companion object {
        const val DEFAULT_COLOR_HEX = 0xFF2563EBL

        /**
         * Konvertiert eine Compose [Color] in einen gültigen 32-Bit ARGB [Long].
         */
        fun colorToHex(color: Color): Long {
            return (color.toArgb().toLong() and 0xFFFFFFFFL)
        }

        /**
         * Bereinigt und normalisiert einen gespeicherten [Long]-Farbwert:
         * 1. Falls der Farbwert im internen Compose 64-Bit-Format (color.value.toLong())
         *    mit den ARGB-Bits in den oberen 32 Bits vorliegt, wird er in die unteren 32 Bits verschoben.
         * 2. Falls kein Alpha-Kanal vorhanden ist (Alpha == 0), wird 0xFF als Alpha gesetzt.
         */
        fun sanitizeColorHex(raw: Long): Long {
            val lower = raw and 0xFFFFFFFFL
            val normalized = if (lower == 0L && raw != 0L) {
                (raw.toULong() shr 32).toLong() and 0xFFFFFFFFL
            } else {
                lower
            }
            return if ((normalized and 0xFF000000L) == 0L) {
                normalized or 0xFF000000L
            } else {
                normalized
            }
        }

        /**
         * Parst einen [Long]-Farbwert in eine Compose [Color], auch wenn er korrupt
         * im internen 64-Bit-Compose-Format gespeichert wurde.
         */
        fun parseColorHex(raw: Long): Color {
            return Color(sanitizeColorHex(raw))
        }
    }
}
