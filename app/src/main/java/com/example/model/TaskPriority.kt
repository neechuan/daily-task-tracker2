package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.ui.graphics.Color

enum class TaskPriority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    val color: Color
        get() = when (this) {
            LOW -> Color(0xFF10B981) // Emerald
            MEDIUM -> Color(0xFFF59E0B) // Amber
            HIGH -> Color(0xFFEF4444) // Red
        }

    val containerColor: Color
        get() = when (this) {
            LOW -> Color(0xFFD1FAE5)
            MEDIUM -> Color(0xFFFEF3C7)
            HIGH -> Color(0xFFFEE2E2)
        }
}
