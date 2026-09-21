package com.auralis.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// BACKWARDS-COMPATIBLE IDENTIFIERS
// =============================================================================

val AuralisPrimary = Color(0xFF7C3AED)
val AuralisOnPrimary = Color(0xFFFFFFFF)
val AuralisPrimaryContainer = Color(0xFFEDE9FE)
val AuralisOnPrimaryContainer = Color(0xFF2E1065)

val AuralisSecondary = Color(0xFF64748B)
val AuralisOnSecondary = Color(0xFFFFFFFF)
val AuralisSecondaryContainer = Color(0xFFF1F5F9)
val AuralisOnSecondaryContainer = Color(0xFF0F172A)

val AuralisTertiary = Color(0xFF0D9488)
val AuralisOnTertiary = Color(0xFFFFFFFF)
val AuralisTertiaryContainer = Color(0xFFCCFBF1)
val AuralisOnTertiaryContainer = Color(0xFF115E59)

val AuralisBackground = Color(0xFFF8FAFC)
val AuralisOnBackground = Color(0xFF0F172A)
val AuralisSurface = Color(0xFFFFFFFF)
val AuralisOnSurface = Color(0xFF0F172A)

// Dark theme
val AuralisPrimaryDark = Color(0xFFA78BFA)
val AuralisOnPrimaryDark = Color(0xFF2E1065)
val AuralisPrimaryContainerDark = Color(0xFF4C1D95)
val AuralisOnPrimaryContainerDark = Color(0xFFEDE9FE)

val AuralisSecondaryDark = Color(0xFF94A3B8)
val AuralisOnSecondaryDark = Color(0xFF0F172A)
val AuralisSecondaryContainerDark = Color(0xFF1E293B)
val AuralisOnSecondaryContainerDark = Color(0xFFF1F5F9)

val AuralisTertiaryDark = Color(0xFF2DD4BF)
val AuralisOnTertiaryDark = Color(0xFF042F2C)
val AuralisTertiaryContainerDark = Color(0xFF115E59)
val AuralisOnTertiaryContainerDark = Color(0xFFCCFBF1)

val AuralisBackgroundDark = Color(0xFF0F0E17)
val AuralisOnBackgroundDark = Color(0xFFF1F5F9)
val AuralisSurfaceDark = Color(0xFF161522)
val AuralisOnSurfaceDark = Color(0xFFF1F5F9)

// AMOLED Pure Black theme
val AuralisBackgroundAmoled = Color(0xFF000000)
val AuralisOnBackgroundAmoled = Color(0xFFEDEDED)
val AuralisSurfaceAmoled = Color(0xFF000000)
val AuralisOnSurfaceAmoled = Color(0xFFEDEDED)
val AuralisSurfaceVariantAmoled = Color(0xFF141414)
val AuralisOnSurfaceVariantAmoled = Color(0xFFB0B0B0)

// =============================================================================
// REFINED MELOFY DESIGN SYSTEM PALETTE
// =============================================================================

// Dark Surface Hierarchy (Obsidian & Deep Slate)
val MelofyBackgroundDark = Color(0xFF0E0D14)
val MelofySurfaceDark = Color(0xFF161520)
val MelofySurfaceVariantDark = Color(0xFF222030)
val MelofySurfaceContainerDark = Color(0xFF1B1926)
val MelofySurfaceContainerHighDark = Color(0xFF262436)
val MelofySurfaceContainerHighestDark = Color(0xFF323046)
val MelofyOutlineDark = Color(0xFF403D56)
val MelofyOutlineVariantDark = Color(0xFF292738)

// Light Surface Hierarchy (Crisp & Clean)
val MelofyBackgroundLight = Color(0xFFF8F9FD)
val MelofySurfaceLight = Color(0xFFFFFFFF)
val MelofySurfaceVariantLight = Color(0xFFEEF0F8)
val MelofySurfaceContainerLight = Color(0xFFF1F3FA)
val MelofySurfaceContainerHighLight = Color(0xFFE8EAF3)
val MelofySurfaceContainerHighestLight = Color(0xFFDFE2EE)
val MelofyOutlineLight = Color(0xFFCBD0E2)
val MelofyOutlineVariantLight = Color(0xFFE2E6F2)

// Pure AMOLED Black
val MelofyBackgroundAmoled = Color(0xFF000000)
val MelofySurfaceAmoled = Color(0xFF000000)
val MelofySurfaceVariantAmoled = Color(0xFF121212)
val MelofySurfaceContainerAmoled = Color(0xFF080808)
val MelofySurfaceContainerHighAmoled = Color(0xFF161616)
val MelofySurfaceContainerHighestAmoled = Color(0xFF222222)
val MelofyOutlineAmoled = Color(0xFF2A2A2A)

// =============================================================================
// CURATED ACCENT COLOR THEMES
// =============================================================================

enum class MelofyAccent(
    val displayName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val containerLight: Color,
    val containerDark: Color
) {
    VIOLET(
        displayName = "Melofy Violet",
        primaryLight = Color(0xFF7C3AED),
        primaryDark = Color(0xFFA78BFA),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF4C1D95)
    ),
    BLUE(
        displayName = "Electric Blue",
        primaryLight = Color(0xFF2563EB),
        primaryDark = Color(0xFF60A5FA),
        containerLight = Color(0xFFDBEAFE),
        containerDark = Color(0xFF1E3A8A)
    ),
    EMERALD(
        displayName = "Emerald Green",
        primaryLight = Color(0xFF059669),
        primaryDark = Color(0xFF34D399),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF064E3B)
    ),
    CORAL(
        displayName = "Sunset Coral",
        primaryLight = Color(0xFFE11D48),
        primaryDark = Color(0xFFFB7185),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF881337)
    ),
    AMBER(
        displayName = "Golden Amber",
        primaryLight = Color(0xFFD97706),
        primaryDark = Color(0xFFFBBF24),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF78350F)
    )
}
