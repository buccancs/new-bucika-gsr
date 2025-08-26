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
        
        binding.temperatureOverlay?.isVisible = show
        
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

            editControlPanel?.isVisible = false
            temperatureRangePanel?.isVisible = false
            
            imageView?.isVisible = true
            basicControlPanel?.isVisible = true
        }
    }
    
    private fun showEditModeUI() {
        binding.apply {

            editControlPanel?.isVisible = true
            temperatureRangePanel?.isVisible = true
            
            imageView?.isVisible = true
            basicControlPanel?.isVisible = true
        }
        
        isEditModeEnabled = true
    }
    
    private fun showMeasurementModeUI() {
        binding.apply {

            temperatureOverlay?.isVisible = true
            measurementControls?.isVisible = true
            
            temperatureRangePanel?.isVisible = true
        }
    }
    
    private fun showSettingsModeUI() {
        binding.apply {

            settingsPanel?.isVisible = true
            
            editControlPanel?.isVisible = false
            measurementControls?.isVisible = false
        }
    }
    
    private fun setupUIControls() {

        binding.apply {
            temperatureOverlay?.isVisible = false
            editControlPanel?.isVisible = false
            settingsPanel?.isVisible = false
            measurementControls?.isVisible = false
        }
    }
    
    private fun setupEventListeners() {
        binding.apply {

            viewModeButton?.setOnClickListener {
                setUIMode(UIMode.VIEW)
            }
            
            editModeButton?.setOnClickListener {
                setUIMode(UIMode.EDIT)
            }
            
            measurementModeButton?.setOnClickListener {
                setUIMode(UIMode.MEASUREMENT)
            }
            
            settingsButton?.setOnClickListener {
                setUIMode(UIMode.SETTINGS)
            }
            
            temperatureToggleButton?.setOnClickListener {
                toggleTemperatureOverlay(!isTemperatureOverlayVisible)
            }
        }
    }
    
    private fun updateTemperatureControls(showOverlay: Boolean) {
        binding.apply {
            temperatureRangePanel?.isVisible = showOverlay
            temperatureInfoPanel?.isVisible = showOverlay
            
            if (showOverlay) {

                updateTemperatureDisplay(currentMinTemp, currentMaxTemp)
            }
        }
    }
    
    private fun updateTemperatureDisplay(minTemp: Float, maxTemp: Float) {
        binding.apply {
            minTemperatureText?.text = String.format("%.1f°C", minTemp)
            maxTemperatureText?.text = String.format("%.1f°C", maxTemp)
            rangeTemperatureText?.text = String.format("Range: %.1f°C", maxTemp - minTemp)
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
        binding.progressBar?.isVisible = show
        binding.loadingOverlay?.isVisible = show
    }
    
    fun showErrorState(message: String) {
        binding.errorText?.apply {
            text = message
            isVisible = true
        }
        binding.errorPanel?.isVisible = true
    }
    
    fun hideErrorState() {
        binding.errorPanel?.isVisible = false
        binding.errorText?.isVisible = false
    }
    
    fun cleanup() {
        onRangeChangeListener = null
    }
