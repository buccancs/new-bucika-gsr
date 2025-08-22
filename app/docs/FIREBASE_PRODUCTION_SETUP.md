# Firebase Production Setup Guide

This guide provides step-by-step instructions for setting up Firebase in production for the Multi-Sensor Recording System for Contactless GSR Prediction Research.

## Table of Contents

1. [Firebase Project Creation](#firebase-project-creation)
2. [Environment Configuration](#environment-configuration)
3. [Authentication Setup](#authentication-setup)
4. [Firestore Security Rules](#firestore-security-rules)
5. [Analytics Dashboard Configuration](#analytics-dashboard-configuration)
6. [Storage Configuration](#storage-configuration)
7. [Build Configuration](#build-configuration)
8. [Deployment](#deployment)
9. [Monitoring and Maintenance](#monitoring-and-maintenance)

## Firebase Project Creation

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Create a project"
3. Project name: `bucika-gsr-research-prod`
4. Enable Google Analytics (recommended for research insights)
5. Choose or create Analytics account
6. Select your region (choose closest to your institution)

### Step 2: Add Android App

1. In Firebase Console, click "Add app" → Android
2. Package name: `com.multisensor.recording`
3. App nickname: `GSR Research App - Production`
4. Debug signing certificate SHA-1: (Generate using instructions below)

```bash
# Generate debug certificate fingerprint
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# For release builds, use your release keystore
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias_name
```

5. Download `google-services.json`
6. Replace the template file in `AndroidApp/google-services.json`

### Step 3: Enable Firebase Services

In Firebase Console, enable the following services:

1. **Authentication** → Sign-in method → Email/Password (Enable)
2. **Firestore Database** → Create database → Start in test mode
3. **Storage** → Get started → Start in test mode
4. **Analytics** (Already enabled during project creation)
5. **Crashlytics** → Enable Crashlytics

## Environment Configuration

### Development Environment Setup

1. Create separate Firebase projects for different environments:
   - `bucika-gsr-research-dev` (Development)
   - `bucika-gsr-research-staging` (Staging)
   - `bucika-gsr-research-prod` (Production)

2. Configure build flavors in `build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "environment"
    
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"bucika-gsr-research-dev\"")
        }
        
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"bucika-gsr-research-staging\"")
        }
        
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"bucika-gsr-research-prod\"")
        }
    }
}
```

3. Place environment-specific `google-services.json` files:
   - `AndroidApp/src/dev/google-services.json`
   - `AndroidApp/src/staging/google-services.json`
   - `AndroidApp/src/prod/google-services.json`

## Authentication Setup

### Step 1: Configure Authentication Methods

1. In Firebase Console → Authentication → Sign-in method
2. Enable Email/Password authentication
3. Optional: Enable additional providers (Google, institutional SSO)

### Step 2: Set Up Custom Claims (Server-side)

For researcher type management, set up Firebase Functions:

```javascript
// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.setResearcherClaims = functions.https.onCall(async (data, context) => {
  // Verify admin privileges
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError('permission-denied', 'Admin access required');
  }

  const { uid, researcherType } = data;
  
  await admin.auth().setCustomUserClaims(uid, {
    researcherType: researcherType
  });
  
  return { success: true };
});
```

### Step 3: Create Initial Admin User

1. Create your first user through the app
2. In Firebase Console → Authentication → Users
3. Find your user and note the UID
4. In Firestore, create initial admin profile:

```javascript
// In Firestore console, create document in researcher_profiles collection
{
  uid: "your-user-uid",
  email: "admin@institution.edu",
  displayName: "Your Name",
  researcherType: "ADMIN",
  institution: "Your Institution",
  department: "Your Department",
  createdAt: firebase.firestore.Timestamp.now(),
  lastActiveAt: firebase.firestore.Timestamp.now(),
  isActive: true
}
```

## Firestore Security Rules

### Step 1: Deploy Security Rules

1. Copy the security rules from `firestore.rules` to Firebase Console
2. Go to Firestore Database → Rules
3. Replace default rules with the comprehensive rules from the file
4. Publish the rules

### Step 2: Test Security Rules

Use the Rules Playground in Firebase Console to test:

```javascript
// Test authenticated user can create session
service = cloud.firestore;
request.auth.uid = 'test-researcher-uid';
request.resource.data = {
  sessionId: 'test-session-123',
  researcherId: 'test-researcher-uid',
  startTime: timestamp.now(),
  deviceCount: 2
};
// Test path: /databases/(default)/documents/recording_sessions/test-session-123
```

## Analytics Dashboard Configuration

### Step 1: Enable Enhanced Analytics

1. In Firebase Console → Analytics → Events
2. Enable enhanced measurement events
3. Configure custom parameters:
   - `researcher_type`
   - `experiment_type`
   - `institution`
   - `session_duration`
   - `data_quality_score`

### Step 2: Create Custom Dashboards

1. Link Firebase to Google Analytics 4
2. In Google Analytics → Configure → Custom Definitions
3. Create custom dimensions:
   - Researcher Type (User-scoped)
   - Experiment Type (Event-scoped)
   - Institution (User-scoped)
   - Device Count (Event-scoped)

### Step 3: Set Up Research-Specific Reports

Create custom reports for:

1. **Session Analysis Report**
   - Primary dimension: Experiment Type
   - Metrics: Session count, average duration, data size
   - Secondary dimension: Researcher Type

2. **Device Usage Report**
   - Primary dimension: Device Count
   - Metrics: Session count, success rate
   - Filter: Event name = recording_session_start

3. **Data Quality Report**
   - Primary dimension: Data Quality Score (ranges)
   - Metrics: Session count, average score
   - Secondary dimension: Equipment used

### Step 4: Configure Alerts

Set up Google Analytics alerts for:
- Unusual session failure rates
- Low data quality scores
- Authentication errors
- App crashes

## Storage Configuration

### Step 1: Configure Storage Rules

In Firebase Console → Storage → Rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Apply the storage rules from firestore.rules file
    // Copy the storage section
  }
}
```

### Step 2: Set Up Storage Structure

Recommended folder structure:
```
gs://bucika-gsr-research-prod.appspot.com/
├── recording_sessions/
│   ├── {sessionId}/
│   │   ├── rgb_video_{timestamp}.mp4
│   │   ├── thermal_video_{timestamp}.mp4
│   │   ├── gsr_data_{timestamp}.csv
│   │   └── session_metadata.json
├── calibration_data/
│   └── {researcherId}/
│       └── {device_type}_{timestamp}.json
├── participant_data/
│   └── {sessionId}/
│       ├── consent_form.pdf
│       └── anonymized_data.zip
└── exports/
    └── {requestId}/
        └── export_{timestamp}.zip
```

### Step 3: Configure Lifecycle Policies

For cost optimisation, set up lifecycle policies:

1. Move files to Nearline storage after 30 days
2. Move to Coldline storage after 90 days
3. Archive completed projects after 1 year
4. Delete temporary export files after 7 days

## Build Configuration

### Step 1: Update Build Scripts

Add flavor-specific configurations:

```kotlin
// build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("../keystores/release.keystore")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimise.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }
}
```

### Step 2: Configure ProGuard Rules

Add Firebase-specific ProGuard rules in `proguard-rules.pro`:

```proguard
# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Research data models
-keep class com.multisensor.recording.firebase.** { *; }
-keep class com.multisensor.recording.ui.firebase.** { *; }
```

## Deployment

### Step 1: Prepare Release Build

```bash
# Build production release
./gradlew assembleProdRelease

# Test the release build
./gradlew testProdReleaseUnitTest
```

### Step 2: Deploy Security Rules

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialise Firebase project
firebase init

# Deploy rules
firebase deploy --only firestore:rules,storage
```

### Step 3: Set Up CI/CD Pipeline

Example GitHub Actions workflow:

```yaml
# .github/workflows/deploy-production.yml
name: Deploy Production

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          
      - name: Build Release
        run: ./gradlew assembleProdRelease
        
      - name: Deploy Firebase Rules
        uses: w9jds/firebase-action@master
        with:
          args: deploy --only firestore:rules,storage
        env:
          FIREBASE_TOKEN: ${{ secrets.FIREBASE_TOKEN }}
```

## Monitoring and Maintenance

### Step 1: Set Up Monitoring

1. **Performance Monitoring**
   - Enable Firebase Performance in console
   - Monitor app startup time, network requests
   - Track custom traces for research workflows

2. **Crashlytics Monitoring**
   - Set up crash alerts
   - Configure custom keys for research context
   - Monitor non-fatal exceptions

3. **Usage Monitoring**
   - Create BigQuery export for advanced analytics
   - Set up daily/weekly usage reports
   - Monitor storage costs and optimise

### Step 2: Backup Strategy

1. **Firestore Backups**
   - Enable automatic daily backups
   - Set up manual backup for critical research periods
   - Test restore procedures

2. **Storage Backups**
   - Use Cloud Storage Transfer Service for additional backups
   - Implement versioning for critical data
   - Regular integrity checks

### Step 3: Security Auditing

1. **Regular Security Reviews**
   - Monthly review of access logs
   - Quarterly review of security rules
   - Annual penetration testing

2. **Compliance Monitoring**
   - GDPR compliance for EU participants
   - HIPAA compliance if applicable
   - Institutional review board requirements

### Step 4: Performance Optimisation

1. **Database Optimisation**
   - Monitor query performance
   - Optimise indexes based on usage patterns
   - Regular cleanup of test data

2. **Storage Optimisation**
   - Implement compression for large files
   - Optimise file formats
   - Regular cleanup of temporary files

## Cost Management

### Monthly Cost Estimates

- **Firestore**: ~$25-50/month (based on 1000 sessions/month)
- **Storage**: ~$20-40/month (based on 100GB data/month)
- **Authentication**: Free for < 50,000 MAU
- **Analytics**: Free
- **Functions**: ~$5-15/month (if using custom claims)

### Cost Optimisation Tips

1. Use Firestore efficiently (optimise queries, use subcollections)
2. Implement storage lifecycle policies
3. Compress large files before upload
4. Regular cleanup of test/development data
5. Monitor usage with billing alerts

## Support and Troubleshooting

### Common Issues

1. **Authentication Issues**
   - Check package name in Firebase console
   - Verify SHA-1 fingerprints
   - Ensure correct google-services.json file

2. **Security Rules Errors**
   - Use Rules Playground for testing
   - Check user authentication state
   - Verify custom claims setup

3. **Performance Issues**
   - Monitor with Firebase Performance
   - Optimise database queries
   - Implement proper caching

### Getting Help

1. Firebase Documentation: https://firebase.google.com/docs
2. Firebase Support: https://firebase.google.com/support
3. Community: Stack Overflow (tag: firebase)
4. Project-specific issues: Create GitHub issue

---

## Checklist

- [ ] Firebase project created for each environment
- [ ] Android app configured with correct package names
- [ ] Authentication enabled and tested
- [ ] Security rules deployed and tested
- [ ] Analytics dashboard configured
- [ ] Storage rules and structure set up
- [ ] Build flavors configured
- [ ] CI/CD pipeline set up
- [ ] Monitoring and alerting configured
- [ ] Backup strategy implemented
- [ ] Cost monitoring enabled
- [ ] Security audit completed
- [ ] Documentation updated
- [ ] Team training completed

This comprehensive setup ensures your Firebase integration is production-ready for research use with proper security, monitoring, and scalability.