#!/usr/bin/env python3
"""
Data Analysis Module for Bucika GSR PC Orchestrator
Provides research-grade analysis capabilities for GSR data.
"""

import pandas as pd
import numpy as np
from pathlib import Path
import json
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass
import statistics
from loguru import logger


@dataclass
class AnalysisResults:
    """Container for analysis results"""
    session_id: str
    duration_minutes: float
    sample_count: int
    mean_gsr: float
    std_gsr: float
    min_gsr: float
    max_gsr: float
    data_quality_score: float
    sync_mark_count: int
    artifacts_detected: int
    analysis_timestamp: str
    recommendations: List[str]


class GSRDataAnalyzer:
    """Advanced GSR data analysis and quality assessment"""
    
    def __init__(self, data_directory: Path = None):
        """Initialize the analyzer with data directory"""
        self.data_directory = data_directory or Path("sessions")
        self.analysis_cache = {}
        
        # Analysis parameters
        self.artifact_threshold = 3.0  # Standard deviations for artifact detection
        self.quality_thresholds = {
            'missing_data_max': 0.05,  # Max 5% missing data for good quality
            'artifact_rate_max': 0.02,  # Max 2% artifacts for good quality
            'signal_consistency_min': 0.7  # Min consistency score
        }
        
    def analyze_session(self, session_id: str) -> Optional[AnalysisResults]:
        """Perform comprehensive analysis of a session"""
        try:
            session_path = self.data_directory / session_id
            if not session_path.exists():
                logger.warning(f"Session directory not found: {session_id}")
                return None
                
            # Load GSR data
            gsr_data = self._load_gsr_data(session_path)
            if gsr_data is None or len(gsr_data) == 0:
                logger.warning(f"No GSR data found for session: {session_id}")
                return None
                
            # Load sync marks
            sync_marks = self._load_sync_marks(session_path)
            
            # Perform analysis
            analysis = self._perform_analysis(session_id, gsr_data, sync_marks)
            
            # Cache results
            self.analysis_cache[session_id] = analysis
            
            # Save analysis report
            self._save_analysis_report(session_path, analysis)
            
            logger.info(f"Analysis completed for session {session_id}")
            return analysis
            
        except Exception as e:
            logger.error(f"Error analyzing session {session_id}: {e}")
            return None
    
    def _load_gsr_data(self, session_path: Path) -> Optional[pd.DataFrame]:
        """Load GSR data from CSV files"""
        gsr_files = list(session_path.glob("gsr_data_*.csv"))
        if not gsr_files:
            return None
            
        # Load the most recent file
        gsr_file = max(gsr_files, key=lambda x: x.stat().st_mtime)
        
        try:
            df = pd.read_csv(gsr_file)
            
            # Convert timestamp to datetime if it's not already
            if 'timestamp' in df.columns:
                df['datetime'] = pd.to_datetime(df['timestamp'], unit='s')
            elif 'datetime' in df.columns:
                df['datetime'] = pd.to_datetime(df['datetime'])
                
            return df
            
        except Exception as e:
            logger.error(f"Error loading GSR data: {e}")
            return None
    
    def _load_sync_marks(self, session_path: Path) -> List[Dict]:
        """Load sync marks from CSV file"""
        sync_file = session_path / "sync_marks.csv"
        if not sync_file.exists():
            return []
            
        try:
            df = pd.read_csv(sync_file)
            return df.to_dict('records')
        except Exception as e:
            logger.error(f"Error loading sync marks: {e}")
            return []
    
    def _perform_analysis(self, session_id: str, gsr_data: pd.DataFrame, 
                         sync_marks: List[Dict]) -> AnalysisResults:
        """Perform comprehensive data analysis"""
        
        # Basic statistics
        gsr_values = gsr_data['gsr_microsiemens'].dropna()
        
        if len(gsr_values) == 0:
            raise ValueError("No valid GSR data found")
        
        # Calculate duration
        if 'datetime' in gsr_data.columns:
            duration = (gsr_data['datetime'].max() - gsr_data['datetime'].min()).total_seconds() / 60
        else:
            duration = len(gsr_data) / 128.0 / 60  # Assuming 128 Hz
        
        # Basic statistics
        mean_gsr = float(gsr_values.mean())
        std_gsr = float(gsr_values.std())
        min_gsr = float(gsr_values.min())
        max_gsr = float(gsr_values.max())
        
        # Artifact detection
        artifacts_detected = self._detect_artifacts(gsr_values)
        
        # Data quality assessment
        quality_score = self._calculate_quality_score(gsr_data, artifacts_detected)
        
        # Generate recommendations
        recommendations = self._generate_recommendations(
            gsr_data, artifacts_detected, quality_score
        )
        
        return AnalysisResults(
            session_id=session_id,
            duration_minutes=duration,
            sample_count=len(gsr_values),
            mean_gsr=mean_gsr,
            std_gsr=std_gsr,
            min_gsr=min_gsr,
            max_gsr=max_gsr,
            data_quality_score=quality_score,
            sync_mark_count=len(sync_marks),
            artifacts_detected=artifacts_detected,
            analysis_timestamp=datetime.now().isoformat(),
            recommendations=recommendations
        )
    
    def _detect_artifacts(self, gsr_values: pd.Series) -> int:
        """Detect artifacts using statistical methods"""
        try:
            # Calculate z-scores
            z_scores = np.abs((gsr_values - gsr_values.mean()) / gsr_values.std())
            
            # Count values beyond threshold
            artifacts = (z_scores > self.artifact_threshold).sum()
            
            return int(artifacts)
            
        except Exception:
            return 0
    
    def _calculate_quality_score(self, gsr_data: pd.DataFrame, artifacts: int) -> float:
        """Calculate overall data quality score (0-1)"""
        try:
            total_samples = len(gsr_data)
            if total_samples == 0:
                return 0.0
            
            # Missing data penalty
            missing_data = gsr_data['gsr_microsiemens'].isna().sum()
            missing_rate = missing_data / total_samples
            missing_score = max(0, 1 - (missing_rate / self.quality_thresholds['missing_data_max']))
            
            # Artifact penalty
            artifact_rate = artifacts / total_samples
            artifact_score = max(0, 1 - (artifact_rate / self.quality_thresholds['artifact_rate_max']))
            
            # Signal consistency (based on coefficient of variation)
            gsr_clean = gsr_data['gsr_microsiemens'].dropna()
            if len(gsr_clean) > 0 and gsr_clean.mean() > 0:
                cv = gsr_clean.std() / gsr_clean.mean()
                consistency_score = max(0, 1 - cv)
            else:
                consistency_score = 0
            
            # Weighted average
            overall_score = (
                0.4 * missing_score + 
                0.4 * artifact_score + 
                0.2 * consistency_score
            )
            
            return min(1.0, max(0.0, overall_score))
            
        except Exception:
            return 0.0
    
    def _generate_recommendations(self, gsr_data: pd.DataFrame, 
                                artifacts: int, quality_score: float) -> List[str]:
        """Generate data quality recommendations"""
        recommendations = []
        
        if quality_score < 0.6:
            recommendations.append("Data quality is below optimal - consider reviewing sensor placement and connection")
        
        if artifacts > len(gsr_data) * 0.05:  # More than 5% artifacts
            recommendations.append("High artifact rate detected - check for movement artifacts or electrical interference")
        
        missing_data = gsr_data['gsr_microsiemens'].isna().sum()
        if missing_data > len(gsr_data) * 0.02:  # More than 2% missing
            recommendations.append("Significant data loss detected - verify sensor connection stability")
        
        if len(gsr_data) < 1000:  # Less than ~8 seconds at 128Hz
            recommendations.append("Session duration is very short - consider longer recording periods for better analysis")
        
        gsr_values = gsr_data['gsr_microsiemens'].dropna()
        if len(gsr_values) > 0:
            if gsr_values.std() / gsr_values.mean() > 1.0:
                recommendations.append("High signal variability detected - verify sensor calibration")
            
            if gsr_values.min() <= 0:
                recommendations.append("Invalid GSR values detected - check sensor calibration and connection")
        
        if not recommendations:
            recommendations.append("Data quality is good - no specific issues detected")
        
        return recommendations
    
    def _save_analysis_report(self, session_path: Path, analysis: AnalysisResults):
        """Save detailed analysis report"""
        try:
            report_path = session_path / "analysis_report.json"
            
            # Convert to dict for JSON serialization
            report_data = {
                'session_id': analysis.session_id,
                'analysis_timestamp': analysis.analysis_timestamp,
                'duration_minutes': analysis.duration_minutes,
                'sample_count': analysis.sample_count,
                'statistics': {
                    'mean_gsr': analysis.mean_gsr,
                    'std_gsr': analysis.std_gsr,
                    'min_gsr': analysis.min_gsr,
                    'max_gsr': analysis.max_gsr,
                },
                'quality_assessment': {
                    'quality_score': analysis.data_quality_score,
                    'artifacts_detected': analysis.artifacts_detected,
                    'sync_mark_count': analysis.sync_mark_count,
                },
                'recommendations': analysis.recommendations
            }
            
            with open(report_path, 'w') as f:
                json.dump(report_data, f, indent=2)
                
            logger.info(f"Analysis report saved to {report_path}")
            
        except Exception as e:
            logger.error(f"Error saving analysis report: {e}")
    
    def export_analysis_summary(self, output_path: Path = None) -> Path:
        """Export summary of all analyses to CSV"""
        if not self.analysis_cache:
            logger.warning("No analysis results to export")
            return None
        
        output_path = output_path or Path("analysis_summary.csv")
        
        try:
            # Convert analysis results to DataFrame
            data = []
            for session_id, analysis in self.analysis_cache.items():
                data.append({
                    'session_id': analysis.session_id,
                    'duration_minutes': analysis.duration_minutes,
                    'sample_count': analysis.sample_count,
                    'mean_gsr': analysis.mean_gsr,
                    'std_gsr': analysis.std_gsr,
                    'min_gsr': analysis.min_gsr,
                    'max_gsr': analysis.max_gsr,
                    'quality_score': analysis.data_quality_score,
                    'artifacts_detected': analysis.artifacts_detected,
                    'sync_mark_count': analysis.sync_mark_count,
                    'analysis_timestamp': analysis.analysis_timestamp
                })
            
            df = pd.DataFrame(data)
            df.to_csv(output_path, index=False)
            
            logger.info(f"Analysis summary exported to {output_path}")
            return output_path
            
        except Exception as e:
            logger.error(f"Error exporting analysis summary: {e}")
            return None
    
    def generate_visualization(self, session_id: str, output_path: Path = None) -> Optional[Path]:
        """Generate visualization plots for a session"""
        try:
            session_path = self.data_directory / session_id
            gsr_data = self._load_gsr_data(session_path)
            
            if gsr_data is None or len(gsr_data) == 0:
                logger.warning(f"No GSR data for visualization: {session_id}")
                return None
            
            output_path = output_path or session_path / "analysis_plots.png"
            
            # Create subplot figure
            fig, axes = plt.subplots(2, 2, figsize=(15, 10))
            fig.suptitle(f'GSR Analysis - Session {session_id}', fontsize=16)
            
            # Time series plot
            if 'datetime' in gsr_data.columns:
                axes[0,0].plot(gsr_data['datetime'], gsr_data['gsr_microsiemens'])
                axes[0,0].set_title('GSR Time Series')
                axes[0,0].set_xlabel('Time')
                axes[0,0].set_ylabel('GSR (μS)')
                plt.setp(axes[0,0].xaxis.get_majorticklabels(), rotation=45)
            
            # Histogram
            gsr_clean = gsr_data['gsr_microsiemens'].dropna()
            axes[0,1].hist(gsr_clean, bins=50, alpha=0.7, edgecolor='black')
            axes[0,1].set_title('GSR Distribution')
            axes[0,1].set_xlabel('GSR (μS)')
            axes[0,1].set_ylabel('Frequency')
            
            # Box plot
            axes[1,0].boxplot(gsr_clean, vert=True)
            axes[1,0].set_title('GSR Box Plot')
            axes[1,0].set_ylabel('GSR (μS)')
            
            # Quality metrics
            if session_id in self.analysis_cache:
                analysis = self.analysis_cache[session_id]
                metrics = [
                    f"Duration: {analysis.duration_minutes:.1f} min",
                    f"Samples: {analysis.sample_count:,}",
                    f"Mean: {analysis.mean_gsr:.2f} μS",
                    f"Std: {analysis.std_gsr:.2f} μS",
                    f"Quality: {analysis.data_quality_score:.1%}",
                    f"Artifacts: {analysis.artifacts_detected}",
                    f"Sync Marks: {analysis.sync_mark_count}"
                ]
                
                axes[1,1].text(0.1, 0.9, '\n'.join(metrics), 
                              transform=axes[1,1].transAxes, 
                              verticalalignment='top',
                              fontfamily='monospace',
                              fontsize=10)
                axes[1,1].set_title('Session Metrics')
                axes[1,1].axis('off')
            
            plt.tight_layout()
            plt.savefig(output_path, dpi=300, bbox_inches='tight')
            plt.close()
            
            logger.info(f"Visualization saved to {output_path}")
            return output_path
            
        except Exception as e:
            logger.error(f"Error generating visualization: {e}")
            return None


class BatchAnalyzer:
    """Batch analysis of multiple sessions"""
    
    def __init__(self, data_directory: Path = None):
        self.analyzer = GSRDataAnalyzer(data_directory)
        self.results = {}
    
    def analyze_all_sessions(self) -> Dict[str, AnalysisResults]:
        """Analyze all sessions in the data directory"""
        sessions_processed = 0
        
        for session_path in self.analyzer.data_directory.iterdir():
            if session_path.is_dir():
                session_id = session_path.name
                logger.info(f"Analyzing session: {session_id}")
                
                result = self.analyzer.analyze_session(session_id)
                if result:
                    self.results[session_id] = result
                    sessions_processed += 1
        
        logger.info(f"Batch analysis completed: {sessions_processed} sessions processed")
        return self.results
    
    def generate_batch_report(self, output_path: Path = None) -> Path:
        """Generate comprehensive batch analysis report"""
        output_path = output_path or Path("batch_analysis_report.json")
        
        try:
            # Calculate aggregate statistics
            if not self.results:
                logger.warning("No analysis results for batch report")
                return output_path
            
            quality_scores = [r.data_quality_score for r in self.results.values()]
            durations = [r.duration_minutes for r in self.results.values()]
            
            batch_summary = {
                'analysis_timestamp': datetime.now().isoformat(),
                'total_sessions': len(self.results),
                'aggregate_statistics': {
                    'mean_quality_score': statistics.mean(quality_scores),
                    'median_quality_score': statistics.median(quality_scores),
                    'mean_duration_minutes': statistics.mean(durations),
                    'total_duration_hours': sum(durations) / 60,
                },
                'session_results': [
                    {
                        'session_id': result.session_id,
                        'duration_minutes': result.duration_minutes,
                        'quality_score': result.data_quality_score,
                        'sample_count': result.sample_count,
                        'artifacts_detected': result.artifacts_detected,
                        'sync_mark_count': result.sync_mark_count,
                        'recommendations': result.recommendations
                    }
                    for result in self.results.values()
                ]
            }
            
            with open(output_path, 'w') as f:
                json.dump(batch_summary, f, indent=2)
            
            logger.info(f"Batch analysis report saved to {output_path}")
            return output_path
            
        except Exception as e:
            logger.error(f"Error generating batch report: {e}")
            return output_path


if __name__ == "__main__":
    # Demo usage
    analyzer = GSRDataAnalyzer()
    batch_analyzer = BatchAnalyzer()
    
    # Analyze all sessions
    results = batch_analyzer.analyze_all_sessions()
    
    # Generate reports
    batch_analyzer.generate_batch_report()
    analyzer.export_analysis_summary()
    
    # Generate visualizations for each session
    for session_id in results.keys():
        analyzer.generate_visualization(session_id)
    
    print(f"Analysis completed for {len(results)} sessions")