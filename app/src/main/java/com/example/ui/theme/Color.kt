package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Construction & Architecture Theme Colors
val BlueprintNavy = Color(0xFF0F172A)
val BlueprintNavyContainer = Color(0xFF1E293B)
val SafetyAmber = Color(0xFFF59E0B)
val SafetyAmberDark = Color(0xFFD97706)
val ConstructionOrange = Color(0xFFEA580C)
val ConcreteSlate = Color(0xFF64748B)
val LightSlate = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)

val SuccessGreen = Color(0xFF10B981)
val AlertRed = Color(0xFFEF4444)
val InfoSky = Color(0xFF0284C7)
val ElectricCyan = Color(0xFF06B6D4)
val VioletAccent = Color(0xFF8B5CF6)

// Category Semantic Colors
val CategorySemen = Color(0xFF3B82F6) // Blue
val CategoryBesi = Color(0xFFEF4444)  // Red
val CategoryBata = Color(0xFFF97316)  // Orange
val CategoryFinishing = Color(0xFF8B5CF6) // Purple
val CategoryKayu = Color(0xFFD97706) // Amber
val CategoryPlumbing = Color(0xFF06B6D4) // Cyan
val CategoryLainnya = Color(0xFF64748B) // Slate

fun getCategoryColor(category: String): Color {
    return when {
        category.contains("Semen", ignoreCase = true) || category.contains("Pasir", ignoreCase = true) -> CategorySemen
        category.contains("Besi", ignoreCase = true) || category.contains("Baja", ignoreCase = true) -> CategoryBesi
        category.contains("Bata", ignoreCase = true) || category.contains("Dinding", ignoreCase = true) -> CategoryBata
        category.contains("Finishing", ignoreCase = true) || category.contains("Cat", ignoreCase = true) -> CategoryFinishing
        category.contains("Kayu", ignoreCase = true) || category.contains("Bekisting", ignoreCase = true) -> CategoryKayu
        category.contains("Plumbing", ignoreCase = true) || category.contains("Elektrik", ignoreCase = true) -> CategoryPlumbing
        else -> CategoryLainnya
    }
}

// Gradients
val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF1E3A8A))
)

val AmberBadgeGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
)

val EmeraldBadgeGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

val GlassCardBorder = Color(0x33CBD5E1)

// Light Theme
val PrimaryLight = Color(0xFF0F2B48)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE0EDFD)
val OnPrimaryContainerLight = Color(0xFF001D36)

val SecondaryLight = Color(0xFFD97706)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFFEF3C7)
val OnSecondaryContainerLight = Color(0xFF78350F)

val TertiaryLight = Color(0xFF0D9488)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFCCFBF1)
val OnTertiaryContainerLight = Color(0xFF115E59)

val BackgroundLight = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)

// Dark Theme
val PrimaryDark = Color(0xFF90CAF9)
val OnPrimaryDark = Color(0xFF003258)
val PrimaryContainerDark = Color(0xFF0F2B48)
val OnPrimaryContainerDark = Color(0xFFD0E4FF)

val SecondaryDark = Color(0xFFFBBF24)
val OnSecondaryDark = Color(0xFF451A03)
val SecondaryContainerDark = Color(0xFF78350F)
val OnSecondaryContainerDark = Color(0xFFFEF3C7)

val TertiaryDark = Color(0xFF2DD4BF)
val OnTertiaryDark = Color(0xFF042F2E)
val TertiaryContainerDark = Color(0xFF115E59)
val OnTertiaryContainerDark = Color(0xFFCCFBF1)

val BackgroundDark = Color(0xFF0B0F17)
val OnBackgroundDark = Color(0xFFE2E8F0)
val SurfaceDark = Color(0xFF111827)
val OnSurfaceDark = Color(0xFFF3F4F6)
val SurfaceVariantDark = Color(0xFF1F2937)
val OnSurfaceVariantDark = Color(0xFF9CA3AF)
