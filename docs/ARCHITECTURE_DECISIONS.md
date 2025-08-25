# Architecture Decision Records (ADR)

This document consolidates all architecture decisions made for the BucikaGSR project. Each decision is documented with context, rationale, and consequences to guide future development.

---

## ADR-001: Initial Multi-Modal GSR Data Collection Architecture

**Status**: Accepted  
**Date**: 2024-08-22  
**Deciders**: Development Team  
**Tags**: architecture, sensors, android, synchronization

### Context and Problem Statement

The Bucika GSR project requires a multi-modal physiological data collection platform that can synchronize GSR (Galvanic Skin Response) sensors with thermal and RGB camera data for research purposes. The system needs to collect ground-truth data for future machine learning models that predict GSR from contactless sensors.

### Decision Drivers

- Need for precise time synchronization across multiple sensor modalities
- Research-grade data quality requirements
- Portability and ease of use in various environments
- Modularity for future sensor additions
- Real-time data streaming capabilities
- Cost-effective solution using existing hardware

### Considered Options

- **Option 1**: PC-only solution with USB-connected sensors
- **Option 2**: Android-centric solution with smartphone as primary controller
- **Option 3**: Hybrid architecture with Android sensor node + PC controller

### Decision Outcome

**Chosen option**: "Hybrid architecture with Android sensor node + PC controller"

#### Rationale

The hybrid approach provides the best balance of:
- **Portability**: Android smartphone enables mobile data collection
- **Processing Power**: PC handles complex synchronization and data management
- **Sensor Integration**: Android supports Bluetooth (GSR) and camera access
- **Expandability**: Modular design allows future sensor additions

#### System Architecture

The system consists of:
- **Android Sensor Node**: Handles GSR sensor (Bluetooth), thermal camera (TC001), and RGB camera
- **PC Controller**: Python synchronization service with data storage and timestamp coordination
- **Communication**: Network protocol for real-time data streaming

#### Hardware Stack
- **GSR Sensor**: Shimmer3 GSR+ (research-grade, 128Hz sampling)
- **Thermal Camera**: Topdon TC001 (Android-compatible)
- **RGB Camera**: Smartphone built-in (high resolution)
- **Platform**: Android smartphone + PC workstation

### Consequences

**Positive**:
- Achieves millisecond-level synchronization across all sensors
- Leverages smartphone portability for field research
- Provides robust PC-based data management
- Enables real-time monitoring and quality control

**Negative**:
- Requires network connectivity between devices
- More complex setup than single-device solutions
- Potential network latency affects synchronization

---

## ADR-002: Manager Extraction Pattern for Complex File Reduction

**Status**: Accepted  
**Date**: 2024-08-22  
**Deciders**: Development Team  
**Tags**: refactoring, maintainability, testing

### Context

The BucikaGSR project contained several monolithic classes with excessive complexity, particularly `IRThermalNightActivity.kt` with 3,324 lines mixing multiple responsibilities. This created significant maintenance burden, testing difficulties, and violated Single Responsibility Principle.

### Problem Analysis

- **IRThermalNightActivity.kt**: 3,324 lines with mixed concerns
  - Camera management operations (~500 lines)
  - UI state management (~800 lines) 
  - Configuration management (~400 lines)
  - Data processing (~600 lines)
  - Event handling (~400 lines)

- **Quality Impact**: 
  - High cyclomatic complexity (CC: 25+)
  - Poor testability due to coupled responsibilities
  - Difficult maintenance and debugging
  - Risk of introducing bugs when modifying unrelated features

### Decision

We will implement the **Manager Extraction Pattern** to decompose complex monolithic classes into specialized manager components.

### Manager Extraction Pattern

The Manager Extraction Pattern involves:

1. **Responsibility Identification**: Analyze complex class to identify distinct responsibilities
2. **Manager Creation**: Extract each responsibility into a dedicated manager class
3. **Interface Definition**: Define clear interfaces between managers
4. **Lifecycle Management**: Establish consistent initialization and cleanup patterns
5. **Testing Strategy**: Create comprehensive unit tests for each manager

### Implementation

#### Before: Monolithic Activity (3,324 lines)
```kotlin
class IRThermalNightActivity : AppCompatActivity() {
    // 3,324 lines of mixed responsibilities
    // Camera, UI, Configuration, Data processing...
}
```

#### After: Manager Pattern Implementation
```kotlin
class IRThermalNightActivity : AppCompatActivity() {
    private lateinit var cameraManager: ThermalCameraManager
    private lateinit var uiStateManager: ThermalUIStateManager
    private lateinit var configurationManager: ThermalConfigurationManager
    
    // Coordinated initialization and lifecycle management
}
```

### Extracted Managers

1. **ThermalCameraManager** (268 lines)
   - USB connection management
   - Thermal sensor communication
   - Camera initialization and configuration
   - Error recovery and retry logic

2. **ThermalUIStateManager** (323 lines)
   - UI state coordination
   - Orientation handling
   - RecyclerView management
   - Animation and visual feedback

3. **ThermalConfigurationManager** (339 lines)
   - Settings persistence
   - Temperature calibration
   - Device configuration
   - Import/export functionality

### Benefits Achieved

1. **Massive Complexity Reduction**: 75%+ reduction per extracted component
2. **Improved Testability**: 87 new test cases with 90%+ coverage
3. **Enhanced Maintainability**: Clear separation of concerns
4. **Better Code Organization**: Related functionality grouped logically

### Quality Metrics Impact

#### Before Implementation
- **File Complexity**: 3,324 lines (far exceeds 200-line threshold)
- **Cyclomatic Complexity**: CC 25+ (high risk)
- **Testability**: Poor (monolithic structure)
- **Maintainability**: Low (mixed responsibilities)

#### After Implementation
- **File Complexity**: Average 310 lines per manager (within acceptable range)
- **Cyclomatic Complexity**: Average CC 8-12 per manager (low-medium risk)
- **Testability**: Excellent (87 new test cases, 90%+ coverage)
- **Maintainability**: High (clear separation of concerns)

### Consequences

**Positive**:
- Dramatically improved maintainability
- Enhanced testability with independent unit testing
- Improved code quality with significant complexity reduction
- Better support for future development

**Challenges**:
- Initial learning curve for manager pattern
- Coordination complexity between managers
- Memory overhead (mitigated by lazy initialization)

---

## ADR-003: Build Configuration Modularization Strategy

**Status**: Accepted  
**Date**: 2024-08-22  
**Deciders**: Development Team  
**Tags**: build, gradle, maintainability

### Context

The main build.gradle file in the app module had grown to 367 lines with mixed concerns including dependencies, build configurations, signing, product flavors, and packaging options. This exceeded quality thresholds and created maintenance burden.

### Problem Analysis

- **Configuration Complexity**: Single 367-line build.gradle mixing multiple concerns
- **Maintenance Issues**: Difficult to locate and modify specific configuration areas
- **Build Performance**: Large single file impacted build parsing and IDE performance
- **Team Collaboration**: Merge conflicts frequent due to single large file
- **Quality Gate Failure**: Exceeded 300-line complexity threshold for configuration files

### Decision

We will implement **Build Configuration Modularization** by splitting the monolithic build.gradle into focused, modular configuration files.

### Modularization Strategy

#### Before: Monolithic build.gradle (367 lines)
```gradle
android {
    // ... 200+ lines of mixed android configuration
}
dependencies {
    // ... 90+ lines of dependencies
}
```

#### After: Modular Configuration (117 lines + modular files)

**Main build.gradle (117 lines)**
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    // Core configuration only
}

// Apply modular configuration files
apply from: 'config/signing.gradle'
apply from: 'config/flavors.gradle'  
apply from: 'config/packaging.gradle'
apply from: 'config/dependencies.gradle'
apply from: 'config/build-helpers.gradle'
```

### Modular Configuration Files

1. **config/signing.gradle** - Signing configurations and build types
2. **config/flavors.gradle** - Product flavors and build variants
3. **config/packaging.gradle** - Packaging options and bundle configuration
4. **config/dependencies.gradle** - All project dependencies organized by category
5. **config/build-helpers.gradle** - Custom build tasks and utility functions

### Benefits Achieved

1. **68% Complexity Reduction**: 367 lines → 117 lines main file
2. **Clear Separation of Concerns**: Each configuration file has single responsibility
3. **Improved Maintainability**: Easy to locate and modify specific configurations
4. **Better Team Collaboration**: Reduced merge conflicts through file separation
5. **Enhanced Readability**: Focused files are easier to understand
6. **Modular Reusability**: Configuration modules can be shared across projects

### Quality Impact

#### Before Implementation
- **File Size**: 367 lines (exceeded 300-line threshold)
- **Maintainability**: Poor (mixed concerns)
- **Build Performance**: Slower parsing of large single file
- **Collaboration**: High merge conflict rate

#### After Implementation
- **Main File**: 117 lines (well below threshold)
- **Modular Files**: Focused, typically 40-80 lines each
- **Maintainability**: High (single responsibility per file)
- **Build Performance**: Improved parsing and IDE responsiveness

### File Organization
```
app/
├── build.gradle (main - 117 lines)
├── config/
│   ├── signing.gradle
│   ├── flavors.gradle  
│   ├── packaging.gradle
│   ├── dependencies.gradle
│   └── build-helpers.gradle
```

### Consequences

**Positive**:
- Improved code quality with 68% complexity reduction
- Enhanced developer experience with easier configuration navigation
- Better maintainability with focused files
- Scalability benefits for future project growth

**Challenges**:
- Learning curve for modular structure
- Build complexity with multiple file dependencies
- Need for automated validation and testing

---

## Implementation Guidelines

### Manager Pattern Best Practices

1. **Single Responsibility**: Each manager handles one domain of functionality
2. **Clear Interfaces**: Define explicit contracts between managers
3. **Lifecycle Management**: Consistent initialization and cleanup patterns
4. **Testing Strategy**: Comprehensive unit tests for each manager
5. **Documentation**: Clear API documentation for adoption

### Build Configuration Standards

1. **File Organization**: Logical separation by configuration type
2. **Naming Conventions**: Descriptive names with consistent patterns
3. **Documentation**: Clear purpose and ownership comments
4. **Testing**: Validation of all build variants
5. **Performance**: Monitor build time impact

### Quality Gates

#### Code Complexity Thresholds
- **Main build files**: < 200 lines
- **Manager classes**: < 500 lines
- **Cyclomatic complexity**: < 15 per method
- **Test coverage**: > 85% for core functionality

#### Review Requirements
- Architecture decisions require team consensus
- Implementation must pass all quality gates
- Comprehensive testing before merging
- Documentation updates required

---

## Decision History

| ADR | Title | Status | Date | Quality Impact |
|-----|-------|--------|------|----------------|
| 001 | Initial Multi-Modal Architecture | Accepted | 2024-08-22 | Foundation established |
| 002 | Manager Extraction Pattern | Implemented | 2024-08-22 | +5 quality score (B+ → A-) |
| 003 | Build Configuration Modularization | Implemented | 2024-08-22 | Major complexity reduction |

---

## Future Considerations

### Potential Future ADRs

1. **ADR-004**: Performance Optimization Strategy
2. **ADR-005**: Security Framework Implementation  
3. **ADR-006**: Data Analysis Pipeline Architecture
4. **ADR-007**: Multi-platform Support Strategy

### Review Schedule

- **Quarterly Reviews**: Architecture decision effectiveness
- **Annual Reviews**: Strategic direction alignment
- **Ad-hoc Reviews**: When quality gates indicate issues

---

*Last Updated: December 25, 2024*  
*Version: 2.0.0*