#!/bin/bash

# Complex File Refactoring Script
# Identifies and plans refactoring for high-complexity files

set -e

echo "🔧 BucikaGSR Complex File Refactoring Analysis"
echo "=============================================="

# Create refactoring directory
mkdir -p refactoring_analysis

echo "📊 Analyzing file complexity..."

# Find files over 300 lines (complexity threshold)
echo "1. Large File Analysis (>300 lines):"
find . -name "*.kt" -o -name "*.java" | xargs wc -l | sort -nr | awk '$1 > 300 {print $1 " lines: " $2}' > refactoring_analysis/large_files.txt

large_file_count=$(cat refactoring_analysis/large_files.txt | wc -l)
echo "   - Files >300 lines: $large_file_count"

# Identify the top 10 most complex files
echo "2. Top 10 Most Complex Files:"
head -10 refactoring_analysis/large_files.txt

# Create refactoring strategy document
cat > refactoring_analysis/complexity_reduction_plan.md << 'EOF'
# Complex File Refactoring Plan

## Objective
Reduce complex file percentage from 44% to 35% by applying modular refactoring patterns to the largest and most complex source files.

## Refactoring Strategy

### Phase 1: Critical Complexity Files (>2000 lines)
**Target**: Files that significantly impact maintainability

1. **IRThermalNightActivity.kt (3,324 lines)**
   - **Complexity Issues**: Massive activity class handling multiple concerns
   - **Refactoring Approach**: 
     - Extract UI management to separate managers
     - Apply MVP or MVVM pattern
     - Create specialized handlers for camera, thermal, GSR operations
   - **Target**: Reduce to <800 lines main activity + modular components

2. **IRThermalActivity.kt (2,686 lines)**
   - **Complexity Issues**: Large thermal processing activity
   - **Refactoring Approach**:
     - Extract thermal data processing to service
     - Separate UI state management
     - Create reusable thermal processing components
   - **Target**: Reduce to <600 lines + modular services

### Phase 2: High Complexity Files (1000-2000 lines)

3. **OpencvTools.java (1,719 lines)**
   - **Complexity Issues**: Monolithic image processing utility
   - **Refactoring Approach**:
     - Split into specialized image processing classes
     - Apply Strategy pattern for different algorithms
     - Create focused utility classes per operation type
   - **Target**: 5-6 focused utility classes <400 lines each

4. **TemperatureView.java (1,562 lines)**
   - **Complexity Issues**: Large custom view with mixed responsibilities
   - **Refactoring Approach**:
     - Extract drawing operations to specialized renderers
     - Separate data handling from presentation
     - Create modular view components
   - **Target**: Main view <500 lines + specialized components

5. **ConnectionImpl.java (1,322 lines)**
   - **Complexity Issues**: Complex BLE connection management
   - **Refactoring Approach**:
     - Apply State pattern for connection states
     - Extract protocol handling to separate classes
     - Create connection strategy interfaces
   - **Target**: Main connection <400 lines + state handlers

## Refactoring Patterns to Apply

### 1. Extract Class Pattern
- **When**: Class has multiple responsibilities
- **How**: Create new classes for each distinct responsibility
- **Example**: UI management, data processing, network handling

### 2. Strategy Pattern  
- **When**: Multiple algorithms for same operation
- **How**: Create strategy interfaces with concrete implementations
- **Example**: Different thermal processing modes, connection protocols

### 3. State Pattern
- **When**: Complex state management
- **How**: Create state classes for each distinct state
- **Example**: BLE connection states, camera states

### 4. Template Method Pattern
- **When**: Similar processes with variations
- **How**: Create abstract base class with template methods
- **Example**: Different activity types with common lifecycle

### 5. Factory Pattern
- **When**: Complex object creation
- **How**: Create factory classes for object instantiation
- **Example**: Different sensor types, processing strategies

## Implementation Phases

### Phase 1: Foundation Refactoring (Week 1)
- [ ] Create base interfaces and abstract classes
- [ ] Extract utility methods to focused utility classes
- [ ] Implement factory patterns for object creation

### Phase 2: Core Component Refactoring (Week 2-3)
- [ ] Refactor IRThermalNightActivity using MVP pattern
- [ ] Refactor IRThermalActivity with service extraction
- [ ] Apply Strategy pattern to OpencvTools

### Phase 3: Supporting Component Refactoring (Week 4)
- [ ] Refactor TemperatureView with renderer pattern
- [ ] Apply State pattern to ConnectionImpl
- [ ] Create specialized view components

### Phase 4: Validation and Testing (Week 5)
- [ ] Update all affected tests
- [ ] Verify functionality after refactoring
- [ ] Measure complexity reduction results
- [ ] Update documentation

## Success Metrics

### Before Refactoring
- Complex files (>300 lines): 44% of codebase
- Average file size: High variance
- Maintainability score: Moderate

### Target After Refactoring  
- Complex files (>300 lines): <35% of codebase
- Average complexity per file: Reduced by 30%
- Maintainability score: High
- Code reusability: Improved through modular design

## Risk Mitigation

### Testing Strategy
- Comprehensive unit tests for extracted components
- Integration tests for refactored activities
- Regression tests for critical functionality

### Incremental Approach
- Refactor one file at a time
- Maintain backward compatibility during transition
- Use feature flags for new implementations

### Review Process
- Code review for each refactored component
- Architecture review for pattern implementations
- Performance testing after major refactoring
EOF

echo "📋 Complexity reduction plan created: refactoring_analysis/complexity_reduction_plan.md"

# Create specific refactoring templates
mkdir -p refactoring_analysis/templates

# Template for extracting managers from large activities
cat > refactoring_analysis/templates/activity_manager_extraction.kt << 'EOF'
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
EOF

# Template for Strategy pattern in processing classes
cat > refactoring_analysis/templates/strategy_pattern_extraction.java << 'EOF'
// Template: Strategy Pattern for Processing Logic

// Before: Monolithic Processing Class (1700+ lines)
public class OpencvTools {
    public static void processImage(Mat image, int processingType) {
        if (processingType == TYPE_THERMAL) {
            // 400+ lines of thermal processing
        } else if (processingType == TYPE_CONTRAST) {
            // 300+ lines of contrast processing  
        } else if (processingType == TYPE_FILTER) {
            // 500+ lines of filter processing
        }
        // ... more processing types
    }
}

// After: Strategy Pattern Implementation
public interface ImageProcessingStrategy {
    Mat process(Mat inputImage, ProcessingParameters params);
}

public class ThermalProcessingStrategy implements ImageProcessingStrategy {
    @Override
    public Mat process(Mat inputImage, ProcessingParameters params) {
        // Focused thermal processing logic (400 lines)
        return processedImage;
    }
}

public class ContrastProcessingStrategy implements ImageProcessingStrategy {
    @Override  
    public Mat process(Mat inputImage, ProcessingParameters params) {
        // Focused contrast processing logic (300 lines)
        return processedImage;
    }
}

public class ImageProcessor {
    private ImageProcessingStrategy strategy;
    
    public void setStrategy(ImageProcessingStrategy strategy) {
        this.strategy = strategy;
    }
    
    public Mat processImage(Mat image, ProcessingParameters params) {
        return strategy.process(image, params);
    }
}

// Usage in main class (now much simpler)
public class OpencvTools {
    private static final ImageProcessor processor = new ImageProcessor();
    
    public static Mat processThermalImage(Mat image, ProcessingParameters params) {
        processor.setStrategy(new ThermalProcessingStrategy());
        return processor.processImage(image, params);
    }
}
EOF

echo "📝 Refactoring templates created:"
echo "   - Activity Manager Extraction: refactoring_analysis/templates/activity_manager_extraction.kt"
echo "   - Strategy Pattern: refactoring_analysis/templates/strategy_pattern_extraction.java"

# Analyze current complexity metrics
echo ""
echo "📈 Current Complexity Metrics:"
total_files=$(find . -name "*.kt" -o -name "*.java" | wc -l)
large_files=$(cat refactoring_analysis/large_files.txt | wc -l)
complexity_percentage=$((large_files * 100 / total_files))

echo "   - Total source files: $total_files"
echo "   - Files >300 lines: $large_files"
echo "   - Complexity percentage: ${complexity_percentage}%"
echo "   - Target percentage: <35%"

if [ $complexity_percentage -gt 35 ]; then
    files_to_reduce=$((large_files - (total_files * 35 / 100)))
    echo "   - Files requiring refactoring: $files_to_reduce"
else
    echo "   - Target already achieved! ✅"
fi

echo ""
echo "✅ Complex file refactoring analysis complete!"
echo "📊 Priority Actions:"
echo "   1. Start with IRThermalNightActivity.kt (3,324 lines)"
echo "   2. Apply Manager extraction pattern"
echo "   3. Measure complexity reduction after each refactoring"
echo "   4. Update tests for refactored components"