import json
import logging
import logging.handlers
import os
import sys
import threading
import time
import traceback
from datetime import datetime
from functools import wraps
from pathlib import Path
from typing import Any, Dict, Optional
class StructuredFormatter(logging.Formatter):
    def format(self, record):
        log_entry = {
            "timestamp": datetime.fromtimestamp(record.created).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "module": record.module,
            "function": record.funcName,
            "line": record.lineno,
            "thread": record.thread,
            "thread_name": record.threadName,
            "message": record.getMessage(),
        }
        if record.exc_info:
            log_entry["exception"] = {
                "type": record.exc_info[0].__name__,
                "message": str(record.exc_info[1]),
                "traceback": traceback.format_exception(*record.exc_info),
            }
        extra_fields = {}
        reserved_fields = {
            "name",
            "msg",
            "args",
            "levelname",
            "levelno",
            "pathname",
            "filename",
            "module",
            "lineno",
            "funcName",
            "created",
            "msecs",
            "relativeCreated",
            "thread",
            "threadName",
            "processName",
            "process",
            "getMessage",
            "exc_info",
            "exc_text",
            "stack_info",
            "asctime",
            "message",
        }
        for key, value in record.__dict__.items():
            if key not in reserved_fields:
                extra_fields[key] = value
        if extra_fields:
            log_entry["extra"] = extra_fields
        return json.dumps(log_entry, default=str)
class PerformanceMonitor:
    _timing_data = {}
    _lock = threading.Lock()
    @classmethod
    def start_timer(cls, operation: str, context: str = None) -> str:
        timer_id = f"{operation}_{int(time.time() * 1000000)}"
        if context:
            timer_id = f"{context}_{timer_id}"
        with cls._lock:
            cls._timing_data[timer_id] = {
                "operation": operation,
                "context": context,
                "start_time": time.perf_counter(),
                "thread": threading.current_thread().name,
            }
        return timer_id
    @classmethod
    def end_timer(cls, timer_id: str, logger: logging.Logger = None) -> float:
        with cls._lock:
            if timer_id not in cls._timing_data:
                if logger:
                    logger.warning(f"Timer {timer_id} not found")
                return 0.0
            timing_info = cls._timing_data.pop(timer_id)
        duration = time.perf_counter() - timing_info["start_time"]
        if logger:
            extra_info = {
                "duration_ms": duration * 1000,
                "operation": timing_info["operation"],
                "context": timing_info["context"],
                "timer_thread": timing_info["thread"],
            }
            logger.info(
                f"Operation '{timing_info['operation']}' completed in {duration:.3f}s",
                extra=extra_info,
            )
        return duration
    @classmethod
    def get_active_timers(cls) -> Dict[str, Dict[str, Any]]:
        with cls._lock:
            return cls._timing_data.copy()
class ColoredFormatter(logging.Formatter):
    COLOURS = {
        "DEBUG": "\x1b[36m",
        "INFO": "\x1b[32m",
        "WARNING": "\x1b[33m",
        "ERROR": "\x1b[31m",
        "CRITICAL": "\x1b[35m",
        "RESET": "\x1b[0m",
    }
    def format(self, record):
        original_levelname = record.levelname
        if record.levelname in self.COLOURS:
            record.levelname = f"{self.COLOURS[record.levelname]}{record.levelname}{self.COLOURS['RESET']}"
        formatted = super().format(record)
        record.levelname = original_levelname
        return formatted
class AppLogger:
    _initialized = False
    _root_logger = None
    _log_dir = None
    _performance_monitor = PerformanceMonitor()
    @classmethod
    def initialize(
        cls,
        log_level: str = "INFO",
        log_dir: Optional[str] = None,
        console_output: bool = True,
        file_output: bool = True,
        structured_logging: bool = True,
    ) -> None:
        if cls._initialized:
            return
        if log_dir is None:
            project_root = Path(__file__).parent.parent.parent
            log_dir = project_root / "logs"
        cls._log_dir = Path(log_dir)
        cls._log_dir.mkdir(parents=True, exist_ok=True)
        cls._root_logger = logging.getLogger()
        cls._root_logger.setLevel(getattr(logging, log_level.upper()))
        cls._root_logger.handlers.clear()
        if console_output:
            console_handler = logging.StreamHandler(sys.stdout)
            console_formatter = ColoredFormatter(
                "%(asctime)s [%(levelname)s] %(name)s: %(message)s", datefmt="%H:%M:%S"
            )
            console_handler.setFormatter(console_formatter)
            console_handler.setLevel(getattr(logging, log_level.upper()))
            cls._root_logger.addHandler(console_handler)
        if file_output:
            app_log_file = cls._log_dir / "application.log"
            file_handler = logging.handlers.RotatingFileHandler(
                app_log_file, maxBytes=10 * 1024 * 1024, backupCount=5
            )
            file_formatter = logging.Formatter(
                "%(asctime)s [%(levelname)s] %(name)s:%(lineno)d - %(message)s",
                datefmt="%Y-%m-%d %H:%M:%S",
            )
            file_handler.setFormatter(file_formatter)
            file_handler.setLevel(logging.DEBUG)
            cls._root_logger.addHandler(file_handler)
            error_log_file = cls._log_dir / "errors.log"
            error_handler = logging.handlers.RotatingFileHandler(
                error_log_file, maxBytes=5 * 1024 * 1024, backupCount=3
            )
            error_handler.setFormatter(file_formatter)
            error_handler.setLevel(logging.ERROR)
            cls._root_logger.addHandler(error_handler)
            if structured_logging:
                json_log_file = cls._log_dir / "structured.log"
                json_handler = logging.handlers.RotatingFileHandler(
                    json_log_file, maxBytes=20 * 1024 * 1024, backupCount=3
                )
                json_handler.setFormatter(StructuredFormatter())
                json_handler.setLevel(logging.DEBUG)
                cls._root_logger.addHandler(json_handler)
        cls._initialized = True
        logger = cls.get_logger("AppLogger")
        logger.info("=== Multi-Sensor Recording System Logging Initialized ===")
        logger.info(f"Log level: {log_level}")
        logger.info(f"Log directory: {cls._log_dir}")
        logger.info(f"Console output: {console_output}")
        logger.info(f"File output: {file_output}")
        logger.info(f"Structured logging: {structured_logging}")
    @classmethod
    def get_logger(cls, name: str) -> logging.Logger:
        if not cls._initialized:
            cls.initialize()
        return logging.getLogger(name)
    @classmethod
    def start_performance_timer(cls, operation: str, context: str = None) -> str:
        return cls._performance_monitor.start_timer(operation, context)
    @classmethod
    def end_performance_timer(cls, timer_id: str, logger_name: str = None) -> float:
        logger = cls.get_logger(logger_name or "PerformanceMonitor")
        return cls._performance_monitor.end_timer(timer_id, logger)
    @classmethod
    def log_memory_usage(cls, context: str, logger_name: str = None) -> None:
        try:
            import psutil
            process = psutil.Process()
            memory_info = process.memory_info()
            logger = cls.get_logger(logger_name or "MemoryMonitor")
            extra_info = {
                "context": context,
                "rss_mb": memory_info.rss / 1024 / 1024,
                "vms_mb": memory_info.vms / 1024 / 1024,
                "percent": process.memory_percent(),
            }
            logger.info(
                f"Memory usage at {context}: RSS={memory_info.rss / 1024 / 1024:.1f}MB, VMS={memory_info.vms / 1024 / 1024:.1f}MB ({process.memory_percent():.1f}%)",
                extra=extra_info,
            )
        except ImportError:
            logger = cls.get_logger(logger_name or "MemoryMonitor")
            logger.debug("psutil not available for memory monitoring")
        except Exception as e:
            logger = cls.get_logger(logger_name or "MemoryMonitor")
            logger.error(f"Error getting memory usage: {e}")
    @classmethod
    def set_level(cls, level: str) -> None:
        if cls._root_logger:
            cls._root_logger.setLevel(getattr(logging, level.upper()))
            for handler in cls._root_logger.handlers:
                if (
                    isinstance(handler, logging.StreamHandler)
                    and handler.stream == sys.stdout
                ):
                    handler.setLevel(getattr(logging, level.upper()))
    @classmethod
    def get_log_dir(cls) -> Optional[Path]:
        return cls._log_dir
    @classmethod
    def get_active_timers(cls) -> Dict[str, Dict[str, Any]]:
        return cls._performance_monitor.get_active_timers()
    @classmethod
    def export_logs(
        cls, start_date: datetime, end_date: datetime, output_format: str = "json"
    ) -> str:
        import csv
        import gzip
        import json
        import os
        if not cls._log_dir:
            raise RuntimeError("Logging not initialized")
        try:
            exports_dir = cls._log_dir / "exports"
            exports_dir.mkdir(exist_ok=True)
            export_filename = f"logs_export_{start_date.strftime('%Y%m%d')}_{end_date.strftime('%Y%m%d')}.{output_format}"
            export_path = exports_dir / export_filename
            log_entries = []
            for log_file in cls._log_dir.glob("**/*.log"):
                try:
                    file_mtime = datetime.fromtimestamp(log_file.stat().st_mtime)
                    if start_date <= file_mtime <= end_date:
                        with open(log_file, "r", encoding="utf-8") as f:
                            for line in f:
                                line = line.strip()
                                if line:
                                    try:
                                        log_entry = json.loads(line)
                                        log_entries.append(log_entry)
                                    except json.JSONDecodeError:
                                        log_entries.append(
                                            {
                                                "timestamp": file_mtime.isoformat(),
                                                "level": "INFO",
                                                "message": line,
                                                "source_file": log_file.name,
                                            }
                                        )
                except Exception as e:
                    logger = cls.get_logger("LogExport")
                    logger.error(f"Error reading log file {log_file}: {e}")
            if output_format.lower() == "json":
                with open(export_path, "w", encoding="utf-8") as f:
                    json.dump(log_entries, f, indent=2, default=str)
            elif output_format.lower() == "csv":
                if log_entries:
                    all_keys = set()
                    for entry in log_entries:
                        all_keys.update(entry.keys())
                    with open(export_path, "w", newline="", encoding="utf-8") as f:
                        writer = csv.DictWriter(f, fieldnames=sorted(all_keys))
                        writer.writeheader()
                        writer.writerows(log_entries)
            elif output_format.lower() == "txt":
                with open(export_path, "w", encoding="utf-8") as f:
                    for entry in log_entries:
                        if isinstance(entry, dict):
                            timestamp = entry.get("timestamp", "Unknown")
                            level = entry.get("level", "INFO")
                            message = entry.get("message", str(entry))
                            f.write(f"[{timestamp}] {level}: {message}\n")
                        else:
                            f.write(f"{entry}\n")
            if export_path.stat().st_size > 1024 * 1024:
                compressed_path = export_path.with_suffix(export_path.suffix + ".gz")
                with open(export_path, "rb") as f_in:
                    with gzip.open(compressed_path, "wb") as f_out:
                        f_out.writelines(f_in)
                export_path.unlink()
                export_path = compressed_path
            logger = cls.get_logger("LogExport")
            logger.info(f"Logs exported to: {export_path}")
            return str(export_path)
        except Exception as e:
            logger = cls.get_logger("LogExport")
            logger.error(f"Error exporting logs: {e}")
            return ""
    @classmethod
    def cleanup_old_logs(cls, retention_days: int = 30) -> Dict[str, Any]:
        import gzip
        import shutil
        from datetime import timedelta
        if not cls._log_dir:
            raise RuntimeError("Logging not initialized")
        cleanup_report = {
            "removed_files": [],
            "compressed_files": [],
            "errors": [],
            "total_space_freed": 0,
        }
        try:
            cutoff_date = datetime.now() - timedelta(days=retention_days)
            archive_cutoff = datetime.now() - timedelta(days=7)
            for log_file in cls._log_dir.glob("**/*.log*"):
                if "exports" in str(log_file):
                    continue
                try:
                    file_mtime = datetime.fromtimestamp(log_file.stat().st_mtime)
                    file_size = log_file.stat().st_size
                    if file_mtime < cutoff_date:
                        log_file.unlink()
                        cleanup_report["removed_files"].append(str(log_file))
                        cleanup_report["total_space_freed"] += file_size
                    elif file_mtime < archive_cutoff and log_file.suffix == ".log":
                        compressed_path = log_file.with_suffix(".log.gz")
                        with open(log_file, "rb") as f_in:
                            with gzip.open(compressed_path, "wb") as f_out:
                                shutil.copyfileobj(f_in, f_out)
                        log_file.unlink()
                        cleanup_report["compressed_files"].append(str(compressed_path))
                        space_saved = file_size - compressed_path.stat().st_size
                        cleanup_report["total_space_freed"] += space_saved
                except Exception as e:
                    error_msg = f"Error processing log file {log_file}: {e}"
                    cleanup_report["errors"].append(error_msg)
            logger = cls.get_logger("LogCleanup")
            logger.info(
                f"Log cleanup completed. Freed {cleanup_report['total_space_freed']} bytes"
            )
            return cleanup_report
        except Exception as e:
            error_msg = f"Error during log cleanup: {e}"
            cleanup_report["errors"].append(error_msg)
            logger = cls.get_logger("LogCleanup")
            logger.error(error_msg)
            return cleanup_report
def get_logger(name: str) -> logging.Logger:
    return AppLogger.get_logger(name)
def performance_timer(operation_name: str = None, context: str = None):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            op_name = operation_name or func.__name__
            timer_id = AppLogger.start_performance_timer(op_name, context)
            try:
                result = func(*args, **kwargs)
                AppLogger.end_performance_timer(timer_id, func.__module__)
                return result
            except Exception as e:
                AppLogger.end_performance_timer(timer_id, func.__module__)
                raise
        return wrapper
    return decorator
def log_function_entry(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        logger = get_logger(func.__module__)
        arg_info = f"args={len(args)}" if args else "no args"
        kwarg_info = f"kwargs={list(kwargs.keys())}" if kwargs else "no kwargs"
        logger.debug(f"-> Entering {func.__name__}({arg_info}, {kwarg_info})")
        try:
            result = func(*args, **kwargs)
            logger.debug(f"<- Exiting {func.__name__} successfully")
            return result
        except Exception as e:
            logger.error(
                f"[FAIL] Exception in {func.__name__}: {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise
    return wrapper
def log_method_entry(method):
    @wraps(method)
    def wrapper(self, *args, **kwargs):
        logger = get_logger(self.__class__.__module__)
        arg_info = f"args={len(args)}" if args else "no args"
        kwarg_info = f"kwargs={list(kwargs.keys())}" if kwargs else "no kwargs"
        logger.debug(
            f"-> Entering {self.__class__.__name__}.{method.__name__}({arg_info}, {kwarg_info})"
        )
        try:
            result = method(self, *args, **kwargs)
            logger.debug(
                f"<- Exiting {self.__class__.__name__}.{method.__name__} successfully"
            )
            return result
        except Exception as e:
            logger.error(
                f"[FAIL] Exception in {self.__class__.__name__}.{method.__name__}: {type(e).__name__}: {e}",
                exc_info=True,
            )
            raise
    return wrapper
def log_exception_context(logger_name: str = None):
    class ExceptionLogger:
        def __init__(self, logger_name):
            self.logger = get_logger(logger_name or __name__)
        def __enter__(self):
            return self
        def __exit__(self, exc_type, exc_val, exc_tb):
            if exc_type is not None:
                self.logger.error(
                    f"Exception occurred: {exc_type.__name__}: {exc_val}", exc_info=True
                )
            return False
    return ExceptionLogger(logger_name)
def log_memory_usage(context: str, logger_name: str = None):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            AppLogger.log_memory_usage(
                f"{context} - before {func.__name__}", logger_name
            )
            try:
                result = func(*args, **kwargs)
                AppLogger.log_memory_usage(
                    f"{context} - after {func.__name__}", logger_name
                )
                return result
            except Exception:
                AppLogger.log_memory_usage(
                    f"{context} - after {func.__name__} (exception)", logger_name
                )
                raise
        return wrapper
    return decorator
AppLogger.initialize()