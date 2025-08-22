import json
import shutil
import threading
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional
import psutil
from PyQt5.QtCore import QObject, pyqtSignal
class SessionRecoveryManager(QObject):
    disk_space_warning = pyqtSignal(str, float)
    disk_space_critical = pyqtSignal(str, float)
    session_recovered = pyqtSignal(str, str)
    file_corruption_detected = pyqtSignal(str, str)
    backup_completed = pyqtSignal(str, str)
    system_health_alert = pyqtSignal(str, str)
    def __init__(
        self, base_sessions_dir: str = "recordings", backup_dir: Optional[str] = None
    ):
        super().__init__()
        self.base_sessions_dir = Path(base_sessions_dir)
        self.backup_dir = Path(backup_dir) if backup_dir else None
        self.recovery_log_file = self.base_sessions_dir / "recovery.log"
        self.disk_warning_threshold_gb = 5.0
        self.disk_critical_threshold_gb = 1.0
        self.max_session_age_days = 30
        self.backup_enabled = self.backup_dir is not None
        self.monitoring_active = False
        self.monitoring_thread = None
        self.stop_monitoring = threading.Event()
        self.init_recovery_system()
        print(f"[DEBUG_LOG] SessionRecoveryManager initialized")
        print(f"[DEBUG_LOG] Base directory: {self.base_sessions_dir}")
        print(f"[DEBUG_LOG] Backup directory: {self.backup_dir}")
        print(f"[DEBUG_LOG] Backup enabled: {self.backup_enabled}")
    def init_recovery_system(self):
        try:
            self.base_sessions_dir.mkdir(parents=True, exist_ok=True)
            if self.backup_dir:
                self.backup_dir.mkdir(parents=True, exist_ok=True)
            if not self.recovery_log_file.exists():
                self.log_recovery_event("system_init", "Recovery system initialized")
            print("[DEBUG_LOG] Recovery system initialized successfully")
        except Exception as e:
            print(f"[DEBUG_LOG] Failed to initialize recovery system: {e}")
    def start_monitoring(self):
        if self.monitoring_active:
            print("[DEBUG_LOG] Monitoring already active")
            return
        self.monitoring_active = True
        self.stop_monitoring.clear()
        self.monitoring_thread = threading.Thread(
            target=self._monitoring_loop, daemon=True
        )
        self.monitoring_thread.start()
        self.log_recovery_event("monitoring_start", "Background monitoring started")
        print("[DEBUG_LOG] Background monitoring started")
    def stop_monitoring(self):
        if not self.monitoring_active:
            return
        self.monitoring_active = False
        self.stop_monitoring.set()
        if self.monitoring_thread and self.monitoring_thread.is_alive():
            self.monitoring_thread.join(timeout=5.0)
        self.log_recovery_event("monitoring_stop", "Background monitoring stopped")
        print("[DEBUG_LOG] Background monitoring stopped")
    def _monitoring_loop(self):
        while not self.stop_monitoring.is_set():
            try:
                self.check_disk_space()
                self.scan_for_corrupted_files()
                self.auto_cleanup_old_sessions()
                if self.stop_monitoring.wait(60):
                    break
            except Exception as e:
                self.log_recovery_event(
                    "monitoring_error", f"Monitoring error: {str(e)}"
                )
                print(f"[DEBUG_LOG] Monitoring error: {e}")
    def check_disk_space(self):
        try:
            usage = psutil.disk_usage(str(self.base_sessions_dir))
            available_gb = usage.free / 1024**3
            if available_gb < self.disk_critical_threshold_gb:
                self.disk_space_critical.emit(str(self.base_sessions_dir), available_gb)
                self.log_recovery_event(
                    "disk_critical",
                    f"Critical disk space: {available_gb:.1f}GB available",
                )
            elif available_gb < self.disk_warning_threshold_gb:
                self.disk_space_warning.emit(str(self.base_sessions_dir), available_gb)
                self.log_recovery_event(
                    "disk_warning", f"Low disk space: {available_gb:.1f}GB available"
                )
            if self.backup_dir and self.backup_dir.exists():
                backup_usage = psutil.disk_usage(str(self.backup_dir))
                backup_available_gb = backup_usage.free / 1024**3
                if backup_available_gb < self.disk_critical_threshold_gb:
                    self.system_health_alert.emit(
                        "backup_disk_critical",
                        f"Backup disk critical: {backup_available_gb:.1f}GB",
                    )
        except Exception as e:
            self.log_recovery_event(
                "disk_check_error", f"Disk space check failed: {str(e)}"
            )
    def scan_for_corrupted_files(self):
        try:
            for session_folder in self.base_sessions_dir.iterdir():
                if not session_folder.is_dir():
                    continue
                for json_file in session_folder.glob("*.json"):
                    if self.is_file_corrupted(json_file):
                        self.file_corruption_detected.emit(
                            str(json_file), "JSON corruption detected"
                        )
                        self.attempt_file_repair(json_file)
        except Exception as e:
            self.log_recovery_event(
                "corruption_scan_error", f"Corruption scan failed: {str(e)}"
            )
    def is_file_corrupted(self, file_path: Path) -> bool:
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                json.load(f)
            return False
        except (json.JSONDecodeError, UnicodeDecodeError, IOError):
            return True
    def attempt_file_repair(self, file_path: Path) -> bool:
        try:
            backup_path = file_path.with_suffix(".json.corrupted")
            shutil.copy2(file_path, backup_path)
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            repaired_content = self.repair_json_content(content)
            if repaired_content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(repaired_content)
                if not self.is_file_corrupted(file_path):
                    self.log_recovery_event(
                        "file_repaired", f"Successfully repaired: {file_path}"
                    )
                    return True
            self.log_recovery_event(
                "file_repair_failed", f"Failed to repair: {file_path}"
            )
            return False
        except Exception as e:
            self.log_recovery_event(
                "file_repair_error", f"Repair error for {file_path}: {str(e)}"
            )
            return False
    def repair_json_content(self, content: str) -> Optional[str]:
        try:
            content = content.replace("\x00", "")
            brace_count = 0
            last_valid_pos = 0
            for i, char in enumerate(content):
                if char == "{":
                    brace_count += 1
                elif char == "}":
                    brace_count -= 1
                    if brace_count == 0:
                        last_valid_pos = i + 1
            if last_valid_pos > 0:
                truncated_content = content[:last_valid_pos]
                try:
                    json.loads(truncated_content)
                    return truncated_content
                except json.JSONDecodeError:
                    pass
            lines = content.split("\n")
            for i in range(len(lines) - 1, -1, -1):
                try:
                    partial_content = "\n".join(lines[:i])
                    if partial_content.strip().endswith(","):
                        partial_content = partial_content.rstrip().rstrip(",")
                    if partial_content.count("{") > partial_content.count("}"):
                        partial_content += "}"
                    json.loads(partial_content)
                    return partial_content
                except json.JSONDecodeError:
                    continue
            return None
        except Exception as e:
            print(f"[DEBUG_LOG] JSON repair error: {e}")
            return None
    def auto_cleanup_old_sessions(self):
        try:
            cutoff_date = datetime.now() - timedelta(days=self.max_session_age_days)
            cleaned_count = 0
            freed_space = 0
            for session_folder in self.base_sessions_dir.iterdir():
                if not session_folder.is_dir():
                    continue
                folder_mtime = datetime.fromtimestamp(session_folder.stat().st_mtime)
                if folder_mtime < cutoff_date:
                    folder_size = self.get_folder_size(session_folder)
                    if self.backup_enabled:
                        self.backup_session(session_folder)
                    shutil.rmtree(session_folder)
                    cleaned_count += 1
                    freed_space += folder_size
                    self.log_recovery_event(
                        "session_cleanup",
                        f"Cleaned up old session: {session_folder.name}",
                    )
            if cleaned_count > 0:
                freed_mb = freed_space / 1024**2
                self.log_recovery_event(
                    "cleanup_complete",
                    f"Cleaned {cleaned_count} sessions, freed {freed_mb:.1f}MB",
                )
        except Exception as e:
            self.log_recovery_event("cleanup_error", f"Auto cleanup failed: {str(e)}")
    def get_folder_size(self, folder_path: Path) -> int:
        total_size = 0
        try:
            for file_path in folder_path.rglob("*"):
                if file_path.is_file():
                    total_size += file_path.stat().st_size
        except Exception as e:
            print(f"[DEBUG_LOG] Error calculating folder size: {e}")
        return total_size
    def backup_session(self, session_folder: Path) -> bool:
        if not self.backup_enabled or not self.backup_dir:
            return False
        try:
            backup_path = self.backup_dir / session_folder.name
            shutil.copytree(session_folder, backup_path, dirs_exist_ok=True)
            self.backup_completed.emit(session_folder.name, str(backup_path))
            self.log_recovery_event(
                "backup_created", f"Backed up session: {session_folder.name}"
            )
            return True
        except Exception as e:
            self.log_recovery_event(
                "backup_error", f"Backup failed for {session_folder.name}: {str(e)}"
            )
            return False
    def recover_incomplete_sessions(self) -> List[Dict]:
        recovered_sessions = []
        try:
            for session_folder in self.base_sessions_dir.iterdir():
                if not session_folder.is_dir():
                    continue
                log_files = list(session_folder.glob("*_log.json"))
                for log_file in log_files:
                    if self.is_session_incomplete(log_file):
                        recovery_info = self.recover_session(log_file)
                        if recovery_info:
                            recovered_sessions.append(recovery_info)
                            self.session_recovered.emit(
                                recovery_info["session_id"],
                                recovery_info["recovery_details"],
                            )
            if recovered_sessions:
                self.log_recovery_event(
                    "sessions_recovered",
                    f"Recovered {len(recovered_sessions)} incomplete sessions",
                )
        except Exception as e:
            self.log_recovery_event(
                "recovery_error", f"Session recovery failed: {str(e)}"
            )
        return recovered_sessions
    def is_session_incomplete(self, log_file: Path) -> bool:
        try:
            with open(log_file, "r", encoding="utf-8") as f:
                session_data = json.load(f)
            if not session_data.get("end_time"):
                return True
            events = session_data.get("events", [])
            has_session_end = any(
                event.get("event") == "session_end" for event in events
            )
            return not has_session_end
        except Exception:
            return True
    def recover_session(self, log_file: Path) -> Optional[Dict]:
        try:
            with open(log_file, "r", encoding="utf-8") as f:
                session_data = json.load(f)
            session_id = session_data.get("session", "unknown")
            if not session_data.get("end_time"):
                end_time = datetime.fromtimestamp(log_file.stat().st_mtime)
                session_data["end_time"] = end_time.isoformat()
                start_time_str = session_data.get("start_time")
                if start_time_str:
                    try:
                        start_time = datetime.fromisoformat(
                            start_time_str.replace("Z", "+00:00")
                        )
                        duration = (
                            end_time - start_time.replace(tzinfo=None)
                        ).total_seconds()
                        session_data["duration"] = duration
                    except Exception:
                        session_data["duration"] = 0
            events = session_data.get("events", [])
            has_session_end = any(
                event.get("event") == "session_end" for event in events
            )
            if not has_session_end:
                end_event = {
                    "event": "session_end",
                    "time": datetime.now().strftime("%H:%M:%S.%f")[:-3],
                    "timestamp": datetime.now().isoformat(),
                    "recovered": True,
                }
                events.append(end_event)
                session_data["events"] = events
            session_data["status"] = "recovered"
            session_data["recovery_time"] = datetime.now().isoformat()
            with open(log_file, "w", encoding="utf-8") as f:
                json.dump(session_data, f, indent=2, ensure_ascii=False)
            recovery_info = {
                "session_id": session_id,
                "log_file": str(log_file),
                "recovery_time": session_data["recovery_time"],
                "recovery_details": "Added missing end_time and session_end event",
            }
            self.log_recovery_event(
                "session_recovered",
                f"Recovered session {session_id}: {recovery_info['recovery_details']}",
            )
            return recovery_info
        except Exception as e:
            self.log_recovery_event(
                "session_recovery_error",
                f"Failed to recover session {log_file}: {str(e)}",
            )
            return None
    def log_recovery_event(self, event_type: str, message: str):
        try:
            timestamp = datetime.now().isoformat()
            log_entry = f"[{timestamp}] {event_type}: {message}\n"
            with open(self.recovery_log_file, "a", encoding="utf-8") as f:
                f.write(log_entry)
        except Exception as e:
            print(f"[DEBUG_LOG] Failed to write recovery log: {e}")
    def get_recovery_statistics(self) -> Dict:
        try:
            stats = {
                "total_sessions": len(list(self.base_sessions_dir.glob("*/"))),
                "disk_usage": {},
                "backup_enabled": self.backup_enabled,
                "monitoring_active": self.monitoring_active,
                "recovery_log_size": 0,
            }
            if self.base_sessions_dir.exists():
                usage = psutil.disk_usage(str(self.base_sessions_dir))
                stats["disk_usage"]["main"] = {
                    "total_gb": usage.total / 1024**3,
                    "used_gb": usage.used / 1024**3,
                    "free_gb": usage.free / 1024**3,
                }
            if self.backup_dir and self.backup_dir.exists():
                backup_usage = psutil.disk_usage(str(self.backup_dir))
                stats["disk_usage"]["backup"] = {
                    "total_gb": backup_usage.total / 1024**3,
                    "used_gb": backup_usage.used / 1024**3,
                    "free_gb": backup_usage.free / 1024**3,
                }
            if self.recovery_log_file.exists():
                stats["recovery_log_size"] = self.recovery_log_file.stat().st_size
            return stats
        except Exception as e:
            print(f"[DEBUG_LOG] Failed to get recovery statistics: {e}")
            return {}
_recovery_manager_instance: Optional[SessionRecoveryManager] = None
def get_recovery_manager(
    base_sessions_dir: str = "recordings", backup_dir: Optional[str] = None
) -> SessionRecoveryManager:
    global _recovery_manager_instance
    if _recovery_manager_instance is None:
        _recovery_manager_instance = SessionRecoveryManager(
            base_sessions_dir, backup_dir
        )
    return _recovery_manager_instance
def reset_recovery_manager() -> None:
    global _recovery_manager_instance
    if _recovery_manager_instance and _recovery_manager_instance.monitoring_active:
        _recovery_manager_instance.stop_monitoring()
    _recovery_manager_instance = None