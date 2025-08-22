import statistics
import threading
import time
from collections import deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Callable, Dict, List, Optional, Tuple, Union
import cv2
import numpy as np
import scipy.ndimage
import scipy.signal
from ..utils.logging_config import get_logger, performance_timer
logger = get_logger(__name__)
class ROIDetectionMethod(Enum):
    FACE_CASCADE = "face_cascade"
    DNN_FACE = "dnn_face"
    MEDIAPIPE = "mediapipe"
    MEDIAPIPE_HANDS = "mediapipe_hands"
    CUSTOM_TRACKER = "custom_tracker"
    MANUAL_SELECTION = "manual"
class SignalExtractionMethod(Enum):
    MEAN_RGB = "mean_rgb"
    ICA_SEPARATION = "ica"
    PCA_PROJECTION = "pca"
    CHROM_METHOD = "chrom"
    POS_METHOD = "pos"
    ADAPTIVE_HYBRID = "adaptive"
@dataclass
class ROIMetrics:
    area_pixels: int = 0
    center_coordinates: Tuple[int, int] = (0, 0)
    stability_score: float = 0.0
    illumination_uniformity: float = 0.0
    motion_magnitude: float = 0.0
    skin_probability: float = 0.0
    signal_to_noise_ratio: float = 0.0
    position_variance: float = 0.0
    size_variance: float = 0.0
    shape_consistency: float = 0.0
    is_valid: bool = False
    confidence_score: float = 0.0
    frame_count: int = 0
@dataclass
class PhysiologicalSignal:
    signal_data: np.ndarray
    sampling_rate: float
    timestamp: float
    signal_type: str = "rppg"
    extraction_method: str = "unknown"
    snr_db: float = 0.0
    signal_quality_index: float = 0.0
    motion_artifacts: float = 0.0
    preprocessing_steps: List[str] = field(default_factory=list)
    roi_metrics: Optional[ROIMetrics] = None
    spectral_features: Optional[Dict] = None
    def get_heart_rate_estimate(
        self, freq_range: Tuple[float, float] = (0.7, 4.0)
    ) -> Optional[float]:
        if len(self.signal_data) < self.sampling_rate * 2:
            return None
        try:
            freqs, psd = scipy.signal.welch(
                self.signal_data,
                fs=self.sampling_rate,
                nperseg=min(512, len(self.signal_data) // 4),
            )
            hr_mask = (freqs >= freq_range[0]) & (freqs <= freq_range[1])
            hr_freqs = freqs[hr_mask]
            hr_psd = psd[hr_mask]
            if len(hr_psd) > 0:
                peak_freq = hr_freqs[np.argmax(hr_psd)]
                heart_rate_bpm = peak_freq * 60.0
                return heart_rate_bpm
        except Exception as e:
            logger.warning(f"Heart rate estimation failed: {e}")
        return None
class AdvancedROIDetector:
    def __init__(
        self,
        method: ROIDetectionMethod = ROIDetectionMethod.MEDIAPIPE_HANDS,
        tracking_enabled: bool = True,
        stability_threshold: float = 0.8,
    ):
        self.method = method
        self.tracking_enabled = tracking_enabled
        self.stability_threshold = stability_threshold
        self._init_detection_models()
        self.current_roi = None
        self.roi_history = deque(maxlen=30)
        self.tracker = None
        self.detection_times = deque(maxlen=100)
        self.roi_metrics = ROIMetrics()
        logger.info(f"AdvancedROIDetector initialized with method: {method.value}")
    def _init_detection_models(self):
        try:
            if self.method == ROIDetectionMethod.FACE_CASCADE:
                cascade_path = (
                    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
                )
                self.face_cascade = cv2.CascadeClassifier(cascade_path)
            elif self.method == ROIDetectionMethod.DNN_FACE:
                self.dnn_net = cv2.dnn.readNetFromTensorflow(
                    "models/opencv_face_detector_uint8.pb",
                    "models/opencv_face_detector.pbtxt",
                )
            elif self.method == ROIDetectionMethod.MEDIAPIPE:
                try:
                    import mediapipe as mp
                    self.mp_face_detection = mp.solutions.face_detection
                    self.mp_drawing = mp.solutions.drawing_utils
                    self.face_detection = self.mp_face_detection.FaceDetection(
                        model_selection=0, min_detection_confidence=0.5
                    )
                except ImportError:
                    logger.warning(
                        "MediaPipe not available, falling back to DNN detection"
                    )
                    self.method = ROIDetectionMethod.DNN_FACE
                    self._init_detection_models()
            elif self.method == ROIDetectionMethod.MEDIAPIPE_HANDS:
                try:
                    import mediapipe as mp
                    self.mp_hands = mp.solutions.hands
                    self.mp_drawing = mp.solutions.drawing_utils
                    self.hands_detection = self.mp_hands.Hands(
                        static_image_mode=False,
                        max_num_hands=2,
                        min_detection_confidence=0.5,
                        min_tracking_confidence=0.5
                    )
                except ImportError:
                    logger.warning(
                        "MediaPipe not available, falling back to custom tracker"
                    )
                    self.method = ROIDetectionMethod.CUSTOM_TRACKER
                    self._init_detection_models()
        except Exception as e:
            logger.error(f"Failed to initialize detection model: {e}")
            self.method = ROIDetectionMethod.FACE_CASCADE
            self._init_detection_models()
    @performance_timer("detect_roi")
    def detect_roi(self, frame: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        detection_start = time.time()
        try:
            roi = None
            if self.tracking_enabled and self.current_roi is not None:
                roi = self._track_roi(frame)
            if roi is None:
                if self.method == ROIDetectionMethod.FACE_CASCADE:
                    roi = self._detect_cascade(frame)
                elif self.method == ROIDetectionMethod.DNN_FACE:
                    roi = self._detect_dnn(frame)
                elif self.method == ROIDetectionMethod.MEDIAPIPE:
                    roi = self._detect_mediapipe(frame)
                elif self.method == ROIDetectionMethod.MEDIAPIPE_HANDS:
                    roi = self._detect_mediapipe_hands(frame)
                elif self.method == ROIDetectionMethod.CUSTOM_TRACKER:
                    roi = self._detect_custom(frame)
            if roi is not None:
                self._update_roi_metrics(roi, frame)
                self.current_roi = roi
                if self.tracking_enabled:
                    self._init_tracker(frame, roi)
            detection_time = time.time() - detection_start
            self.detection_times.append(detection_time)
            return roi
        except Exception as e:
            logger.error(f"ROI detection failed: {e}")
            return None
    def _detect_cascade(self, frame: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        grey = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self.face_cascade.detectMultiScale(
            grey, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80)
        )
        if len(faces) > 0:
            largest_face = max(faces, key=lambda f: f[2] * f[3])
            return tuple(largest_face)
        return None
    def _detect_dnn(self, frame: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        h, w = frame.shape[:2]
        blob = cv2.dnn.blobFromImage(frame, 1.0, (300, 300), [104, 117, 123])
        self.dnn_net.setInput(blob)
        detections = self.dnn_net.forward()
        best_confidence = 0
        best_roi = None
        for i in range(detections.shape[2]):
            confidence = detections[0, 0, i, 2]
            if confidence > 0.5 and confidence > best_confidence:
                best_confidence = confidence
                box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
                x, y, x1, y1 = box.astype(int)
                best_roi = x, y, x1 - x, y1 - y
        return best_roi
    def _detect_mediapipe(
        self, frame: np.ndarray
    ) -> Optional[Tuple[int, int, int, int]]:
        if not hasattr(self, "face_detection"):
            return None
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = self.face_detection.process(rgb_frame)
        if results.detections:
            detection = results.detections[0]
            bbox = detection.location_data.relative_bounding_box
            h, w = frame.shape[:2]
            x = int(bbox.xmin * w)
            y = int(bbox.ymin * h)
            width = int(bbox.width * w)
            height = int(bbox.height * h)
            return x, y, width, height
        return None
    
    def _detect_mediapipe_hands(
        self, frame: np.ndarray
    ) -> Optional[Tuple[int, int, int, int]]:
        if not hasattr(self, "hands_detection"):
            return None
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = self.hands_detection.process(rgb_frame)
        if results.multi_hand_landmarks:
            # Get the first detected hand
            landmarks = results.multi_hand_landmarks[0]
            h, w = frame.shape[:2]
            
            # Calculate bounding box from hand landmarks
            x_coords = []
            y_coords = []
            for landmark in landmarks.landmark:
                x_coords.append(int(landmark.x * w))
                y_coords.append(int(landmark.y * h))
            
            if x_coords and y_coords:
                min_x, max_x = min(x_coords), max(x_coords)
                min_y, max_y = min(y_coords), max(y_coords)
                
                # Add some padding around the hand
                padding = 20
                x = max(0, min_x - padding)
                y = max(0, min_y - padding)
                width = min(w - x, max_x - min_x + 2 * padding)
                height = min(h - y, max_y - min_y + 2 * padding)
                
                return x, y, width, height
        return None
    
    def _detect_custom(self, frame: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        return self._detect_cascade(frame)
    def _track_roi(self, frame: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        if self.tracker is None:
            return None
        try:
            success, bbox = self.tracker.update(frame)
            if success:
                return tuple(map(int, bbox))
        except Exception as e:
            logger.debug(f"ROI tracking failed: {e}")
        return None
    def _init_tracker(self, frame: np.ndarray, roi: Tuple[int, int, int, int]):
        try:
            self.tracker = cv2.TrackerCSRT_create()
            self.tracker.init(frame, roi)
        except Exception as e:
            logger.debug(f"Tracker initialization failed: {e}")
            self.tracker = None
    def _update_roi_metrics(self, roi: Tuple[int, int, int, int], frame: np.ndarray):
        x, y, w, h = roi
        self.roi_metrics.area_pixels = w * h
        self.roi_metrics.center_coordinates = x + w // 2, y + h // 2
        self.roi_metrics.frame_count += 1
        self.roi_history.append(roi)
        if len(self.roi_history) > 1:
            self._calculate_stability_metrics()
        roi_region = frame[y : y + h, x : x + w]
        if roi_region.size > 0:
            self._analyze_roi_content(roi_region)
    def _calculate_stability_metrics(self):
        if len(self.roi_history) < 2:
            return
        centres = [(x + w // 2, y + h // 2) for x, y, w, h in self.roi_history]
        center_x = [c[0] for c in centres]
        center_y = [c[1] for c in centres]
        if len(center_x) > 1:
            self.roi_metrics.position_variance = (
                np.var(center_x) + np.var(center_y)
            ) / 2.0
        areas = [(w * h) for x, y, w, h in self.roi_history]
        if len(areas) > 1:
            self.roi_metrics.size_variance = np.var(areas)
        motion_scores = []
        for i in range(1, len(self.roi_history)):
            prev_center = centres[i - 1]
            curr_center = centres[i]
            motion = np.sqrt(
                (curr_center[0] - prev_center[0]) ** 2
                + (curr_center[1] - prev_center[1]) ** 2
            )
            motion_scores.append(motion)
        if motion_scores:
            avg_motion = np.mean(motion_scores)
            self.roi_metrics.motion_magnitude = avg_motion
            self.roi_metrics.stability_score = max(0.0, 1.0 - avg_motion / 50.0)
    def _analyze_roi_content(self, roi_region: np.ndarray):
        try:
            gray_roi = cv2.cvtColor(roi_region, cv2.COLOR_BGR2GRAY)
            mean_intensity = np.mean(gray_roi)
            std_intensity = np.std(gray_roi)
            if mean_intensity > 0:
                self.roi_metrics.illumination_uniformity = 1.0 - min(
                    1.0, std_intensity / mean_intensity
                )
            self.roi_metrics.skin_probability = self._estimate_skin_probability(
                roi_region
            )
            self.roi_metrics.is_valid = (
                self.roi_metrics.stability_score > self.stability_threshold
                and self.roi_metrics.illumination_uniformity > 0.3
                and self.roi_metrics.skin_probability > 0.5
            )
            self.roi_metrics.confidence_score = (
                0.4 * self.roi_metrics.stability_score
                + 0.3 * self.roi_metrics.illumination_uniformity
                + 0.3 * self.roi_metrics.skin_probability
            )
        except Exception as e:
            logger.debug(f"ROI content analysis failed: {e}")
    def _estimate_skin_probability(self, roi_region: np.ndarray) -> float:
        try:
            ycrcb = cv2.cvtColor(roi_region, cv2.COLOR_BGR2YCrCb)
            lower_skin = np.array([0, 133, 77])
            upper_skin = np.array([255, 173, 127])
            skin_mask = cv2.inRange(ycrcb, lower_skin, upper_skin)
            skin_pixels = np.sum(skin_mask > 0)
            total_pixels = skin_mask.shape[0] * skin_mask.shape[1]
            if total_pixels > 0:
                return skin_pixels / total_pixels
        except Exception as e:
            logger.debug(f"Skin probability estimation failed: {e}")
        return 0.5
    def get_metrics(self) -> ROIMetrics:
        return self.roi_metrics
    def reset_tracking(self):
        self.current_roi = None
        self.roi_history.clear()
        self.tracker = None
        self.roi_metrics = ROIMetrics()
        logger.info("ROI tracking reset")
class PhysiologicalSignalExtractor:
    def __init__(
        self,
        method: SignalExtractionMethod = SignalExtractionMethod.CHROM_METHOD,
        sampling_rate: float = 30.0,
        signal_length_seconds: float = 10.0,
    ):
        self.method = method
        self.sampling_rate = sampling_rate
        self.buffer_size = int(sampling_rate * signal_length_seconds)
        self.red_buffer = deque(maxlen=self.buffer_size)
        self.green_buffer = deque(maxlen=self.buffer_size)
        self.blue_buffer = deque(maxlen=self.buffer_size)
        self.signal_buffer = deque(maxlen=self.buffer_size)
        self.init_filters()
        logger.info(
            f"PhysiologicalSignalExtractor initialized: {method.value}, {sampling_rate}Hz, {signal_length_seconds}s buffer"
        )
    def init_filters(self):
        nyquist = self.sampling_rate / 2.0
        low_freq = 0.7 / nyquist
        high_freq = 4.0 / nyquist
        self.bp_filter_b, self.bp_filter_a = scipy.signal.butter(
            4, [low_freq, high_freq], btype="band"
        )
        self.notch_filters = []
        self.ma_window = int(self.sampling_rate * 2)
    @performance_timer("extract_signal")
    def extract_signal(self, roi_region: np.ndarray) -> Optional[PhysiologicalSignal]:
        try:
            mean_rgb = self._calculate_mean_rgb(roi_region)
            if mean_rgb is None:
                return None
            self.red_buffer.append(mean_rgb[2])
            self.green_buffer.append(mean_rgb[1])
            self.blue_buffer.append(mean_rgb[0])
            if len(self.red_buffer) < self.sampling_rate * 2:
                return None
            if self.method == SignalExtractionMethod.MEAN_RGB:
                signal = self._extract_mean_rgb()
            elif self.method == SignalExtractionMethod.CHROM_METHOD:
                signal = self._extract_chrominance()
            elif self.method == SignalExtractionMethod.POS_METHOD:
                signal = self._extract_pos()
            elif self.method == SignalExtractionMethod.ICA_SEPARATION:
                signal = self._extract_ica()
            elif self.method == SignalExtractionMethod.PCA_PROJECTION:
                signal = self._extract_pca()
            elif self.method == SignalExtractionMethod.ADAPTIVE_HYBRID:
                signal = self._extract_adaptive()
            else:
                signal = self._extract_mean_rgb()
            if signal is not None:
                signal = self._post_process_signal(signal)
                phys_signal = PhysiologicalSignal(
                    signal_data=signal,
                    sampling_rate=self.sampling_rate,
                    timestamp=time.time(),
                    extraction_method=self.method.value,
                )
                self._calculate_quality_metrics(phys_signal, roi_region)
                return phys_signal
        except Exception as e:
            logger.error(f"Signal extraction failed: {e}")
        return None
    def _calculate_mean_rgb(self, roi_region: np.ndarray) -> Optional[np.ndarray]:
        if roi_region.size == 0:
            return None
        mean_values = np.mean(roi_region.reshape(-1, 3), axis=0)
        return mean_values
    def _extract_mean_rgb(self) -> Optional[np.ndarray]:
        if len(self.green_buffer) < 10:
            return None
        return np.array(list(self.green_buffer))
    def _extract_chrominance(self) -> Optional[np.ndarray]:
        if len(self.red_buffer) < 10:
            return None
        R = np.array(list(self.red_buffer))
        G = np.array(list(self.green_buffer))
        B = np.array(list(self.blue_buffer))
        R_norm = R / np.mean(R)
        G_norm = G / np.mean(G)
        B_norm = B / np.mean(B)
        X = 3 * R_norm - 2 * G_norm
        Y = 1.5 * R_norm + G_norm - 1.5 * B_norm
        alpha = np.std(X) / np.std(Y)
        pulse_signal = X - alpha * Y
        return pulse_signal
    def _extract_pos(self) -> Optional[np.ndarray]:
        if len(self.red_buffer) < 10:
            return None
        R = np.array(list(self.red_buffer))
        G = np.array(list(self.green_buffer))
        B = np.array(list(self.blue_buffer))
        R_norm = R / np.mean(R) - 1
        G_norm = G / np.mean(G) - 1
        B_norm = B / np.mean(B) - 1
        H = np.array([R_norm, G_norm, B_norm])
        C = np.array([[0, 1, -1], [-2, 1, 1]])
        S = np.dot(C, H)
        pulse_signal = S[0] - np.std(S[0]) / np.std(S[1]) * S[1]
        return pulse_signal
    def _extract_ica(self) -> Optional[np.ndarray]:
        try:
            from sklearn.decomposition import FastICA
            if len(self.red_buffer) < self.sampling_rate * 5:
                return self._extract_chrominance()
            signals = np.array(
                [list(self.red_buffer), list(self.green_buffer), list(self.blue_buffer)]
            ).T
            ica = FastICA(n_components=3, random_state=42)
            components = ica.fit_transform(signals)
            best_component = 0
            best_power = 0
            for i in range(components.shape[1]):
                component = components[:, i]
                freqs, psd = scipy.signal.welch(component, fs=self.sampling_rate)
                hr_mask = (freqs >= 0.7) & (freqs <= 4.0)
                hr_power = np.sum(psd[hr_mask])
                if hr_power > best_power:
                    best_power = hr_power
                    best_component = i
            return components[:, best_component]
        except ImportError:
            logger.warning("scikit-learn not available for ICA, using CHROM method")
            return self._extract_chrominance()
        except Exception as e:
            logger.warning(f"ICA extraction failed: {e}, using fallback")
            return self._extract_chrominance()
    def _extract_pca(self) -> Optional[np.ndarray]:
        try:
            from sklearn.decomposition import PCA
            if len(self.red_buffer) < 10:
                return None
            signals = np.array(
                [list(self.red_buffer), list(self.green_buffer), list(self.blue_buffer)]
            ).T
            pca = PCA(n_components=3)
            components = pca.fit_transform(signals)
            return components[:, 0]
        except ImportError:
            logger.warning("scikit-learn not available for PCA, using CHROM method")
            return self._extract_chrominance()
        except Exception as e:
            logger.warning(f"PCA extraction failed: {e}, using fallback")
            return self._extract_chrominance()
    def _extract_adaptive(self) -> Optional[np.ndarray]:
        methods = [self._extract_chrominance, self._extract_pos, self._extract_mean_rgb]
        best_signal = None
        best_snr = -np.inf
        for method in methods:
            try:
                signal = method()
                if signal is not None and len(signal) > 0:
                    snr = self._calculate_snr(signal)
                    if snr > best_snr:
                        best_snr = snr
                        best_signal = signal
            except Exception as e:
                logger.debug(f"Adaptive method failed: {e}")
                continue
        return best_signal
    def _post_process_signal(self, signal: np.ndarray) -> np.ndarray:
        try:
            signal = signal - np.mean(signal)
            if len(signal) >= max(len(self.bp_filter_a), len(self.bp_filter_b)) * 3:
                signal = scipy.signal.filtfilt(
                    self.bp_filter_b, self.bp_filter_a, signal
                )
            signal = scipy.signal.detrend(signal)
            if np.std(signal) > 0:
                signal = (signal - np.mean(signal)) / np.std(signal)
            return signal
        except Exception as e:
            logger.warning(f"Signal post-processing failed: {e}")
            return signal
    def _calculate_quality_metrics(
        self, phys_signal: PhysiologicalSignal, roi_region: np.ndarray
    ):
        try:
            signal = phys_signal.signal_data
            phys_signal.snr_db = self._calculate_snr(signal)
            phys_signal.signal_quality_index = self._calculate_sqi(signal)
            phys_signal.motion_artifacts = self._assess_motion_artifacts(roi_region)
            phys_signal.preprocessing_steps = [
                "mean_rgb_calculation",
                f"extraction_method_{self.method.value}",
                "bandpass_filtering",
                "detrending",
                "normalisation",
            ]
            phys_signal.spectral_features = self._calculate_spectral_features(signal)
        except Exception as e:
            logger.warning(f"Quality metrics calculation failed: {e}")
    def _calculate_snr(self, signal: np.ndarray) -> float:
        try:
            freqs, psd = scipy.signal.welch(signal, fs=self.sampling_rate)
            hr_mask = (freqs >= 0.7) & (freqs <= 4.0)
            signal_power = np.sum(psd[hr_mask])
            noise_mask = ~hr_mask
            noise_power = np.sum(psd[noise_mask])
            if noise_power > 0:
                snr_ratio = signal_power / noise_power
                return 10 * np.log10(snr_ratio)
        except Exception as e:
            logger.debug(f"SNR calculation failed: {e}")
        return 0.0
    def _calculate_sqi(self, signal: np.ndarray) -> float:
        try:
            freqs, psd = scipy.signal.welch(signal, fs=self.sampling_rate)
            hr_mask = (freqs >= 0.7) & (freqs <= 4.0)
            hr_power = np.sum(psd[hr_mask])
            total_power = np.sum(psd)
            spectral_concentration = hr_power / total_power if total_power > 0 else 0
            hr_psd = psd[hr_mask]
            if len(hr_psd) > 0:
                peak_prominence = (
                    np.max(hr_psd) / np.mean(hr_psd) if np.mean(hr_psd) > 0 else 0
                )
                peak_prominence = min(1.0, peak_prominence / 10.0)
            else:
                peak_prominence = 0
            sqi = 0.6 * spectral_concentration + 0.4 * peak_prominence
            return min(1.0, max(0.0, sqi))
        except Exception as e:
            logger.debug(f"SQI calculation failed: {e}")
            return 0.0
    def _assess_motion_artifacts(self, roi_region: np.ndarray) -> float:
        try:
            gray_roi = cv2.cvtColor(roi_region, cv2.COLOR_BGR2GRAY)
            grad_x = cv2.Sobel(gray_roi, cv2.CV_64F, 1, 0, ksize=3)
            grad_y = cv2.Sobel(gray_roi, cv2.CV_64F, 0, 1, ksize=3)
            grad_magnitude = np.sqrt(grad_x**2 + grad_y**2)
            motion_score = np.std(grad_magnitude) / 255.0
            return min(1.0, motion_score)
        except Exception as e:
            logger.debug(f"Motion assessment failed: {e}")
            return 0.5
    def _calculate_spectral_features(self, signal: np.ndarray) -> Dict:
        try:
            freqs, psd = scipy.signal.welch(signal, fs=self.sampling_rate)
            hr_mask = (freqs >= 0.7) & (freqs <= 4.0)
            hr_freqs = freqs[hr_mask]
            hr_psd = psd[hr_mask]
            features = {}
            if len(hr_psd) > 0:
                peak_idx = np.argmax(hr_psd)
                features["peak_frequency_hz"] = hr_freqs[peak_idx]
                features["estimated_hr_bpm"] = hr_freqs[peak_idx] * 60
                features["total_power"] = np.sum(psd)
                features["hr_band_power"] = np.sum(hr_psd)
                features["hr_power_ratio"] = (
                    features["hr_band_power"] / features["total_power"]
                )
                normalized_psd = psd / np.sum(psd)
                features["spectral_entropy"] = -np.sum(
                    normalized_psd * np.log2(normalized_psd + 1e-12)
                )
            return features
        except Exception as e:
            logger.debug(f"Spectral features calculation failed: {e}")
            return {}
def create_complete_pipeline(camera_indices: List[int] = [0, 1]) -> Dict:
    logger.info("Creating complete CV preprocessing pipeline")
    roi_detector = AdvancedROIDetector(
        method=ROIDetectionMethod.MEDIAPIPE_HANDS,
        tracking_enabled=True,
        stability_threshold=0.8,
    )
    signal_extractor = PhysiologicalSignalExtractor(
        method=SignalExtractionMethod.CHROM_METHOD,
        sampling_rate=30.0,
        signal_length_seconds=10.0,
    )
    pipeline = {
        "roi_detector": roi_detector,
        "signal_extractor": signal_extractor,
        "camera_indices": camera_indices,
        "initialized": True,
        "version": "1.0.0",
    }
    logger.info("CV preprocessing pipeline created successfully")
    return pipeline
if __name__ == "__main__":
    pipeline = create_complete_pipeline()
    logger.info(f"Pipeline components: {list(pipeline.keys())}")