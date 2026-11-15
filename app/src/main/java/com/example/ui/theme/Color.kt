package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.ProjectAccent

val PaperBackgroundLight = Color(0xFFFAF8F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1EDE6)
val SurfaceContainerLowLight = Color(0xFFF5F2EC)
val SurfaceContainerHighLight = Color(0xFFE9E4DA)
val OutlineLight = Color(0xFFD8D1C5)
val InkDark = Color(0xFF181A1F)
val InkMutedLight = Color(0xFF5C6270)

val CobaltPrimaryLight = Color(0xFF2452EB)
val CobaltContainerLight = Color(0xFFDCE5FF)
val CobaltOnContainerLight = Color(0xFF0A2068)

val TerracottaSecondaryLight = Color(0xFFD96236)
val TerracottaContainerLight = Color(0xFFFFE2D6)
val TerracottaOnContainerLight = Color(0xFF5E1F09)

val EmeraldTertiaryLight = Color(0xFF15803D)
val EmeraldContainerLight = Color(0xFFD6F5E0)
val EmeraldOnContainerLight = Color(0xFF073B1B)

val ErrorLight = Color(0xFFC93B3B)
val ErrorContainerLight = Color(0xFFFCE4E4)
val OnErrorContainerLight = Color(0xFF5F1414)

val SlateBackgroundDark = Color(0xFF111318)
val SurfaceDark = Color(0xFF191C24)
val SurfaceVariantDark = Color(0xFF232834)
val SurfaceContainerLowDark = Color(0xFF151820)
val SurfaceContainerHighDark = Color(0xFF2B3140)
val OutlineDark = Color(0xFF343B4D)
val InkLight = Color(0xFFF3F1EC)
val InkMutedDark = Color(0xFFA2A8B6)

val CobaltPrimaryDark = Color(0xFF7096FF)
val CobaltContainerDark = Color(0xFF1C337A)
val CobaltOnContainerDark = Color(0xFFDCE5FF)

val TerracottaSecondaryDark = Color(0xFFF28B66)
val TerracottaContainerDark = Color(0xFF6B2B14)
val TerracottaOnContainerDark = Color(0xFFFFE2D6)

val EmeraldTertiaryDark = Color(0xFF4ADE80)
val EmeraldContainerDark = Color(0xFF114D27)
val EmeraldOnContainerDark = Color(0xFFD6F5E0)

val ErrorDark = Color(0xFFF27979)
val ErrorContainerDark = Color(0xFF5C1B1B)
val OnErrorContainerDark = Color(0xFFFCE4E4)

val UrgentRed = Color(0xFFD93838)
val HighAmber = Color(0xFFD97706)
val MediumCobalt = Color(0xFF2452EB)
val LowSage = Color(0xFF4B7F62)

fun ProjectAccent.color(isDark: Boolean): Color = when (this) {
  ProjectAccent.COBALT -> if (isDark) Color(0xFF7096FF) else Color(0xFF2452EB)
  ProjectAccent.TERRACOTTA -> if (isDark) Color(0xFFF28B66) else Color(0xFFD96236)
  ProjectAccent.EMERALD -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
  ProjectAccent.AMBER -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
  ProjectAccent.PLUM -> if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
  ProjectAccent.TEAL -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
}

fun ProjectAccent.containerColor(isDark: Boolean): Color = when (this) {
  ProjectAccent.COBALT -> if (isDark) Color(0xFF1D2F66) else Color(0xFFE3EBFF)
  ProjectAccent.TERRACOTTA -> if (isDark) Color(0xFF592614) else Color(0xFFFFE5DB)
  ProjectAccent.EMERALD -> if (isDark) Color(0xFF114223) else Color(0xFFD9F7E3)
  ProjectAccent.AMBER -> if (isDark) Color(0xFF52390B) else Color(0xFFFEF0C7)
  ProjectAccent.PLUM -> if (isDark) Color(0xFF3E1863) else Color(0xFFF3E8FF)
  ProjectAccent.TEAL -> if (isDark) Color(0xFF11403B) else Color(0xFFCCFBF1)
}
