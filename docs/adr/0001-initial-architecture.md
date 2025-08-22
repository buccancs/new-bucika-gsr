# ADR-0001: Initial Multi-Modal GSR Data Collection Architecture

**Status**: Accepted  
**Date**: 2024-08-22  
**Deciders**: Development Team  
**Tags**: architecture, sensors, android, synchronization

## Context and Problem Statement

The Bucika GSR project requires a multi-modal physiological data collection platform that can synchronize GSR (Galvanic Skin Response) sensors with thermal and RGB camera data for research purposes. The system needs to collect ground-truth data for future machine learning models that predict GSR from contactless sensors.

```mermaid
graph TD
    A[Research Need] --> B[Contactless GSR Prediction]
    B --> C[Multi-Modal Data Required]
    C --> D[Synchronization Challenge]
    D --> E[Integrated Platform Solution]
```

## Decision Drivers

- Need for precise time synchronization across multiple sensor modalities
- Research-grade data quality requirements
- Portability and ease of use in various environments
- Modularity for future sensor additions
- Real-time data streaming capabilities
- Cost-effective solution using existing hardware

## Considered Options

- **Option 1**: PC-only solution with USB-connected sensors
- **Option 2**: Android-centric solution with smartphone as primary controller
- **Option 3**: Hybrid architecture with Android sensor node + PC controller

```mermaid
graph LR
    A[Architecture Options] --> B[PC-Only]
    A --> C[Android-Centric]
    A --> D[Hybrid Android+PC]
    B --> E{Evaluation}
    C --> E
    D --> E
    E --> F[Hybrid Selected]
    
    style F fill:#90EE90
```

## Decision Outcome

**Chosen option**: "Hybrid architecture with Android sensor node + PC controller"

### Rationale

The hybrid approach provides the best balance of:
- **Portability**: Android smartphone enables mobile data collection
- **Processing Power**: PC handles complex synchronization and data management
- **Sensor Integration**: Android supports Bluetooth (GSR) and camera access
- **Expandability**: Modular design allows future sensor additions

### System Architecture

```mermaid
graph TB
    subgraph "Android Sensor Node"
        A1[GSR Sensor via Bluetooth]
        A2[Thermal Camera TC001]
        A3[RGB Camera Built-in]
        A4[Android App Controller]
        A1 --> A4
        A2 --> A4
        A3 --> A4
    end
    
    subgraph "PC Controller"
        P1[Python Synchronization Service]
        P2[Data Storage Manager]
        P3[Timestamp Coordinator]
        P1 --> P2
        P1 --> P3
    end
    
    A4 <-->|Network Protocol| P1
    P2 --> D[Synchronized Dataset]
    
    style D fill:#FFE4B5
```

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

**Neutral**:
- Requires both Android and Python development skills
- Standard hardware components reduce costs

## Implementation

```mermaid
gantt
    title Implementation Timeline
    dateFormat 2024-01-01
    section Android Development
    App Framework     :2024-01-01, 14d
    Sensor Integration:2024-01-15, 21d
    section PC Development
    Sync Service      :2024-01-08, 14d
    Data Management   :2024-01-22, 14d
    section Integration
    Network Protocol  :2024-02-05, 10d
    Testing & Validation:2024-02-15, 14d
```

## Technical Components

### Hardware Stack
- **GSR Sensor**: Shimmer3 GSR+ (research-grade, 128Hz sampling)
- **Thermal Camera**: Topdon TC001 (Android-compatible)
- **RGB Camera**: Smartphone built-in (high resolution)
- **Platform**: Android smartphone + PC workstation

### Software Stack
- **Android**: Java/Kotlin app with sensor APIs
- **PC**: Python synchronization service
- **Communication**: JSON over TCP/UDP with optional TLS
- **Storage**: Multi-modal data with synchronized timestamps

### Data Flow

```mermaid
sequenceDiagram
    participant PC as PC Controller
    participant App as Android App
    participant GSR as GSR Sensor
    participant Thermal as Thermal Camera
    participant RGB as RGB Camera
    
    PC->>App: Start Recording Command
    App->>GSR: Begin GSR Sampling
    App->>Thermal: Start Thermal Capture
    App->>RGB: Start RGB Recording
    
    loop Data Collection
        GSR->>App: GSR Sample + Timestamp
        Thermal->>App: Thermal Frame + Timestamp
        RGB->>App: RGB Frame + Timestamp
        App->>PC: Synchronized Data Stream
    end
    
    PC->>App: Stop Recording Command
    App->>PC: Final Data + Session Metadata
```

## Links

- [System Requirements](../requirements.md)
- [Sensor Specifications](../hardware-specs.md)
- [Implementation Guide](../../GRADLE_SETUP.md)

---

*This ADR establishes the foundational architecture for the Bucika GSR multi-modal data collection platform.*