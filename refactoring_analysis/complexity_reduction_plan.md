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
