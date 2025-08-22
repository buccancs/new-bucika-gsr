# Firebase Analytics Dashboard Configuration

This document provides detailed instructions for setting up comprehensive analytics dashboards for the Multi-Sensor Recording System for Contactless GSR Prediction Research.

## Custom Analytics Events Reference

### Research Session Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `recording_session_start` | Recording session initiated | `session_id`, `device_count`, `experiment_type` |
| `recording_session_end` | Recording session completed | `session_id`, `duration_ms`, `data_size_bytes`, `participant_count` |
| `session_paused` | Recording session paused | `session_id`, `pause_reason` |
| `session_resumed` | Recording session resumed | `session_id`, `pause_duration_ms` |

### Device and Sensor Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `gsr_sensor_connected` | GSR sensor connected | `sensor_id`, `sensor_type`, `connection_method` |
| `gsr_sensor_disconnected` | GSR sensor disconnected | `sensor_id`, `disconnect_reason`, `data_loss` |
| `thermal_camera_used` | Thermal camera activated | `camera_model`, `resolution`, `frame_rate` |
| `camera_calibration` | Camera calibration performed | `camera_type`, `success`, `error_count` |

### Data Quality Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `calibration_performed` | Device calibration | `calibration_type`, `success`, `duration_ms` |
| `data_quality_check` | Data quality assessment | `session_id`, `quality_score`, `issue_count`, `primary_issue` |
| `synchronization_performed` | Device synchronisation | `device_count`, `success`, `time_drift_ms` |

### Research Workflow Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `workflow_step` | Research workflow step | `step_name`, `session_id`, `step_duration_ms` |
| `participant_consent` | Participant consent logged | `consent_type`, `granted` |
| `research_project_created` | New research project | `project_type`, `collaborator_count` |

### Data Export and Analysis Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `data_export` | Data exported | `export_format`, `file_size_bytes`, `session_count`, `export_type` |
| `analysis_performed` | Data analysis completed | `analysis_type`, `session_id`, `processing_time_ms` |
| `cloud_upload` | File uploaded to cloud | `file_type`, `file_size_bytes`, `success`, `upload_time_ms` |
| `cloud_download` | File downloaded from cloud | `file_type`, `file_size_bytes`, `success` |

### Error and Performance Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `system_error` | System error occurred | `error_type`, `error_message`, `severity` |
| `performance_metric` | Performance measurement | `metric_name`, `metric_value`, `metric_unit` |
| `battery_usage` | Battery usage tracking | `session_id`, `battery_level`, `session_duration_ms` |

### Authentication Events

| Event Name | Description | Parameters |
|------------|-------------|------------|
| `user_authentication` | User signed in | `auth_method`, `researcher_type` |

## Custom User Properties

### Researcher Context Properties

| Property Name | Description | Example Values |
|---------------|-------------|----------------|
| `researcher_type` | Type of researcher | `RESEARCHER`, `PRINCIPAL_INVESTIGATOR`, `STUDENT` |
| `institution` | Research institution | `University College London`, `MIT` |
| `research_area` | Research focus area | `psychology`, `neuroscience`, `hci` |
| `app_version` | Application version | `1.0.0`, `1.1.0-beta` |

## Google Analytics 4 Dashboard Setup

### Dashboard 1: Research Session Overview

**Purpose**: High-level overview of research activity

**Widgets**:
1. **Total Sessions** (Scorecard)
   - Metric: Event count
   - Event name: recording_session_start
   - Date range: Last 30 days

2. **Session Duration Trends** (Time series)
   - Metric: Average of duration_ms parameter
   - Event name: recording_session_end
   - Dimension: Date

3. **Experiment Types** (Pie chart)
   - Metric: Event count
   - Event name: recording_session_start
   - Dimension: experiment_type

4. **Data Volume** (Bar chart)
   - Metric: Sum of data_size_bytes parameter
   - Event name: recording_session_end
   - Dimension: Week

### Dashboard 2: Device and Data Quality

**Purpose**: Monitor device performance and data quality

**Widgets**:
1. **Device Connection Success Rate** (Scorecard)
   - Metric: Event count ratio
   - Numerator: gsr_sensor_connected events
   - Denominator: gsr_sensor_connected + gsr_sensor_disconnected events

2. **Data Quality Distribution** (Histogram)
   - Metric: Event count
   - Event name: data_quality_check
   - Dimension: quality_score (bucketed)

3. **Calibration Success Rate** (Scorecard)
   - Metric: Average of success parameter
   - Event name: calibration_performed

4. **Synchronisation Accuracy** (Time series)
   - Metric: Average of time_drift_ms parameter
   - Event name: synchronization_performed
   - Dimension: Date

### Dashboard 3: Researcher Activity

**Purpose**: Track researcher usage patterns

**Widgets**:
1. **Active Researchers** (Scorecard)
   - Metric: Active users
   - Date range: Last 7 days

2. **Sessions by Researcher Type** (Bar chart)
   - Metric: Event count
   - Event name: recording_session_start
   - Dimension: researcher_type

3. **Institution Activity** (Table)
   - Metric: Event count, Active users
   - Event name: recording_session_start
   - Dimension: institution

4. **Peak Usage Hours** (Heatmap)
   - Metric: Event count
   - Event name: recording_session_start
   - Dimensions: Hour of day, Day of week

### Dashboard 4: System Performance

**Purpose**: Monitor system health and performance

**Widgets**:
1. **Error Rate** (Scorecard)
   - Metric: Event count per session
   - Event name: system_error

2. **Error Types** (Pie chart)
   - Metric: Event count
   - Event name: system_error
   - Dimension: error_type

3. **App Performance** (Time series)
   - Metric: Average of metric_value parameter
   - Event name: performance_metric
   - Dimension: metric_name
   - Filter: metric_name = "app_startup_time"

4. **Battery Impact** (Scatter plot)
   - X-axis: session_duration_ms
   - Y-axis: battery_level change
   - Event name: battery_usage

## BigQuery Integration for Advanced Analytics

### Export Setup

1. In Firebase Console → Analytics → BigQuery Linking
2. Enable daily export to BigQuery
3. Choose dataset location (same region as Firebase project)

### Custom SQL Queries

#### 1. Session Success Rate Analysis

```sql
WITH session_events AS (
  SELECT
    user_pseudo_id,
    event_timestamp,
    event_name,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'session_id') AS session_id,
    (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'device_count') AS device_count
  FROM `project-id.analytics_dataset.events_*`
  WHERE _TABLE_SUFFIX BETWEEN '20240101' AND '20241231'
    AND event_name IN ('recording_session_start', 'recording_session_end')
)

SELECT
  DATE(TIMESTAMP_MICROS(event_timestamp)) AS date,
  COUNT(DISTINCT CASE WHEN event_name = 'recording_session_start' THEN session_id END) AS started_sessions,
  COUNT(DISTINCT CASE WHEN event_name = 'recording_session_end' THEN session_id END) AS completed_sessions,
  SAFE_DIVIDE(
    COUNT(DISTINCT CASE WHEN event_name = 'recording_session_end' THEN session_id END),
    COUNT(DISTINCT CASE WHEN event_name = 'recording_session_start' THEN session_id END)
  ) AS completion_rate
FROM session_events
GROUP BY date
ORDER BY date
```

#### 2. Data Quality Trends

```sql
SELECT
  DATE(TIMESTAMP_MICROS(event_timestamp)) AS date,
  AVG((SELECT value.double_value FROM UNNEST(event_params) WHERE key = 'quality_score')) AS avg_quality_score,
  COUNT(*) AS quality_checks_performed,
  COUNTIF((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'issue_count') > 0) AS sessions_with_issues
FROM `project-id.analytics_dataset.events_*`
WHERE _TABLE_SUFFIX BETWEEN '20240101' AND '20241231'
  AND event_name = 'data_quality_check'
GROUP BY date
ORDER BY date
```

#### 3. Research Productivity Analysis

```sql
WITH researcher_sessions AS (
  SELECT
    user_pseudo_id,
    (SELECT value.string_value FROM UNNEST(user_properties) WHERE key = 'researcher_type') AS researcher_type,
    (SELECT value.string_value FROM UNNEST(user_properties) WHERE key = 'institution') AS institution,
    COUNT(DISTINCT (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'session_id')) AS total_sessions,
    SUM((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'duration_ms')) AS total_duration_ms,
    AVG((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'duration_ms')) AS avg_session_duration_ms
  FROM `project-id.analytics_dataset.events_*`
  WHERE _TABLE_SUFFIX BETWEEN '20240101' AND '20241231'
    AND event_name = 'recording_session_end'
  GROUP BY user_pseudo_id, researcher_type, institution
)

SELECT
  researcher_type,
  institution,
  COUNT(*) AS number_of_researchers,
  AVG(total_sessions) AS avg_sessions_per_researcher,
  AVG(avg_session_duration_ms / 60000) AS avg_session_duration_minutes
FROM researcher_sessions
WHERE researcher_type IS NOT NULL
GROUP BY researcher_type, institution
ORDER BY researcher_type, institution
```

## Automated Reporting Setup

### 1. Data Studio Integration

1. Connect BigQuery to Data Studio
2. Create scheduled reports for:
   - Weekly research activity summary
   - Monthly data quality report
   - Quarterly researcher usage report

### 2. Email Alerts Configuration

Set up alerts in Google Analytics for:

**Critical Alerts** (Immediate notification):
- Error rate > 5% in any hour
- Session completion rate < 80% in any day
- No research activity for > 24 hours

**Warning Alerts** (Daily digest):
- Data quality score < 0.7 for any session
- Device disconnection rate > 10% in any day
- Battery usage > 20% per hour

**Information Alerts** (Weekly digest):
- New researcher registrations
- Usage pattern changes
- Storage cost increases

### 3. Slack Integration

Create webhook for research team notifications:

```javascript
// Google Apps Script for automated Slack notifications
function sendWeeklyReport() {
  const data = getAnalyticsData(); // Fetch from Analytics API
  
  const message = {
    text: `📊 Weekly Research Activity Report`,
    blocks: [
      {
        type: "section",
        text: {
          type: "mrkdwn",
          text: `*Sessions Completed:* ${data.sessions}\n*Average Quality Score:* ${data.quality}\n*Active Researchers:* ${data.researchers}`
        }
      }
    ]
  };
  
  const response = UrlFetchApp.fetch(SLACK_WEBHOOK_URL, {
    method: 'POST',
    contentType: 'application/json',
    payload: JSON.stringify(message)
  });
}
```

## Research Insights Dashboard

### Key Metrics to Track

1. **Research Efficiency**
   - Sessions per researcher per week
   - Average session duration
   - Data collection rate (GB per session)

2. **Data Quality Indicators**
   - Quality score distribution
   - Calibration success rates
   - Synchronisation accuracy

3. **System Reliability**
   - Error rates by component
   - Uptime percentage
   - Performance metrics

4. **Researcher Experience**
   - Session completion rates
   - Feature usage patterns
   - Support ticket correlation

### Custom Calculated Metrics

Create calculated fields in Data Studio:

1. **Research Productivity Score**
   ```
   (Completed Sessions * Avg Quality Score) / Total Active Time
   ```

2. **System Reliability Index**
   ```
   (1 - Error Rate) * Uptime Percentage * Avg Performance Score
   ```

3. **Data Collection Efficiency**
   ```
   Total Data Size / (Session Count * Average Session Duration)
   ```

## Implementation Checklist

- [ ] Firebase Analytics enabled and configured
- [ ] Custom events implemented in Android app
- [ ] User properties set for researcher context
- [ ] Google Analytics 4 dashboards created
- [ ] BigQuery export enabled
- [ ] Custom SQL queries developed and tested
- [ ] Data Studio reports configured
- [ ] Email alerts set up
- [ ] Slack integration configured
- [ ] Weekly/monthly reporting automated
- [ ] Research team trained on dashboard usage
- [ ] Performance benchmarks established
- [ ] Review process scheduled (monthly)

This comprehensive analytics setup provides deep insights into research activity, system performance, and data quality, enabling evidence-based optimisation of the research platform.