package com.multisensor.recording.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.multisensor.recording.ui.compose.navigation.MainNavigation
import com.multisensor.recording.ui.theme.MultiSensorTheme
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var logger: Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        enableEdgeToEdge()

        try {
            setContent {
                MultiSensorTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainNavigation()
                    }
                }
            }

            logger.info("ComposeMainActivity initialized successfully")

        } catch (e: SecurityException) {
            logger.error("Permission error during ComposeMainActivity initialization: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state during ComposeMainActivity initialization: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error during ComposeMainActivity initialization: ${e.message}", e)
        }
    }
}
