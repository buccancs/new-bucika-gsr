package com.topdon.tc001.managers

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topdon.tc001.databinding.ActivityIrGalleryEditBinding
import kotlinx.coroutines.launch

/**
 * Gallery UI State Manager
 * 
 * Manages all UI state operations for the IRGalleryEditActivity including:
 * - Temperature measurement overlay management
 * - Seekbar and control panel state
 * - Dialog and popup management
 * - UI element visibility and interaction states
 * - User input validation and feedback
 * 
 * This manager centralizes UI state logic to improve maintainability and
 * provide consistent UI behavior across different user interaction scenarios.
 * 
 * @see IRGalleryEditActivity
 */
class GalleryUIStateManager(
    private val binding: ActivityIrGalleryEditBinding,
    private val lifecycleOwner: LifecycleOwner
) {
    
    companion object {
        private const val TAG = "GalleryUIStateManager"
        private const val ANIMATION_DURATION = 200L
        private const val MIN_TEMP_RANGE = 5.0f
    }
    
    // UI state tracking
    private var isTemperatureOverlayVisible = false
    private var isSeekBarActive = false
    private var isEditModeEnabled = false
    private var currentUIMode = UIMode.VIEW
    
    // Temperature range parameters
    private var currentMinTemp = 0f
    private var currentMaxTemp = 10000f
    
    /**
     * UI operation modes
     */
    enum class UIMode {
        VIEW,           // View-only mode
        EDIT,           // Edit mode with controls
        MEASUREMENT,    // Temperature measurement mode
        SETTINGS        // Settings and configuration mode
    }
    
    /**
     * Initialize the UI state manager
     */
    fun initialize() {
        setupUIControls()
        setupEventListeners()
        setUIMode(UIMode.VIEW)
    }
    
    /**
     * Set the UI mode and update all related UI elements
     */
    fun setUIMode(mode: UIMode) {
        currentUIMode = mode
        
        lifecycleOwner.lifecycleScope.launch {
            updateUIForMode(mode)
        }
    }
    
    /**
     * Toggle temperature measurement overlay visibility
     */
    fun toggleTemperatureOverlay(show: Boolean) {
        isTemperatureOverlayVisible = show
        
        binding.temperatureOverlay?.isVisible = show
        
        // Update related controls
        updateTemperatureControls(show)
    }
    
    /**
     * Setup temperature range seekbar
     */

    
    /**
     * Update UI elements for specific mode
     */
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
    
    /**
     * Configure UI for view mode
     */
    private fun showViewModeUI() {
        binding.apply {
            // Hide editing controls
            editControlPanel?.isVisible = false
            temperatureRangePanel?.isVisible = false
            
            // Show basic view controls
            imageView?.isVisible = true
            basicControlPanel?.isVisible = true
        }
    }
    
    /**
     * Configure UI for edit mode
     */
    private fun showEditModeUI() {
        binding.apply {
            // Show editing controls
            editControlPanel?.isVisible = true
            temperatureRangePanel?.isVisible = true
            
            // Show image view
            imageView?.isVisible = true
            basicControlPanel?.isVisible = true
        }
        
        isEditModeEnabled = true
    }
    
    /**
     * Configure UI for measurement mode
     */
    private fun showMeasurementModeUI() {
        binding.apply {
            // Show measurement overlay
            temperatureOverlay?.isVisible = true
            measurementControls?.isVisible = true
            
            // Show temperature range controls
            temperatureRangePanel?.isVisible = true
        }
    }
    
    /**
     * Configure UI for settings mode
     */
    private fun showSettingsModeUI() {
        binding.apply {
            // Show settings panel
            settingsPanel?.isVisible = true
            
            // Hide other panels temporarily
            editControlPanel?.isVisible = false
            measurementControls?.isVisible = false
        }
    }
    
    /**
     * Setup UI controls and initial state
     */
    private fun setupUIControls() {
        // Initialize all UI elements to default state
        binding.apply {
            temperatureOverlay?.isVisible = false
            editControlPanel?.isVisible = false
            settingsPanel?.isVisible = false
            measurementControls?.isVisible = false
        }
    }
    
    /**
     * Setup event listeners for UI elements
     */
    private fun setupEventListeners() {
        binding.apply {
            // Setup click listeners for mode switching
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
            
            // Setup overlay toggle
            temperatureToggleButton?.setOnClickListener {
                toggleTemperatureOverlay(!isTemperatureOverlayVisible)
            }
        }
    }
    
    /**
     * Update temperature-related controls
     */
    private fun updateTemperatureControls(showOverlay: Boolean) {
        binding.apply {
            temperatureRangePanel?.isVisible = showOverlay
            temperatureInfoPanel?.isVisible = showOverlay
            
            if (showOverlay) {
                // Update temperature display
                updateTemperatureDisplay(currentMinTemp, currentMaxTemp)
            }
        }
    }
    
    /**
     * Update temperature display values
     */
    private fun updateTemperatureDisplay(minTemp: Float, maxTemp: Float) {
        binding.apply {
            minTemperatureText?.text = String.format("%.1f°C", minTemp)
            maxTemperatureText?.text = String.format("%.1f°C", maxTemp)
            rangeTemperatureText?.text = String.format("Range: %.1f°C", maxTemp - minTemp)
        }
    }
    
    /**
     * Handle temperature range change from seekbar
     */
    private fun onTemperatureRangeChanged(leftValue: Float, rightValue: Float) {
        currentMinTemp = leftValue
        currentMaxTemp = rightValue
        
        // Update display
        updateTemperatureDisplay(leftValue, rightValue)
        
        // Notify listeners (would be implemented based on actual callback needs)
        onRangeChangeListener?.invoke(leftValue, rightValue)
    }
    
    /**
     * Set temperature range change listener
     */
    var onRangeChangeListener: ((Float, Float) -> Unit)? = null
    
    /**
     * Get current UI state information
     */
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
    
    /**
     * UI state data class
     */
    data class UIState(
        val mode: UIMode,
        val isTemperatureOverlayVisible: Boolean,
        val isSeekBarActive: Boolean,
        val isEditModeEnabled: Boolean,
        val currentMinTemp: Float,
        val currentMaxTemp: Float
    )
    
    /**
     * Show loading state
     */
    fun showLoadingState(show: Boolean) {
        binding.progressBar?.isVisible = show
        binding.loadingOverlay?.isVisible = show
    }
    
    /**
     * Show error state with message
     */
    fun showErrorState(message: String) {
        binding.errorText?.apply {
            text = message
            isVisible = true
        }
        binding.errorPanel?.isVisible = true
    }
    
    /**
     * Hide error state
     */
    fun hideErrorState() {
        binding.errorPanel?.isVisible = false
        binding.errorText?.isVisible = false
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        onRangeChangeListener = null
    }
