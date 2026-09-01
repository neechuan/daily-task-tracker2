package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TaskCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val lightContainerColor: Color
) {
    WORK("Work", Icons.Default.Work, Color(0xFF3B82F6), Color(0xFFDBEAFE)),
    PERSONAL("Personal", Icons.Default.Person, Color(0xFF8B5CF6), Color(0xFFEDE9FE)),
    HEALTH("Health", Icons.Default.FitnessCenter, Color(0xFF10B981), Color(0xFFD1FAE5)),
    STUDY("Study", Icons.Default.Book, Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    SHOPPING("Shopping", Icons.Default.ShoppingCart, Color(0xFFEC4899), Color(0xFFFCE7F3)),
    OTHER("Other", Icons.Default.MoreHoriz, Color(0xFF6B7280), Color(0xFFF3F4F6))
}
