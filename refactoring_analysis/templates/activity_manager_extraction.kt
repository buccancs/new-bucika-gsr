// Template: Activity Manager Extraction Pattern

// Before: Large Activity (3000+ lines)
class IRThermalNightActivity : BaseActivity {
    // Camera management code (500+ lines)
    // UI state management code (800+ lines)  
    // Data processing code (600+ lines)
    // Event handling code (400+ lines)
    // Configuration code (300+ lines)
}

// After: Modular Activity with Managers
class IRThermalNightActivity : BaseActivity {
    private lateinit var cameraManager: ThermalCameraManager
    private lateinit var uiStateManager: ThermalUIStateManager
    private lateinit var dataProcessor: ThermalDataProcessor
    private lateinit var configurationManager: ThermalConfigurationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeManagers()
    }
    
    private fun initializeManagers() {
        cameraManager = ThermalCameraManager(this)
        uiStateManager = ThermalUIStateManager(this)
        dataProcessor = ThermalDataProcessor()
        configurationManager = ThermalConfigurationManager(this)
    }
}

// Extracted Manager Example
class ThermalCameraManager(private val activity: Activity) {
    
    fun initializeCamera() {
        // Camera initialization logic (previously 500+ lines in activity)
    }
    
    fun handleCameraEvents(event: CameraEvent) {
        // Camera event handling logic
    }
    
    fun configureCamera(settings: CameraSettings) {
        // Camera configuration logic  
    }
}
