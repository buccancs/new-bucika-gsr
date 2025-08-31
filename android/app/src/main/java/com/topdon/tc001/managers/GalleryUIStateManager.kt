package com.topdon.tc001.managers

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.databinding.ActivityIrGalleryEditBinding
import kotlinx.coroutines.launch

class GalleryUIStateManager(
    private val binding: ActivityIrGalleryEditBinding,
    private val lifecycleOwner: LifecycleOwner
) {
    
    companion object {
        private const val TAG = "GalleryUIStateManager"
        private const val ANIMATION_DURATION = 200L
        private const val MIN_TEMP_RANGE = 5.0f
    }
    
    private var isTemperatureOverlayVisible = false
    private var isSeekBarActive = false
    private var isEditModeEnabled = false
    private var currentUIMode = UIMode.VIEW
    
    private var currentMinTemp = 0f
    private var currentMaxTemp = 10000f
    
    enum class UIMode {
        VIEW,
        EDIT,
        MEASUREMENT,
        SETTINGS
    }
    
    fun initialize() {
        setupUIControls()
        setupEventListeners()
        setUIMode(UIMode.VIEW)
    }
    
    fun setUIMode(mode: UIMode) {
        currentUIMode = mode
        
        lifecycleOwner.lifecycleScope.launch {
            updateUIForMode(mode)
        }
    }
    
    fun toggleTemperatureOverlay(show: Boolean) {
        isTemperatureOverlayVisible = show
        
        binding.temperatureView.isVisible = show
        
        updateTemperatureControls(show)
    }
    
    private suspend fun updateUIForMode(mode: UIMode) {
        when (mode) {
            UIMode.VIEW -> {
                showViewModeUI()
            }
            UIMode.EDIT -> {
                showEditModeUI()
            }
            UIMode.MEASUREMENT -> {
                showMeasurementModeUI()
            }
            UIMode.SETTINGS -> {
                showSettingsModeUI()
            }
        }
    }
    
    private fun showViewModeUI() {
        binding.apply {
            // Hide editing controls in view mode
            editRecyclerSecond.isVisible = false
            editRecyclerFirst.isVisible = false
            
            // Show main image view
            irImageView.isVisible = true
        }
    }
    
    private fun showEditModeUI() {
        binding.apply {
            // Show editing controls in edit mode
            editRecyclerSecond.isVisible = true
            editRecyclerFirst.isVisible = true
            
            // Show main image view
            irImageView.isVisible = true
        }
        
        isEditModeEnabled = true
    }
    
    private fun showMeasurementModeUI() {
        binding.apply {
            // Show temperature overlay for measurement mode
            temperatureView.isVisible = true
            colorBarView.isVisible = true
            
            // Show editing controls for measurement
            editRecyclerSecond.isVisible = true
            editRecyclerFirst.isVisible = true
        }
    }
    
    private fun showSettingsModeUI() {
        binding.apply {
            // Hide editing controls in settings mode
            editRecyclerSecond.isVisible = false
            editRecyclerFirst.isVisible = false
            
            // Show temperature controls for settings
            colorBarView.isVisible = true
        }
    }
    
    private fun setupUIControls() {
        binding.apply {
            // Initialize UI controls visibility
            temperatureView.isVisible = false
            editRecyclerSecond.isVisible = false
            editRecyclerFirst.isVisible = false
            colorBarView.isVisible = false
        }
    }
    
    private fun setupEventListeners() {
        binding.apply {
            // Setup temperature lock/unlock button
            temperatureIvLock.setOnClickListener {
                // Toggle temperature lock state
                isSeekBarActive = !isSeekBarActive
                updateTemperatureLockUI()
            }
            
            // Setup temperature input button
            temperatureIvInput.setOnClickListener {
                setUIMode(UIMode.EDIT)
            }
            
            // Temperature view click to toggle overlay
            temperatureView.setOnClickListener {
                toggleTemperatureOverlay(!isTemperatureOverlayVisible)
            }
        }
    }
    
    private fun updateTemperatureControls(showOverlay: Boolean) {
        binding.apply {
            // Show/hide temperature-related controls
            colorBarView.isVisible = showOverlay
            
            if (showOverlay) {
                updateTemperatureDisplay(currentMinTemp, currentMaxTemp)
            }
        }
    }
    
    private fun updateTemperatureDisplay(minTemp: Float, maxTemp: Float) {
        binding.apply {
            // Update temperature display text
            tvTempContent.text = String.format("%.1f°C - %.1f°C", minTemp, maxTemp)
        }
    }
    
    private fun updateTemperatureLockUI() {
        binding.apply {
            val lockIcon = if (isSeekBarActive) {
                com.topdon.lib.core.R.drawable.svg_pseudo_bar_lock
            } else {
                com.topdon.lib.core.R.drawable.svg_pseudo_bar_unlock
            }
            temperatureIvLock.setImageResource(lockIcon)
        }
    }
    
    private fun onTemperatureRangeChanged(leftValue: Float, rightValue: Float) {
        currentMinTemp = leftValue
        currentMaxTemp = rightValue
        
        updateTemperatureDisplay(leftValue, rightValue)
        
        onRangeChangeListener?.invoke(leftValue, rightValue)
    }
    
    var onRangeChangeListener: ((Float, Float) -> Unit)? = null
    
    fun getCurrentUIState(): UIState {
        return UIState(
            mode = currentUIMode,
            isTemperatureOverlayVisible = isTemperatureOverlayVisible,
            isSeekBarActive = isSeekBarActive,
            isEditModeEnabled = isEditModeEnabled,
            currentMinTemp = currentMinTemp,
            currentMaxTemp = currentMaxTemp
        )
    }
    
    data class UIState(
        val mode: UIMode,
        val isTemperatureOverlayVisible: Boolean,
        val isSeekBarActive: Boolean,
        val isEditModeEnabled: Boolean,
        val currentMinTemp: Float,
        val currentMaxTemp: Float
    )
    
    fun showLoadingState(show: Boolean) {
        binding.apply {
            // Show/hide loading state using existing UI components
            irImageView.isVisible = !show
            if (show) {
                // Could add a loading overlay here if needed
                titleView.setTitleText("Loading...")
            } else {
                titleView.setTitleText("Gallery Edit")
            }
        }
    }
    
    fun showErrorState(message: String) {
        binding.apply {
            // Show error state using existing UI components
            titleView.setTitleText("Error: $message")
            // Could show error overlay on the image view
        }
    }
    
    fun hideErrorState() {
        binding.apply {
            // Hide error state and restore normal UI
            titleView.setTitleText("Gallery Edit")
        }
    }
    
    fun cleanup() {
        onRangeChangeListener = null
    }
}
