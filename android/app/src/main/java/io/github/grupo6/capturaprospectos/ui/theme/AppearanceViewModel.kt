package io.github.grupo6.capturaprospectos.ui.theme

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencia visual sin datos personales; sobrevive a giro y a una nueva apertura. */
class AppearanceViewModel(private val preferences: SharedPreferences) : ViewModel() {
    private val mutable = MutableStateFlow(preferences.getBoolean("dark_mode", true))
    val darkMode = mutable.asStateFlow()
    fun setDarkMode(value: Boolean) {
        mutable.value = value
        preferences.edit().putBoolean("dark_mode", value).apply()
    }
}
