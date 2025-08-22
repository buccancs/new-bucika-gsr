import asyncio
import json
import logging
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any, Callable
from dataclasses import asdict
try:
    from .endurance_testing import EnduranceTestRunner, EnduranceTestConfig, EnduranceTestResult
    from .graceful_degradation import (
        GracefulDegradationManager, PerformanceThresholds,
        SystemMetrics, PerformanceLevel
    )
    from ..utils.logging_config import get_logger
except ImportError:
    import sys
    current_dir = Path(__file__).parent
    sys.path.insert(0, str(current_dir))
    from endurance_testing import EnduranceTestRunner, EnduranceTestConfig, EnduranceTestResult
    from graceful_degradation import (
        GracefulDegradationManager, PerformanceThresholds,
        SystemMetrics, PerformanceLevel
    )
    from PythonApp.utils.logging_config import get_logger
class PerformanceMonitorIntegration:
    def __init__(self, config_file: Optional[str] = None):
        self.logger = get_logger(__name__)
        self.config = self._load_configuration(config_file)
        self.degradation_manager = GracefulDegradationManager(
            self.config.get("performance_thresholds")
        )
        self.monitoring_active = False
        self.performance_history: List[Dict[str, Any]] = []
        self.alerts_log: List[Dict[str, Any]] = []
        self.status_change_callbacks: List[Callable] = []
        self.performance_alert_callbacks: List[Callable] = []
        self.degradation_manager.add_level_change_callback(self._on_performance_level_change)
        self.degradation_manager.add_metrics_callback(self._get_application_metrics)
        self.logger.info("PerformanceMonitorIntegration: Initialized integrated performance monitoring")
    def _load_configuration(self, config_file: Optional[str]) -> Dict[str, Any]:
        default_config = {
            "performance_thresholds": PerformanceThresholds(
                cpu_degraded_threshold=70.0,
                cpu_critical_threshold=85.0,
                memory_degraded_threshold=80.0,
                memory_critical_threshold=90.0
            ),
            "endurance_test": {
                "default_duration_hours": 8.0,
                "monitoring_interval_seconds": 30.0,
                "memory_leak_threshold_mb": 100.0
            },
            "monitoring": {
                "history_retention_hours": 24,
                "alert_cooldown_minutes": 15,
                "status_report_interval_minutes": 60
            },
            "output": {
                "reports_directory": "performance_reports",
                "enable_detailed_logging": True,
                "auto_export_reports": True
            }
        }
        if config_file and Path(config_file).exists():
            try:
                with open(config_file, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
                self.logger.info(f"PerformanceMonitorIntegration: Loaded configuration from {config_file}")
            except Exception as e:
                self.logger.warning(f"PerformanceMonitorIntegration: Error loading config file: {e}")
        return default_config
    def _get_application_metrics(self) -> SystemMetrics:
        current_time = time.time()
        return SystemMetrics(
            timestamp=current_time,
            cpu_percent=0.0,
            memory_percent=0.0,
            disk_write_speed_mbps=0.0,
            network_speed_mbps=0.0,
            queue_sizes={
                "video_processing": 0,
                "thermal_processing": 0,
                "data_aggregation": 0
            },
            response_times={
                "frame_processing": 50.0,
                "device_communication": 25.0
            },
            active_recordings=0,
            frame_processing_rate=30.0,
            preview_enabled=True,
            thermal_processing_enabled=True
        )
    def _on_performance_level_change(self, new_level: PerformanceLevel):
        alert = {
            "timestamp": time.time(),
            "type": "performance_level_change",
            "level": new_level.value,
            "message": f"Performance level changed to {new_level.value}"
        }
        self.alerts_log.append(alert)
        self.logger.info(f"PerformanceMonitorIntegration: {alert['message']}")
        for callback in self.performance_alert_callbacks:
            try:
                callback(alert)
            except Exception as e:
                self.logger.error(f"PerformanceMonitorIntegration: Error in alert callback: {e}")
    async def start_monitoring(self):
        if self.monitoring_active:
            self.logger.warning("PerformanceMonitorIntegration: Monitoring already active")
            return
        self.monitoring_active = True
        await self.degradation_manager.start_monitoring()
        asyncio.create_task(self._periodic_status_reporting())
        self.logger.info("PerformanceMonitorIntegration: Started integrated performance monitoring")
    async def stop_monitoring(self):
        self.monitoring_active = False
        await self.degradation_manager.stop_monitoring()
        self.logger.info("PerformanceMonitorIntegration: Stopped performance monitoring")
    async def _periodic_status_reporting(self):
        interval_seconds = self.config["monitoring"]["status_report_interval_minutes"] * 60
        while self.monitoring_active:
            try:
                status = self.get_complete_status()
                self.performance_history.append({
                    "timestamp": time.time(),
                    "status": status
                })
                max_age = time.time() - (self.config["monitoring"]["history_retention_hours"] * 3600)
                self.performance_history = [
                    h for h in self.performance_history
                    if h["timestamp"] > max_age
                ]
                if self.config["output"]["auto_export_reports"]:
                    await self._auto_export_report()
                await asyncio.sleep(interval_seconds)
            except asyncio.CancelledError:
                break
            except Exception as e:
                self.logger.error(f"PerformanceMonitorIntegration: Error in status reporting: {e}")
                await asyncio.sleep(interval_seconds)
    async def _auto_export_report(self):
        try:
            reports_dir = Path(self.config["output"]["reports_directory"])
            reports_dir.mkdir(exist_ok=True)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_file = reports_dir / f"performance_report_{timestamp}.json"
            report = self.generate_performance_report()
            with open(report_file, 'w') as f:
                json.dump(report, f, indent=2, default=str)
            self.logger.debug(f"PerformanceMonitorIntegration: Auto-exported report to {report_file}")
        except Exception as e:
            self.logger.error(f"PerformanceMonitorIntegration: Error auto-exporting report: {e}")
    async def run_endurance_test(self,
                                duration_hours: Optional[float] = None,
                                custom_config: Optional[EnduranceTestConfig] = None) -> EnduranceTestResult:
        self.logger.info("PerformanceMonitorIntegration: Starting integrated endurance test")
        if custom_config is None:
            if duration_hours is None:
                duration_hours = self.config["endurance_test"]["default_duration_hours"]
            custom_config = EnduranceTestConfig(
                duration_hours=duration_hours,
                monitoring_interval_seconds=self.config["endurance_test"]["monitoring_interval_seconds"],
                memory_leak_threshold_mb=self.config["endurance_test"]["memory_leak_threshold_mb"],
                output_directory=self.config["output"]["reports_directory"],
                enable_detailed_logging=self.config["output"]["enable_detailed_logging"]
            )
        was_monitoring = self.monitoring_active
        if was_monitoring:
            await self.stop_monitoring()
        try:
            runner = EnduranceTestRunner(custom_config)
            result = await runner.run_endurance_test()
            self.logger.info(f"PerformanceMonitorIntegration: Endurance test completed - Success: {result.success}")
            alert = {
                "timestamp": time.time(),
                "type": "endurance_test_completed",
                "test_id": result.test_id,
                "success": result.success,
                "duration_hours": result.duration_hours,
                "memory_leak_detected": result.memory_leak_detected,
                "recommendations": result.recommendations
            }
            self.alerts_log.append(alert)
            return result
        finally:
            if was_monitoring:
                await self.start_monitoring()
    def get_complete_status(self) -> Dict[str, Any]:
        degradation_status = self.degradation_manager.get_current_status()
        status = {
            "timestamp": time.time(),
            "monitoring_active": self.monitoring_active,
            "performance_level": degradation_status["current_level"],
            "active_degradations": degradation_status["active_degradations"],
            "frame_drop_stats": degradation_status["frame_drop_stats"],
            "quality_settings": degradation_status["quality_settings"],
            "queue_status": degradation_status["queue_status"],
            "recent_alerts_count": len([
                alert for alert in self.alerts_log
                if time.time() - alert["timestamp"] < 3600
            ]),
            "recommendations": self._generate_current_recommendations()
        }
        return status
    def _generate_current_recommendations(self) -> List[str]:
        recommendations = []
        status = self.degradation_manager.get_current_status()
        current_level = status["current_level"]
        if current_level == "critical":
            recommendations.append(
                "System is in critical performance state. Consider reducing recording quality "
                "or number of active devices."
            )
        elif current_level == "degraded":
            recommendations.append(
                "Performance degradation detected. Monitor system resources and consider "
                "optimising recording parameters."
            )
        frame_stats = status.get("frame_drop_stats", {})
        if frame_stats.get("drop_rate_actual", 0) > 0:
            recommendations.append(
                f"Frame dropping active at {frame_stats['drop_rate_actual']:.1%}. "
                "This helps prevent memory overflow but may affect recording quality."
            )
        recent_alerts = [
            alert for alert in self.alerts_log
            if time.time() - alert["timestamp"] < 1800
        ]
        if len(recent_alerts) > 5:
            recommendations.append(
                f"High alert frequency detected ({len(recent_alerts)} alerts in 30 minutes). "
                "Consider reviewing system configuration or workload."
            )
        if not recommendations:
            recommendations.append("System performance is stable. Continue monitoring.")
        return recommendations
    def generate_performance_report(self) -> Dict[str, Any]:
        current_status = self.get_complete_status()
        history_analysis = self._analyze_performance_history()
        alert_summary = self._analyze_alerts()
        report = {
            "report_timestamp": datetime.now().isoformat(),
            "monitoring_period_hours": self.config["monitoring"]["history_retention_hours"],
            "current_status": current_status,
            "performance_history_analysis": history_analysis,
            "alert_summary": alert_summary,
            "configuration": self.config,
            "recommendations": {
                "immediate": self._generate_current_recommendations(),
                "long_term": self._generate_long_term_recommendations(history_analysis)
            }
        }
        return report
    def _analyze_performance_history(self) -> Dict[str, Any]:
        if not self.performance_history:
            return {"insufficient_data": True}
        levels = [h["status"]["performance_level"] for h in self.performance_history]
        level_counts = {
            "optimal": levels.count("optimal"),
            "good": levels.count("good"),
            "degraded": levels.count("degraded"),
            "critical": levels.count("critical")
        }
        total = len(levels)
        level_percentages = {k: (v / total * 100) if total > 0 else 0 for k, v in level_counts.items()}
        recent_levels = levels[-10:] if len(levels) >= 10 else levels
        degradation_trend = sum(1 for level in recent_levels if level in ["degraded", "critical"]) / len(recent_levels)
        return {
            "total_measurements": total,
            "level_distribution": level_percentages,
            "recent_degradation_rate": degradation_trend,
            "stability_score": level_percentages.get("optimal", 0) + level_percentages.get("good", 0)
        }
    def _analyze_alerts(self) -> Dict[str, Any]:
        if not self.alerts_log:
            return {"no_alerts": True}
        alert_types = {}
        for alert in self.alerts_log:
            alert_type = alert.get("type", "unknown")
            alert_types[alert_type] = alert_types.get(alert_type, 0) + 1
        recent_threshold = time.time() - 3600
        recent_alerts = [alert for alert in self.alerts_log if alert["timestamp"] > recent_threshold]
        return {
            "total_alerts": len(self.alerts_log),
            "alert_types": alert_types,
            "recent_alerts_count": len(recent_alerts),
            "alert_frequency_per_hour": len(recent_alerts)
        }
    def _generate_long_term_recommendations(self, history_analysis: Dict[str, Any]) -> List[str]:
        recommendations = []
        if history_analysis.get("insufficient_data"):
            recommendations.append("Insufficient historical data. Continue monitoring to build performance baseline.")
            return recommendations
        stability_score = history_analysis.get("stability_score", 0)
        degradation_rate = history_analysis.get("recent_degradation_rate", 0)
        if stability_score < 70:
            recommendations.append(
                "System stability is below optimal levels. Consider hardware upgrades, "
                "software optimisation, or workload reduction."
            )
        if degradation_rate > 0.3:
            recommendations.append(
                "Frequent performance degradation detected. Review system capacity planning "
                "and consider implementing preventive measures."
            )
        if stability_score > 90:
            recommendations.append(
                "Excellent system stability. Current configuration is well-suited for the workload."
            )
        return recommendations
    def add_status_change_callback(self, callback: Callable):
        self.status_change_callbacks.append(callback)
    def add_performance_alert_callback(self, callback: Callable):
        self.performance_alert_callbacks.append(callback)
    def should_drop_frame(self) -> bool:
        return self.degradation_manager.should_drop_frame()
    def get_adapted_recording_settings(self, **baseline_settings) -> Dict[str, Any]:
        return self.degradation_manager.get_adapted_quality_settings(**baseline_settings)
_performance_monitor: Optional[PerformanceMonitorIntegration] = None
def get_performance_monitor(config_file: Optional[str] = None) -> PerformanceMonitorIntegration:
    global _performance_monitor
    if _performance_monitor is None:
        _performance_monitor = PerformanceMonitorIntegration(config_file)
    return _performance_monitor
async def main():
    import argparse
    parser = argparse.ArgumentParser(description="Integrated Performance Monitoring System")
    parser.add_argument("--config", type=str, help="Configuration file path")
    parser.add_argument("--endurance-test", action="store_true", help="Run endurance test")
    parser.add_argument("--duration", type=float, default=0.1, help="Test duration in hours")
    parser.add_argument("--monitor", action="store_true", help="Start real-time monitoring")
    parser.add_argument("--verbose", action="store_true", help="Enable verbose logging")
    args = parser.parse_args()
    level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    monitor = PerformanceMonitorIntegration(args.config)
    try:
        if args.endurance_test:
            print(f"Running endurance test for {args.duration} hours...")
            result = await monitor.run_endurance_test(args.duration)
            print(f"\n=== Endurance Test Results ===")
            print(f"Test ID: {result.test_id}")
            print(f"Success: {'[PASS]' if result.success else '[FAIL]'}")
            print(f"Duration: {result.duration_hours:.2f} hours")
            print(f"Memory Growth: {result.memory_growth_mb:.1f}MB")
            print(f"Memory Leak: {'Yes' if result.memory_leak_detected else 'No'}")
        elif args.monitor:
            print("Starting real-time performance monitoring...")
            await monitor.start_monitoring()
            print("Monitoring active. Press Ctrl+C to stop and generate report.")
            try:
                while True:
                    await asyncio.sleep(10)
                    status = monitor.get_complete_status()
                    print(f"Status: {status['performance_level']} | "
                          f"Degradations: {len(status['active_degradations'])} | "
                          f"Alerts: {status['recent_alerts_count']}")
            except KeyboardInterrupt:
                print("\nStopping monitoring...")
            await monitor.stop_monitoring()
            report = monitor.generate_performance_report()
            report_file = f"performance_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(report_file, 'w') as f:
                json.dump(report, f, indent=2, default=str)
            print(f"Performance report saved to: {report_file}")
        else:
            print("Generating current status report...")
            status = monitor.get_complete_status()
            print(f"Performance Level: {status['performance_level']}")
            print(f"Active Degradations: {len(status['active_degradations'])}")
            print(f"Recent Alerts: {status['recent_alerts_count']}")
            print("\nRecommendations:")
            for i, rec in enumerate(status['recommendations'], 1):
                print(f"  {i}. {rec}")
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
if __name__ == "__main__":
    asyncio.run(main())
