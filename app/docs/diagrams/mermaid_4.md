# Chapter 4 Mermaid Diagrams

This file contains all mermaid diagrams used in Chapter 4 of the thesis (System Design and Architecture).

## Figure 4.1: Multi-Sensor Recording System Architecture Overview

```mermaid
graph TB
    subgraph SYSTEM_OVERVIEW ["Multi-Sensor Recording System Architecture"]
        direction TB
        
        subgraph USER_LAYER ["User Interaction Layer"]
            RESEARCHER["Research Operator<br/>• Session Configuration<br/>• Real-time Monitoring<br/>• Data Analysis"]
            SUBJECT["Research Subject<br/>• Natural Behaviour<br/>• Minimal Interference<br/>• Contactless Measurement"]
        end
        
        subgraph CONTROL_LAYER ["Control and Coordination Layer"]
            DESKTOP_APP["Python Desktop Controller<br/>• Session Management<br/>• Device Coordination<br/>• Real-time Monitoring<br/>• Data Export"]
            
            subgraph SYNC_SUBSYSTEM ["Synchronisation Subsystem"]
                MASTER_CLOCK["Master Clock<br/>• Temporal Reference<br/>• Drift Compensation<br/>• Precision Timing"]
                SYNC_PROTOCOL["Sync Protocol<br/>• Clock Distribution<br/>• Event Coordination<br/>• Status Monitoring"]
            end
        end
        
        subgraph SENSOR_LAYER ["Sensor Collection Layer"]
            subgraph MOBILE_PLATFORM_1 ["Mobile Platform 1"]
                ANDROID_APP_1["Android Controller<br/>• Sensor Coordination<br/>• Local Processing<br/>• Network Communication"]
                THERMAL_CAM_1["Thermal Camera<br/>TopDon TC001<br/>• 256x192 Resolution<br/>• USB-C Interface"]
                GSR_SENSOR["GSR Sensor<br/>Shimmer3 GSR+<br/>• Bluetooth LE<br/>• 128Hz Sampling"]
                VIDEO_CAM_1["RGB Camera<br/>• 4K Recording<br/>• Built-in Sensor<br/>• Hardware Sync"]
            end
            
            subgraph MOBILE_PLATFORM_2 ["Mobile Platform 2"]
                ANDROID_APP_2["Android Controller<br/>• Secondary Node<br/>• Coordinated Recording<br/>• Backup Data"]
                THERMAL_CAM_2["Thermal Camera<br/>TopDon TC001<br/>• Coordinated Capture<br/>• USB-C Interface"]
                VIDEO_CAM_2["RGB Camera<br/>• 4K Recording<br/>• Synchronised Capture<br/>• Multi-angle View"]
            end
        end
        
        subgraph DATA_LAYER ["Data Processing and Storage Layer"]
            REAL_TIME_PROC["Real-time Processing<br/>• Stream Analysis<br/>• Quality Monitoring<br/>• Event Detection"]
            LOCAL_STORAGE["Local Storage<br/>• Session Data<br/>• Raw Recordings<br/>• Processed Results"]
            EXPORT_SYSTEM["Export System<br/>• Data Formatting<br/>• File Organisation<br/>• Research Integration"]
        end
        
        subgraph NETWORK_LAYER ["Network Communication Layer"]
            TCP_PROTOCOL["TCP/IP Network<br/>• Command & Control<br/>• Status Updates<br/>• Configuration"]
            BLUETOOTH_PROTOCOL["Bluetooth LE<br/>• Sensor Data<br/>• Low Latency<br/>• Direct Connection"]
            JSON_MESSAGING["JSON Protocol<br/>• Structured Commands<br/>• Status Messages<br/>• Configuration Data"]
        end
    end
    
    %% User interactions
    RESEARCHER --> DESKTOP_APP
    SUBJECT -.->|Measured by| THERMAL_CAM_1
    SUBJECT -.->|Measured by| THERMAL_CAM_2
    SUBJECT -.->|Minimal contact| GSR_SENSOR
    SUBJECT -.->|Recorded by| VIDEO_CAM_1
    SUBJECT -.->|Recorded by| VIDEO_CAM_2
    
    %% Control flow
    DESKTOP_APP --> MASTER_CLOCK
    MASTER_CLOCK --> SYNC_PROTOCOL
    SYNC_PROTOCOL --> ANDROID_APP_1
    SYNC_PROTOCOL --> ANDROID_APP_2
    
    %% Hardware integration
    ANDROID_APP_1 --> THERMAL_CAM_1
    ANDROID_APP_1 --> VIDEO_CAM_1
    ANDROID_APP_1 -.->|Bluetooth LE| GSR_SENSOR
    ANDROID_APP_2 --> THERMAL_CAM_2
    ANDROID_APP_2 --> VIDEO_CAM_2
    
    %% Data processing flow
    ANDROID_APP_1 --> REAL_TIME_PROC
    ANDROID_APP_2 --> REAL_TIME_PROC
    GSR_SENSOR --> REAL_TIME_PROC
    REAL_TIME_PROC --> LOCAL_STORAGE
    LOCAL_STORAGE --> EXPORT_SYSTEM
    
    %% Network communication
    DESKTOP_APP --> TCP_PROTOCOL
    TCP_PROTOCOL --> JSON_MESSAGING
    JSON_MESSAGING --> ANDROID_APP_1
    JSON_MESSAGING --> ANDROID_APP_2
    GSR_SENSOR --> BLUETOOTH_PROTOCOL
    BLUETOOTH_PROTOCOL --> ANDROID_APP_1
    
    classDef user fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    classDef control fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef sensor fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
    classDef data fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
    classDef network fill:#fff8e1,stroke:#ffc107,stroke-width:2px
    classDef hardware fill:#ffebee,stroke:#f44336,stroke-width:2px
    
    class RESEARCHER,SUBJECT user
    class DESKTOP_APP,MASTER_CLOCK,SYNC_PROTOCOL control
    class ANDROID_APP_1,ANDROID_APP_2 sensor
    class REAL_TIME_PROC,LOCAL_STORAGE,EXPORT_SYSTEM data
    class TCP_PROTOCOL,BLUETOOTH_PROTOCOL,JSON_MESSAGING network
    class THERMAL_CAM_1,THERMAL_CAM_2,GSR_SENSOR,VIDEO_CAM_1,VIDEO_CAM_2 hardware
```

## Figure 4.2: Android App Clean Architecture

```mermaid
graph TB
    subgraph ANDROID ["Android Application Clean Architecture"]
        direction TB
        
        subgraph PRESENTATION ["Presentation Layer - UI and User Interaction"]
            direction TB
            
            subgraph ACTIVITIES ["Activities and Fragments"]
                MA["MainActivity<br/>• Main UI Orchestrator<br/>• Fragment Navigation<br/>• Lifecycle Management"]
                RF["RecordingFragment<br/>• Recording Controls UI<br/>• Real-time Status Display<br/>• User Interaction Handler"]
                DF["DevicesFragment<br/>• Device Management UI<br/>• Connection Status Display<br/>• Pairing Interface"]
                CF["CalibrationFragment<br/>• Sensor Calibration UI<br/>• Validation Controls<br/>• Configuration Interface"]
                FF["FilesFragment<br/>• File Management UI<br/>• Browse Recordings<br/>• Export Controls"]
            end
            
            subgraph VIEWMODELS ["ViewModels and State Management"]
                MVM["MainViewModel<br/>• UI State Coordination<br/>• LiveData Management<br/>• Event Handling"]
                RSM["RecordingStateManager<br/>• Recording State Logic<br/>• Status Broadcasting<br/>• Error Handling"]
                DSM["DeviceStateManager<br/>• Device Connection States<br/>• Health Monitoring<br/>• Status Updates"]
            end
        end
        
        subgraph DOMAIN ["Domain Layer - Business Logic and Use Cases"]
            direction TB
            
            subgraph RECORDING ["Recording Components"]
                CR["CameraRecorder<br/>• Camera2 API Integration<br/>• 4K Video + RAW Capture<br/>• Concurrent Recording"]
                TR["ThermalRecorder<br/>• TopDon SDK Integration<br/>• Thermal Image Processing<br/>• Real-time Capture"]
                SR["ShimmerRecorder<br/>• Bluetooth GSR Integration<br/>• Physiological Data Collection<br/>• Real-time Streaming"]
            end
            
            subgraph SESSION ["Session Management"]
                SM["SessionManager<br/>• Recording Session Logic<br/>• Lifecycle Coordination<br/>• State Persistence"]
                SI["SessionInfo<br/>• Session Metadata<br/>• Status Tracking<br/>• Configuration Storage"]
                SS["SensorSample<br/>• Data Point Abstraction<br/>• Timestamp Synchronisation<br/>• Format Standardisation"]
            end
            
            subgraph COMMUNICATION ["Communication Layer"]
                PCH["PCCommunicationHandler<br/>• PC Socket Communication<br/>• Command Processing<br/>• Protocol Implementation"]
                CM["ConnectionManager<br/>• Network Management<br/>• Reconnection Logic<br/>• Health Monitoring"]
                PS["PreviewStreamer<br/>• Live Preview Streaming<br/>• Real-time Transmission<br/>• Quality Management"]
            end
        end
        
        subgraph DATA ["Data Layer - Storage and Device Integration"]
            direction TB
            
            subgraph DEVICE_MGT ["Device Management"]
                DST["DeviceStatusTracker<br/>• Multi-Device Status<br/>• Health Monitoring<br/>• Performance Metrics"]
                BM["BluetoothManager<br/>• Bluetooth LE Connectivity<br/>• Shimmer Integration<br/>• Pairing Management"]
                UM["USBManager<br/>• USB-C OTG Management<br/>• Thermal Camera Control<br/>• Device Detection"]
            end
            
            subgraph STORAGE ["Storage and Persistence"]
                FS["FileSystemManager<br/>• Local Storage Management<br/>• Session Organisation<br/>• File Hierarchy"]
                MS["MetadataSerializer<br/>• JSON Serialisation<br/>• Session Persistence<br/>• Data Integrity"]
                CS["ConfigurationStore<br/>• Settings Persistence<br/>• Shared Preferences<br/>• Configuration Management"]
            end
        end
    end
    
    %% Layer Interactions
    MA ==> MVM
    MVM ==> SM
    RF ==> RSM
    DF ==> DSM
    CF ==> CM
    FF ==> FS
    
    %% Domain Layer Internal
    MVM ==> CR
    MVM ==> TR
    MVM ==> SR
    SM ==> SI
    CR ==> PCH
    TR ==> PCH
    SR ==> PCH
    PCH ==> CM
    
    %% Data Layer
    DSM ==> DST
    BM ==> DST
    UM ==> DST
    SM ==> MS
    MS ==> FS
    
    classDef presentation fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef domain fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef data fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    
    class MA,RF,DF,CF,FF,MVM,RSM,DSM presentation
    class CR,TR,SR,SM,SI,SS,PCH,CM,PS domain
    class DST,BM,UM,FS,MS,CS data
```

## Figure 4.3: PC App Architecture (Python PyQt5)

```mermaid
graph TB
    subgraph PC_APP ["PC Application Architecture - Python and PyQt5"]
        direction TB
        
        subgraph UI_LAYER ["User Interface Layer - PyQt5 Framework"]
            direction TB
            
            subgraph MAIN_UI ["Main Application Windows"]
                MW["MainWindow<br/>• Primary Application Window<br/>• Menu Bar and Toolbar<br/>• Status Bar Management<br/>• Central Widget Coordination"]
                DW["DeviceWindow<br/>• Device Management Interface<br/>• Real-time Status Display<br/>• Connection Control Panel<br/>• Health Monitoring Dashboard"]
                RW["RecordingWindow<br/>• Recording Control Interface<br/>• Live Preview Management<br/>• Session Configuration<br/>• Progress Monitoring"]
                CW["CalibrationWindow<br/>• Sensor Calibration Interface<br/>• Validation Controls<br/>• Configuration Management<br/>• Quality Assurance Tools"]
            end
            
            subgraph WIDGETS ["Custom Widgets and Components"]
                PW["PreviewWidget<br/>• Live Video Preview<br/>• Multi-stream Display<br/>• Real-time Rendering<br/>• Quality Controls"]
                SW["StatusWidget<br/>• System Status Display<br/>• Performance Metrics<br/>• Alert Management<br/>• Health Indicators"]
                LW["LogWidget<br/>• Application Logging<br/>• Event History<br/>• Debug Information<br/>• Error Tracking"]
                FW["FileWidget<br/>• File Management Interface<br/>• Session Browser<br/>• Export Controls<br/>• Metadata Display"]
            end
        end
        
        subgraph BUSINESS ["Business Logic Layer - Core Application Logic"]
            direction TB
            
            subgraph CONTROLLERS ["Control Components"]
                AC["ApplicationController<br/>• Main Application Logic<br/>• Event Coordination<br/>• State Management<br/>• Command Processing"]
                DC["DeviceController<br/>• Device Management Logic<br/>• Connection Orchestration<br/>• Status Monitoring<br/>• Command Distribution"]
                RC["RecordingController<br/>• Recording Session Logic<br/>• Multi-stream Coordination<br/>• Quality Management<br/>• Error Recovery"]
                CC["CalibrationController<br/>• Calibration Process Logic<br/>• Validation Algorithms<br/>• Configuration Management<br/>• Quality Assurance"]
            end
            
            subgraph MANAGERS ["Management Services"]
                SM["SessionManager<br/>• Session Lifecycle Management<br/>• Metadata Coordination<br/>• State Persistence<br/>• Archive Management"]
                DM["DeviceManager<br/>• Multi-device Coordination<br/>• Health Monitoring<br/>• Connection Pool Management<br/>• Error Handling"]
                FM["FileManager<br/>• File System Management<br/>• Storage Organisation<br/>• Backup Coordination<br/>• Cleanup Operations"]
                NM["NetworkManager<br/>• Network Communication<br/>• Socket Management<br/>• Protocol Handling<br/>• Reconnection Logic"]
            end
        end
        
        subgraph DATA_LAYER ["Data Access Layer - Storage and Communication"]
            direction TB
            
            subgraph COMMUNICATION ["Communication Services"]
                SocketServer["SocketServer<br/>• TCP Socket Management<br/>• Client Connection Handling<br/>• Protocol Implementation<br/>• Message Routing"]
                CommandProcessor["CommandProcessor<br/>• Command Parsing and Validation<br/>• Response Generation<br/>• Error Handling<br/>• Protocol Compliance"]
                DataStreamer["DataStreamer<br/>• Real-time Data Streaming<br/>• Multi-client Broadcasting<br/>• Quality of Service<br/>• Buffer Management"]
            end
            
            subgraph STORAGE ["Storage Services"]
                FileHandler["FileHandler<br/>• File I/O Operations<br/>• Directory Management<br/>• Metadata Storage<br/>• Version Control"]
                DatabaseManager["DatabaseManager<br/>• SQLite Integration<br/>• Session Metadata<br/>• Query Optimisation<br/>• Data Integrity"]
                ConfigManager["ConfigManager<br/>• Configuration Storage<br/>• Settings Persistence<br/>• Default Management<br/>• Validation"]
            end
            
            subgraph SENSORS ["Sensor Integration"]
                CameraHandler["CameraHandler<br/>• USB Camera Integration<br/>• OpenCV Processing<br/>• Frame Capture<br/>• Quality Control"]
                DataCollector["DataCollector<br/>• Multi-source Data Collection<br/>• Timestamp Synchronisation<br/>• Format Standardisation<br/>• Quality Assurance"]
                SyncManager["SyncManager<br/>• Clock Synchronisation<br/>• Multi-device Timing<br/>• Latency Compensation<br/>• Drift Correction"]
            end
        end
    end
    
    %% UI Layer Connections
    MW ==> AC
    DW ==> DC
    RW ==> RC
    CW ==> CC
    
    %% Widget to Controller Connections
    PW ==> RC
    SW ==> DC
    LW ==> AC
    FW ==> FM
    
    %% Business Logic Internal Connections
    AC ==> SM
    DC ==> DM
    RC ==> SM
    CC ==> DM
    SM ==> FM
    DM ==> NM
    
    %% Data Layer Connections
    NM ==> SocketServer
    SocketServer ==> CommandProcessor
    CommandProcessor ==> DataStreamer
    SM ==> FileHandler
    FM ==> DatabaseManager
    AC ==> ConfigManager
    
    %% Sensor Integration
    RC ==> CameraHandler
    DataStreamer ==> DataCollector
    DataCollector ==> SyncManager
    
    classDef ui fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef business fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef data fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    
    class MW,DW,RW,CW,PW,SW,LW,FW ui
    class AC,DC,RC,CC,SM,DM,FM,NM business
    class SocketServer,CommandProcessor,DataStreamer,FileHandler,DatabaseManager,ConfigManager,CameraHandler,DataCollector,SyncManager data
```

## Figure 4.4: Multi-Layer System Architecture

```mermaid
graph TB
    subgraph "Multi-Layer System Architecture"
        subgraph "Application Layer"
            subgraph "User Interface Applications"
                ANDROID_UI[Android Mobile App<br/>Recording Interface<br/>Preview Display<br/>Configuration Controls]
                PC_UI[PC Desktop Controller<br/>PyQt5 Interface<br/>Multi-device Monitoring<br/>Central Control Panel]
                WEB_UI[Web Interface<br/>Browser-based Control<br/>Real-time Dashboard<br/>Analytics Visualisation]
            end
            
            subgraph "Application Services"
                SESSION_APP[Session Management App<br/>Recording Orchestration<br/>Timing Coordination<br/>State Management]
                CALIB_APP[Calibration Application<br/>Geometric Calibration<br/>Thermal Calibration<br/>Quality Assessment]
                EXPORT_APP[Export Application<br/>Data Packaging<br/>Compression Service<br/>Documentation Generator]
            end
        end
        
        subgraph "Business Logic Layer"
            subgraph "Core Business Services"
                RECORD_SERV[Recording Service<br/>Multi-stream Coordination<br/>Synchronisation Logic<br/>Quality Monitoring]
                DEVICE_SERV[Device Management Service<br/>Connection Management<br/>Status Monitoring<br/>Configuration Service]
                DATA_SERV[Data Processing Service<br/>Stream Processing<br/>Real-time Analysis<br/>Storage Coordination]
            end
            
            subgraph "Integration Services"
                SENSOR_INT[Sensor Integration<br/>GSR Processing<br/>Thermal Processing<br/>Camera Processing]
                NETWORK_INT[Network Integration<br/>Socket Communication<br/>Protocol Management<br/>Quality Management]
                STORAGE_INT[Storage Integration<br/>File Management<br/>Backup Coordination<br/>Metadata Management]
            end
        end
        
        subgraph "Service Layer"
            subgraph "Communication Services"
                SOCKET_SERV[Socket Service<br/>TCP/UDP Communication<br/>Connection Management<br/>Protocol Handling]
                STREAM_SERV[Streaming Service<br/>Video Streaming<br/>Data Streaming<br/>Real-time Delivery]
                SYNC_SERV[Synchronisation Service<br/>Clock Management<br/>Offset Calculation<br/>Precision Control]
            end
            
            subgraph "Data Services"
                FILE_SERV[File Service<br/>Storage Management<br/>Naming Convention<br/>Organisation Logic]
                META_SERV[Metadata Service<br/>JSON Processing<br/>Configuration Management<br/>Schema Validation]
                BACKUP_SERV[Backup Service<br/>Replication Logic<br/>Integrity Verification<br/>Archive Management]
            end
        end
        
        subgraph "Infrastructure Layer"
            subgraph "Hardware Abstraction"
                CAM_HAL[Camera HAL<br/>Camera2 API<br/>Video Capture<br/>Device Control]
                USB_HAL[USB HAL<br/>Webcam Interface<br/>Thermal Camera<br/>Power Management]
                BT_HAL[Bluetooth HAL<br/>BLE Communication<br/>Shimmer Protocol<br/>Pairing Management]
            end
            
            subgraph "Operating System Interface"
                ANDROID_OS[Android OS Interface<br/>Permission Management<br/>Resource Management<br/>System Services]
                WINDOWS_OS[Windows OS Interface<br/>Driver Management<br/>Performance Monitoring<br/>Security Services]
                LINUX_OS[Linux OS Interface<br/>Real-time Support<br/>Process Management<br/>System Optimisation]
            end
        end
    end
    
    %% Layer Connections
    ANDROID_UI --> SESSION_APP
    PC_UI --> CALIB_APP
    WEB_UI --> EXPORT_APP
    
    SESSION_APP --> RECORD_SERV
    CALIB_APP --> DEVICE_SERV
    EXPORT_APP --> DATA_SERV
    
    RECORD_SERV --> SENSOR_INT
    DEVICE_SERV --> NETWORK_INT
    DATA_SERV --> STORAGE_INT
    
    SENSOR_INT --> SOCKET_SERV
    NETWORK_INT --> STREAM_SERV
    STORAGE_INT --> SYNC_SERV
    
    SOCKET_SERV --> FILE_SERV
    STREAM_SERV --> META_SERV
    SYNC_SERV --> BACKUP_SERV
    
    FILE_SERV --> CAM_HAL
    META_SERV --> USB_HAL
    BACKUP_SERV --> BT_HAL
    
    CAM_HAL --> ANDROID_OS
    USB_HAL --> WINDOWS_OS
    BT_HAL --> LINUX_OS
    
    classDef user fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    classDef business fill:#e8f5e8,stroke:#4caf50,stroke-width:2px
    classDef service fill:#e3f2fd,stroke:#2196f3,stroke-width:2px
    classDef infrastructure fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
    
    class ANDROID_UI,PC_UI,WEB_UI,SESSION_APP,CALIB_APP,EXPORT_APP user
    class RECORD_SERV,DEVICE_SERV,DATA_SERV,SENSOR_INT,NETWORK_INT,STORAGE_INT business
    class SOCKET_SERV,STREAM_SERV,SYNC_SERV,FILE_SERV,META_SERV,BACKUP_SERV service
    class CAM_HAL,USB_HAL,BT_HAL,ANDROID_OS,WINDOWS_OS,LINUX_OS infrastructure
```
