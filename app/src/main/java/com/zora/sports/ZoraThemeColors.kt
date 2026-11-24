
package com.zora.sports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ZoraThemeColors {

    var isLight by mutableStateOf(false)

    val background: Color
        get() = if (isLight) Color(0xFFF4F7FA) else Color(0xFF071321)

    val panel: Color
        get() = if (isLight) Color.White else Color(0xFF102337)

    val panelLight: Color
        get() = if (isLight) Color(0xFFE8EEF3) else Color(0xFF18334A)

    val accent: Color
        get() = if (isLight) Color(0xFF087F9A) else Color(0xFF42DDF5)

    val text: Color
        get() = if (isLight) Color(0xFF142536) else Color(0xFFF4FAFF)

    val muted: Color
        get() = if (isLight) Color(0xFF647789) else Color(0xFF91A5B8)

    val border: Color
        get() = if (isLight) Color(0xFFD7E0E8) else Color(0xFF294156)

    val navigation: Color
        get() = if (isLight) Color(0xFFE7EDF2) else Color(0xFF0B1B2B)
}
