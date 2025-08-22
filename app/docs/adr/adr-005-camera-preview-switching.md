# ADR-005: Camera Preview Switching Implementation

## Status

Accepted

## Context

Users required the ability to switch between RGB camera and thermal camera previews rather than having both previews displayed simultaneously. The existing implementation displayed both camera feeds at all times, which could be overwhelming and reduced the focus on specific data streams during research sessions.

### Research Requirements

1. **Focused Data Collection**: Researchers needed to focus on either RGB or thermal data streams without visual distraction from the other preview
2. **User Control**: Manual switching capability to change preview modes based on experimental protocols
3. **Device Coordination**: Both cameras must remain initialised for proper device coordination even when only one preview is visible
4. **Clear Indication**: Users must have clear visual feedback about which camera preview is currently active

### Technical Constraints

1. **Initialisation Dependency**: Both RGB and thermal cameras must be initialised for proper system coordination with the PC controller
2. **State Management**: Preview switching state must be maintained independently of device initialisation state
3. **UI Consistency**: Switching interface must follow established Material Design patterns and integrate with unified UI components
4. **Performance**: Preview switching must be immediate without affecting recording quality or system performance

## Decision

Implement a toggle switch interface that allows users to choose between RGB and thermal camera previews while maintaining initialisation of both camera systems for proper device coordination.

### Implementation Strategy

1. **Toggle Switch Interface**:
   - Clear "RGB" and "Thermal" labels with Material 3 SegmentedButton
   - Visual indication of active preview mode
   - Immediate preview switching without system reinitialization

2. **Selective Preview Visibility**:
   - Only the selected camera preview is visible in the UI
   - Non-selected preview components remain initialised but hidden
   - State management through Compose remember state

3. **Maintained Device Coordination**:
   - Both RGB and thermal cameras remain initialised regardless of preview selection
   - PC controller communication includes both camera streams for synchronisation
   - Device coordination protocols unaffected by preview switching

4. **Enhanced User Experience**:
   - Smooth transitions between preview modes
   - Consistent preview sizing and positioning
   - Integration with unified status indicators from CommonIndicators.kt

## Implementation Details

### UI Components

```kotlin
// Camera preview switching state
var showThermalCamera by remember { mutableStateOf(false) }

// Toggle switch implementation
SegmentedButton(
    onClick = { showThermalCamera = !showThermalCamera },
    selected = showThermalCamera
) {
    Text(if (showThermalCamera) "Thermal" else "RGB")
}

// Conditional preview display
if (showThermalCamera) {
    // Thermal preview visible
    ThermalPreview(...)
} else {
    // RGB preview visible  
    CameraPreview(...)
}
```

### Integration Points

1. **RecordingScreen.kt**: Primary implementation location with state management
2. **CommonIndicators.kt**: Unified status overlays work with both preview modes
3. **DeviceConnectionManager.kt**: Ensures both cameras remain coordinated regardless of preview selection
4. **Test Coverage**: Comprehensive tests validate switching functionality and device coordination

### State Management

- **Preview Selection State**: Managed at RecordingScreen level through Compose state
- **Device Initialisation State**: Independent of preview selection, both cameras always initialised
- **UI State Synchronisation**: Preview switching state synchronised with unified status indicators

## Consequences

### Positive

- **Improved User Experience**: Researchers can focus on specific data streams without visual clutter
- **Enhanced Research Workflow**: Clear preview modes support different experimental phases
- **Maintained System Functionality**: Full device coordination preserved while adding user control
- **Consistent UI Patterns**: Integration with unified components provides consistent styling
- **Performance Optimisation**: Reduced UI complexity by showing only necessary preview

### Negative

- **Increased State Complexity**: Additional state management for preview selection
- **Testing Surface**: Expanded test requirements to cover all switching scenarios
- **UI Layout Considerations**: Need to handle different preview aspect ratios and sizing

### Risk Mitigation

- **Comprehensive Testing**: `RecordingScreenTest.kt` validates all switching scenarios
- **Device Coordination Testing**: Ensures both cameras remain properly initialised
- **Integration Testing**: Validates switching works seamlessly with unified components
- **Manual Validation**: Tested across different Android devices and screen sizes

## Technical Implementation

### Core Changes

1. **RecordingScreen.kt** (Enhanced):
   - Added camera switching state management
   - Implemented SegmentedButton for preview selection
   - Conditional rendering of preview components
   - State persistence during configuration changes

2. **Test Coverage** (New):
   - `RecordingScreenTest.kt`: Validates camera switching functionality
   - Switch state management testing
   - Preview visibility validation
   - Device coordination confirmation

### User Interface Flow

1. **Default State**: RGB camera preview displayed by default
2. **User Action**: Tap toggle switch to change preview mode
3. **State Update**: `showThermalCamera` state updated immediately  
4. **UI Update**: Preview components recomposed to show selected camera
5. **Device State**: Both cameras remain initialised and coordinated

### Integration with Existing Systems

- **Unified Components**: Leverages `DeviceStatusOverlay` for consistent status display
- **Device Management**: Works seamlessly with `DeviceConnectionManager` initialisation
- **Recording System**: Compatible with all recording functionalities regardless of preview mode
- **PC Communication**: Full communication protocol maintained for both camera systems

## References

- **Material Design 3**: SegmentedButton component guidelines for toggle interfaces
- **Android Compose**: State management best practices for preview switching
- **Research UX Patterns**: User interface patterns for scientific data collection applications
- **ADR-004**: UI Component Unification - Leverages unified components for consistent experience

---

**Decision Date**: August 12, 2025  
**Status**: Implemented and Tested  
**Impact**: High - Significantly improves user experience and research workflow while maintaining full system functionality