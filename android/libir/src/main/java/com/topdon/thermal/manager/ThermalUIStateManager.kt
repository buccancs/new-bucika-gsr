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

class ThermalUIStateManager(
    activity: Activity,
    private val binding: ActivityThermalIrNightBinding,
    private val lifecycleOwner: LifecycleOwner
) {
    private val activityRef = WeakReference(activity)
    private val context: Context get() = activityRef.get() ?: throw IllegalStateException("Activity reference lost")
    
    private var isFullScreen: Boolean = false
    private var currentOrientation: Int = Configuration.ORIENTATION_PORTRAIT
    private var isRecording: Boolean = false
    private var isAmplifyEnabled: Boolean = false
    
    private var cameraItemAdapter: CameraItemAdapter? = null
    private var measureItemAdapter: MeasureItemAdapter? = null
    private var targetItemAdapter: TargetItemAdapter? = null
    
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
    
    private fun setupRecyclerViews() {
        setupCameraRecyclerView()
        setupMeasureRecyclerView()
        setupTargetRecyclerView()
    }
    
    private fun setupCameraRecyclerView() {
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 3)
            cameraItemAdapter = CameraItemAdapter { cameraItem ->
                handleCameraItemClick(cameraItem)
            }
            adapter = cameraItemAdapter
        }
    }
    
    private fun setupMeasureRecyclerView() {
        binding.measureRecycler?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            measureItemAdapter = MeasureItemAdapter { measureItem ->
                handleMeasureItemClick(measureItem)
            }
            adapter = measureItemAdapter
        }
    }
    
    private fun setupTargetRecyclerView() {
        binding.targetRecycler?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            targetItemAdapter = TargetItemAdapter { targetItem ->
                handleTargetItemClick(targetItem)
            }
            adapter = targetItemAdapter
        }
    }
    
    private fun setupUIListeners() {

    }
    
    private fun configureInitialStates() {

        updateLoadingState(false)
        updateRecordingState(false)
        updateAmplifyState(false)
        
        handleOrientationChange(context.resources.configuration.orientation)
    }
    
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
    
    private fun configurePortraitLayout() {
        activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 3)
        }
        
        updateUIForOrientation(false)
    }
    
    private fun configureLandscapeLayout() {
        activityRef.get()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        binding.thermalRecyclerNight.apply {
            layoutManager = GridLayoutManager(context, 5)
        }
        
        updateUIForOrientation(true)
    }
    
    private fun updateRecyclerViewLayouts() {
        val isLandscape = currentOrientation == Configuration.ORIENTATION_LANDSCAPE
        
        (binding.thermalRecyclerNight.layoutManager as? GridLayoutManager)?.spanCount = 
            if (isLandscape) 5 else 3
            
        cameraItemAdapter?.notifyDataSetChanged()
        measureItemAdapter?.notifyDataSetChanged()
        targetItemAdapter?.notifyDataSetChanged()
    }
    
    private fun updateUIForOrientation(isLandscape: Boolean) {

        binding.apply {

            if (isLandscape) {

            } else {

            }
        }
    }
    
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
    
    fun updateLoadingState(isLoading: Boolean) {
        binding.loadingIndicator?.isVisible = isLoading
        
        binding.thermalRecyclerNight.isEnabled = !isLoading
        binding.temperatureSeekbar?.isEnabled = !isLoading
        
        Log.d(TAG, "Loading state: $isLoading")
    }
    
    fun updateRecordingState(recording: Boolean) {
        isRecording = recording
        
        binding.recordingIndicator?.isVisible = recording
        binding.recordButton?.isSelected = recording
        
        Log.d(TAG, "Recording state: $recording")
    }
    
    fun updateAmplifyState(enabled: Boolean) {
        isAmplifyEnabled = enabled
        
        binding.amplifyButton?.isSelected = enabled
        binding.amplifyIndicator?.isVisible = enabled
        
        Log.d(TAG, "Amplify state: $enabled")
    }
    
    private fun handleCameraItemClick(cameraItem: Any) {

        Log.d(TAG, "Camera item clicked: $cameraItem")

    }
    
    private fun handleMeasureItemClick(measureItem: Any) {

        Log.d(TAG, "Measure item clicked: $measureItem")
    }
    
    private fun handleTargetItemClick(targetItem: Any) {

        Log.d(TAG, "Target item clicked: $targetItem")
    }
    
    fun updateTemperatureDisplay(temperature: String) {
        binding.temperatureText?.text = temperature
    }
    
    fun updateStatusInfo(status: String) {
        binding.statusText?.text = status
    }
    
    fun setComponentVisibility(componentId: UIComponent, visible: Boolean) {
        when (componentId) {
            UIComponent.TEMPERATURE_SEEKBAR -> binding.temperatureSeekbar?.isVisible = visible
            UIComponent.MEASURE_RECYCLER -> binding.measureRecycler?.isVisible = visible
            UIComponent.TARGET_RECYCLER -> binding.targetRecycler?.isVisible = visible
            UIComponent.CAMERA_RECYCLER -> binding.thermalRecyclerNight.isVisible = visible
            UIComponent.LOADING_INDICATOR -> binding.loadingIndicator?.isVisible = visible
        }
    }
    
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
