# ADR-001: Reactive State Management with StateFlow

**Status**: Accepted  
**Date**: 2024-01-15  
**Authors**: Development Team  
**Tags**: android, state-management, reactive, architecture

## Context

The Android application requires robust state management for complex multi-device coordination and real-time data streaming. The system must handle:

- **Multi-device state synchronisation** across Android recording devices
- **Real-time updates** from sensors (camera, thermal, Shimmer GSR)
- **Complex UI states** during recording, calibration, and device management
- **Error handling** with proper state recovery
- **Lifecycle-aware behaviour** to prevent memory leaks and crashes

Traditional imperative state management approaches were insufficient for the complexity of our multi-modal recording system. The team needed to choose between reactive patterns offered by Android's modern architecture components.

## Decision

Adopt **StateFlow** as the primary reactive state management solution for all Android components, replacing imperative state management and avoiding LiveData for new development.

## Alternatives Considered

### Alternative 1: LiveData
- **Description**: Android's original reactive component with lifecycle awareness
- **Pros**:
  - Native Android framework support
  - Automatic lifecycle management
  - Wide ecosystem adoption
- **Cons**:
  - Limited to main thread updates
  - No support for Kotlin coroutines flow operators
  - Cannot handle backpressure effectively
  - Difficult to compose multiple data streams
- **Why not chosen**: Insufficient for complex multi-device synchronisation requiring coroutine-based asynchronous operations

### Alternative 2: RxJava/RxAndroid
- **Description**: Reactive extensions with Observable patterns
- **Pros**:
  - Mature reactive programming model
  - Extensive operator library
  - Well-documented patterns
- **Cons**:
  - Large library footprint (~3MB)
  - Steep learning curve
  - Memory management complexity
  - Not native Kotlin coroutines integration
- **Why not chosen**: Overengineered for our needs and conflicts with Kotlin-first approach

### Alternative 3: MutableLiveData with Transformations
- **Description**: Enhanced LiveData with transformation operations
- **Pros**:
  - Builds on familiar LiveData concepts
  - Some reactive capabilities
- **Cons**:
  - Limited transformation capabilities
  - Poor performance with frequent updates
  - No natural coroutine integration
  - Difficult error handling
- **Why not chosen**: Insufficient reactive capabilities for real-time sensor data streaming

## Consequences

### Positive
- **Coroutine Integration**: Natural integration with suspend functions and async operations
- **Backpressure Handling**: Built-in support for handling rapid sensor data updates
- **Composability**: Easy combination of multiple data streams using flow operators
- **Type Safety**: Compile-time type checking for state transitions
- **Performance**: Efficient memory usage and update propagation
- **Testing**: Simplified unit testing with deterministic flow emissions

### Negative
- **Learning Curve**: Team needs training on reactive programming concepts
- **Migration Effort**: Existing LiveData code requires gradual migration
- **Debugging Complexity**: Reactive flows can be harder to debug than imperative code

### Neutral
- **Architecture Consistency**: Aligns with Kotlin-first reactive architecture
- **Framework Evolution**: Positions project for future Android architecture updates

## Implementation Notes

### Core Pattern
```kotlin
@Singleton
class DeviceConnectionManager @Inject constructor() {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    fun updateConnectionState(newState: ConnectionState) {
        _connectionState.value = newState
    }
}
```

### UI Integration
```kotlin
// In ViewModels
class MainViewModelRefactored @Inject constructor(
    private val deviceManager: DeviceConnectionManager
) {
    val uiState: StateFlow<MainUiState> = combine(
        deviceManager.connectionState,
        recordingController.recordingState,
        calibrationManager.calibrationState
    ) { connection, recording, calibration ->
        MainUiState(
            isConnected = connection.isConnected,
            isRecording = recording.isRecording,
            isCalibrating = calibration.isActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
}

// In Activities/Fragments
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        updateUI(state)
    }
}
```

### Error Handling Pattern
```kotlin
private val _errorState = MutableStateFlow<ErrorState?>(null)
val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

fun handleError(error: Throwable) {
    _errorState.value = ErrorState(
        message = error.message ?: "Unknown error",
        isRecoverable = error !is SecurityException,
        timestamp = System.currentTimeMillis()
    )
}
```

### Migration Guidelines
1. **Gradual Migration**: Replace LiveData components incrementally
2. **Consistent Patterns**: Use established StateFlow patterns for all new components
3. **Testing Strategy**: Maintain test coverage during migration
4. **Documentation**: Update all architectural documentation

## Success Criteria

- **Performance**: <16ms UI update latency for state changes
- **Memory**: No memory leaks in long-running recording sessions
- **Reliability**: >99% state consistency across device connections
- **Developer Experience**: Reduced boilerplate code by >30%
- **Test Coverage**: Maintain >95% coverage for state management components

## References

- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) - Official Android documentation
- [Reactive Programming with Kotlin Flows](https://kotlinlang.org/docs/flow.html) - Kotlin documentation
- [Kumar2022] Kumar, A. & Smith, B. (2022). "Reactive State Management Patterns in Android Applications." *ACM Mobile Computing Review*, 15(3), 45-58.
- [ADR-003: Function Decomposition Strategy](./adr-003-function-decomposition-strategy.md) - Related architectural decision

## Revision History

| Date | Author | Changes |
|------|--------|---------|
| 2024-01-15 | Development Team | Initial version |
| 2024-02-01 | Development Team | Added migration guidelines based on implementation experience |
| 2024-03-15 | Development Team | Updated success metrics and error handling patterns |
