# Firebase Integration Documentation

## Overview

This document describes the comprehensive Firebase integration added to the Multi-Sensor Recording System for Contactless GSR Prediction Research. The integration provides production-ready cloud-based analytics, data storage, authentication, and monitoring capabilities specifically designed for research workflows.

## Firebase Services Integrated

### 1. Firebase Authentication
**Purpose**: Multi-researcher access control and user management
**Service Class**: `FirebaseAuthService`

**Features**:
- Email/password authentication for researchers
- Researcher type management (RESEARCHER, PRINCIPAL_INVESTIGATOR, ADMIN, etc.)
- User profile management with institution and department tracking
- Secure session management with automatic state monitoring

**Key Functions**:
- `signInWithEmailAndPassword()` - Secure researcher sign-in
- `createResearcherAccount()` - New researcher registration
- `updateUserProfile()` - Profile management
- `sendPasswordResetEmail()` - Password recovery

### 2. Firebase Analytics
**Purpose**: Comprehensive research activity tracking and insights
**Service Class**: `FirebaseAnalyticsService` (Enhanced)

**Key Events Tracked**:
- `recording_session_start/end` - Complete session lifecycle tracking
- `gsr_sensor_connected/disconnected` - Device connectivity monitoring
- `thermal_camera_used` - Camera usage with specifications
- `calibration_performed` - Device calibration success tracking
- `data_quality_check` - Data quality assessment metrics
- `synchronization_performed` - Device synchronisation accuracy
- `data_export` - Research data export tracking
- `analysis_performed` - Data analysis completion
- `cloud_upload/download` - File transfer monitoring
- `system_error` - Error tracking for debugging
- `user_authentication` - Sign-in activity
- `participant_consent` - Ethics compliance tracking

**User Properties**:
- `researcher_type` - Researcher classification
- `institution` - Research institution
- `research_area` - Research focus area
- `app_version` - Version tracking for analytics segmentation

### 3. Firebase Firestore
**Purpose**: Structured research data storage with authentication integration
**Service Class**: `FirebaseFirestoreService` (Enhanced)

**Data Structures**:
- `RecordingSession` - Enhanced session metadata with collaboration support
- `ResearchProject` - Project management with PI and collaborator tracking
- `ResearcherProfile` - Complete researcher profile management
- Calibration data with researcher attribution
- System errors with user context
- Data export requests with approval workflow

**Collections**:
- `researcher_profiles` - Researcher profile and access management
- `recording_sessions` - Session data with collaboration features
- `research_projects` - Project organisation and management
- `calibration_data` - Device calibration records
- `system_errors` - Error logs for debugging
- `data_export_requests` - Data export tracking and approval
- `collaboration_invites` - Researcher collaboration management

**Enhanced Features**:
- Authentication-aware data access (only own/collaborated data)
- Researcher collaboration on sessions and projects
- Usage statistics and analytics
- Data quality scoring and tracking

### 4. Firebase Storage
**Purpose**: Secure file storage with authentication and organisation
**Service Class**: `FirebaseStorageService`

**File Organisation**:
```
recording_sessions/
  ├── session-id-1/
  │   ├── rgb_video_timestamp_filename.mp4
  │   ├── thermal_video_timestamp_filename.mp4
  │   ├── gsr_data_timestamp_filename.csv
  │   └── calibration_timestamp_filename.json
  ├── session-id-2/
  │   └── ...
calibration_data/
  └── researcher-id/
      └── device_type_timestamp.json
participant_data/
  └── session-id/
      ├── consent_form.pdf
      └── anonymized_data.zip
exports/
  └── request-id/
      └── export_timestamp.zip
```

**Features**:
- Automatic file organisation by session and researcher
- Secure access control based on authentication
- Batch upload/download capabilities
- File metadata tracking with researcher attribution
- Storage usage monitoring and cost optimisation

### 5. Firebase Crashlytics
**Purpose**: Enhanced stability monitoring for research reliability
**Integration**: Automatic crash reporting with research context

**Features**:
- Automatic crash detection and reporting
- Custom exception logging with session context
- Research-specific error tracking (device failures, sync issues)
- Stability monitoring for research environment reliability

## Production Security Implementation

### Comprehensive Firestore Security Rules

The implementation includes production-ready security rules (`firestore.rules`) that provide:

- **Authentication-based access control**: All data access requires valid authentication
- **Researcher type authorisation**: Different access levels for different researcher types
- **Data ownership enforcement**: Users can only access their own data and collaborated projects
- **Collaboration support**: Secure sharing between researchers on projects
- **Admin oversight**: Administrative access for system management
- **Audit trail**: Complete logging of data access and modifications

### Storage Security Rules

Firebase Storage rules ensure:
- File access restricted to authenticated researchers
- Ownership-based access control for research files
- Collaboration support for shared research projects
- Metadata-based access control with researcher attribution

## UI Integration

### Enhanced Firebase Status Screen
**Component**: `FirebaseStatusScreen` and `FirebaseStatusViewModel`

**Features**:
- Real-time authentication status monitoring
- Service health indicators for all Firebase services
- Usage statistics display (sessions, data size, duration)
- Test functionality for each service
- Recent activity logs with success/error indicators
- Sign-out functionality with confirmation

### Firebase Authentication Screen
**Component**: `FirebaseAuthScreen` and `FirebaseAuthViewModel`

**Features**:
- Professional researcher sign-in interface
- New researcher account creation with role selection
- Password reset functionality
- Researcher type selection (PI, Researcher, Assistant, Student, Admin)
- Form validation and error handling
- Seamless integration with main app flow

## Production Deployment

### Environment Configuration

The implementation supports multiple environments:
- **Development**: `bucika-gsr-research-dev`
- **Staging**: `bucika-gsr-research-staging`
- **Production**: `bucika-gsr-research-prod`

Each environment has:
- Separate Firebase projects
- Environment-specific configurations
- Isolated data and user management
- Appropriate security rule variations

### Build Flavors Support

Ready for build flavor configuration:
```kotlin
productFlavors {
    create("dev") { /* dev config */ }
    create("staging") { /* staging config */ }
    create("prod") { /* production config */ }
}
```

## Analytics Dashboard Configuration

### Custom Events and Parameters

Comprehensive event tracking includes:
- Research session lifecycle events
- Device and sensor performance metrics
- Data quality and synchronisation tracking
- User activity and workflow analysis
- Error and performance monitoring
- Cloud storage usage patterns

### BigQuery Integration Ready

Analytics data export configured for:
- Advanced research activity analysis
- Custom SQL queries for insights
- Automated reporting workflows
- Cost optimisation analysis

## Implementation Details

### Dependency Injection (Hilt)
All Firebase services are managed through Hilt dependency injection via enhanced `FirebaseModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAnalytics(...)
    @Provides @Singleton fun provideFirebaseAuth(...)
    @Provides @Singleton fun provideFirebaseFirestore(...)
    @Provides @Singleton fun provideFirebaseStorage(...)
    // Enhanced service providers with authentication integration
}
```

### Application Initialisation
Firebase is initialised in `MultiSensorApplication.onCreate()` with full service enablement:

```kotlin
// Initialise Firebase with all services
FirebaseApp.initializeApp(this)
FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
// Authentication state monitoring enabled
```

## Usage Examples

### Authenticated Research Session Workflow

```kotlin
@Inject lateinit var analyticsService: FirebaseAnalyticsService
@Inject lateinit var firestoreService: FirebaseFirestoreService
@Inject lateinit var authService: FirebaseAuthService

// Researcher authentication
val authResult = authService.signInWithEmailAndPassword(email, password)
if (authResult.isSuccess) {
    // Set researcher context
    analyticsService.setResearcherType(user.researcherType.name)
    analyticsService.setInstitution(user.institution)
    
    // Start authenticated session
    analyticsService.logRecordingSessionStart(sessionId, deviceCount, experimentType)
    val session = RecordingSession(
        startTime = Date(),
        deviceCount = deviceCount,
        experimentType = experimentType,
        participantId = participantId
        // researcherId automatically set by service
    )
    firestoreService.saveRecordingSession(session)
}
```

### Collaborative Research Project Management

```kotlin
// Create research project
val project = ResearchProject(
    title = "GSR Stress Response Study",
    description = "Investigating contactless GSR for stress detection",
    experimentType = "stress_response",
    institution = "University College London",
    ethicsApprovalNumber = "UCL/2024/001"
)
val projectResult = firestoreService.createResearchProject(project)

// Add collaborator to project
firestoreService.addCollaboratorToSession(sessionId, "colleague@institution.edu")

// Track project creation
analyticsService.logResearchProjectCreated(project.experimentType, collaboratorCount)
```

### Enhanced Data Quality Tracking

```kotlin
// Perform data quality assessment
val qualityScore = assessDataQuality(sessionData)
val issues = identifyDataIssues(sessionData)

// Log quality metrics
analyticsService.logDataQualityCheck(sessionId, qualityScore, issues)

// Update session with quality score
firestoreService.updateRecordingSessionEnd(
    sessionId, endTime, filePaths, dataSize, qualityScore
)
```

## Documentation and Guides

### Comprehensive Setup Guides

1. **[Production Setup Guide](FIREBASE_PRODUCTION_SETUP.md)** - Complete production deployment instructions
2. **[Analytics Dashboard Configuration](FIREBASE_ANALYTICS_DASHBOARDS.md)** - Dashboard setup and custom reporting
3. **Security Rules Documentation** - Detailed explanation of access control implementation

### Testing and Validation

- **Unit Tests**: Complete test coverage for all Firebase services
- **Integration Tests**: Authentication flow and service interaction testing
- **UI Tests**: Authentication and status screen validation
- **Security Testing**: Rules validation and penetration testing guidelines

## Benefits for Research

1. **Enhanced Security**: Multi-researcher authentication with role-based access control
2. **Data Integrity**: Authenticated data storage with ownership tracking
3. **Collaboration**: Secure sharing and collaboration between researchers
4. **Analytics Insights**: Comprehensive usage analytics for research optimisation
5. **Reliability**: Enhanced crash reporting and error tracking
6. **Scalability**: Cloud infrastructure handles large research datasets
7. **Compliance**: SOC 2 compliance with institutional data protection requirements
8. **Audit Trail**: Complete tracking of data access and modifications
9. **Cost Management**: Optimised storage and query patterns for cost efficiency
10. **Research Insights**: Advanced analytics for research workflow optimisation

## Production Readiness Checklist

- [x] **Authentication System**: Complete multi-researcher access control
- [x] **Security Rules**: Comprehensive Firestore and Storage security implementation
- [x] **Analytics Enhancement**: Production-ready event tracking and insights
- [x] **UI Integration**: Professional authentication and status interfaces
- [x] **Documentation**: Complete setup and configuration guides
- [x] **Environment Support**: Development, staging, and production configurations
- [x] **Error Handling**: Robust error handling and user feedback
- [x] **Performance Optimisation**: Efficient queries and data patterns
- [x] **Cost Optimisation**: Storage lifecycle and query optimisation
- [x] **Monitoring Setup**: Health monitoring and alerting configuration

## Support and Maintenance

### Monitoring and Alerts
Production monitoring includes:
- Service health dashboards
- Error rate alerting
- Performance monitoring
- Cost tracking and optimisation
- Usage pattern analysis

### Backup and Recovery
- Automated Firestore backups
- Storage redundancy and versioning
- Disaster recovery procedures
- Data export and migration tools

### Security Auditing
- Regular access log reviews
- Security rule validation
- Penetration testing guidelines
- Compliance monitoring

This comprehensive Firebase integration transforms the GSR research platform into a production-ready, secure, collaborative system suitable for modern research workflows while maintaining all existing functionality and enhancing the overall research experience.