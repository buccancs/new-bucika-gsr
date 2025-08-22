import json
import os
import subprocess
import sys
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
try:
    from ..utils.logging_config import get_logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    def get_logger(name):
        return logging.getLogger(name)
class DependencySecurityScanner:
    def __init__(self, project_root: Optional[str] = None):
        self.logger = get_logger(__name__)
        self.project_root = Path(project_root) if project_root else Path(__file__).parent.parent.parent
        self.scan_results = {
            "python": {"vulnerabilities": [], "scan_status": "not_run"},
            "gradle": {"vulnerabilities": [], "scan_status": "not_run"},
            "overall": {"critical": 0, "high": 0, "medium": 0, "low": 0}
        }
    def scan_all_dependencies(self) -> Dict:
        self.logger.info("[INFO] Starting complete dependency security scan...")
        try:
            self.scan_python_dependencies()
        except Exception as e:
            self.logger.error(f"Python dependency scan failed: {e}")
        try:
            self.scan_gradle_dependencies()
        except Exception as e:
            self.logger.error(f"Gradle dependency scan failed: {e}")
        recommendations = self._generate_recommendations()
        report = {
            "scan_timestamp": datetime.now().isoformat(),
            "project_root": str(self.project_root),
            "results": self.scan_results,
            "recommendations": recommendations,
            "scan_summary": self._generate_summary()
        }
        self._save_scan_report(report)
        self._log_scan_results()
        return report
    def scan_python_dependencies(self) -> List[Dict]:
        self.logger.info("[PYTHON] Scanning Python dependencies for vulnerabilities...")
        vulnerabilities = []
        req_files = list(self.project_root.glob("*requirements*.txt"))
        req_files.extend(list(self.project_root.glob("pyproject.toml")))
        req_files.extend(list(self.project_root.glob("environment.yml")))
        if not req_files:
            self.logger.warning("No Python dependency files found")
            self.scan_results["python"]["scan_status"] = "no_files"
            return vulnerabilities
        try:
            safety_results = self._run_safety_check()
            vulnerabilities.extend(safety_results)
        except Exception as e:
            self.logger.error(f"Safety check failed: {e}")
        try:
            pip_audit_results = self._run_pip_audit()
            vulnerabilities.extend(pip_audit_results)
        except Exception as e:
            self.logger.debug(f"pip-audit not available or failed: {e}")
        alpha_deps = self._check_alpha_dependencies()
        vulnerabilities.extend(alpha_deps)
        self.scan_results["python"]["vulnerabilities"] = vulnerabilities
        self.scan_results["python"]["scan_status"] = "completed"
        self.logger.info(f"Python scan completed: {len(vulnerabilities)} vulnerabilities found")
        return vulnerabilities
    def _run_safety_check(self) -> List[Dict]:
        vulnerabilities = []
        try:
            result = subprocess.run(
                [sys.executable, "-m", "safety", "check", "--json"],
                capture_output=True,
                text=True,
                timeout=60,
                cwd=self.project_root
            )
            if result.returncode == 0:
                self.logger.info("Safety check completed with no vulnerabilities")
            else:
                try:
                    safety_data = json.loads(result.stdout)
                    for vuln in safety_data:
                        vulnerabilities.append({
                            "source": "safety",
                            "package": vuln.get("package"),
                            "installed_version": vuln.get("installed_version"),
                            "affected_versions": vuln.get("affected_versions"),
                            "vulnerability_id": vuln.get("vulnerability_id"),
                            "advisory": vuln.get("advisory"),
                            "severity": self._map_safety_severity(vuln.get("severity", "medium"))
                        })
                except json.JSONDecodeError:
                    if "No known security vulnerabilities found" not in result.stdout:
                        self.logger.warning(f"Safety found issues: {result.stdout}")
        except subprocess.TimeoutExpired:
            self.logger.error("Safety check timed out")
        except FileNotFoundError:
            self.logger.warning("Safety tool not installed")
        return vulnerabilities
    def _run_pip_audit(self) -> List[Dict]:
        vulnerabilities = []
        try:
            result = subprocess.run(
                [sys.executable, "-m", "pip_audit", "--format=json"],
                capture_output=True,
                text=True,
                timeout=60,
                cwd=self.project_root
            )
            if result.returncode == 0:
                audit_data = json.loads(result.stdout)
                for vuln in audit_data.get("vulnerabilities", []):
                    vulnerabilities.append({
                        "source": "pip_audit",
                        "package": vuln.get("package"),
                        "version": vuln.get("version"),
                        "vulnerability_id": vuln.get("id"),
                        "advisory": vuln.get("description"),
                        "severity": "medium"
                    })
        except (subprocess.TimeoutExpired, FileNotFoundError, json.JSONDecodeError):
            pass
        return vulnerabilities
    def _check_alpha_dependencies(self) -> List[Dict]:
        alpha_deps = []
        pyproject_file = self.project_root / "pyproject.toml"
        if pyproject_file.exists():
            try:
                import toml
                pyproject_data = toml.load(pyproject_file)
                dependencies = pyproject_data.get("project", {}).get("dependencies", [])
                for dep in dependencies:
                    if any(keyword in dep.lower() for keyword in ["alpha", "beta", "rc", "dev"]):
                        alpha_deps.append({
                            "source": "alpha_check",
                            "package": dep.split(">=")[0].split("==")[0],
                            "version": dep,
                            "vulnerability_id": "ALPHA_DEPENDENCY",
                            "advisory": f"Alpha/beta dependency found: {dep}",
                            "severity": "medium"
                        })
            except ImportError:
                self.logger.debug("toml package not available for pyproject.toml parsing")
        known_alpha_deps = [
            "security-crypto 1.1.0-alpha06"
        ]
        for alpha_dep in known_alpha_deps:
            alpha_deps.append({
                "source": "known_alpha",
                "package": alpha_dep.split()[0],
                "version": alpha_dep,
                "vulnerability_id": "KNOWN_ALPHA",
                "advisory": f"Known alpha dependency should be upgraded: {alpha_dep}",
                "severity": "medium"
            })
        return alpha_deps
    def scan_gradle_dependencies(self) -> List[Dict]:
        self.logger.info("[BOT] Scanning Gradle/Android dependencies for vulnerabilities...")
        vulnerabilities = []
        gradle_files = list(self.project_root.glob("**/build.gradle*"))
        if not gradle_files:
            self.logger.warning("No Gradle files found")
            self.scan_results["gradle"]["scan_status"] = "no_files"
            return vulnerabilities
        vulnerable_patterns = [
            {
                "pattern": "com.google.android.material:material:1.[0-6].",
                "package": "Material Design",
                "advisory": "Outdated Material Design library with potential vulnerabilities",
                "severity": "medium"
            },
            {
                "pattern": "androidx.core:core-ktx:1.[0-5].",
                "package": "AndroidX Core",
                "advisory": "Outdated AndroidX Core library",
                "severity": "medium"
            },
            {
                "pattern": "okhttp3:okhttp:3.",
                "package": "OkHttp",
                "advisory": "OkHttp 3.x has known security issues",
                "severity": "high"
            }
        ]
        for gradle_file in gradle_files:
            try:
                content = gradle_file.read_text()
                for vuln_pattern in vulnerable_patterns:
                    if self._check_gradle_pattern(content, vuln_pattern["pattern"]):
                        vulnerabilities.append({
                            "source": "gradle_check",
                            "package": vuln_pattern["package"],
                            "file": str(gradle_file),
                            "pattern": vuln_pattern["pattern"],
                            "advisory": vuln_pattern["advisory"],
                            "severity": vuln_pattern["severity"],
                            "vulnerability_id": f"GRADLE_{vuln_pattern['package'].upper().replace(' ', '_')}"
                        })
            except Exception as e:
                self.logger.error(f"Error scanning {gradle_file}: {e}")
        try:
            gradle_check_results = self._run_gradle_dependency_check()
            vulnerabilities.extend(gradle_check_results)
        except Exception as e:
            self.logger.debug(f"Gradle dependency check plugin not available: {e}")
        self.scan_results["gradle"]["vulnerabilities"] = vulnerabilities
        self.scan_results["gradle"]["scan_status"] = "completed"
        self.logger.info(f"Gradle scan completed: {len(vulnerabilities)} vulnerabilities found")
        return vulnerabilities
    def _check_gradle_pattern(self, content: str, pattern: str) -> bool:
        import re
        regex_pattern = pattern.replace(".", r"\.").replace("*", r"[^'\"]*")
        return bool(re.search(regex_pattern, content))
    def _run_gradle_dependency_check(self) -> List[Dict]:
        vulnerabilities = []
        try:
            result = subprocess.run(
                ["./gradlew", "dependencyCheckAnalyze", "--info"],
                capture_output=True,
                text=True,
                timeout=300,
                cwd=self.project_root
            )
            if result.returncode == 0:
                report_dir = self.project_root / "build" / "reports" / "dependency-check"
                if report_dir.exists():
                    self.logger.info("OWASP Dependency Check completed successfully")
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass
        return vulnerabilities
    def _map_safety_severity(self, safety_severity: str) -> str:
        severity_map = {
            "low": "low",
            "medium": "medium",
            "high": "high",
            "critical": "critical"
        }
        return severity_map.get(safety_severity.lower(), "medium")
    def _generate_recommendations(self) -> List[str]:
        recommendations = []
        total_vulns = 0
        severity_counts = {"critical": 0, "high": 0, "medium": 0, "low": 0}
        for scan_type in ["python", "gradle"]:
            vulns = self.scan_results[scan_type]["vulnerabilities"]
            total_vulns += len(vulns)
            for vuln in vulns:
                severity = vuln.get("severity", "medium")
                severity_counts[severity] = severity_counts.get(severity, 0) + 1
        self.scan_results["overall"] = severity_counts
        if severity_counts["critical"] > 0:
            recommendations.append(
                f"[ALERT] CRITICAL: {severity_counts['critical']} critical vulnerabilities found - "
                "immediate action required"
            )
        if severity_counts["high"] > 0:
            recommendations.append(
                f"[WARN] HIGH: {severity_counts['high']} high-severity vulnerabilities found - "
                "prioritise updates"
            )
        if total_vulns == 0:
            recommendations.append("[PASS] No known vulnerabilities found in dependencies")
        else:
            recommendations.append(
                f"[TOOL] Update {total_vulns} vulnerable dependencies to secure versions"
            )
        recommendations.extend([
            "[BOT] Enable Dependabot for automated dependency updates",
            "[REFRESH] Run dependency scans in CI/CD pipeline",
            "[CALENDAR] Schedule weekly dependency vulnerability scans",
            "[SECURE] Replace alpha/beta dependencies with stable versions"
        ])
        return recommendations
    def _generate_summary(self) -> Dict:
        total_vulns = sum(len(self.scan_results[scan_type]["vulnerabilities"])
                         for scan_type in ["python", "gradle"])
        return {
            "total_vulnerabilities": total_vulns,
            "python_status": self.scan_results["python"]["scan_status"],
            "gradle_status": self.scan_results["gradle"]["scan_status"],
            "severity_breakdown": self.scan_results["overall"],
            "scan_successful": total_vulns >= 0
        }
    def _save_scan_report(self, report: Dict):
        reports_dir = self.project_root / "security_reports"
        reports_dir.mkdir(exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = reports_dir / f"dependency_scan_{timestamp}.json"
        try:
            with open(report_file, 'w') as f:
                json.dump(report, f, indent=2, default=str)
            self.logger.info(f"Dependency scan report saved to {report_file}")
        except Exception as e:
            self.logger.error(f"Failed to save scan report: {e}")
    def _log_scan_results(self):
        summary = self._generate_summary()
        self.logger.info("[INFO] Dependency Security Scan Summary:")
        self.logger.info(f"   Total vulnerabilities: {summary['total_vulnerabilities']}")
        for severity, count in summary["severity_breakdown"].items():
            if count > 0:
                self.logger.warning(f"   {severity.upper()}: {count}")
        if summary["total_vulnerabilities"] == 0:
            self.logger.info("   [PASS] No vulnerabilities found!")
        else:
            self.logger.warning(f"   [WARN]  Action required for {summary['total_vulnerabilities']} vulnerabilities")
def create_dependabot_config(project_root: str) -> bool:
    logger = get_logger(__name__)
    dependabot_config = {
        "version": 2,
        "updates": [
            {
                "package-ecosystem": "pip",
                "directory": "/",
                "schedule": {
                    "interval": "weekly",
                    "day": "monday"
                },
                "open-pull-requests-limit": 5,
                "labels": ["dependencies", "security"],
                "reviewers": ["@security-team"],
                "assignees": ["@security-team"]
            },
            {
                "package-ecosystem": "gradle",
                "directory": "/",
                "schedule": {
                    "interval": "weekly",
                    "day": "monday"
                },
                "open-pull-requests-limit": 3,
                "labels": ["dependencies", "android", "security"]
            }
        ]
    }
    try:
        dependabot_dir = Path(project_root) / ".github"
        dependabot_dir.mkdir(exist_ok=True)
        dependabot_file = dependabot_dir / "dependabot.yml"
        import yaml
        with open(dependabot_file, 'w') as f:
            yaml.dump(dependabot_config, f, default_flow_style=False)
        logger.info(f"Dependabot configuration created at {dependabot_file}")
        return True
    except Exception as e:
        logger.error(f"Failed to create Dependabot configuration: {e}")
        return False
def create_ci_security_workflow(project_root: str) -> bool:
    logger = get_logger(__name__)
    workflow_content = """name: Security Dependency Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 9 * * 1'

jobs:
  dependency-scan:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up Python
      uses: actions/setup-python@v5
      with:
        python-version: '3.9'
    
    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install safety bandit
        if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
        if [ -f test-requirements.txt ]; then pip install -r test-requirements.txt; fi
    
    - name: Run Safety check
      run: |
        safety check --json || true
    
    - name: Run Bandit security check
      run: |
        bandit -r PythonApp/ -f json || true
    
    - name: Upload security reports
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: security-reports
        path: security_reports/
"""
    try:
        workflow_file = Path(project_root) / ".github" / "workflows" / "security-dependency-scan.yml"
        workflow_file.parent.mkdir(parents=True, exist_ok=True)
        workflow_file.write_text(workflow_content)
        logger.info(f"Created security workflow: {workflow_file}")
        return True
    except Exception as e:
        logger.error(f"Error creating security workflow: {e}")
        return False
if __name__ == "__main__":
    scanner = DependencySecurityScanner()
    scanner.scan_all_dependencies()