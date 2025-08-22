Version B: Full Structure

- **Chapter 1. Introduction**

- 1.1 Background and Motivation

- 1.2 Research Problem and Objectives

  - 1.2.1 Problem Context and Significance

  - 1.2.2 Aim and Specific Objectives

- 1.3 Thesis Structure and Scope

- **Chapter 2. Background and Literature Review**

- 2.1 Emotion Analysis Applications

- 2.2 Contactless Physiological Measurement: Rationale and Approaches

- 2.3 Definitions of Stress in Literature

  - 2.3.1 Scientific Definitions of "Stress"

  - 2.3.2 Colloquial and Operational Definitions

- 2.4 Cortisol vs. GSR in Stress Measurement

  - 2.4.1 Cortisol as a Stress Biomarker

  - 2.4.2 Galvanic Skin Response (Electrodermal Activity)

  - 2.4.3 Comparative Analysis of Cortisol and GSR

- 2.5 GSR Physiology and Limitations

  - 2.5.1 Principles of Electrodermal Activity

  - 2.5.2 Limitations of GSR for Stress Detection

- 2.6 Thermal Cues of Stress

  - 2.6.1 Physiological Thermal Responses to Stress

  - 2.6.2 Thermal Imaging in Stress and Emotion Research

- 2.7 RGB vs. Thermal Imaging: A Machine Learning Perspective

  - 2.7.1 Stress Detection via RGB Video (Visible Spectrum)

  - 2.7.2 Stress Detection via Thermal Imaging (Infrared Spectrum)

  - 2.7.3 Multi-Modal RGB+Thermal Approaches (Hypothesis)

- 2.8 Sensor Device Rationale

  - 2.8.1 Shimmer3 GSR+ Sensor (Features and Selection Justification)

  - 2.8.2 Topdon Thermal Camera (Specifications and Selection
    Justification)

- **Chapter 3. Requirements and Analysis**

- 3.1 Problem Context and Opportunity Analysis

  - 3.1.1 Current Physiological Measurement Landscape

  - 3.1.2 Evolution of Measurement Paradigms

  - 3.1.3 Limitations of Existing Approaches

  - 3.1.4 Identified Research Gap and Opportunity

- 3.2 Requirements Engineering Methodology

  - 3.2.1 Stakeholder Analysis and Requirements Elicitation

  - 3.2.2 System Requirements Analysis Framework

- 3.3 Functional Requirements

  - 3.3.1 Multi-Device Coordination and Synchronisation Requirements

  - 3.3.2 Sensor Integration and Data Acquisition Requirements

  - 3.3.3 Real-Time Data Processing and Analysis Requirements

  - 3.3.4 Session Management and User Interface Requirements

- 3.4 Non-Functional Requirements

  - 3.4.1 Performance and Scalability Requirements

  - 3.4.2 Reliability and Data Integrity Requirements

  - 3.4.3 Usability and Accessibility Requirements

- 3.5 Use Cases

  - 3.5.1 Primary Use Cases (Key System Scenarios)

  - 3.5.2 Secondary Use Cases (Maintenance and Extensions)

- 3.6 System Analysis

  - 3.6.1 Data Flow Analysis

  - 3.6.2 Component Interaction Analysis

  - 3.6.3 Scalability Considerations

- 3.7 Data Requirements

  - 3.7.1 Data Types and Volume Expectations

  - 3.7.2 Data Quality and Storage Requirements

- **Chapter 4. Design and Implementation**

- 4.1 System Architecture Overview

  - 4.1.1 PC--Android System Topology and Components

  - 4.1.2 Overall Architectural Design Philosophy

- 4.2 Distributed System Design

  - 4.2.1 Synchronisation Architecture (Multi-Device Coordination)

  - 4.2.2 Fault Tolerance and Recovery Mechanisms

  - 4.2.3 Communication Model and Protocol

- 4.3 Android Application Architecture

  - 4.3.1 Recording Management Component

  - 4.3.2 High-Resolution Video Capture (RGB Camera)

  - 4.3.3 Thermal Camera Integration (Topdon)

  - 4.3.4 Shimmer GSR Sensor Integration

- 4.4 Desktop Controller Architecture

  - 4.4.1 Session Coordination Module

  - 4.4.2 Computer Vision Processing Pipeline

  - 4.4.3 Calibration System Implementation

- 4.5 Communication and Networking Design

  - 4.5.1 Control Protocol Implementation

  - 4.5.2 Data Streaming Mechanism

- 4.6 Data Processing Pipeline

  - 4.6.1 Real-Time Signal Processing Framework

  - 4.6.2 Synchronisation Engine Design

- 4.7 Implementation Challenges and Solutions

  - 4.7.1 Multi-Platform Compatibility

  - 4.7.2 Real-Time Synchronisation Challenges

  - 4.7.3 Resource Management and Optimisation

- 4.8 Technology Stack and Design Decisions

  - 4.8.1 Android Platform and Library Choices

  - 4.8.2 Desktop (Python) Framework Choices

  - 4.8.3 Communication Protocol Selection

  - 4.8.4 Database/Storage Design Decision

- 4.9 Android Application Implementation and Features

  - 4.9.1 Multi-Sensor Data Collection (4K Video, Thermal, GSR)

  - 4.9.2 Session Lifecycle Management

  - 4.9.3 Networking and Data Transfer Management

  - 4.9.4 User Interface and Interaction Design

- 4.10 Python Desktop Controller Implementation

  - 4.10.1 Application Architecture and Module Integration

  - 4.10.2 Graphical User Interface (Desktop Dashboard)

  - 4.10.3 Network Layer and Device Coordination

  - 4.10.4 Webcam Service and Computer Vision Integration

  - 4.10.5 Calibration and Validation Tools

  - 4.10.6 Stimulus Presentation and Experiment Control

- 4.11 Data Processing and Quality Management

  - 4.11.1 Real-Time Data Processing Performance

  - 4.11.2 Data Quality Assurance Measures

- 4.12 Testing and QA Integration in Design

  - 4.12.1 Built-in Testing Strategy and Framework

  - 4.12.2 Performance Monitoring and Optimisation

- 4.13 Multi-Device Synchronisation Implementation

  - 4.13.1 Temporal Coordination Algorithm

- **Chapter 5. Evaluation and Testing**

- 5.1 Testing Strategy Overview

  - 5.1.1 Multi-Tiered Testing Approach and Hardware Simulation Strategy

  - 5.1.2 Architecture-Mirrored Testing Structure

- 5.2 Unit Testing (Android and PC Components)

  - 5.2.1 Android Unit Tests (JUnit and Robolectric Framework)

  - 5.2.2 PC Unit Tests (pytest and unittest Framework)

  - 5.2.3 Security and Architecture Enforcement Testing

- 5.3 Integration Testing (Multi-Device synchronisation & Networking)

  - 5.3.1 Multi-Device synchronisation Testing with DeviceSimulator

  - 5.3.2 Networking and Data Exchange Protocol Validation

  - 5.3.3 Cross-Platform Integration with MockShimmerDevice

- 5.4 System Performance Evaluation

  - 5.4.1 Endurance Testing (8-hour Continuous Operation)

  - 5.4.2 Memory Leak Detection and Performance Monitoring

  - 5.4.3 CPU and Throughput Performance Analysis

  - 5.4.4 System Stability and Resource Management

- 5.5 Results Analysis and Discussion

  - 5.5.1 Comprehensive Testing Campaign Results

  - 5.5.2 Integration Testing Validation of System Architecture

  - 5.5.3 Performance Evaluation and Long-term Operation Validation

  - 5.5.4 Areas for Future Improvement and Hardware-Specific Testing

- **Chapter 6. Conclusions**

- 6.1 Project Achievements Summary

  - 6.1.1 Key Deliverables and Outcomes

  - 6.1.2 Technical Innovation Achievements

- 6.2 Goals Assessment and Validation

  - 6.2.1 Evaluation of Primary Goals

  - 6.2.2 Secondary Goals and Unexpected Outcomes

- 6.3 Critical Evaluation of Results

  - 6.3.1 System Design Strengths and Challenges

  - 6.3.2 Comparison with Existing Solutions

- 6.4 System Performance Analysis

  - 6.4.1 Performance Characteristics and Metrics

  - 6.4.2 Validation of Performance Results

- 6.5 Technical Contributions and Innovations

  - 6.5.1 Research Methodology Contributions

  - 6.5.2 Software Engineering Contributions

- 6.6 Limitations and Constraints

  - 6.6.1 Technical Limitations

  - 6.6.2 Practical and Operational Constraints

- 6.7 Future Work and Extensions

  - 6.7.1 Short-Term Enhancement Opportunities

  - 6.7.2 Long-Term Research Directions

- **Appendices**

- **Appendix A: System Manual**

  - A.1 Component Documentation Reference

  - A.2 Validated System Configuration

  - A.3 Configuration Management Guidelines

  - A.4 Architecture Extension Guidelines

  - A.5 Troubleshooting and Maintenance

- **Appendix B: User Manual**

  - B.1 First-Time Setup Instructions

  - B.2 Recording Session Management

  - B.3 Data Export and Analysis Procedures

- **Appendix C: Supporting Documentation and Data**

  - C.1 Hardware Specifications and Calibration Data

  - C.2 Network Protocol Specifications

  - C.3 Research Protocol Documentation

- **Appendix D: Test Results and Reports**

  - D.1 complete Testing Results Summary

  - D.2 Statistical Validation Results

  - D.3 Reliability and Stress Test Reports

- **Appendix E: Evaluation Data and Analysis**

  - E.1 User Experience Evaluation Data

  - E.2 Scientific Validation Data and Protocols

  - E.3 System Performance Data and Logs

- **Appendix F: Code Listings**

  - F.1 Core Synchronisation Algorithm Code

  - F.2 Multi-Modal Data Pipeline Implementation

  - F.3 Android Sensor Integration Framework Code
