# ADR-004: UI Component Unification Strategy

## Status

Accepted

## Context

The Android application suffered from extensive code duplication across UI components, with over 500 lines of duplicate code spread across multiple implementations of recording indicators, status overlays, and preview cards. This duplication created maintenance challenges, inconsistent styling, and increased the likelihood of bugs due to multiple implementations of the same functionality.

### Problems Identified

1. **Multiple RecordingIndicator Implementations**: Three separate implementations of recording indicators existed across `CameraPreview.kt`, `ThermalPreview.kt`, and other components, each with slightly different styling and behaviour.

2. **Duplicate Status Overlays**: Device connection status overlays were implemented multiple times with inconsistent appearance and state management.

3. **Inconsistent Preview Card Styling**: Preview containers used different styling approaches, leading to visual inconsistencies across the application.

4. **Legacy Components**: Deprecated `RecordingFragment.kt` (220+ lines) and `fragment_recording.xml` (190+ lines) contained obsolete code that was no longer used but remained in the codebase.

5. **Maintenance Overhead**: Changes to common UI patterns required updates in multiple locations, increasing development time and error potential.

## Decision

Implement a unified component architecture through the creation of `CommonIndicators.kt` containing shared UI components that eliminate duplication while maintaining identical functionality.

### Implementation Strategy

1. **Create Unified Components**:
   - `RecordingIndicator`: Single implementation for all recording status indicators
   - `DeviceStatusOverlay`: Unified status overlay for camera and device connections
   - `PreviewCard`: Common card container with consistent styling patterns

2. **Eliminate Duplicate Implementations**:
   - Remove private RecordingIndicator implementations from individual preview components
   - Replace multiple status overlay implementations with unified DeviceStatusOverlay
   - Standardize preview card styling through shared PreviewCard component

3. **Remove Legacy Code**:
   - Delete deprecated `RecordingFragment.kt` and `fragment_recording.xml`
   - Remove unused ThermalPreviewCard implementations
   - Clean up obsolete UI patterns and styling approaches

4. **Ensure Functional Equivalence**:
   - Maintain exact same visual appearance and behaviour
   - Preserve all animation and interaction patterns
   - Ensure backward compatibility with existing component usage

## Consequences

### Positive

- **Significant Code Reduction**: Eliminated 500+ lines of duplicate code while maintaining identical functionality
- **Improved Maintainability**: Single source of truth for common UI patterns reduces maintenance overhead
- **Consistent User Experience**: Unified components ensure consistent styling and behaviour across the application
- **Reduced Bug Potential**: Single implementation eliminates inconsistencies and reduces testing surface area
- **Enhanced Development Velocity**: Future UI changes can be made in one location and propagate throughout the application

### Negative

- **Initial Refactoring Effort**: Required comprehensive review and testing to ensure functional equivalence
- **Component Dependencies**: Shared components create dependencies that must be managed carefully
- **Import Updates**: Existing files required import statement updates to reference unified components

### Risk Mitigation

- **Comprehensive Testing**: Created `CommonIndicatorsTest.kt` to validate all unified components
- **Integration Testing**: Implemented `UnifiedComponentsIntegrationTest.kt` to ensure components work together properly
- **Visual Validation**: Manual testing confirmed identical appearance and behaviour to original implementations
- **Gradual Migration**: Unified components were introduced alongside existing implementations before full replacement

## Implementation Details

### Created Files

- `AndroidApp/src/main/java/com/multisensor/recording/ui/components/CommonIndicators.kt` (154 lines)
- `AndroidApp/src/test/java/com/multisensor/recording/ui/components/CommonIndicatorsTest.kt` (139 lines)
- `AndroidApp/src/test/java/com/multisensor/recording/ui/components/UnifiedComponentsIntegrationTest.kt` (166 lines)

### Modified Files

- `AndroidApp/src/main/java/com/multisensor/recording/ui/components/CameraPreview.kt` (reduced by ~200 lines)
- `AndroidApp/src/main/java/com/multisensor/recording/ui/components/ThermalPreview.kt` (reduced by ~100 lines)
- `AndroidApp/src/main/java/com/multisensor/recording/ui/compose/screens/RecordingScreen.kt` (removed duplicate FAB, streamlined)

### Deleted Files

- `AndroidApp/src/main/java/com/multisensor/recording/ui/fragments/RecordingFragment.kt` (221 lines)
- `AndroidApp/src/main/res/layout/fragment_recording.xml` (197 lines)

### Code Metrics

- **Total Lines Removed**: 500+ lines of duplicate and obsolete code
- **Total Lines Added**: 459 lines (unified components + comprehensive tests)
- **Net Code Reduction**: ~40 lines while adding extensive test coverage
- **Duplication Elimination**: 100% removal of RecordingIndicator and status overlay duplication

## Technical Foundation

This ADR builds upon existing architectural decisions:

- **ADR-001**: Reactive State Management - Unified components utilise StateFlow patterns for consistent state updates
- **ADR-002**: Strict Type Safety - All unified components maintain complete type safety
- **ADR-003**: Function Decomposition Strategy - Follows single responsibility principle through specialised component functions

## References

- **Material Design Guidelines**: Unified components follow Material 3 design principles for consistency
- **Android Architecture Components**: Utilizes Compose best practices for reactive UI development
- **Code Quality Standards**: Implements established patterns for maintainable Android UI development

---

**Decision Date**: August 12, 2025  
**Status**: Implemented and Tested  
**Impact**: High - Significant improvement in code maintainability and user experience consistency