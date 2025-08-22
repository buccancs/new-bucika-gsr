"""
Radiometric Temperature Processing for Topdon TC001 Thermal Camera

This module implements explicit radiometric temperature extraction from raw thermal
sensor data, matching the thesis documentation claims for temperature-units logging
and calibrated thermal measurements.
"""

import struct
import numpy as np
import logging
import csv
import json
import time
from typing import Optional, Dict, Tuple, List, Any
from dataclasses import dataclass
from pathlib import Path
import base64


@dataclass
class ThermalFrame:
    """Thermal frame data with radiometric temperature information."""
    frame_id: int
    timestamp_ms: int
    raw_thermal_data: np.ndarray  # 256x192 raw sensor values
    radiometric_temperatures: np.ndarray  # 256x192 calibrated temperatures in Celsius
    device_temperature: float  # Internal device temperature in Celsius
    emissivity: float  # Surface emissivity setting
    reflected_temperature: float  # Reflected temperature compensation
    atmospheric_temperature: float  # Atmospheric temperature compensation
    distance_meters: float  # Distance to object for atmospheric compensation
    humidity_percent: float  # Relative humidity for atmospheric correction


@dataclass
class ThermalCalibrationData:
    """Calibration parameters for radiometric temperature conversion."""
    # Planck's radiation law constants for thermal sensor
    planck_r1: float = 21106.77
    planck_r2: float = 0.012545258
    planck_b: float = 1501.0
    planck_f: float = 1.0
    planck_o: float = -7340.0
    
    # Atmospheric transmission parameters
    alpha1: float = 0.006569
    alpha2: float = 0.01262
    beta1: float = -0.002276
    beta2: float = -0.00667
    x: float = 1.9
    
    # Default environmental parameters
    default_emissivity: float = 0.95
    default_reflected_temp: float = 20.0
    default_atmospheric_temp: float = 20.0
    default_distance: float = 1.0
    default_humidity: float = 50.0


class RadiometricProcessor:
    """Processes raw thermal data to calibrated radiometric temperatures."""
    
    def __init__(self, calibration: Optional[ThermalCalibrationData] = None, 
                 logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.calibration = calibration or ThermalCalibrationData()
        self.frame_counter = 0
        
        # Thermal sensor specifications (Topdon TC001)
        self.sensor_width = 256
        self.sensor_height = 192
        self.temperature_range_min = -20.0  # Celsius
        self.temperature_range_max = 550.0  # Celsius
        
        self.logger.info("RadiometricProcessor initialized for Topdon TC001 (256x192)")
    
    def process_raw_frame(self, raw_data: bytes, timestamp_ms: int, 
                         device_temp: float = 25.0,
                         environmental_params: Optional[Dict[str, float]] = None) -> ThermalFrame:
        """
        Process raw thermal frame to extract radiometric temperatures.
        
        Args:
            raw_data: Raw 16-bit thermal sensor data (256*192*2 bytes)
            timestamp_ms: Frame timestamp in milliseconds
            device_temp: Internal device temperature in Celsius
            environmental_params: Environmental correction parameters
        
        Returns:
            ThermalFrame with calibrated radiometric temperatures
        """
        try:
            # Parse environmental parameters
            env = environmental_params or {}
            emissivity = env.get('emissivity', self.calibration.default_emissivity)
            reflected_temp = env.get('reflected_temperature', self.calibration.default_reflected_temp)
            atmospheric_temp = env.get('atmospheric_temperature', self.calibration.default_atmospheric_temp)
            distance = env.get('distance_meters', self.calibration.default_distance)
            humidity = env.get('humidity_percent', self.calibration.default_humidity)
            
            # Convert raw data to 16-bit unsigned integers
            raw_array = np.frombuffer(raw_data, dtype=np.uint16)
            raw_thermal_data = raw_array.reshape((self.sensor_height, self.sensor_width))
            
            # Apply radiometric calibration using Planck's law
            radiometric_temps = self._apply_radiometric_calibration(
                raw_thermal_data, device_temp, emissivity, reflected_temp,
                atmospheric_temp, distance, humidity
            )
            
            # Create thermal frame
            frame = ThermalFrame(
                frame_id=self.frame_counter,
                timestamp_ms=timestamp_ms,
                raw_thermal_data=raw_thermal_data,
                radiometric_temperatures=radiometric_temps,
                device_temperature=device_temp,
                emissivity=emissivity,
                reflected_temperature=reflected_temp,
                atmospheric_temperature=atmospheric_temp,
                distance_meters=distance,
                humidity_percent=humidity
            )
            
            self.frame_counter += 1
            
            self.logger.debug(f"Processed thermal frame {frame.frame_id}: "
                            f"temp range {radiometric_temps.min():.1f}degC to {radiometric_temps.max():.1f}degC")
            
            return frame
            
        except Exception as e:
            self.logger.error(f"Failed to process thermal frame: {e}")
            raise
    
    def _apply_radiometric_calibration(self, raw_data: np.ndarray, device_temp: float,
                                     emissivity: float, reflected_temp: float,
                                     atmospheric_temp: float, distance: float,
                                     humidity: float) -> np.ndarray:
        """
        Apply radiometric calibration to convert raw sensor values to temperatures.
        
        Uses Planck's radiation law with atmospheric and emissivity corrections.
        """
        # Convert raw sensor values to radiance
        calibrated_radiance = (raw_data.astype(np.float64) - self.calibration.planck_o) / self.calibration.planck_r2
        
        # Apply Planck's law to get object temperature
        # T = B / ln(R1 / (radiance - O) + F)
        object_radiance = calibrated_radiance - self.calibration.planck_o
        planck_term = self.calibration.planck_r1 / (object_radiance + 1e-6) + self.calibration.planck_f
        
        # Avoid log of negative or zero values
        planck_term = np.maximum(planck_term, 1e-6)
        object_temp_kelvin = self.calibration.planck_b / np.log(planck_term)
        
        # Convert to Celsius
        object_temp_celsius = object_temp_kelvin - 273.15
        
        # Apply atmospheric transmission correction
        atmospheric_transmission = self._calculate_atmospheric_transmission(
            distance, humidity, atmospheric_temp
        )
        
        # Apply emissivity correction
        # Corrected temperature accounts for reflected radiation and atmospheric effects
        reflected_radiance = self._temperature_to_radiance(reflected_temp + 273.15)
        atmospheric_radiance = self._temperature_to_radiance(atmospheric_temp + 273.15)
        
        # Correct for emissivity and atmospheric effects
        corrected_radiance = (calibrated_radiance - 
                            (1 - emissivity) * reflected_radiance - 
                            (1 - atmospheric_transmission) * atmospheric_radiance) / \
                           (emissivity * atmospheric_transmission)
        
        # Convert corrected radiance back to temperature
        corrected_planck_term = self.calibration.planck_r1 / (corrected_radiance + 1e-6) + self.calibration.planck_f
        corrected_planck_term = np.maximum(corrected_planck_term, 1e-6)
        corrected_temp_kelvin = self.calibration.planck_b / np.log(corrected_planck_term)
        corrected_temp_celsius = corrected_temp_kelvin - 273.15
        
        # Clamp to sensor range
        corrected_temp_celsius = np.clip(corrected_temp_celsius, 
                                       self.temperature_range_min, 
                                       self.temperature_range_max)
        
        return corrected_temp_celsius
    
    def _calculate_atmospheric_transmission(self, distance: float, humidity: float, 
                                         atmospheric_temp: float) -> float:
        """Calculate atmospheric transmission coefficient."""
        # Simplified atmospheric transmission model
        # In practice, this would use more sophisticated atmospheric models
        water_vapor_pressure = humidity / 100.0 * self._saturation_vapor_pressure(atmospheric_temp)
        
        # Atmospheric attenuation coefficients (wavelength dependent)
        attenuation = (self.calibration.alpha1 * np.sqrt(water_vapor_pressure) + 
                      self.calibration.alpha2 * np.sqrt(water_vapor_pressure)) * distance + \
                     (self.calibration.beta1 * np.sqrt(water_vapor_pressure) + 
                      self.calibration.beta2 * np.sqrt(water_vapor_pressure)) * distance**2
        
        transmission = np.exp(-attenuation)
        return np.clip(transmission, 0.1, 1.0)  # Reasonable bounds
    
    def _saturation_vapor_pressure(self, temp_celsius: float) -> float:
        """Calculate saturation vapor pressure using Magnus formula."""
        return 6.112 * np.exp((17.67 * temp_celsius) / (temp_celsius + 243.5))
    
    def _temperature_to_radiance(self, temp_kelvin: float) -> float:
        """Convert temperature to radiance using Planck's law."""
        return self.calibration.planck_r1 / (np.exp(self.calibration.planck_b / temp_kelvin) - self.calibration.planck_f) + self.calibration.planck_o
    
    def get_temperature_statistics(self, frame: ThermalFrame) -> Dict[str, float]:
        """Calculate temperature statistics for a thermal frame."""
        temps = frame.radiometric_temperatures
        return {
            'min_temperature_c': float(np.min(temps)),
            'max_temperature_c': float(np.max(temps)),
            'mean_temperature_c': float(np.mean(temps)),
            'std_temperature_c': float(np.std(temps)),
            'median_temperature_c': float(np.median(temps)),
            'q25_temperature_c': float(np.percentile(temps, 25)),
            'q75_temperature_c': float(np.percentile(temps, 75))
        }


class ThermalCsvLogger:
    """Logs thermal data to CSV format with radiometric temperatures."""
    
    def __init__(self, output_file: str, logger: Optional[logging.Logger] = None):
        self.output_file = Path(output_file)
        self.logger = logger or logging.getLogger(__name__)
        self.csv_file = None
        self.csv_writer = None
        self.frame_count = 0
        
        # CSV header matching thesis documentation format
        self.csv_header = [
            'timestamp_ms',
            'frame_id', 
            'min_temp_c',
            'max_temp_c',
            'mean_temp_c',
            'std_temp_c',
            'median_temp_c',
            'device_temp_c',
            'emissivity',
            'reflected_temp_c',
            'atmospheric_temp_c',
            'distance_m',
            'humidity_percent',
            'raw_data_base64',  # Base64 encoded raw thermal data
            'radiometric_data_base64'  # Base64 encoded calibrated temperatures
        ]
    
    def open(self):
        """Open CSV file for writing."""
        try:
            self.output_file.parent.mkdir(parents=True, exist_ok=True)
            self.csv_file = open(self.output_file, 'w', newline='')
            self.csv_writer = csv.writer(self.csv_file)
            self.csv_writer.writerow(self.csv_header)
            self.logger.info(f"Opened thermal CSV log: {self.output_file}")
        except Exception as e:
            self.logger.error(f"Failed to open thermal CSV log: {e}")
            raise
    
    def log_frame(self, frame: ThermalFrame, processor: RadiometricProcessor):
        """Log thermal frame to CSV."""
        if not self.csv_writer:
            raise RuntimeError("CSV logger not opened")
        
        try:
            # Calculate temperature statistics
            stats = processor.get_temperature_statistics(frame)
            
            # Encode thermal data as base64 for storage
            raw_data_bytes = frame.raw_thermal_data.astype(np.uint16).tobytes()
            radiometric_data_bytes = frame.radiometric_temperatures.astype(np.float32).tobytes()
            
            raw_data_base64 = base64.b64encode(raw_data_bytes).decode('utf-8')
            radiometric_data_base64 = base64.b64encode(radiometric_data_bytes).decode('utf-8')
            
            # Write CSV row
            row = [
                frame.timestamp_ms,
                frame.frame_id,
                stats['min_temperature_c'],
                stats['max_temperature_c'],
                stats['mean_temperature_c'],
                stats['std_temperature_c'],
                stats['median_temperature_c'],
                frame.device_temperature,
                frame.emissivity,
                frame.reflected_temperature,
                frame.atmospheric_temperature,
                frame.distance_meters,
                frame.humidity_percent,
                raw_data_base64,
                radiometric_data_base64
            ]
            
            self.csv_writer.writerow(row)
            self.frame_count += 1
            
            if self.frame_count % 100 == 0:
                self.csv_file.flush()  # Periodic flush for data safety
                self.logger.debug(f"Logged {self.frame_count} thermal frames")
                
        except Exception as e:
            self.logger.error(f"Failed to log thermal frame: {e}")
            raise
    
    def close(self):
        """Close CSV file."""
        if self.csv_file:
            self.csv_file.close()
            self.csv_file = None
            self.csv_writer = None
            self.logger.info(f"Closed thermal CSV log with {self.frame_count} frames")


def create_thermal_processor(calibration_file: Optional[str] = None,
                           logger: Optional[logging.Logger] = None) -> RadiometricProcessor:
    """Create radiometric processor with optional calibration file."""
    logger = logger or logging.getLogger(__name__)
    
    calibration = ThermalCalibrationData()
    
    if calibration_file and Path(calibration_file).exists():
        try:
            with open(calibration_file, 'r') as f:
                cal_data = json.load(f)
            
            # Update calibration parameters from file
            for key, value in cal_data.items():
                if hasattr(calibration, key):
                    setattr(calibration, key, value)
                    
            logger.info(f"Loaded thermal calibration from {calibration_file}")
        except Exception as e:
            logger.warning(f"Failed to load calibration file {calibration_file}: {e}")
            logger.info("Using default calibration parameters")
    
    return RadiometricProcessor(calibration, logger)


if __name__ == "__main__":
    # Example usage and testing
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    # Create processor
    processor = create_thermal_processor(logger=logger)
    
    # Simulate thermal frame processing
    logger.info("Testing radiometric temperature processing...")
    
    # Create synthetic raw thermal data (256x192x2 bytes)
    raw_size = 256 * 192 * 2
    synthetic_raw = np.random.randint(20000, 40000, size=256*192, dtype=np.uint16)
    raw_bytes = synthetic_raw.tobytes()
    
    # Process frame
    timestamp = int(time.time() * 1000)
    environmental_params = {
        'emissivity': 0.95,
        'reflected_temperature': 22.0,
        'atmospheric_temperature': 20.0,
        'distance_meters': 1.5,
        'humidity_percent': 45.0
    }
    
    frame = processor.process_raw_frame(
        raw_bytes, timestamp, device_temp=28.5, 
        environmental_params=environmental_params
    )
    
    # Display results
    stats = processor.get_temperature_statistics(frame)
    logger.info("Thermal frame processing results:")
    logger.info(f"  Frame ID: {frame.frame_id}")
    logger.info(f"  Temperature range: {stats['min_temperature_c']:.1f}degC to {stats['max_temperature_c']:.1f}degC")
    logger.info(f"  Mean temperature: {stats['mean_temperature_c']:.1f}degC +/- {stats['std_temperature_c']:.1f}degC")
    logger.info(f"  Device temperature: {frame.device_temperature:.1f}degC")
    logger.info(f"  Environmental: epsilon={frame.emissivity}, RH={frame.humidity_percent}%")
    
    # Test CSV logging
    csv_logger = ThermalCsvLogger("/tmp/thermal_test.csv", logger)
    csv_logger.open()
    csv_logger.log_frame(frame, processor)
    csv_logger.close()
    
    logger.info("CSV logging test completed - check /tmp/thermal_test.csv")
    logger.info("Radiometric processing test completed successfully")