package com.topdon.thermal.manager

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.thermal.adapter.CameraItemAdapter
import com.topdon.thermal.adapter.MeasureItemAdapter
import com.topdon.thermal.adapter.TargetItemAdapter
import com.topdon.thermal.databinding.ActivityThermalIrNightBinding
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Thermal UI State Management Component
 * 
 * Extracted from IRThermalNightActivity to handle all UI state management
 * with improved separation of concerns and reduced complexity.
 * 
 * Responsibilities:
 * - UI component state management
 * - Screen orientation handling
 * - RecyclerView setup and management
 * - Loading state management
 * - UI visibility and layout updates
 * - Screen rotation and configuration changes
 */
class ThermalUIStateManager(
    activity: Activity,
    private val binding: ActivityThermalIrNightBinding,
    private val lifecycleOwner: LifecycleOwner
) {
    private val activityRef = WeakReference(activity)
    private val context: Context get() = activityRef.get() ?: throw IllegalStateException("Activity reference lost")
    
    // UI State
    private var isFullScreen: Boolean = false
    private var currentOrientation: Int = Configuration.ORIENTATION_PORTRAIT
    private var isRecording: Boolean = false
    private var isAmplifyEnabled: Boolean = false
    
    // RecyclerView adapters
    private var cameraItemAdapter: CameraItemAdapter? = null
    private var measureItemAdapter: MeasureItemAdapter? = null
    private var targetItemAdapter: TargetItemAdapter? = null
    
    /**
     * Initialize UI components and states
     */
    fun initializeUI() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                setupRecyclerViews()
                setupUIListeners()
                configureInitialStates()
                Log.d(TAG, "UI state manager initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "UI initialization failed", e)
            }
        }
    }
    
    /**
     * Set up RecyclerView components
     */
    private fun setupRecyclerViews() {
        setupCameraRecyclerView()
        setupMeasureRecyclerView()
        setupTargetRecyclerView()
    }
    
    /**
     * Set up camera items RecyclerView
     */
    private fun setupCameraRecyclerView() {
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 3)
            cameraItemAdapter = CameraItemAdapter { cameraItem ->
                handleCameraItemClick(cameraItem)
            }
            adapter = cameraItemAdapter
        }
    }
    
    /**
     * Set up measure items RecyclerView
     */
    private fun setupMeasureRecyclerView() {
        binding.measureRecycler?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            measureItemAdapter = MeasureItemAdapter { measureItem ->
                handleMeasureItemClick(measureItem)
            }
            adapter = measureItemAdapter
        }
    }
    
    /**
     * Set up target items RecyclerView
     */
    private fun setupTargetRecyclerView() {
        binding.targetRecycler?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            targetItemAdapter = TargetItemAdapter { targetItem ->
                handleTargetItemClick(targetItem)
            }
            adapter = targetItemAdapter
        }
    }
    
    /**
     * Set up UI event listeners
     */
    private fun setupUIListeners() {
        // Temperature seekbar listeners will be set up here
        // Button click listeners
        // Touch listeners for custom views
    }
    
    /**
     * Configure initial UI states
     */
    private fun configureInitialStates() {
        // Set initial visibility states
        updateLoadingState(false)
        updateRecordingState(false)
        updateAmplifyState(false)
        
        // Set initial orientation
        handleOrientationChange(context.resources.configuration.orientation)
    }
    
    /**
     * Handle screen orientation changes
     */
    fun handleOrientationChange(orientation: Int) {
        if (currentOrientation == orientation) return
        
        currentOrientation = orientation
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                when (orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> {
                        configurePortraitLayout()
                    }
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        configureLandscapeLayout()
                    }
                }
                
                updateRecyclerViewLayouts()
                Log.d(TAG, "Orientation changed to: ${if (orientation == Configuration.ORIENTATION_PORTRAIT) "Portrait" else "Landscape"}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle orientation change", e)
            }
        }
    }
    
    /**
     * Configure portrait layout
     */
    private fun configurePortraitLayout() {
        activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // Adjust UI components for portrait mode
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 3)
        }
        
        // Adjust other UI elements for portrait
        updateUIForOrientation(false) // false = portrait
    }
    
    /**
     * Configure landscape layout
     */
    private fun configureLandscapeLayout() {
        activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Adjust UI components for landscape mode
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 5)
        }
        
        // Adjust other UI elements for landscape
        updateUIForOrientation(true) // true = landscape
    }
    
    /**
     * Update RecyclerView layouts based on orientation
     */
    private fun updateRecyclerViewLayouts() {
        val isLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
        
        // Update grid span count based on orientation
        (binding.thermalRecyclerNight.layoutManager as? GridLayoutManager)?.spanCount = 
            if (isLandscape) 5 else 3
            
        // Refresh adapters
        cameraItemAdapter?.notifyDataSetChanged()
        measureItemAdapter?.notifyDataSetChanged()
        targetItemAdapter?.notifyDataSetChanged()
    }
    
    /**
     * Update UI elements based on orientation
     */
    private fun updateUIForOrientation(isLandscape: Boolean) {
        // Adjust component visibility and positioning for orientation
        binding.apply {
            // Example adjustments (actual implementation depends on specific UI needs)
            if (isLandscape) {
                // Landscape-specific UI adjustments
            } else {
                // Portrait-specific UI adjustments
            }
        }
    }
    
    /**
     * Toggle full screen mode
     */
    fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        
        activityRef.get()?.let { activity ->
            if (isFullScreen) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        
        Log.d(TAG, "Full screen mode: $isFullScreen")
    }
    
    /**
     * Update loading state
     */
    fun updateLoadingState(isLoading: Boolean) {
        binding.loadingIndicator?.isVisible = isLoading
        
        // Disable/enable UI components during loading
        binding.thermalRecyclerNight.isEnabled = !isLoading
        binding.temperatureSeekbar?.isEnabled = !isLoading
        
        Log.d(TAG, "Loading state: $isLoading")
    }
    
    /**
     * Update recording state
     */
    fun updateRecordingState(recording: Boolean) {
        isRecording = recording
        
        // Update recording UI indicators
        binding.recordingIndicator?.isVisible = recording
        binding.recordButton?.isSelected = recording
        
        Log.d(TAG, "Recording state: $recording")
    }
    
    /**
     * Update amplify state
     */
    fun updateAmplifyState(enabled: Boolean) {
        isAmplifyEnabled = enabled
        
        // Update amplify UI indicators
        binding.amplifyButton?.isSelected = enabled
        binding.amplifyIndicator?.isVisible = enabled
        
        Log.d(TAG, "Amplify state: $enabled")
    }
    
    /**
     * Handle camera item clicks
     */
    private fun handleCameraItemClick(cameraItem: Any) {
        // Handle camera item selection
        Log.d(TAG, "Camera item clicked: $cameraItem")
        // This will delegate to the main activity or use callback
    }
    
    /**
     * Handle measure item clicks
     */
    private fun handleMeasureItemClick(measureItem: Any) {
        // Handle measure item selection
        Log.d(TAG, "Measure item clicked: $measureItem")
    }
    
    /**
     * Handle target item clicks
     */
    private fun handleTargetItemClick(targetItem: Any) {
        // Handle target item selection
        Log.d(TAG, "Target item clicked: $targetItem")
    }
    
    /**
     * Update temperature display
     */
    fun updateTemperatureDisplay(temperature: String) {
        binding.temperatureText?.text = temperature
    }
    
    /**
     * Update status information
     */
    fun updateStatusInfo(status: String) {
        binding.statusText?.text = status
    }
    
    /**
     * Show/hide UI components
     */
    fun setComponentVisibility(componentId: UIComponent, visible: Boolean) {
        when (componentId) {
            UIComponent.TEMPERATURE_SEEKBAR -> binding.temperatureSeekbar?.isVisible = visible
            UIComponent.MEASURE_RECYCLER -> binding.measureRecycler?.isVisible = visible
            UIComponent.TARGET_RECYCLER -> binding.targetRecycler?.isVisible = visible
            UIComponent.CAMERA_RECYCLER -> binding.thermalRecyclerNight.isVisible = visible
            UIComponent.LOADING_INDICATOR -> binding.loadingIndicator?.isVisible = visible
        }
    }
    
    /**
     * Get current UI state
     */
    fun getCurrentState(): UIState {
        return UIState(
            isFullScreen = isFullScreen,
            currentOrientation = currentOrientation,
            isRecording = isRecording,
            isAmplifyEnabled = isAmplifyEnabled
        )
    }
    
    enum class UIComponent {
        TEMPERATURE_SEEKBAR,
        MEASURE_RECYCLER,
        TARGET_RECYCLER,
        CAMERA_RECYCLER,
        LOADING_INDICATOR
    }
    
    data class UIState(
        val isFullScreen: Boolean,
        val currentOrientation: Int,
        val isRecording: Boolean,
        val isAmplifyEnabled: Boolean
    )
    
    companion object {
        private const val TAG = "ThermalUIStateManager"
    }
}