package io.github.grupo6.capturaprospectos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.data.local.openContextSession
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.ui.context.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext
        val owner = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ContextSessionOwner::class.java)
                return ContextSessionOwner({ openContextSession(appContext) }) as T
            }
        })[ContextSessionOwner::class.java]
        setContent {
            CapturaProspectosG6Theme {
                val session by owner.uiState.collectAsStateWithLifecycle()
                Scaffold(Modifier.fillMaxSize()) { padding ->
                    val modifier = Modifier.fillMaxSize().padding(padding)
                    when (val current = session) {
                        ContextSessionState.Opening -> DemoContextScreen(DemoContextUiState(), {}, {}, {}, {}, {}, modifier)
                        ContextSessionState.Unavailable -> DemoContextScreen(
                            DemoContextUiState(DemoContextState.StorageUnavailable), {}, owner::retry, {}, {}, {}, modifier)
                        is ContextSessionState.Ready -> DemoContextRoute(current.viewModel, modifier)
                    }
                }
            }
        }
    }
}
