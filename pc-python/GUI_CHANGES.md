# Simplified PyQt6 GUI Implementation

## Overview

The Python PC Orchestrator GUI has been completely redesigned to focus on the 3 core research requirements as requested:

### New 3-Tab Structure

1. **📷 Image Preview** - IR+RGB images from connected phones
2. **🎬 Emotion Videos** - Video playback for emotion illicitation  
3. **📱 Device Monitor** - Device connections and session management

## GUI Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Bucika GSR PC Orchestrator - Research Platform                             │
│ File  View  Help                                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ 🖥️ WebSocket: ✅ | mDNS: ✅ | TimeSync: ✅    📱 Active: 0 | Devices: 0     │
├─────────────────────────────────────────────────────────────────────────────┤
│ [📷 Image Preview] [🎬 Emotion Videos] [📱 Device Monitor]                   │
│                                                                             │
│ ┌─ Tab 1: Image Preview ─────────────────────────────────────────────────┐  │
│ │  IR + RGB Image Preview from Connected Phones                           │  │
│ │                                                                          │  │
│ │  ┌─ Device 1 ─────────────┐  ┌─ Device 2 ─────────────┐                │  │
│ │  │ 📱 Phone 1 (dev_001)    │  │ 📱 Phone 2 (dev_002)    │                │  │
│ │  │ ┌─────────┐ ┌─────────┐ │  │ ┌─────────┐ ┌─────────┐ │                │  │
│ │  │ │🌡️ IR    │ │📷 RGB   │ │  │ │🌡️ IR    │ │📷 RGB   │ │                │  │
│ │  │ │ Camera  │ │ Camera  │ │  │ │ Camera  │ │ Camera  │ │                │  │
│ │  │ │ [Image] │ │ [Image] │ │  │ │ [Image] │ │ [Image] │ │                │  │
│ │  │ └─────────┘ └─────────┘ │  │ └─────────┘ └─────────┘ │                │  │
│ │  │ ⏱️ Last: 14:32:15       │  │ ⏱️ Last: 14:32:16       │                │  │
│ │  └─────────────────────────┘  └─────────────────────────┘                │  │
│ │                                                                          │  │
│ │  [🔄 Refresh] [💾 Save Images] [☑️ Auto-refresh every 5s]                │  │
│ └──────────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

```
┌─ Tab 2: Emotion Videos ─────────────────────────────────────────────────┐
│ ┌─ Video List ─────────────┐ ┌─ Video Player ─────────────────────────┐ │
│ │ Emotion Illicitation     │ │                                        │ │
│ │ Videos                   │ │        [Video Display Area]            │ │
│ │                          │ │                                        │ │
│ │ • happy_scene_1.mp4      │ │                                        │ │
│ │ • sad_movie_clip.mp4     │ │                                        │ │
│ │ • fear_spider.mp4        │ │                                        │ │
│ │ • anger_traffic.mov      │ │                                        │ │
│ │                          │ │                                        │ │
│ │ 📹 fear_spider.mp4       │ │ [▶️ Play] [⏹️ Stop] [████████▒▒] 75%   │ │
│ │ 📁 Size: 12.5 MB         │ │ Speed: [1.0x ▼] Keyboard: Space/Arrows │ │
│ │                          │ │                                        │ │
│ │ [📁 Browse Videos...]    │ │                                        │ │
│ │ [🔄 Refresh]             │ │                                        │ │
│ │ Category: [All      ▼]   │ │                                        │ │
│ └──────────────────────────┘ └────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

```
┌─ Tab 3: Device Monitor ─────────────────────────────────────────────────┐
│ Connected Devices & Active Sessions          🟢 2 devices connected     │
│                                                                         │
│ ┌─ 📱 Connected Devices ─────────────────────────────────────────────┐  │
│ │ Device ID  │ Name     │ Type        │ Version │ Battery │ Status   │  │
│ │ dev_001    │ Phone 1  │ Android GSR │ 2.1.0   │ 85%     │ 🟢 Conn. │  │
│ │ dev_002    │ Phone 2  │ Android GSR │ 2.1.0   │ 92%     │ 🟢 Conn. │  │
│ └─────────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│ ┌─ 🎯 Active Recording Sessions ─────────────────────────────────────┐   │
│ │ Session ID │ Device  │ State   │ Started  │ Duration │ Samples │ Act.│  │
│ │ sess_001   │ dev_001 │ RECORD  │ 14:30:12 │ 00:02:03 │ 15,680  │🛑 S │  │
│ │ sess_002   │ dev_002 │ NEW     │ 14:32:08 │ 00:00:00 │ 0       │📊 V │  │
│ └─────────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│ [🔄 Refresh All] [▶️ Start New Session] [⏹️ Stop All Sessions]           │
└─────────────────────────────────────────────────────────────────────────┘
```

## Key Changes Made

### ✅ Completed Changes

1. **Removed 5 unnecessary tabs:**
   - ❌ Logs tab (removed)
   - ❌ Data Analysis tab (removed)  
   - ❌ ML Analysis tab (removed)
   - ❌ Real-time Plot tab (removed)
   - ❌ Help tab (removed)

2. **Created 3 focused tabs:**
   - ✅ Image Preview tab - Shows IR+RGB images from each connected phone
   - ✅ Emotion Videos tab - Professional video player for emotion illicitation
   - ✅ Device Monitor tab - Combined device connections and session management

3. **Simplified codebase:**
   - Reduced GUI file from ~2,700 lines to ~1,200 lines
   - Removed complex analytics, ML, and plotting features
   - Focused on core research functionality
   - Clean, professional research interface

### 🎯 New Features

**Image Preview Tab:**
- Individual widgets for each connected device  
- Side-by-side IR and RGB image display
- Real-time timestamp updates
- Auto-refresh capability
- Save current images functionality

**Emotion Videos Tab:**
- Professional video player with controls (play, pause, stop)
- Frame-by-frame navigation with keyboard shortcuts
- Variable playback speed (0.5x to 2.0x)
- Progress bar with frame counting
- Category filtering for emotion types
- Browse and import video files

**Device Monitor Tab:**
- Real-time device connection status
- Battery level monitoring  
- Session state tracking with color coding
- Duration and sample count display
- Start/stop session controls
- Combined device and session management

### 🛠️ Technical Implementation

**Class Structure:**
- `SimplifiedMainWindow` - Main GUI class (replaces PyQt6MainWindow)
- `ImagePreviewWidget` - Individual device image display
- `VideoPlayer` - Professional video playback controls
- `VideoWidget` - Custom video display area
- `MainWindowManager` - GUI lifecycle management

**Key Methods:**
- `_create_image_preview_tab()` - IR+RGB image preview setup
- `_create_emotion_video_tab()` - Video player and controls  
- `_create_device_monitoring_tab()` - Device and session tables
- `_update_device_images()` - Real-time image updates
- `_refresh_emotion_video_list()` - Video file management

## Status

✅ **Complete** - The simplified 3-tab GUI has been implemented as requested:
- Image preview for IR+RGB from phones
- Video playback for emotion illicitation  
- Device monitoring and connection management
- All unnecessary tabs and features removed
- Clean, research-focused interface maintained

The implementation maintains full compatibility with the existing WebSocket protocol and session management while providing a streamlined interface focused on the core research requirements.