"""
Advanced research tools for comprehensive GSR data analysis and export.
"""

import json
import csv
import zipfile
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
import asyncio
from dataclasses import dataclass, asdict
from loguru import logger

try:
    import pandas as pd
    import numpy as np
    import matplotlib.pyplot as plt
    import matplotlib.dates as mdates
    from matplotlib.backends.backend_pdf import PdfPages
    import seaborn as sns
    ANALYSIS_AVAILABLE = True
except ImportError:
    ANALYSIS_AVAILABLE = False

try:
    from scipy import stats, signal
    from scipy.stats import pearsonr, spearmanr
    SCIPY_AVAILABLE = True
except ImportError:
    SCIPY_AVAILABLE = False


@dataclass
class ResearchSession:
    """Complete research session data structure"""
    session_id: str
    device_id: str
    participant_id: Optional[str] = None
    session_name: str = ""
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    duration_minutes: float = 0.0
    gsr_samples: int = 0
    sync_marks: List[Dict] = None
    metadata: Dict[str, Any] = None
    quality_score: float = 0.0
    analysis_completed: bool = False
    
    def __post_init__(self):
        if self.sync_marks is None:
            self.sync_marks = []
        if self.metadata is None:
            self.metadata = {}


@dataclass
class ResearchReport:
    """Comprehensive research report structure"""
    report_id: str
    generated_at: datetime
    sessions: List[ResearchSession]
    total_participants: int
    total_duration_hours: float
    aggregate_statistics: Dict[str, Any]
    cross_session_analysis: Dict[str, Any]
    recommendations: List[str]
    data_quality_summary: Dict[str, Any]
    export_formats: List[str]


class SessionExporter:
    """Advanced session data export functionality"""
    
    def __init__(self, data_directory: Path):
        self.data_directory = Path(data_directory)
    
    async def export_session_comprehensive(self, session_id: str, 
                                         export_format: str = "complete",
                                         output_dir: Path = None) -> Path:
        """Export complete session data in various formats"""
        
        if output_dir is None:
            output_dir = self.data_directory / "exports"
        output_dir.mkdir(exist_ok=True)
        
        session_path = self.data_directory / session_id
        if not session_path.exists():
            raise ValueError(f"Session {session_id} not found")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        if export_format == "csv":
            return await self._export_csv(session_id, session_path, output_dir, timestamp)
        elif export_format == "json":
            return await self._export_json(session_id, session_path, output_dir, timestamp)
        elif export_format == "research_package":
            return await self._export_research_package(session_id, session_path, output_dir, timestamp)
        elif export_format == "complete":
            return await self._export_complete_package(session_id, session_path, output_dir, timestamp)
        else:
            raise ValueError(f"Unknown export format: {export_format}")
    
    async def _export_csv(self, session_id: str, session_path: Path, 
                         output_dir: Path, timestamp: str) -> Path:
        """Export session data as CSV"""
        output_file = output_dir / f"{session_id}_data_{timestamp}.csv"
        
        # Find GSR data files
        gsr_files = list(session_path.glob("gsr_data_*.csv"))
        
        if not gsr_files:
            raise ValueError(f"No GSR data found for session {session_id}")
        
        # Combine all GSR data files
        combined_data = []
        for gsr_file in gsr_files:
            try:
                if ANALYSIS_AVAILABLE:
                    df = pd.read_csv(gsr_file)
                    combined_data.append(df)
                else:
                    # Fallback without pandas
                    with open(gsr_file, 'r') as f:
                        reader = csv.DictReader(f)
                        for row in reader:
                            combined_data.append(row)
            except Exception as e:
                logger.warning(f"Error reading GSR file {gsr_file}: {e}")
        
        # Write combined data
        if ANALYSIS_AVAILABLE and combined_data and isinstance(combined_data[0], pd.DataFrame):
            combined_df = pd.concat(combined_data, ignore_index=True)
            combined_df.to_csv(output_file, index=False)
        else:
            # Fallback CSV writing
            if combined_data:
                with open(output_file, 'w', newline='') as f:
                    if combined_data:
                        fieldnames = combined_data[0].keys()
                        writer = csv.DictWriter(f, fieldnames=fieldnames)
                        writer.writeheader()
                        for row_data in combined_data:
                            writer.writerows(row_data if isinstance(row_data, list) else [row_data])
        
        logger.info(f"CSV export completed: {output_file}")
        return output_file
    
    async def _export_json(self, session_id: str, session_path: Path, 
                          output_dir: Path, timestamp: str) -> Path:
        """Export session data as structured JSON"""
        output_file = output_dir / f"{session_id}_complete_{timestamp}.json"
        
        session_data = {
            "session_id": session_id,
            "export_timestamp": timestamp,
            "export_format": "json",
            "gsr_data": [],
            "sync_marks": [],
            "metadata": {},
            "files": []
        }
        
        # Load GSR data
        gsr_files = list(session_path.glob("gsr_data_*.csv"))
        for gsr_file in gsr_files:
            try:
                if ANALYSIS_AVAILABLE:
                    df = pd.read_csv(gsr_file)
                    session_data["gsr_data"].extend(df.to_dict('records'))
                else:
                    with open(gsr_file, 'r') as f:
                        reader = csv.DictReader(f)
                        session_data["gsr_data"].extend(list(reader))
                session_data["files"].append(str(gsr_file.name))
            except Exception as e:
                logger.warning(f"Error loading GSR data from {gsr_file}: {e}")
        
        # Load sync marks
        sync_file = session_path / "sync_marks.csv"
        if sync_file.exists():
            try:
                with open(sync_file, 'r') as f:
                    reader = csv.DictReader(f)
                    session_data["sync_marks"] = list(reader)
            except Exception as e:
                logger.warning(f"Error loading sync marks: {e}")
        
        # Load metadata
        metadata_file = session_path / "session_metadata.json"
        if metadata_file.exists():
            try:
                with open(metadata_file, 'r') as f:
                    session_data["metadata"] = json.load(f)
            except Exception as e:
                logger.warning(f"Error loading metadata: {e}")
        
        # Load analysis results if available
        analysis_file = session_path / "analysis_report.json"
        if analysis_file.exists():
            try:
                with open(analysis_file, 'r') as f:
                    session_data["analysis_results"] = json.load(f)
            except Exception as e:
                logger.warning(f"Error loading analysis results: {e}")
        
        # Save JSON
        with open(output_file, 'w') as f:
            json.dump(session_data, f, indent=2, default=str)
        
        logger.info(f"JSON export completed: {output_file}")
        return output_file
    
    async def _export_research_package(self, session_id: str, session_path: Path, 
                                     output_dir: Path, timestamp: str) -> Path:
        """Export session as a research package with all analysis"""
        output_file = output_dir / f"{session_id}_research_package_{timestamp}.zip"
        
        with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
            
            # Add all session files
            for file_path in session_path.rglob("*"):
                if file_path.is_file():
                    arcname = f"{session_id}/{file_path.relative_to(session_path)}"
                    zipf.write(file_path, arcname)
            
            # Generate research summary
            research_summary = await self._generate_research_summary(session_id, session_path)
            summary_json = json.dumps(research_summary, indent=2, default=str)
            zipf.writestr(f"{session_id}/RESEARCH_SUMMARY.json", summary_json)
            
            # Generate README
            readme_content = self._generate_research_readme(session_id, research_summary)
            zipf.writestr(f"{session_id}/README.md", readme_content)
            
            # Add visualization plots if available
            await self._add_research_visualizations(zipf, session_id, session_path)
        
        logger.info(f"Research package export completed: {output_file}")
        return output_file
    
    async def _export_complete_package(self, session_id: str, session_path: Path, 
                                     output_dir: Path, timestamp: str) -> Path:
        """Export everything - complete comprehensive package"""
        output_file = output_dir / f"{session_id}_COMPLETE_{timestamp}.zip"
        
        with zipfile.ZipFile(output_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
            
            # Original data files
            for file_path in session_path.rglob("*"):
                if file_path.is_file():
                    arcname = f"original_data/{file_path.relative_to(session_path)}"
                    zipf.write(file_path, arcname)
            
            # Export CSV format
            csv_file = await self._export_csv(session_id, session_path, Path("/tmp"), timestamp)
            zipf.write(csv_file, f"exports/{csv_file.name}")
            csv_file.unlink()  # Cleanup
            
            # Export JSON format  
            json_file = await self._export_json(session_id, session_path, Path("/tmp"), timestamp)
            zipf.write(json_file, f"exports/{json_file.name}")
            json_file.unlink()  # Cleanup
            
            # Research analysis
            research_summary = await self._generate_research_summary(session_id, session_path)
            summary_json = json.dumps(research_summary, indent=2, default=str)
            zipf.writestr("analysis/research_summary.json", summary_json)
            
            # Documentation
            readme_content = self._generate_complete_readme(session_id, research_summary)
            zipf.writestr("README.md", readme_content)
            
            # Visualizations
            await self._add_research_visualizations(zipf, session_id, session_path, prefix="visualizations/")
            
            # Data dictionary
            data_dict = self._generate_data_dictionary()
            zipf.writestr("documentation/DATA_DICTIONARY.md", data_dict)
        
        logger.info(f"Complete package export completed: {output_file}")
        return output_file
    
    async def _generate_research_summary(self, session_id: str, session_path: Path) -> Dict[str, Any]:
        """Generate comprehensive research summary"""
        summary = {
            "session_id": session_id,
            "analysis_timestamp": datetime.now().isoformat(),
            "data_files_found": [],
            "statistics": {},
            "quality_metrics": {},
            "temporal_analysis": {},
            "recommendations": []
        }
        
        # Scan for data files
        gsr_files = list(session_path.glob("gsr_data_*.csv"))
        summary["data_files_found"] = [str(f.name) for f in gsr_files]
        
        if not gsr_files or not ANALYSIS_AVAILABLE:
            summary["recommendations"].append("Limited analysis available - install pandas and scipy for full research features")
            return summary
        
        try:
            # Load and analyze GSR data
            all_data = []
            for gsr_file in gsr_files:
                df = pd.read_csv(gsr_file)
                all_data.append(df)
            
            if all_data:
                combined_df = pd.concat(all_data, ignore_index=True)
                
                # Basic statistics
                if 'GSR_Value' in combined_df.columns:
                    gsr_col = 'GSR_Value'
                elif 'Raw_GSR_µS' in combined_df.columns:
                    gsr_col = 'Raw_GSR_µS'
                else:
                    gsr_col = combined_df.select_dtypes(include=[np.number]).columns[0]
                
                gsr_data = combined_df[gsr_col].dropna()
                
                summary["statistics"] = {
                    "total_samples": len(gsr_data),
                    "mean_gsr": float(gsr_data.mean()),
                    "std_gsr": float(gsr_data.std()),
                    "min_gsr": float(gsr_data.min()),
                    "max_gsr": float(gsr_data.max()),
                    "median_gsr": float(gsr_data.median()),
                    "q25_gsr": float(gsr_data.quantile(0.25)),
                    "q75_gsr": float(gsr_data.quantile(0.75)),
                    "range_gsr": float(gsr_data.max() - gsr_data.min()),
                    "coefficient_of_variation": float(gsr_data.std() / gsr_data.mean()) if gsr_data.mean() != 0 else 0
                }
                
                # Quality metrics
                missing_ratio = combined_df[gsr_col].isna().sum() / len(combined_df)
                summary["quality_metrics"] = {
                    "completeness": 1.0 - missing_ratio,
                    "missing_samples": int(combined_df[gsr_col].isna().sum()),
                    "valid_samples": int(len(gsr_data)),
                    "data_quality_score": min(1.0, max(0.0, 1.0 - missing_ratio - (gsr_data.std() / gsr_data.mean() > 2.0) * 0.3))
                }
                
                # Temporal analysis
                if 'Timestamp' in combined_df.columns:
                    timestamps = pd.to_datetime(combined_df['Timestamp'], errors='coerce')
                    duration = (timestamps.max() - timestamps.min()).total_seconds()
                    sampling_rate = len(gsr_data) / duration if duration > 0 else 0
                    
                    summary["temporal_analysis"] = {
                        "duration_seconds": duration,
                        "start_time": timestamps.min().isoformat() if not pd.isna(timestamps.min()) else None,
                        "end_time": timestamps.max().isoformat() if not pd.isna(timestamps.max()) else None,
                        "estimated_sampling_rate": sampling_rate,
                        "gaps_detected": int(timestamps.isna().sum())
                    }
                
                # Generate recommendations
                if missing_ratio > 0.05:
                    summary["recommendations"].append(f"Data completeness is {(1-missing_ratio)*100:.1f}% - consider investigating data collection issues")
                if gsr_data.std() / gsr_data.mean() > 2.0:
                    summary["recommendations"].append("High variability detected - check for artifacts or consider filtering")
                if sampling_rate < 50 and sampling_rate > 0:
                    summary["recommendations"].append(f"Sampling rate ({sampling_rate:.1f} Hz) is lower than recommended 128Hz for research")
                if len(gsr_data) < 1000:
                    summary["recommendations"].append("Short recording duration - consider longer sessions for better analysis")
                
        except Exception as e:
            logger.error(f"Error generating research summary: {e}")
            summary["error"] = str(e)
            summary["recommendations"].append("Error during analysis - check data format and file integrity")
        
        return summary
    
    def _generate_research_readme(self, session_id: str, research_summary: Dict) -> str:
        """Generate README for research package"""
        return f"""# GSR Research Data Package: {session_id}

## Overview
This package contains comprehensive GSR (Galvanic Skin Response) data and analysis for session `{session_id}`.

**Generated:** {research_summary.get('analysis_timestamp', 'Unknown')}

## Contents
- `gsr_data_*.csv` - Raw GSR measurement data
- `sync_marks.csv` - Event synchronization markers (if available)
- `session_metadata.json` - Session configuration and metadata
- `analysis_report.json` - Automated data quality analysis
- `RESEARCH_SUMMARY.json` - Comprehensive statistical summary

## Data Summary
- **Total Samples:** {research_summary.get('statistics', {}).get('total_samples', 'N/A')}
- **Duration:** {research_summary.get('temporal_analysis', {}).get('duration_seconds', 'N/A')} seconds
- **Data Quality:** {research_summary.get('quality_metrics', {}).get('data_quality_score', 'N/A'):.1%}
- **Mean GSR:** {research_summary.get('statistics', {}).get('mean_gsr', 'N/A')} µS

## Research Recommendations
{chr(10).join(f"- {rec}" for rec in research_summary.get('recommendations', ['No specific recommendations']))}

## Data Format
GSR data follows standardized format with columns:
- `Timestamp` - Unix timestamp in seconds
- `DateTime` - Human-readable timestamp  
- `Raw_GSR_µS` - Raw GSR measurement in microsiemens
- `Filtered_GSR_µS` - Filtered GSR data (if available)
- `Signal_Quality` - Quality indicator (0.0-1.0)

## Usage
This data is suitable for:
- Physiological response research
- Emotion recognition studies  
- Stress and arousal analysis
- Psychophysiology experiments

## Citation
Please cite the Bucika GSR platform in publications using this data.
"""
    
    def _generate_complete_readme(self, session_id: str, research_summary: Dict) -> str:
        """Generate comprehensive README for complete package"""
        return f"""# Complete GSR Data Package: {session_id}

## Package Structure
```
{session_id}_COMPLETE/
├── original_data/          # Original session files
│   ├── gsr_data_*.csv     # Raw GSR measurements
│   ├── sync_marks.csv     # Event synchronization
│   ├── session_metadata.json
│   └── uploads/           # Device uploads (videos, etc.)
├── exports/               # Formatted exports
│   ├── *_data_*.csv      # Combined CSV data
│   └── *_complete_*.json # Complete JSON export
├── analysis/              # Research analysis
│   └── research_summary.json
├── visualizations/        # Generated plots and charts
├── documentation/         # Data dictionaries and guides
└── README.md             # This file
```

## Data Overview
{self._generate_research_readme(session_id, research_summary)}

## Export Formats
This package includes data in multiple formats:
1. **CSV** - For statistical analysis software (R, SPSS, etc.)
2. **JSON** - For programming applications
3. **Research Package** - Complete with metadata and analysis
4. **Visualizations** - Charts and plots for publication

## Quality Assurance
- Data completeness: {research_summary.get('quality_metrics', {}).get('completeness', 'N/A'):.1%}
- Quality score: {research_summary.get('quality_metrics', {}).get('data_quality_score', 'N/A'):.1%}
- Missing samples: {research_summary.get('quality_metrics', {}).get('missing_samples', 'N/A')}

## Technical Specifications
- Platform: Bucika GSR PC Orchestrator (Python)
- Export timestamp: {research_summary.get('analysis_timestamp', 'Unknown')}
- Analysis features: Statistical summary, quality assessment, visualization generation
- File formats: CSV, JSON, ZIP archive
"""
    
    def _generate_data_dictionary(self) -> str:
        """Generate data dictionary documentation"""
        return """# GSR Data Dictionary

## Primary Data Files

### gsr_data_*.csv
Main GSR measurement data with temporal information.

| Column | Type | Unit | Description |
|--------|------|------|-------------|
| Timestamp | float | seconds | Unix timestamp (seconds since epoch) |
| DateTime | string | ISO 8601 | Human-readable timestamp |
| Raw_GSR_µS | float | microsiemens | Raw GSR measurement |
| Filtered_GSR_µS | float | microsiemens | Filtered GSR (optional) |
| Raw_Temp_°C | float | celsius | Temperature measurement |
| Signal_Quality | float | 0.0-1.0 | Quality indicator |
| Sequence | integer | count | Sample sequence number |

### sync_marks.csv
Event synchronization markers for experimental coordination.

| Column | Type | Description |
|--------|------|-------------|
| timestamp | float | Event timestamp (Unix seconds) |
| datetime | string | Human-readable event time |
| mark_id | string | Unique marker identifier |
| description | string | Event description |
| session_id | string | Associated session |

### session_metadata.json
Session configuration and device information.

```json
{
  "session_id": "unique_session_identifier",
  "device_id": "android_device_identifier", 
  "session_name": "human_readable_name",
  "started_at": "ISO 8601 timestamp",
  "ended_at": "ISO 8601 timestamp",
  "capabilities": ["GSR", "THERMAL", "VIDEO"],
  "device_info": {
    "manufacturer": "device_manufacturer",
    "model": "device_model", 
    "android_version": "version",
    "app_version": "app_version"
  }
}
```

## Analysis Files

### analysis_report.json
Automated data quality and statistical analysis.

### quality_report.json  
Comprehensive data validation results.

### research_summary.json
Statistical summary and research recommendations.

## Data Quality Indicators

### Signal_Quality Values
- 1.0: Perfect signal quality
- 0.8-0.9: High quality, suitable for analysis
- 0.6-0.7: Moderate quality, some artifacts possible
- 0.4-0.5: Low quality, filtering recommended
- 0.0-0.3: Poor quality, potential data issues

### Recommended Filters
- Sampling rate: 128 Hz (nominal)
- Low-pass filter: 10 Hz cutoff recommended
- Artifact removal: Z-score > 3.0 standard deviations
- Minimum session duration: 30 seconds for meaningful analysis

## Research Applications
This data format supports:
- Physiological computing research
- Affective computing studies
- Stress and emotion recognition
- Human-computer interaction research
- Psychophysiological experimentation
"""
    
    async def _add_research_visualizations(self, zipf: zipfile.ZipFile, 
                                         session_id: str, session_path: Path,
                                         prefix: str = "") -> None:
        """Add research-quality visualizations to export package"""
        
        if not ANALYSIS_AVAILABLE:
            return
        
        try:
            # Load GSR data
            gsr_files = list(session_path.glob("gsr_data_*.csv"))
            if not gsr_files:
                return
            
            all_data = []
            for gsr_file in gsr_files:
                df = pd.read_csv(gsr_file)
                all_data.append(df)
            
            combined_df = pd.concat(all_data, ignore_index=True)
            
            # Determine GSR column
            if 'GSR_Value' in combined_df.columns:
                gsr_col = 'GSR_Value'
            elif 'Raw_GSR_µS' in combined_df.columns:
                gsr_col = 'Raw_GSR_µS'
            else:
                gsr_col = combined_df.select_dtypes(include=[np.number]).columns[0]
            
            gsr_data = combined_df[gsr_col].dropna()
            
            # Set up matplotlib for publication quality
            plt.style.use('default')
            plt.rcParams.update({
                'figure.dpi': 300,
                'savefig.dpi': 300,
                'font.size': 10,
                'axes.titlesize': 12,
                'axes.labelsize': 11,
                'xtick.labelsize': 9,
                'ytick.labelsize': 9,
                'legend.fontsize': 9
            })
            
            # Generate time series plot
            fig, ax = plt.subplots(figsize=(12, 6))
            
            if 'Timestamp' in combined_df.columns:
                timestamps = pd.to_datetime(combined_df['Timestamp'], unit='s', errors='coerce')
                ax.plot(timestamps, gsr_data, linewidth=0.8, alpha=0.8, color='blue')
                ax.set_xlabel('Time')
                ax.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M:%S'))
                ax.tick_params(axis='x', rotation=45)
            else:
                ax.plot(gsr_data, linewidth=0.8, alpha=0.8, color='blue')
                ax.set_xlabel('Sample')
            
            ax.set_ylabel(f'GSR ({gsr_col})')
            ax.set_title(f'GSR Time Series - Session {session_id}')
            ax.grid(True, alpha=0.3)
            
            # Add statistics annotation
            stats_text = f'μ={gsr_data.mean():.3f}, σ={gsr_data.std():.3f}, n={len(gsr_data)}'
            ax.text(0.02, 0.98, stats_text, transform=ax.transAxes, 
                   verticalalignment='top', bbox=dict(boxstyle='round', facecolor='white', alpha=0.8))
            
            plt.tight_layout()
            
            # Save to zip
            from io import BytesIO
            img_buffer = BytesIO()
            plt.savefig(img_buffer, format='png', bbox_inches='tight', dpi=300)
            img_buffer.seek(0)
            zipf.writestr(f"{prefix}gsr_timeseries.png", img_buffer.read())
            plt.close()
            
            # Generate histogram
            fig, ax = plt.subplots(figsize=(8, 6))
            ax.hist(gsr_data, bins=50, alpha=0.7, color='skyblue', edgecolor='black')
            ax.set_xlabel(f'GSR Value ({gsr_col})')
            ax.set_ylabel('Frequency')
            ax.set_title(f'GSR Distribution - Session {session_id}')
            ax.grid(True, alpha=0.3)
            
            # Add distribution statistics
            ax.axvline(gsr_data.mean(), color='red', linestyle='--', label=f'Mean: {gsr_data.mean():.3f}')
            ax.axvline(gsr_data.median(), color='green', linestyle='--', label=f'Median: {gsr_data.median():.3f}')
            ax.legend()
            
            plt.tight_layout()
            
            img_buffer = BytesIO()
            plt.savefig(img_buffer, format='png', bbox_inches='tight', dpi=300)
            img_buffer.seek(0)
            zipf.writestr(f"{prefix}gsr_distribution.png", img_buffer.read())
            plt.close()
            
            # Generate summary statistics plot
            fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(12, 8))
            
            # Time series (smaller)
            if 'Timestamp' in combined_df.columns:
                timestamps = pd.to_datetime(combined_df['Timestamp'], unit='s', errors='coerce')
                ax1.plot(timestamps, gsr_data, linewidth=0.5, alpha=0.8)
                ax1.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M'))
            else:
                ax1.plot(gsr_data, linewidth=0.5, alpha=0.8)
            ax1.set_title('GSR Time Series')
            ax1.grid(True, alpha=0.3)
            
            # Histogram
            ax2.hist(gsr_data, bins=30, alpha=0.7, color='lightblue')
            ax2.set_title('GSR Distribution') 
            ax2.set_xlabel(f'GSR ({gsr_col})')
            
            # Box plot
            ax3.boxplot(gsr_data, vert=True)
            ax3.set_title('GSR Box Plot')
            ax3.set_ylabel(f'GSR ({gsr_col})')
            
            # Statistics summary
            stats_data = {
                'Mean': gsr_data.mean(),
                'Std': gsr_data.std(), 
                'Min': gsr_data.min(),
                'Max': gsr_data.max(),
                'Median': gsr_data.median()
            }
            
            ax4.bar(stats_data.keys(), stats_data.values(), color='lightgreen')
            ax4.set_title('Summary Statistics')
            ax4.set_ylabel('Value')
            ax4.tick_params(axis='x', rotation=45)
            
            plt.suptitle(f'GSR Analysis Summary - Session {session_id}', fontsize=14, fontweight='bold')
            plt.tight_layout()
            
            img_buffer = BytesIO()
            plt.savefig(img_buffer, format='png', bbox_inches='tight', dpi=300)
            img_buffer.seek(0)
            zipf.writestr(f"{prefix}gsr_analysis_summary.png", img_buffer.read())
            plt.close()
            
        except Exception as e:
            logger.error(f"Error generating visualizations for {session_id}: {e}")


class MultiSessionAnalyzer:
    """Advanced multi-session research analysis"""
    
    def __init__(self, data_directory: Path):
        self.data_directory = Path(data_directory)
    
    async def analyze_multiple_sessions(self, session_ids: List[str]) -> ResearchReport:
        """Perform cross-session analysis for research"""
        
        report_id = f"multi_session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        sessions = []
        
        # Analyze each session
        for session_id in session_ids:
            session_data = await self._analyze_single_session(session_id)
            if session_data:
                sessions.append(session_data)
        
        if not sessions:
            raise ValueError("No valid sessions found for analysis")
        
        # Cross-session analysis
        aggregate_stats = self._compute_aggregate_statistics(sessions)
        cross_session_analysis = await self._perform_cross_session_analysis(sessions)
        recommendations = self._generate_multi_session_recommendations(sessions, cross_session_analysis)
        quality_summary = self._compute_quality_summary(sessions)
        
        # Create comprehensive report
        report = ResearchReport(
            report_id=report_id,
            generated_at=datetime.now(),
            sessions=sessions,
            total_participants=len(set(s.participant_id for s in sessions if s.participant_id)),
            total_duration_hours=sum(s.duration_minutes for s in sessions) / 60.0,
            aggregate_statistics=aggregate_stats,
            cross_session_analysis=cross_session_analysis,
            recommendations=recommendations,
            data_quality_summary=quality_summary,
            export_formats=["json", "csv", "research_package", "complete"]
        )
        
        return report
    
    async def _analyze_single_session(self, session_id: str) -> Optional[ResearchSession]:
        """Analyze a single session for multi-session report"""
        
        session_path = self.data_directory / session_id
        if not session_path.exists():
            logger.warning(f"Session path not found: {session_path}")
            return None
        
        try:
            # Load session metadata
            metadata_file = session_path / "session_metadata.json"
            metadata = {}
            if metadata_file.exists():
                with open(metadata_file, 'r') as f:
                    metadata = json.load(f)
            
            # Load GSR data for basic analysis
            gsr_files = list(session_path.glob("gsr_data_*.csv"))
            gsr_samples = 0
            duration_minutes = 0.0
            quality_score = 0.0
            
            if gsr_files and ANALYSIS_AVAILABLE:
                all_data = []
                for gsr_file in gsr_files:
                    df = pd.read_csv(gsr_file)
                    all_data.append(df)
                
                if all_data:
                    combined_df = pd.concat(all_data, ignore_index=True)
                    gsr_samples = len(combined_df)
                    
                    if 'Timestamp' in combined_df.columns:
                        timestamps = pd.to_datetime(combined_df['Timestamp'], unit='s', errors='coerce')
                        duration_minutes = (timestamps.max() - timestamps.min()).total_seconds() / 60.0
                    
                    # Simple quality score based on completeness
                    if 'Raw_GSR_µS' in combined_df.columns:
                        quality_score = 1.0 - (combined_df['Raw_GSR_µS'].isna().sum() / len(combined_df))
            
            # Load sync marks
            sync_marks = []
            sync_file = session_path / "sync_marks.csv"
            if sync_file.exists():
                with open(sync_file, 'r') as f:
                    reader = csv.DictReader(f)
                    sync_marks = list(reader)
            
            # Create session object
            session = ResearchSession(
                session_id=session_id,
                device_id=metadata.get('device_id', 'unknown'),
                participant_id=metadata.get('participant_id'),
                session_name=metadata.get('session_name', session_id),
                start_time=datetime.fromisoformat(metadata['started_at']) if 'started_at' in metadata else None,
                end_time=datetime.fromisoformat(metadata['ended_at']) if 'ended_at' in metadata else None,
                duration_minutes=duration_minutes,
                gsr_samples=gsr_samples,
                sync_marks=sync_marks,
                metadata=metadata,
                quality_score=quality_score,
                analysis_completed=True
            )
            
            return session
            
        except Exception as e:
            logger.error(f"Error analyzing session {session_id}: {e}")
            return None
    
    def _compute_aggregate_statistics(self, sessions: List[ResearchSession]) -> Dict[str, Any]:
        """Compute aggregate statistics across sessions"""
        
        return {
            "total_sessions": len(sessions),
            "total_samples": sum(s.gsr_samples for s in sessions),
            "total_duration_minutes": sum(s.duration_minutes for s in sessions),
            "average_duration_minutes": sum(s.duration_minutes for s in sessions) / len(sessions),
            "average_samples_per_session": sum(s.gsr_samples for s in sessions) / len(sessions),
            "quality_scores": {
                "mean": sum(s.quality_score for s in sessions) / len(sessions),
                "min": min(s.quality_score for s in sessions),
                "max": max(s.quality_score for s in sessions),
                "std": np.std([s.quality_score for s in sessions]) if ANALYSIS_AVAILABLE else 0.0
            },
            "sync_marks_total": sum(len(s.sync_marks) for s in sessions),
            "sessions_with_sync_marks": sum(1 for s in sessions if s.sync_marks)
        }
    
    async def _perform_cross_session_analysis(self, sessions: List[ResearchSession]) -> Dict[str, Any]:
        """Perform advanced cross-session analysis"""
        
        analysis = {
            "temporal_patterns": {},
            "quality_correlation": {},
            "duration_analysis": {},
            "device_comparison": {}
        }
        
        # Temporal patterns
        session_times = [s.start_time for s in sessions if s.start_time]
        if session_times:
            hours = [t.hour for t in session_times]
            days_of_week = [t.weekday() for t in session_times]
            
            analysis["temporal_patterns"] = {
                "preferred_hours": max(set(hours), key=hours.count) if hours else None,
                "hours_distribution": {h: hours.count(h) for h in set(hours)} if hours else {},
                "weekday_distribution": {d: days_of_week.count(d) for d in set(days_of_week)} if days_of_week else {}
            }
        
        # Quality correlation analysis
        if len(sessions) > 1 and ANALYSIS_AVAILABLE:
            durations = [s.duration_minutes for s in sessions]
            qualities = [s.quality_score for s in sessions]
            samples = [s.gsr_samples for s in sessions]
            
            if len(set(durations)) > 1 and len(set(qualities)) > 1:
                correlation_duration_quality = pearsonr(durations, qualities)[0] if SCIPY_AVAILABLE else 0.0
                correlation_samples_quality = pearsonr(samples, qualities)[0] if SCIPY_AVAILABLE else 0.0
                
                analysis["quality_correlation"] = {
                    "duration_vs_quality": correlation_duration_quality,
                    "samples_vs_quality": correlation_samples_quality,
                    "interpretation": self._interpret_correlation(correlation_duration_quality)
                }
        
        # Duration analysis
        durations = [s.duration_minutes for s in sessions]
        analysis["duration_analysis"] = {
            "mean_duration": np.mean(durations) if durations else 0,
            "std_duration": np.std(durations) if durations else 0,
            "min_duration": min(durations) if durations else 0,
            "max_duration": max(durations) if durations else 0,
            "sessions_under_5min": sum(1 for d in durations if d < 5),
            "sessions_over_30min": sum(1 for d in durations if d > 30)
        }
        
        # Device comparison
        device_stats = {}
        for session in sessions:
            device_id = session.device_id
            if device_id not in device_stats:
                device_stats[device_id] = {
                    "session_count": 0,
                    "total_samples": 0,
                    "total_duration": 0.0,
                    "quality_scores": []
                }
            
            device_stats[device_id]["session_count"] += 1
            device_stats[device_id]["total_samples"] += session.gsr_samples
            device_stats[device_id]["total_duration"] += session.duration_minutes
            device_stats[device_id]["quality_scores"].append(session.quality_score)
        
        # Compute averages for each device
        for device_id, stats in device_stats.items():
            stats["average_quality"] = sum(stats["quality_scores"]) / len(stats["quality_scores"])
            stats["average_duration"] = stats["total_duration"] / stats["session_count"]
            stats["average_samples"] = stats["total_samples"] / stats["session_count"]
        
        analysis["device_comparison"] = device_stats
        
        return analysis
    
    def _interpret_correlation(self, correlation: float) -> str:
        """Interpret correlation coefficient"""
        abs_corr = abs(correlation)
        if abs_corr < 0.1:
            return "No correlation"
        elif abs_corr < 0.3:
            return "Weak correlation"
        elif abs_corr < 0.5:
            return "Moderate correlation"
        elif abs_corr < 0.7:
            return "Strong correlation"
        else:
            return "Very strong correlation"
    
    def _generate_multi_session_recommendations(self, sessions: List[ResearchSession], 
                                              cross_analysis: Dict[str, Any]) -> List[str]:
        """Generate recommendations for multi-session research"""
        
        recommendations = []
        
        # Data quality recommendations
        avg_quality = sum(s.quality_score for s in sessions) / len(sessions)
        if avg_quality < 0.8:
            recommendations.append(f"Average data quality ({avg_quality:.1%}) could be improved - check sensor connections and environmental conditions")
        
        # Duration recommendations
        avg_duration = sum(s.duration_minutes for s in sessions) / len(sessions)
        if avg_duration < 5:
            recommendations.append(f"Average session duration ({avg_duration:.1f} min) is quite short - consider longer recordings for better analysis")
        elif avg_duration > 60:
            recommendations.append(f"Average session duration ({avg_duration:.1f} min) is long - monitor for fatigue effects")
        
        # Consistency recommendations
        durations = [s.duration_minutes for s in sessions]
        if ANALYSIS_AVAILABLE and len(durations) > 1:
            cv = np.std(durations) / np.mean(durations)
            if cv > 0.5:
                recommendations.append("High variability in session durations - consider standardizing recording protocols")
        
        # Device consistency
        unique_devices = len(set(s.device_id for s in sessions))
        if unique_devices > 1:
            device_qualities = {}
            for session in sessions:
                device_id = session.device_id
                if device_id not in device_qualities:
                    device_qualities[device_id] = []
                device_qualities[device_id].append(session.quality_score)
            
            device_avg_qualities = {d: sum(q)/len(q) for d, q in device_qualities.items()}
            quality_range = max(device_avg_qualities.values()) - min(device_avg_qualities.values())
            
            if quality_range > 0.2:
                recommendations.append(f"Quality varies significantly between devices (range: {quality_range:.1%}) - consider device calibration")
        
        # Temporal recommendations
        if "temporal_patterns" in cross_analysis:
            hours_dist = cross_analysis["temporal_patterns"].get("hours_distribution", {})
            if len(hours_dist) > 1:
                most_common_hour = max(hours_dist, key=hours_dist.get)
                recommendations.append(f"Most sessions occur at hour {most_common_hour} - consider time-of-day effects in analysis")
        
        # Sample size recommendations
        total_samples = sum(s.gsr_samples for s in sessions)
        if total_samples < 10000:
            recommendations.append(f"Total sample size ({total_samples:,}) may be limited for robust statistical analysis")
        
        return recommendations
    
    def _compute_quality_summary(self, sessions: List[ResearchSession]) -> Dict[str, Any]:
        """Compute data quality summary across sessions"""
        
        qualities = [s.quality_score for s in sessions]
        
        return {
            "overall_quality_score": sum(qualities) / len(qualities),
            "quality_distribution": {
                "excellent": sum(1 for q in qualities if q >= 0.9),
                "good": sum(1 for q in qualities if 0.7 <= q < 0.9),
                "fair": sum(1 for q in qualities if 0.5 <= q < 0.7),
                "poor": sum(1 for q in qualities if q < 0.5)
            },
            "sessions_with_sync_marks": sum(1 for s in sessions if s.sync_marks),
            "total_sync_marks": sum(len(s.sync_marks) for s in sessions),
            "sessions_analyzed": len(sessions),
            "sessions_complete": sum(1 for s in sessions if s.analysis_completed)
        }


# Export the main classes
__all__ = [
    "SessionExporter", 
    "MultiSessionAnalyzer", 
    "ResearchSession", 
    "ResearchReport"
]