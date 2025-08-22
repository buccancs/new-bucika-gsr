# ADR Template

Use this template for all Architecture Decision Records (ADRs).

## ADR-XXXX: [Decision Title]

**Status**: [Proposed | Accepted | Deprecated | Superseded]  
**Date**: YYYY-MM-DD  
**Deciders**: [Names of decision makers]  
**Tags**: [relevant tags]

## Context and Problem Statement

Describe the context and problem statement that led to this decision.

```mermaid
graph TD
    A[Current State] --> B[Problem/Challenge]
    B --> C[Need for Decision]
    C --> D[This ADR]
```

## Decision Drivers

- [Driver 1]
- [Driver 2]
- [Driver 3]

## Considered Options

- **Option 1**: [Description]
- **Option 2**: [Description]  
- **Option 3**: [Description]

```mermaid
graph LR
    A[Problem] --> B[Option 1]
    A --> C[Option 2]
    A --> D[Option 3]
    B --> E{Evaluation}
    C --> E
    D --> E
    E --> F[Selected Option]
```

## Decision Outcome

**Chosen option**: "[Option X]"

### Rationale

Explain why this option was selected.

### Consequences

**Positive**:
- [Positive consequence 1]
- [Positive consequence 2]

**Negative**:
- [Negative consequence 1]
- [Negative consequence 2]

**Neutral**:
- [Neutral consequence 1]

## Implementation

```mermaid
gantt
    title Implementation Timeline
    dateFormat YYYY-MM-DD
    section Phase 1
    Planning     :2024-01-01, 7d
    Development  :2024-01-08, 14d
    section Phase 2
    Testing      :2024-01-22, 7d
    Deployment   :2024-01-29, 3d
```

## Links

- [Related ADR]
- [Documentation]
- [Issues/Tickets]

---

*This ADR follows the format from [MADR](https://adr.github.io/madr/)*