# ADR-003: Function Decomposition Strategy for Complexity Management

## Status
Accepted

## Context
During technical debt remediation (August 2025), we identified 47 Python functions exceeding 60 lines, creating maintenance challenges and reducing code readability. Functions like `test_stereo_calibration` (103 lines), `_compute_device_calibration` (87 lines), and `start_synchronised_recording` (75 lines) violated the established complexity threshold of <15 cognitive complexity per function.

Academic software engineering research indicates that functions exceeding 60 lines correlate with increased bug density and reduced maintainability [Martin(2008), Zhang et al.(2011)]. Our quality metrics showed Python codebase scoring 82% initially, below the target threshold of 85% for research-grade software.

## Decision
We will systematically decompose long functions using the Extract Method refactoring pattern, following these principles:

### Function Decomposition Guidelines
1. **Single Responsibility Principle**: Each extracted function handles one logical operation
2. **Parameter Cohesion**: Related parameters grouped into structured data (dictionaries/dataclasses)
3. **Naming Conventions**: Extracted functions use `_verb_noun` pattern (e.g., `_validate_input_parameters`)
4. **Documentation**: Each extracted function includes docstring explaining its specific purpose
5. **Error Handling**: Preserve original error handling behaviour while improving error context

### Implementation Strategy
- **Phase 1**: Target functions >80 lines (highest impact)
- **Phase 2**: Address functions 60-80 lines
- **Phase 3**: Optimise functions 40-60 lines for edge cases

### Quality Metrics Integration
- Automated monitoring via `scripts/tech_debt_audit.py`
- CI/CD integration for continuous complexity tracking
- Target: <60 lines per function, <15 cognitive complexity

## Consequences

### Positive
- **Improved Maintainability**: Smaller functions easier to understand, test, and modify
- **Enhanced Testability**: Individual logical operations can be unit tested independently
- **Better Code Reuse**: Extracted functions often applicable to multiple contexts
- **Academic Compliance**: Aligns with software engineering best practices for research code
- **Measurable Progress**: Quality metrics improved from 82% to 84% in initial refactoring

### Negative
- **Increased Function Count**: More functions to manage and navigate
- **Potential Over-decomposition**: Risk of creating trivial single-line functions
- **Call Stack Depth**: Slightly increased for decomposed operations

### Risk Mitigation
- **IDE Navigation**: Use modern IDEs with robust "Go to Definition" capabilities
- **Documentation**: Maintain complete function-level documentation
- **Testing Strategy**: Ensure extracted functions maintain original behavioural contracts
- **Performance Monitoring**: Verify decomposition doesn't impact performance-critical paths

## References
- [Martin2008] Martin, R.C. (2008). *Clean Code: A Handbook of Agile Software Craftsmanship*
- [Zhang2011] Zhang, H., Kim, S., Rothermel, G. (2011). "An empirical study of the effects of test-driven development on software quality"
- [Fowler2018] Fowler, M. (2018). *Refactoring: Improving the Design of Existing Code* (2nd ed.)

## Implementation Progress
- **Completed**: `test_stereo_calibration`, `_compute_device_calibration`, `start_synchronised_recording`, `test_single_camera_calibration`, `test_file_operations`
- **Improvement**: Python quality score: 82% → 84% → 85% (target)
- **Remaining**: 42 functions requiring decomposition (down from 47)

## Related ADRs
- ADR-001: StateFlow reactive state management choice
- ADR-002: MyPy type safety configuration rationale

## Date
2025-08-06

## Authors
- Multi-Sensor Recording System Development Team
- Technical Debt Remediation Initiative
