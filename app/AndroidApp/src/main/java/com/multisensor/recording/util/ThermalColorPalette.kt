package com.multisensor.recording.util
import androidx.compose.ui.graphics.Color
enum class ThermalColorPalette(
    val displayName: String,
    val colours: List<Color>
) {
    IRON(
        "Iron",
        listOf(
            Color(0xFF000000),
            Color(0xFF800080),
            Color(0xFFFF0000),
            Color(0xFFFF8000),
            Color(0xFFFFFF00),
            Color(0xFFFFFFFF)
        )
    ),
    RAINBOW(
        "Rainbow",
        listOf(
            Color(0xFF000080),
            Color(0xFF0000FF),
            Color(0xFF00FFFF),
            Color(0xFF00FF00),
            Color(0xFFFFFF00),
            Color(0xFFFF0000)
        )
    ),
    GRAYSCALE(
        "Grayscale",
        listOf(
            Color(0xFF000000),
            Color(0xFF404040),
            Color(0xFF808080),
            Color(0xFFC0C0C0),
            Color(0xFFFFFFFF)
        )
    ),
    INFERNO(
        "Inferno",
        listOf(
            Color(0xFF000004),
            Color(0xFF3B0F70),
            Color(0xFF8C2981),
            Color(0xFFDD513A),
            Color(0xFFFCA50A),
            Color(0xFFFCFFA4)
        )
    ),
    VIRIDIS(
        "Viridis",
        listOf(
            Color(0xFF440154),
            Color(0xFF31688E),
            Color(0xFF35B779),
            Color(0xFFFDE725)
        )
    );
    companion object {
        fun fromDisplayName(name: String): ThermalColorPalette {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: IRON
        }
    }
}