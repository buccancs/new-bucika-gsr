import json
import logging
import queue
import socket
import threading
import time
from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Any, Callable, Dict, List, Optional, Tuple
import cv2
import numpy as np
from calibration_quality_assessment import (
    CalibrationQualityAssessment,
    CalibrationQualityResult,
    PatternType,
)
from real_time_calibration_feedback import MultiCameraCalibrationManager
class CalibrationPhase(Enum):
    INITIALIZATION = "initialization"
    PATTERN_DETECTION = "pattern_detection"
    DATA_COLLECTION = "data_collection"
    QUALITY_VALIDATION = "quality_validation"
    STEREO_CALIBRATION = "stereo_calibration"
    RESULT_AGGREGATION = "result_aggregation"
    COMPLETION = "completion"
@dataclass
class DeviceCalibrationInfo:
    device_id: str
    device_name: str
    device_type: str
    ip_address: str
    port: int
    cameras: List[str]
    capabilities: List[str]
    status: str = "disconnected"
    last_seen: float = 0.0
@dataclass
class CalibrationSession:
    session_id: str
    pattern_type: PatternType
    target_images_per_camera: int
    quality_threshold: float
    devices: List[DeviceCalibrationInfo]
    current_phase: CalibrationPhase = CalibrationPhase.INITIALIZATION
    start_time: float = 0.0
    collected_images: Dict[str, List[np.ndarray]] = field(default_factory=dict)
    calibration_results: Dict[str, Any] = field(default_factory=dict)
@dataclass
class CalibrationCommand:
    command_type: str
    session_id: str
    target_device: Optional[str] = None
    parameters: Dict[str, Any] = field(default_factory=dict)
    timestamp: float = 0.0
@dataclass
class CalibrationResponse:
    response_type: str
    session_id: str
    device_id: str
    success: bool
    data: Dict[str, Any] = field(default_factory=dict)
    error_message: Optional[str] = None
    timestamp: float = 0.0
class CrossDeviceCalibrationCoordinator:
    def __init__(self, logger=None, coordination_port=8910):
        self.logger = logger or logging.getLogger(__name__)
        self.coordination_port = coordination_port
        self.active_sessions: Dict[str, CalibrationSession] = {}
        self.connected_devices: Dict[str, DeviceCalibrationInfo] = {}
        self.server_socket: Optional[socket.socket] = None
        self.server_thread: Optional[threading.Thread] = None
        self.is_running = False
        self.client_connections: Dict[str, socket.socket] = {}
        self.quality_assessment = CalibrationQualityAssessment(logger=self.logger)
        self.local_camera_manager = MultiCameraCalibrationManager(logger=self.logger)
        self.session_callbacks: List[Callable[[str, CalibrationPhase], None]] = []
        self.device_callbacks: List[Callable[[DeviceCalibrationInfo], None]] = []
        self.command_queue = queue.Queue()
        self.response_queue = queue.Queue()
        self.processing_thread: Optional[threading.Thread] = None
        self.logger.info("CrossDeviceCalibrationCoordinator initialized")
    def start_coordination_server(self) -> bool:
        try:
            self.logger.info(
                f"Starting calibration coordination server on port {self.coordination_port}"
            )
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(("0.0.0.0", self.coordination_port))
            self.server_socket.listen(10)
            self.is_running = True
            self.server_thread = threading.Thread(
                target=self._server_loop, name="CalibrationCoordinator"
            )
            self.server_thread.daemon = True
            self.server_thread.start()
            self.processing_thread = threading.Thread(
                target=self._processing_loop, name="CalibrationProcessor"
            )
            self.processing_thread.daemon = True
            self.processing_thread.start()
            self.logger.info("Calibration coordination server started successfully")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start coordination server: {e}")
            return False
    def stop_coordination_server(self):
        try:
            self.logger.info("Stopping calibration coordination server")
            self.is_running = False
            for device_id, client_socket in self.client_connections.items():
                try:
                    client_socket.close()
                except:
                    pass
            self.client_connections.clear()
            if self.server_socket:
                self.server_socket.close()
                self.server_socket = None
            if self.server_thread and self.server_thread.is_alive():
                self.server_thread.join(timeout=5.0)
            if self.processing_thread and self.processing_thread.is_alive():
                self.processing_thread.join(timeout=5.0)
            self.logger.info("Calibration coordination server stopped")
        except Exception as e:
            self.logger.error(f"Error stopping coordination server: {e}")
    def create_calibration_session(
        self,
        session_id: str,
        pattern_type: PatternType,
        target_images_per_camera: int = 20,
        quality_threshold: float = 0.7,
    ) -> bool:
        try:
            if session_id in self.active_sessions:
                self.logger.error(f"Session {session_id} already exists")
                return False
            session = CalibrationSession(
                session_id=session_id,
                pattern_type=pattern_type,
                target_images_per_camera=target_images_per_camera,
                quality_threshold=quality_threshold,
                devices=list(self.connected_devices.values()),
                start_time=time.time(),
            )
            self.active_sessions[session_id] = session
            self.logger.info(f"Created calibration session: {session_id}")
            for callback in self.session_callbacks:
                try:
                    callback(session_id, CalibrationPhase.INITIALIZATION)
                except Exception as e:
                    self.logger.error(f"Error in session callback: {e}")
            return True
        except Exception as e:
            self.logger.error(f"Error creating calibration session: {e}")
            return False
    def start_calibration_session(self, session_id: str) -> bool:
        try:
            if session_id not in self.active_sessions:
                self.logger.error(f"Session {session_id} not found")
                return False
            session = self.active_sessions[session_id]
            session.current_phase = CalibrationPhase.PATTERN_DETECTION
            command = CalibrationCommand(
                command_type="start_calibration",
                session_id=session_id,
                parameters={
                    "pattern_type": session.pattern_type.value,
                    "target_images": session.target_images_per_camera,
                    "quality_threshold": session.quality_threshold,
                },
                timestamp=time.time(),
            )
            self._broadcast_command(command)
            self.logger.info(f"Started calibration session: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Error starting calibration session: {e}")
            return False
    def add_calibration_image(
        self,
        session_id: str,
        device_id: str,
        camera_id: str,
        image: np.ndarray,
        quality_result: CalibrationQualityResult,
    ) -> bool:
        try:
            if session_id not in self.active_sessions:
                self.logger.error(f"Session {session_id} not found")
                return False
            session = self.active_sessions[session_id]
            camera_key = f"{device_id}_{camera_id}"
            if camera_key not in session.collected_images:
                session.collected_images[camera_key] = []
            if quality_result.overall_quality_score < session.quality_threshold:
                self.logger.warning(
                    f"Image quality too low: {quality_result.overall_quality_score:.3f}"
                )
                return False
            session.collected_images[camera_key].append(image.copy())
            self.logger.info(
                f"Added calibration image for {camera_key}: {len(session.collected_images[camera_key])}/{session.target_images_per_camera}"
            )
            self._check_collection_progress(session_id)
            return True
        except Exception as e:
            self.logger.error(f"Error adding calibration image: {e}")
            return False
    def perform_stereo_calibration(
        self, session_id: str, camera_pair: Tuple[str, str]
    ) -> Optional[Dict[str, Any]]:
        try:
            validation_result = self._validate_stereo_calibration_inputs(session_id, camera_pair)
            if not validation_result:
                return None
            session, images1, images2 = validation_result
            self.logger.info(f"Performing stereo calibration for {camera_pair}")
            corner_extraction_result = self._extract_stereo_corner_points(session, images1, images2)
            if not corner_extraction_result:
                return None
            object_points, image_points1, image_points2 = corner_extraction_result
            calibration_matrices = self._perform_individual_camera_calibrations(
                object_points, image_points1, image_points2, images1[0].shape[:2][::-1]
            )
            if not calibration_matrices:
                return None
            stereo_params = self._perform_stereo_calculation(
                object_points, image_points1, image_points2, calibration_matrices
            )
            if not stereo_params:
                return None
            stereo_result = self._format_stereo_calibration_results(
                camera_pair, stereo_params, len(object_points)
            )
            camera1_key, camera2_key = camera_pair
            stereo_key = f"stereo_{camera1_key}_{camera2_key}"
            session.calibration_results[stereo_key] = stereo_result
            self.logger.info(
                f"Stereo calibration completed for {camera_pair}: error={stereo_params['ret']:.4f}, pairs={len(object_points)}"
            )
            return stereo_result
        except Exception as e:
            self.logger.error(f"Error performing stereo calibration: {e}")
            return None
    def _validate_stereo_calibration_inputs(
        self, session_id: str, camera_pair: Tuple[str, str]
    ) -> Optional[Tuple[Any, List, List]]:
        if session_id not in self.active_sessions:
            self.logger.error(f"Session {session_id} not found")
            return None
        session = self.active_sessions[session_id]
        camera1_key, camera2_key = camera_pair
        if (camera1_key not in session.collected_images or
            camera2_key not in session.collected_images):
            self.logger.error(f"Missing images for stereo calibration: {camera_pair}")
            return None
        images1 = session.collected_images[camera1_key]
        images2 = session.collected_images[camera2_key]
        if len(images1) != len(images2):
            self.logger.error(
                f"Mismatched image counts for stereo calibration: {len(images1)} vs {len(images2)}"
            )
            return None
        return session, images1, images2
    def _extract_stereo_corner_points(
        self, session: Any, images1: List, images2: List
    ) -> Optional[Tuple[List, List, List]]:
        object_points = []
        image_points1 = []
        image_points2 = []
        if session.pattern_type == PatternType.CHESSBOARD:
            pattern_size = (6, 9)
            objp = np.zeros((pattern_size[0] * pattern_size[1], 3), np.float32)
            objp[:, :2] = np.mgrid[0:pattern_size[0], 0:pattern_size[1]].T.reshape(-1, 2)
        else:
            self.logger.error(
                f"Stereo calibration not implemented for pattern type: {session.pattern_type}"
            )
            return None
        criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
        for img1, img2 in zip(images1, images2):
            gray1 = cv2.cvtColor(img1, cv2.COLOR_BGR2GRAY) if len(img1.shape) == 3 else img1
            gray2 = cv2.cvtColor(img2, cv2.COLOR_BGR2GRAY) if len(img2.shape) == 3 else img2
            found1, corners1 = cv2.findChessboardCorners(gray1, pattern_size)
            found2, corners2 = cv2.findChessboardCorners(gray2, pattern_size)
            if found1 and found2:
                corners1 = cv2.cornerSubPix(gray1, corners1, (11, 11), (-1, -1), criteria)
                corners2 = cv2.cornerSubPix(gray2, corners2, (11, 11), (-1, -1), criteria)
                object_points.append(objp)
                image_points1.append(corners1)
                image_points2.append(corners2)
        if len(object_points) < 10:
            self.logger.error(
                f"Insufficient valid image pairs for stereo calibration: {len(object_points)}"
            )
            return None
        return object_points, image_points1, image_points2
    def _perform_individual_camera_calibrations(
        self, object_points: List, image_points1: List, image_points2: List, img_shape: Tuple
    ) -> Optional[Dict[str, Any]]:
        try:
            ret1, mtx1, dist1, rvecs1, tvecs1 = cv2.calibrateCamera(
                object_points, image_points1, img_shape, None, None
            )
            ret2, mtx2, dist2, rvecs2, tvecs2 = cv2.calibrateCamera(
                object_points, image_points2, img_shape, None, None
            )
            return {
                'mtx1': mtx1, 'dist1': dist1,
                'mtx2': mtx2, 'dist2': dist2,
                'img_shape': img_shape
            }
        except Exception as e:
            self.logger.error(f"Error in individual camera calibrations: {e}")
            return None
    def _perform_stereo_calculation(
        self, object_points: List, image_points1: List, image_points2: List,
        calibration_matrices: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        try:
            flags = cv2.CALIB_FIX_INTRINSIC
            criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 100, 1e-05)
            ret, mtx1, dist1, mtx2, dist2, R, T, E, F = cv2.stereoCalibrate(
                object_points, image_points1, image_points2,
                calibration_matrices['mtx1'], calibration_matrices['dist1'],
                calibration_matrices['mtx2'], calibration_matrices['dist2'],
                calibration_matrices['img_shape'],
                criteria=criteria, flags=flags
            )
            R1, R2, P1, P2, Q, validPixROI1, validPixROI2 = cv2.stereoRectify(
                mtx1, dist1, mtx2, dist2, calibration_matrices['img_shape'], R, T
            )
            return {
                'ret': ret, 'mtx1': mtx1, 'dist1': dist1, 'mtx2': mtx2, 'dist2': dist2,
                'R': R, 'T': T, 'E': E, 'F': F,
                'R1': R1, 'R2': R2, 'P1': P1, 'P2': P2, 'Q': Q,
                'validPixROI1': validPixROI1, 'validPixROI2': validPixROI2
            }
        except Exception as e:
            self.logger.error(f"Error in stereo calibration calculation: {e}")
            return None
    def _format_stereo_calibration_results(
        self, camera_pair: Tuple[str, str], stereo_params: Dict[str, Any], num_pairs: int
    ) -> Dict[str, Any]:
        return {
            "camera_pair": camera_pair,
            "reprojection_error": stereo_params['ret'],
            "camera1_matrix": stereo_params['mtx1'].tolist(),
            "camera1_distortion": stereo_params['dist1'].tolist(),
            "camera2_matrix": stereo_params['mtx2'].tolist(),
            "camera2_distortion": stereo_params['dist2'].tolist(),
            "rotation_matrix": stereo_params['R'].tolist(),
            "translation_vector": stereo_params['T'].tolist(),
            "essential_matrix": stereo_params['E'].tolist(),
            "fundamental_matrix": stereo_params['F'].tolist(),
            "rectification_R1": stereo_params['R1'].tolist(),
            "rectification_R2": stereo_params['R2'].tolist(),
            "projection_P1": stereo_params['P1'].tolist(),
            "projection_P2": stereo_params['P2'].tolist(),
            "disparity_to_depth_Q": stereo_params['Q'].tolist(),
            "valid_pixel_ROI1": stereo_params['validPixROI1'],
            "valid_pixel_ROI2": stereo_params['validPixROI2'],
            "image_pairs_used": num_pairs,
            "timestamp": time.time(),
        }
    def get_session_status(self, session_id: str) -> Optional[Dict[str, Any]]:
        try:
            if session_id not in self.active_sessions:
                return None
            session = self.active_sessions[session_id]
            total_cameras = len(session.devices) * 2
            collected_cameras = len(session.collected_images)
            progress_per_camera = {}
            for camera_key, images in session.collected_images.items():
                progress_per_camera[camera_key] = {
                    "collected": len(images),
                    "target": session.target_images_per_camera,
                    "progress": len(images) / session.target_images_per_camera,
                }
            status = {
                "session_id": session_id,
                "current_phase": session.current_phase.value,
                "start_time": session.start_time,
                "elapsed_time": time.time() - session.start_time,
                "devices": len(session.devices),
                "total_cameras": total_cameras,
                "active_cameras": collected_cameras,
                "progress_per_camera": progress_per_camera,
                "calibration_results": list(session.calibration_results.keys()),
                "overall_progress": (
                    collected_cameras / total_cameras if total_cameras > 0 else 0
                ),
            }
            return status
        except Exception as e:
            self.logger.error(f"Error getting session status: {e}")
            return None
    def add_session_callback(self, callback: Callable[[str, CalibrationPhase], None]):
        self.session_callbacks.append(callback)
    def add_device_callback(self, callback: Callable[[DeviceCalibrationInfo], None]):
        self.device_callbacks.append(callback)
    def _server_loop(self):
        self.logger.info("Calibration coordination server loop started")
        try:
            while self.is_running:
                try:
                    self.server_socket.settimeout(1.0)
                    client_socket, client_addr = self.server_socket.accept()
                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, client_addr),
                        name=f"CalibrationClient-{client_addr[0]}",
                    )
                    client_thread.daemon = True
                    client_thread.start()
                except socket.timeout:
                    continue
                except socket.error as e:
                    if self.is_running:
                        self.logger.error(f"Socket error in server loop: {e}")
                    break
        except Exception as e:
            self.logger.error(f"Fatal error in server loop: {e}")
        finally:
            self.logger.info("Calibration coordination server loop ended")
    def _handle_client(
        self, client_socket: socket.socket, client_addr: Tuple[str, int]
    ):
        device_id = None
        try:
            while self.is_running:
                data = client_socket.recv(4096)
                if not data:
                    break
                try:
                    message = json.loads(data.decode("utf-8"))
                except json.JSONDecodeError:
                    self.logger.error(f"Invalid JSON from {client_addr}")
                    continue
                if message.get("type") == "device_registration":
                    device_id = self._handle_device_registration(
                        message, client_socket, client_addr
                    )
                elif message.get("type") == "calibration_response":
                    self._handle_calibration_response(message)
                elif message.get("type") == "image_data":
                    self._handle_image_data(message)
        except Exception as e:
            self.logger.error(f"Error handling client {client_addr}: {e}")
        finally:
            try:
                client_socket.close()
                if device_id and device_id in self.client_connections:
                    del self.client_connections[device_id]
                    if device_id in self.connected_devices:
                        self.connected_devices[device_id].status = "disconnected"
            except:
                pass
    def _handle_device_registration(
        self,
        message: Dict[str, Any],
        client_socket: socket.socket,
        client_addr: Tuple[str, int],
    ) -> Optional[str]:
        try:
            device_info = DeviceCalibrationInfo(
                device_id=message["device_id"],
                device_name=message["device_name"],
                device_type=message["device_type"],
                ip_address=client_addr[0],
                port=client_addr[1],
                cameras=message.get("cameras", []),
                capabilities=message.get("capabilities", []),
                status="connected",
                last_seen=time.time(),
            )
            self.connected_devices[device_info.device_id] = device_info
            self.client_connections[device_info.device_id] = client_socket
            self.logger.info(
                f"Device registered: {device_info.device_name} ({device_info.device_id})"
            )
            for callback in self.device_callbacks:
                try:
                    callback(device_info)
                except Exception as e:
                    self.logger.error(f"Error in device callback: {e}")
            return device_info.device_id
        except Exception as e:
            self.logger.error(f"Error handling device registration: {e}")
            return None
    def _handle_calibration_response(self, message: Dict[str, Any]):
        try:
            response = CalibrationResponse(
                response_type=message["response_type"],
                session_id=message["session_id"],
                device_id=message["device_id"],
                success=message["success"],
                data=message.get("data", {}),
                error_message=message.get("error_message"),
                timestamp=message.get("timestamp", time.time()),
            )
            self.response_queue.put(response)
        except Exception as e:
            self.logger.error(f"Error handling calibration response: {e}")
    def _handle_image_data(self, message: Dict[str, Any]):
        try:
            import base64
            image_data = base64.b64decode(message["image_data"])
            image_array = np.frombuffer(image_data, dtype=np.uint8)
            image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
            quality_data = message.get("quality_result", {})
            self.add_calibration_image(
                message["session_id"],
                message["device_id"],
                message["camera_id"],
                image,
                quality_data,
            )
        except Exception as e:
            self.logger.error(f"Error handling image data: {e}")
    def _processing_loop(self):
        while self.is_running:
            try:
                try:
                    response = self.response_queue.get(timeout=1.0)
                    self._process_calibration_response(response)
                except queue.Empty:
                    continue
            except Exception as e:
                self.logger.error(f"Error in processing loop: {e}")
                time.sleep(1.0)
    def _process_calibration_response(self, response: CalibrationResponse):
        try:
            self.logger.info(
                f"Processing response: {response.response_type} from {response.device_id}"
            )
            if response.response_type == "calibration_started":
                self._handle_calibration_started(response)
            elif response.response_type == "image_captured":
                self._handle_image_captured(response)
            elif response.response_type == "calibration_completed":
                self._handle_calibration_completed(response)
        except Exception as e:
            self.logger.error(f"Error processing calibration response: {e}")
    def _handle_calibration_started(self, response: CalibrationResponse):
        self.logger.info(f"Calibration started on device: {response.device_id}")
    def _handle_image_captured(self, response: CalibrationResponse):
        self.logger.info(f"Image captured on device: {response.device_id}")
    def _handle_calibration_completed(self, response: CalibrationResponse):
        self.logger.info(f"Calibration completed on device: {response.device_id}")
    def _broadcast_command(self, command: CalibrationCommand):
        try:
            command_json = json.dumps(asdict(command))
            for device_id, client_socket in self.client_connections.items():
                try:
                    client_socket.send(command_json.encode("utf-8"))
                except Exception as e:
                    self.logger.error(f"Error sending command to {device_id}: {e}")
        except Exception as e:
            self.logger.error(f"Error broadcasting command: {e}")
    def _check_collection_progress(self, session_id: str):
        try:
            session = self.active_sessions[session_id]
            all_complete = True
            for camera_key, images in session.collected_images.items():
                if len(images) < session.target_images_per_camera:
                    all_complete = False
                    break
            if (
                all_complete
                and session.current_phase == CalibrationPhase.DATA_COLLECTION
            ):
                session.current_phase = CalibrationPhase.STEREO_CALIBRATION
                self.logger.info(f"Data collection complete for session {session_id}")
                for callback in self.session_callbacks:
                    try:
                        callback(session_id, CalibrationPhase.STEREO_CALIBRATION)
                    except Exception as e:
                        self.logger.error(f"Error in session callback: {e}")
        except Exception as e:
            self.logger.error(f"Error checking collection progress: {e}")
    def cleanup(self):
        try:
            self.logger.info("Cleaning up CrossDeviceCalibrationCoordinator")
            self.stop_coordination_server()
            self.local_camera_manager.cleanup()
            self.active_sessions.clear()
            self.connected_devices.clear()
            self.logger.info("CrossDeviceCalibrationCoordinator cleanup completed")
        except Exception as e:
            self.logger.error(f"Error during cleanup: {e}")
if __name__ == "__main__":
    pass
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    coordinator = CrossDeviceCalibrationCoordinator()
    try:
        if coordinator.start_coordination_server():
            print("Calibration coordination server started")
            session_id = "test_calibration_session"
            coordinator.create_calibration_session(
                session_id,
                PatternType.CHESSBOARD,
                target_images_per_camera=10,
                quality_threshold=0.6,
            )
            print(f"Created calibration session: {session_id}")
            try:
                while True:
                    time.sleep(1)
                    status = coordinator.get_session_status(session_id)
                    if status:
                        print(f"Session progress: {status['overall_progress']:.1%}")
            except KeyboardInterrupt:
                print("\nShutting down...")
    finally:
        coordinator.cleanup()
        print("Coordinator stopped")