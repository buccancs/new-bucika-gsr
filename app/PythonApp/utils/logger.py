"""
Simplified logging interface - delegates to the comprehensive logging_config module.

This module provides a simplified interface to maintain backwards compatibility
while using the unified logging system from logging_config.py.
"""

from .logging_config import AppLogger, get_logger as get_unified_logger
from enum import Enum

class LogLevel(Enum):
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARNING = "WARNING"
    ERROR = "ERROR"
    CRITICAL = "CRITICAL"

# Delegate to the unified logging system
class LoggerManager:
    """Simplified interface that delegates to AppLogger."""
    
    def __init__(self, log_directory="logs", max_file_size_mb=10, backup_count=5):
        # Initialize the unified logging system
        AppLogger.initialize(
            log_dir=log_directory,
            console_output=True,
            file_output=True,
            structured_logging=True
        )
        self.log_directory = log_directory
        self.max_file_size_mb = max_file_size_mb
        self.backup_count = backup_count
    
    def get_logger(self, name):
        """Get a logger instance using the unified system."""
        return AppLogger.get_logger(name)
    
    def log_structured(self, logger_name, level, message, **kwargs):
        """Log structured data using the unified system."""
        logger = self.get_logger(logger_name)
        if logger:
            if level == LogLevel.DEBUG:
                logger.debug(message, extra=kwargs)
            elif level == LogLevel.INFO:
                logger.info(message, extra=kwargs)
            elif level == LogLevel.WARNING:
                logger.warning(message, extra=kwargs)
            elif level == LogLevel.ERROR:
                logger.error(message, extra=kwargs)
            elif level == LogLevel.CRITICAL:
                logger.critical(message, extra=kwargs)
    
    def log_performance(self, operation, duration_ms, **metadata):
        """Log performance data using the unified system."""
        timer_id = AppLogger.start_performance_timer(operation)
        # Simulate the duration for consistency
        import time
        time.sleep(duration_ms / 1000.0)
        AppLogger.end_performance_timer(timer_id, "performance")
    
    def export_logs(self, start_date, end_date, output_format="json"):
        """Export logs using the unified system."""
        return AppLogger.export_logs(start_date, end_date, output_format)
    
    def cleanup_old_logs(self, retention_days=30):
        """Clean up old logs using the unified system."""
        return AppLogger.cleanup_old_logs(retention_days)

# Global instance for backwards compatibility
logger_manager = None

def get_logger_manager():
    """Get the global logger manager instance."""
    global logger_manager
    if logger_manager is None:
        logger_manager = LoggerManager()
    return logger_manager

# Convenience functions that delegate to the unified system
def log_info(logger_name, message, **kwargs):
    get_unified_logger(logger_name).info(message, extra=kwargs)

def log_error(logger_name, message, **kwargs):
    get_unified_logger(logger_name).error(message, extra=kwargs)

def log_debug(logger_name, message, **kwargs):
    get_unified_logger(logger_name).debug(message, extra=kwargs)

def log_warning(logger_name, message, **kwargs):
    get_unified_logger(logger_name).warning(message, extra=kwargs)

def log_critical(logger_name, message, **kwargs):
    get_unified_logger(logger_name).critical(message, extra=kwargs)