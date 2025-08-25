"""
Advanced Machine Learning and Signal Processing for GSR Analysis.
Research-grade algorithms for physiological data processing.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, asdict
from loguru import logger

try:
    from scipy import signal, stats
    from scipy.fft import fft, fftfreq
    from scipy.signal import butter, filtfilt, find_peaks, periodogram
    from scipy.stats import zscore, pearsonr, spearmanr
    SCIPY_AVAILABLE = True
except ImportError:
    SCIPY_AVAILABLE = False
    logger.warning("SciPy not available - advanced signal processing features disabled")

try:
    from sklearn.preprocessing import StandardScaler, MinMaxScaler
    from sklearn.decomposition import PCA, FastICA
    from sklearn.cluster import KMeans, DBSCAN
    from sklearn.ensemble import IsolationForest
    from sklearn.metrics import silhouette_score
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    logger.warning("Scikit-learn not available - machine learning features disabled")


@dataclass
class GSRFeatures:
    """Comprehensive GSR feature extraction results"""
    
    # Basic statistics
    mean_gsr: float
    std_gsr: float
    min_gsr: float
    max_gsr: float
    median_gsr: float
    range_gsr: float
    
    # Temporal features
    sampling_rate: float
    duration_seconds: float
    total_samples: int
    
    # Variability features
    coefficient_of_variation: float
    interquartile_range: float
    mean_absolute_deviation: float
    
    # Distribution features
    skewness: float
    kurtosis: float
    percentile_5: float
    percentile_95: float
    
    # Signal processing features
    signal_energy: float
    spectral_centroid: float
    dominant_frequency: float
    spectral_bandwidth: float
    zero_crossing_rate: float
    
    # Physiological features
    arousal_events: int
    relaxation_events: int
    tonic_level: float
    phasic_activity: float
    response_amplitude: float
    
    # Quality indicators
    signal_to_noise_ratio: float
    artifact_percentage: float
    data_completeness: float
    quality_score: float


@dataclass
class EmotionClassification:
    """Emotion classification results from GSR analysis"""
    
    dominant_emotion: str
    confidence: float
    emotional_intensity: float
    
    # Emotion probabilities
    calm: float
    excited: float
    stressed: float
    relaxed: float
    aroused: float
    
    # Context
    analysis_window_seconds: float
    features_used: List[str]
    classification_method: str


@dataclass
class ArtifactDetection:
    """Advanced artifact detection results"""
    
    artifacts_found: int
    artifact_positions: List[int]
    artifact_types: List[str]
    artifact_severities: List[float]
    clean_data_percentage: float
    recommended_filters: List[str]
    
    # Artifact categories
    motion_artifacts: int
    electrical_artifacts: int
    thermal_artifacts: int
    sensor_artifacts: int


class AdvancedGSRAnalyzer:
    """Advanced GSR analysis with machine learning and signal processing"""
    
    def __init__(self, sampling_rate: float = 128.0):
        self.sampling_rate = sampling_rate
        self.nyquist_freq = sampling_rate / 2.0
        
    def extract_comprehensive_features(self, gsr_data: np.ndarray, 
                                     timestamps: Optional[np.ndarray] = None) -> GSRFeatures:
        """Extract comprehensive feature set from GSR data"""
        
        if len(gsr_data) == 0:
            raise ValueError("Empty GSR data provided")
        
        # Remove NaN values
        valid_mask = ~np.isnan(gsr_data)
        clean_data = gsr_data[valid_mask]
        
        if len(clean_data) < 10:
            raise ValueError("Insufficient valid data points")
        
        # Basic statistics
        mean_gsr = np.mean(clean_data)
        std_gsr = np.std(clean_data)
        min_gsr = np.min(clean_data)
        max_gsr = np.max(clean_data)
        median_gsr = np.median(clean_data)
        range_gsr = max_gsr - min_gsr
        
        # Temporal features
        duration_seconds = len(gsr_data) / self.sampling_rate
        total_samples = len(gsr_data)
        
        # Variability features
        cv = std_gsr / mean_gsr if mean_gsr != 0 else 0
        iqr = np.percentile(clean_data, 75) - np.percentile(clean_data, 25)
        mad = np.mean(np.abs(clean_data - mean_gsr))
        
        # Distribution features
        if SCIPY_AVAILABLE:
            skewness = stats.skew(clean_data)
            kurtosis = stats.kurtosis(clean_data)
        else:
            skewness = 0.0
            kurtosis = 0.0
        
        p5 = np.percentile(clean_data, 5)
        p95 = np.percentile(clean_data, 95)
        
        # Signal processing features
        signal_energy = np.sum(clean_data ** 2)
        
        if SCIPY_AVAILABLE and len(clean_data) > 128:
            # Frequency domain analysis
            freqs, psd = periodogram(clean_data, fs=self.sampling_rate)
            
            # Spectral centroid
            spectral_centroid = np.sum(freqs * psd) / np.sum(psd)
            
            # Dominant frequency
            dominant_freq_idx = np.argmax(psd[1:]) + 1  # Skip DC component
            dominant_frequency = freqs[dominant_freq_idx]
            
            # Spectral bandwidth
            spectral_bandwidth = np.sqrt(np.sum(((freqs - spectral_centroid) ** 2) * psd) / np.sum(psd))
            
        else:
            spectral_centroid = 0.0
            dominant_frequency = 0.0
            spectral_bandwidth = 0.0
        
        # Zero crossing rate
        zero_crossings = np.where(np.diff(np.signbit(clean_data - mean_gsr)))[0]
        zero_crossing_rate = len(zero_crossings) / len(clean_data) if len(clean_data) > 1 else 0
        
        # Physiological features
        tonic_phasic = self._decompose_tonic_phasic(clean_data)
        tonic_level = np.mean(tonic_phasic['tonic'])
        phasic_activity = np.std(tonic_phasic['phasic'])
        
        # Detect physiological events
        arousal_events, relaxation_events = self._detect_physiological_events(clean_data)
        response_amplitude = self._calculate_response_amplitude(tonic_phasic['phasic'])
        
        # Quality indicators
        snr = self._calculate_snr(clean_data)
        artifact_pct = self._estimate_artifact_percentage(clean_data)
        data_completeness = np.sum(valid_mask) / len(gsr_data)
        quality_score = self._calculate_quality_score(clean_data, data_completeness, snr, artifact_pct)
        
        return GSRFeatures(
            mean_gsr=mean_gsr,
            std_gsr=std_gsr,
            min_gsr=min_gsr,
            max_gsr=max_gsr,
            median_gsr=median_gsr,
            range_gsr=range_gsr,
            sampling_rate=self.sampling_rate,
            duration_seconds=duration_seconds,
            total_samples=total_samples,
            coefficient_of_variation=cv,
            interquartile_range=iqr,
            mean_absolute_deviation=mad,
            skewness=skewness,
            kurtosis=kurtosis,
            percentile_5=p5,
            percentile_95=p95,
            signal_energy=signal_energy,
            spectral_centroid=spectral_centroid,
            dominant_frequency=dominant_frequency,
            spectral_bandwidth=spectral_bandwidth,
            zero_crossing_rate=zero_crossing_rate,
            arousal_events=arousal_events,
            relaxation_events=relaxation_events,
            tonic_level=tonic_level,
            phasic_activity=phasic_activity,
            response_amplitude=response_amplitude,
            signal_to_noise_ratio=snr,
            artifact_percentage=artifact_pct,
            data_completeness=data_completeness,
            quality_score=quality_score
        )
    
    def classify_emotional_state(self, gsr_data: np.ndarray, 
                               window_seconds: float = 30.0) -> List[EmotionClassification]:
        """Classify emotional states from GSR data using sliding windows"""
        
        if not SKLEARN_AVAILABLE:
            logger.warning("Emotion classification requires scikit-learn")
            return []
        
        window_samples = int(window_seconds * self.sampling_rate)
        if len(gsr_data) < window_samples:
            window_samples = len(gsr_data)
        
        classifications = []
        step_size = window_samples // 2  # 50% overlap
        
        for start_idx in range(0, len(gsr_data) - window_samples + 1, step_size):
            end_idx = start_idx + window_samples
            window_data = gsr_data[start_idx:end_idx]
            
            # Extract features for this window
            try:
                features = self.extract_comprehensive_features(window_data)
                emotion = self._classify_emotion_from_features(features, window_seconds)
                classifications.append(emotion)
            except Exception as e:
                logger.error(f"Error classifying emotion for window {start_idx}-{end_idx}: {e}")
                continue
        
        return classifications
    
    def detect_advanced_artifacts(self, gsr_data: np.ndarray) -> ArtifactDetection:
        """Advanced artifact detection using multiple methods"""
        
        artifacts = []
        artifact_types = []
        artifact_severities = []
        
        # Statistical outlier detection (Z-score method)
        if SCIPY_AVAILABLE:
            z_scores = np.abs(zscore(gsr_data, nan_policy='omit'))
            statistical_outliers = np.where(z_scores > 3.0)[0]
            
            for idx in statistical_outliers:
                artifacts.append(idx)
                artifact_types.append('statistical_outlier')
                artifact_severities.append(min(z_scores[idx] / 3.0, 3.0))  # Cap at 3.0
        
        # Gradient-based artifact detection (sudden changes)
        if len(gsr_data) > 1:
            gradient = np.gradient(gsr_data)
            gradient_threshold = 3 * np.std(gradient[~np.isnan(gradient)])
            gradient_artifacts = np.where(np.abs(gradient) > gradient_threshold)[0]
            
            for idx in gradient_artifacts:
                artifacts.append(idx)
                artifact_types.append('gradient_artifact')
                severity = min(np.abs(gradient[idx]) / gradient_threshold, 3.0)
                artifact_severities.append(severity)
        
        # Isolation Forest for anomaly detection
        if SKLEARN_AVAILABLE and len(gsr_data) > 50:
            try:
                # Create feature matrix from sliding windows
                window_size = min(10, len(gsr_data) // 10)
                features_matrix = []
                
                for i in range(len(gsr_data) - window_size + 1):
                    window = gsr_data[i:i + window_size]
                    if not np.any(np.isnan(window)):
                        features = [
                            np.mean(window),
                            np.std(window),
                            np.max(window) - np.min(window),
                            np.median(window)
                        ]
                        features_matrix.append(features)
                
                if features_matrix:
                    features_array = np.array(features_matrix)
                    
                    isolation_forest = IsolationForest(contamination=0.1, random_state=42)
                    anomaly_labels = isolation_forest.fit_predict(features_array)
                    
                    anomaly_indices = np.where(anomaly_labels == -1)[0]
                    for idx in anomaly_indices:
                        if idx < len(gsr_data):
                            artifacts.append(idx)
                            artifact_types.append('ml_anomaly')
                            artifact_severities.append(1.5)  # Moderate severity for ML detected
                            
            except Exception as e:
                logger.warning(f"ML-based artifact detection failed: {e}")
        
        # Categorize artifacts by type
        motion_artifacts = sum(1 for t in artifact_types if 'gradient' in t)
        electrical_artifacts = sum(1 for t in artifact_types if 'statistical' in t)
        thermal_artifacts = 0  # Would require temperature data
        sensor_artifacts = sum(1 for t in artifact_types if 'ml_anomaly' in t)
        
        # Calculate clean data percentage
        unique_artifacts = set(artifacts)
        clean_data_percentage = 1.0 - (len(unique_artifacts) / len(gsr_data))
        
        # Recommend filters based on artifact types
        recommended_filters = []
        if motion_artifacts > 0:
            recommended_filters.append('low_pass_filter_5hz')
        if electrical_artifacts > len(gsr_data) * 0.05:  # More than 5% outliers
            recommended_filters.append('median_filter')
        if clean_data_percentage < 0.8:
            recommended_filters.append('artifact_removal')
        
        return ArtifactDetection(
            artifacts_found=len(unique_artifacts),
            artifact_positions=sorted(list(unique_artifacts)),
            artifact_types=artifact_types,
            artifact_severities=artifact_severities,
            clean_data_percentage=clean_data_percentage,
            recommended_filters=recommended_filters,
            motion_artifacts=motion_artifacts,
            electrical_artifacts=electrical_artifacts,
            thermal_artifacts=thermal_artifacts,
            sensor_artifacts=sensor_artifacts
        )
    
    def apply_advanced_filtering(self, gsr_data: np.ndarray, filter_type: str = 'adaptive') -> np.ndarray:
        """Apply advanced filtering techniques to GSR data"""
        
        if not SCIPY_AVAILABLE:
            logger.warning("Advanced filtering requires scipy")
            return gsr_data
        
        filtered_data = gsr_data.copy()
        
        # Remove NaN values for processing
        valid_mask = ~np.isnan(filtered_data)
        if not np.any(valid_mask):
            return filtered_data
        
        valid_data = filtered_data[valid_mask]
        
        if filter_type == 'low_pass' or filter_type == 'adaptive':
            # Butterworth low-pass filter
            cutoff_freq = min(5.0, self.nyquist_freq * 0.8)  # 5 Hz or 80% of Nyquist
            b, a = butter(4, cutoff_freq / self.nyquist_freq, btype='low')
            valid_data = filtfilt(b, a, valid_data)
        
        if filter_type == 'median' or filter_type == 'adaptive':
            # Median filter for artifact removal
            kernel_size = min(5, len(valid_data) // 10)
            if kernel_size >= 3:
                valid_data = signal.medfilt(valid_data, kernel_size=kernel_size)
        
        if filter_type == 'savgol' or filter_type == 'adaptive':
            # Savitzky-Golay filter for smoothing
            window_length = min(11, len(valid_data) // 4)
            if window_length >= 3 and window_length % 2 == 1:  # Must be odd
                valid_data = signal.savgol_filter(valid_data, window_length, 3)
        
        if filter_type == 'wavelet' and len(valid_data) > 64:
            # Wavelet denoising (simplified)
            try:
                import pywt
                coeffs = pywt.wavedec(valid_data, 'db4', level=4)
                threshold = 0.1 * np.max(np.abs(coeffs[-1]))
                coeffs_thresh = [pywt.threshold(c, threshold, 'soft') for c in coeffs]
                valid_data = pywt.waverec(coeffs_thresh, 'db4')
            except ImportError:
                logger.warning("Wavelet filtering requires PyWavelets")
        
        # Put filtered data back
        filtered_data[valid_mask] = valid_data[:np.sum(valid_mask)]
        
        return filtered_data
    
    def perform_dimensionality_reduction(self, gsr_features_matrix: np.ndarray, 
                                       method: str = 'pca') -> Dict[str, Any]:
        """Perform dimensionality reduction for feature analysis"""
        
        if not SKLEARN_AVAILABLE:
            logger.warning("Dimensionality reduction requires scikit-learn")
            return {}
        
        # Standardize features
        scaler = StandardScaler()
        scaled_features = scaler.fit_transform(gsr_features_matrix)
        
        results = {}
        
        if method == 'pca' or method == 'all':
            # Principal Component Analysis
            pca = PCA(n_components=min(scaled_features.shape[1], 5))
            pca_features = pca.fit_transform(scaled_features)
            
            results['pca'] = {
                'transformed_features': pca_features,
                'explained_variance_ratio': pca.explained_variance_ratio_,
                'cumulative_variance': np.cumsum(pca.explained_variance_ratio_),
                'n_components': pca.n_components_,
                'feature_importance': pca.components_
            }
        
        if method == 'ica' or method == 'all':
            # Independent Component Analysis
            n_components = min(scaled_features.shape[1], 5)
            ica = FastICA(n_components=n_components, random_state=42)
            ica_features = ica.fit_transform(scaled_features)
            
            results['ica'] = {
                'transformed_features': ica_features,
                'mixing_matrix': ica.mixing_,
                'n_components': n_components
            }
        
        return results
    
    def cluster_physiological_states(self, gsr_features_matrix: np.ndarray, 
                                   n_clusters: Optional[int] = None) -> Dict[str, Any]:
        """Cluster physiological states based on GSR features"""
        
        if not SKLEARN_AVAILABLE:
            logger.warning("Clustering requires scikit-learn")
            return {}
        
        # Standardize features
        scaler = StandardScaler()
        scaled_features = scaler.fit_transform(gsr_features_matrix)
        
        results = {}
        
        # Determine optimal number of clusters if not provided
        if n_clusters is None:
            # Use elbow method for K-means
            max_clusters = min(10, scaled_features.shape[0] // 2)
            inertias = []
            silhouette_scores = []
            
            for k in range(2, max_clusters + 1):
                kmeans = KMeans(n_clusters=k, random_state=42)
                cluster_labels = kmeans.fit_predict(scaled_features)
                inertias.append(kmeans.inertia_)
                
                if len(set(cluster_labels)) > 1:  # Need at least 2 clusters for silhouette
                    sil_score = silhouette_score(scaled_features, cluster_labels)
                    silhouette_scores.append(sil_score)
                else:
                    silhouette_scores.append(0)
            
            # Choose k with highest silhouette score
            if silhouette_scores:
                optimal_k = np.argmax(silhouette_scores) + 2
            else:
                optimal_k = 3  # Default
        else:
            optimal_k = n_clusters
        
        # K-Means clustering
        kmeans = KMeans(n_clusters=optimal_k, random_state=42)
        kmeans_labels = kmeans.fit_predict(scaled_features)
        
        results['kmeans'] = {
            'labels': kmeans_labels,
            'cluster_centers': kmeans.cluster_centers_,
            'n_clusters': optimal_k,
            'silhouette_score': silhouette_score(scaled_features, kmeans_labels) if len(set(kmeans_labels)) > 1 else 0
        }
        
        # DBSCAN clustering
        try:
            eps = np.percentile(np.sqrt(np.sum((scaled_features[None, :] - scaled_features[:, None])**2, axis=2)), 10)
            dbscan = DBSCAN(eps=eps, min_samples=3)
            dbscan_labels = dbscan.fit_predict(scaled_features)
            
            results['dbscan'] = {
                'labels': dbscan_labels,
                'n_clusters': len(set(dbscan_labels)) - (1 if -1 in dbscan_labels else 0),
                'n_noise_points': np.sum(dbscan_labels == -1),
                'eps': eps
            }
            
        except Exception as e:
            logger.warning(f"DBSCAN clustering failed: {e}")
        
        return results
    
    def _decompose_tonic_phasic(self, gsr_data: np.ndarray) -> Dict[str, np.ndarray]:
        """Decompose GSR signal into tonic and phasic components"""
        
        if not SCIPY_AVAILABLE:
            return {'tonic': gsr_data, 'phasic': np.zeros_like(gsr_data)}
        
        # Simple decomposition using low-pass filter for tonic component
        # Tonic component: slow changes (< 0.5 Hz)
        cutoff_freq = min(0.5, self.nyquist_freq * 0.1)
        b, a = butter(2, cutoff_freq / self.nyquist_freq, btype='low')
        tonic = filtfilt(b, a, gsr_data)
        
        # Phasic component: fast changes (residual)
        phasic = gsr_data - tonic
        
        return {'tonic': tonic, 'phasic': phasic}
    
    def _detect_physiological_events(self, gsr_data: np.ndarray) -> Tuple[int, int]:
        """Detect arousal and relaxation events in GSR data"""
        
        if not SCIPY_AVAILABLE:
            return 0, 0
        
        # Compute derivative to find rapid changes
        gradient = np.gradient(gsr_data)
        
        # Find peaks in positive gradient (arousal events)
        arousal_threshold = np.std(gradient) * 1.5
        arousal_peaks, _ = find_peaks(gradient, height=arousal_threshold)
        
        # Find peaks in negative gradient (relaxation events)  
        relaxation_peaks, _ = find_peaks(-gradient, height=arousal_threshold)
        
        return len(arousal_peaks), len(relaxation_peaks)
    
    def _calculate_response_amplitude(self, phasic_data: np.ndarray) -> float:
        """Calculate mean response amplitude from phasic component"""
        
        if not SCIPY_AVAILABLE:
            return np.std(phasic_data)
        
        # Find peaks in phasic component
        peaks, _ = find_peaks(np.abs(phasic_data), height=np.std(phasic_data))
        
        if len(peaks) == 0:
            return 0.0
        
        # Calculate mean amplitude of peaks
        peak_amplitudes = np.abs(phasic_data[peaks])
        return np.mean(peak_amplitudes)
    
    def _calculate_snr(self, gsr_data: np.ndarray) -> float:
        """Calculate signal-to-noise ratio"""
        
        signal_power = np.var(gsr_data)
        
        # Estimate noise as high-frequency component
        if SCIPY_AVAILABLE and len(gsr_data) > 10:
            # High-pass filter to isolate noise
            cutoff_freq = min(10.0, self.nyquist_freq * 0.8)
            b, a = butter(2, cutoff_freq / self.nyquist_freq, btype='high')
            noise = filtfilt(b, a, gsr_data)
            noise_power = np.var(noise)
            
            if noise_power > 0:
                return 10 * np.log10(signal_power / noise_power)
        
        return 20.0  # Default reasonable SNR
    
    def _estimate_artifact_percentage(self, gsr_data: np.ndarray) -> float:
        """Estimate percentage of data that are artifacts"""
        
        if SCIPY_AVAILABLE:
            # Use Z-score method
            z_scores = np.abs(zscore(gsr_data, nan_policy='omit'))
            artifacts = np.sum(z_scores > 2.5)  # Less strict than 3.0
            return artifacts / len(gsr_data)
        
        # Fallback: use simple outlier detection
        q1, q3 = np.percentile(gsr_data, [25, 75])
        iqr = q3 - q1
        lower_bound = q1 - 1.5 * iqr
        upper_bound = q3 + 1.5 * iqr
        
        outliers = np.sum((gsr_data < lower_bound) | (gsr_data > upper_bound))
        return outliers / len(gsr_data)
    
    def _calculate_quality_score(self, gsr_data: np.ndarray, 
                               completeness: float, snr: float, artifact_pct: float) -> float:
        """Calculate overall quality score"""
        
        # Normalize SNR (typical range 0-40 dB)
        snr_normalized = min(snr / 40.0, 1.0)
        
        # Artifact penalty
        artifact_penalty = 1.0 - min(artifact_pct, 1.0)
        
        # Combine factors
        quality = (completeness * 0.4 + snr_normalized * 0.3 + artifact_penalty * 0.3)
        
        return max(0.0, min(1.0, quality))
    
    def _classify_emotion_from_features(self, features: GSRFeatures, 
                                      window_seconds: float) -> EmotionClassification:
        """Classify emotion from extracted features (rule-based approach)"""
        
        # Simple rule-based classification
        # This would be replaced with trained ML models in production
        
        # Arousal level based on phasic activity and response amplitude
        arousal_score = min((features.phasic_activity + features.response_amplitude) / 2.0, 1.0)
        
        # Stress indicators
        stress_indicators = [
            features.coefficient_of_variation > 0.5,
            features.arousal_events > features.relaxation_events,
            features.zero_crossing_rate > 0.1,
            features.spectral_centroid > 1.0
        ]
        stress_score = sum(stress_indicators) / len(stress_indicators)
        
        # Emotion probabilities (simplified model)
        if arousal_score > 0.7:
            if stress_score > 0.5:
                dominant_emotion = "stressed"
                probabilities = {"stressed": 0.7, "excited": 0.2, "aroused": 0.1, "calm": 0.0, "relaxed": 0.0}
            else:
                dominant_emotion = "excited"
                probabilities = {"excited": 0.6, "aroused": 0.3, "stressed": 0.1, "calm": 0.0, "relaxed": 0.0}
        elif arousal_score < 0.3:
            if stress_score < 0.3:
                dominant_emotion = "relaxed"
                probabilities = {"relaxed": 0.7, "calm": 0.3, "excited": 0.0, "stressed": 0.0, "aroused": 0.0}
            else:
                dominant_emotion = "calm"
                probabilities = {"calm": 0.6, "relaxed": 0.3, "stressed": 0.1, "excited": 0.0, "aroused": 0.0}
        else:
            dominant_emotion = "aroused"
            probabilities = {"aroused": 0.5, "excited": 0.2, "stressed": 0.2, "calm": 0.1, "relaxed": 0.0}
        
        confidence = probabilities[dominant_emotion]
        emotional_intensity = (arousal_score + stress_score) / 2.0
        
        return EmotionClassification(
            dominant_emotion=dominant_emotion,
            confidence=confidence,
            emotional_intensity=emotional_intensity,
            calm=probabilities["calm"],
            excited=probabilities["excited"],
            stressed=probabilities["stressed"],
            relaxed=probabilities["relaxed"],
            aroused=probabilities["aroused"],
            analysis_window_seconds=window_seconds,
            features_used=['phasic_activity', 'response_amplitude', 'cv', 'arousal_events'],
            classification_method='rule_based_v1'
        )


# Export main classes
__all__ = [
    "AdvancedGSRAnalyzer", 
    "GSRFeatures", 
    "EmotionClassification", 
    "ArtifactDetection"
]