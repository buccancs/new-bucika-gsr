package com.multisensor.recording.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @Test
    fun customColors_areNotNull() {
        assertNotNull(RecordingActive)
        assertNotNull(RecordingInactive)
        assertNotNull(ConnectionGreen)
        assertNotNull(DisconnectedRed)
        assertNotNull(CalibrationBlue)
    }

    @Test
    fun customColors_haveCorrectValues() {
        assertEquals(Color(0xFFE57373), RecordingActive)
        assertEquals(Color(0xFF9E9E9E), RecordingInactive)
        assertEquals(Color(0xFF4CAF50), ConnectionGreen)
        assertEquals(Color(0xFFF44336), DisconnectedRed)
        assertEquals(Color(0xFF2196F3), CalibrationBlue)
    }

    @Test
    fun material3Colors_areCorrect() {
        assertEquals(Color(0xFFD0BCFF), Purple80)
        assertEquals(Color(0xFF6650a4), Purple40)
    }
}
