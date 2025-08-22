# Firebase Integration Documentation

## Overview

This document describes the Firebase integration added to the Multi-Sensor Recording System for Contactless GSR Prediction Research. The integration provides cloud-based analytics, data storage, and monitoring capabilities specifically designed for research workflows.

## Firebase Services Integrated

### 1. Firebase Analytics
**Purpose**: Track research activities and app usage patterns
**Service Class**: `FirebaseAnalyticsService`

**Key Events Tracked**:
- `recording_session_start` - When a recording session begins
- `recording_session_end` - When a recording session completes
- `gsr_sensor_connected` - When GSR sensors are connected
- `thermal_camera_used` - When thermal camera is activated
- `calibration_performed` - When device calibration occurs
- `data_export` - When research data is exported

**User Properties**:
- `user_type` - Set to "researcher"
- `experiment_type` - Type of research experiment
- Custom properties for research context

### 2. Firebase Firestore
**Purpose**: Store research metadata and session information
**Service Class**: `FirebaseFirestoreService`

**Data Structures**:
- `RecordingSession` - Complete session metadata including:
  - Session ID and timestamps
  - Device count and sensor IDs
  - Camera models and resolutions
  - Calibration data
  - File paths and data sizes
  - Researcher and participant IDs
  - Experiment type and notes

**Collections**:
- `recording_sessions` - Primary research session data
- `calibration_data` - Device calibration records
- `system_errors` - Error logs for debugging

### 3. Firebase Storage
**Purpose**: Store large sensor data files in the cloud
**Service Class**: `FirebaseStorageService`

**File Organisation**:
```
recording_sessions/
  ├── session-id-1/
  │   ├── rgb_video_timestamp_filename.mp4
  │   ├── thermal_video_timestamp_filename.mp4
  │   ├── gsr_data_timestamp_filename.csv
  │   └── calibration_timestamp_filename.json
  └── session-id-2/
      └── ...
```

**Features**:
- Automatic file organisation by session
- Batch upload/download capabilities
- File metadata tracking
- Storage usage monitoring

### 4. Firebase Crashlytics
**Purpose**: Monitor app stability and crash reporting
**Integration**: Automatic crash reporting in `MainActivity`

**Features**:
- Automatic crash detection and reporting
- Custom exception logging
- Stability monitoring for research reliability

### 5. Firebase Authentication (Ready)
**Purpose**: User management for multi-researcher environments
**Status**: Dependencies added, ready for implementation

## Implementation Details

### Dependency Injection (Hilt)
All Firebase services are managed through Hilt dependency injection via `FirebaseModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAnalytics(...)
    @Provides @Singleton fun provideFirebaseFirestore(...)
    @Provides @Singleton fun provideFirebaseStorage(...)
    // Service providers...
}
```

### Application Initialisation
Firebase is initialised in `MultiSensorApplication.onCreate()`:

```kotlin
// Initialise Firebase
FirebaseApp.initializeApp(this)
FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
```

### UI Integration
Firebase status can be monitored through `FirebaseStatusScreen` which provides:
- Service status indicators
- Usage statistics
- Test functionality
- Recent activity logs

## Usage Examples

### Recording Session Tracking
```kotlin
@Inject lateinit var analyticsService: FirebaseAnalyticsService
@Inject lateinit var firestoreService: FirebaseFirestoreService

// Start session
analyticsService.logRecordingSessionStart(sessionId, deviceCount)
val session = RecordingSession(...)
firestoreService.saveRecordingSession(session)

// End session
analyticsService.logRecordingSessionEnd(sessionId, duration, dataSize)
firestoreService.updateRecordingSessionEnd(sessionId, endTime, filePaths, totalSize)
```

### File Upload to Cloud Storage
```kotlin
@Inject lateinit var storageService: FirebaseStorageService

val files = mapOf(
    "rgb_video" to File("video.mp4"),
    "thermal_video" to File("thermal.mp4"),
    "gsr_data" to File("data.csv")
)

val result = storageService.uploadSessionFiles(sessionId, files)
if (result.isSuccess) {
    val downloadUrls = result.getOrThrow()
    // Store URLs in Firestore
}
```

### Analytics Event Logging
```kotlin
// Log sensor connection
analyticsService.logGSRSensorConnected("shimmer-001")

// Log calibration
analyticsService.logCalibrationPerformed("camera_calibration", success = true)

// Log data export
analyticsService.logDataExport("CSV", fileSizeBytes)
```

## Configuration

### Firebase Project Setup
1. Create a Firebase project at https://console.firebase.google.com
2. Add Android app with package name `com.multisensor.recording`
3. Download `google-services.json` and replace the template file
4. Enable Analytics, Firestore, Storage, and Crashlytics services

### Build Configuration
Firebase dependencies are managed through Firebase BOM:
```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
implementation("com.google.firebase:firebase-analytics-ktx")
implementation("com.google.firebase:firebase-crashlytics-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
```

### Security Rules
Example Firestore security rules for research data:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow researchers to read/write their own sessions
    match /recording_sessions/{sessionId} {
      allow read, write: if request.auth != null 
        && resource.data.researcherId == request.auth.uid;
    }
  }
}
```

## Testing

### Unit Tests
Comprehensive unit tests are provided for all Firebase services:
- `FirebaseAnalyticsServiceTest` - Analytics event logging
- `FirebaseFirestoreServiceTest` - Database operations
- `FirebaseStorageServiceTest` - File operations (can be added)

### Integration Testing
The `FirebaseStatusScreen` provides manual testing capabilities:
- Test Analytics - Logs sample events
- Test Firestore - Creates test session document
- View service status and statistics

## Benefits for Research

1. **Data Integrity**: Cloud backup of all research data
2. **Analytics Insights**: Understanding of app usage patterns
3. **Reliability Monitoring**: Crash reporting for stable research environment
4. **Collaboration**: Shared cloud storage for research teams
5. **Scalability**: Cloud infrastructure handles large datasets
6. **Compliance**: Firebase provides SOC 2 compliance for research data

## Next Steps

1. Set up actual Firebase project (currently using template configuration)
2. Configure security rules for research data access
3. Implement Firebase Authentication for multi-user access
4. Add real-time data synchronisation for collaborative research
5. Set up Firebase Functions for automated data processing
6. Configure Analytics dashboards for research insights

## Support

For Firebase-related issues:
- Check Firebase Console for service status
- Review Firebase documentation: https://firebase.google.com/docs
- Use `FirebaseStatusScreen` for integration diagnostics
- Check application logs for Firebase initialisation messages