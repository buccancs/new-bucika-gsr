#!/usr/bin/env python3
"""
Universal Cross-Platform Test Runner for Multi-Sensor Recording System

This script provides a Python-based cross-platform alternative to the shell scripts,
ensuring consistent testing experience across Windows, Linux, and macOS.

Usage:
    python run_local_tests.py [MODE] [OPTIONS]

Examples:
    python run_local_tests.py
    python run_local_tests.py full
    python run_local_tests.py requirements
    python run_local_tests.py quick --install-deps
    python run_local_tests.py pc
    python run_local_tests.py android
    python run_local_tests.py gui
"""

import argparse
import os
import platform
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple
import shutil

# Color support detection
try:
    # Windows color support
    if platform.system() == "Windows":
        import colorama
        colorama.init()
        HAS_COLOR = True
    else:
        HAS_COLOR = True
except ImportError:
    HAS_COLOR = False

class Colors:
    """Cross-platform color codes"""
    if HAS_COLOR:
        RED = '\033[0;31m'
        GREEN = '\033[0;32m'
        YELLOW = '\033[1;33m'
        BLUE = '\033[0;34m'
        NC = '\033[0m'  # No Color
    else:
        RED = GREEN = YELLOW = BLUE = NC = ''

class CrossPlatformTestRunner:
    """Cross-platform test runner that works on Windows, Linux, and macOS"""
    
    def __init__(self):
        self.project_root = Path(__file__).parent
        self.system = platform.system()
        
    def print_status(self, message: str):
        """Print status message with color"""
        print(f"{Colors.BLUE}[INFO]{Colors.NC} {message}")
        
    def print_success(self, message: str):
        """Print success message with color"""
        print(f"{Colors.GREEN}[SUCCESS]{Colors.NC} {message}")
        
    def print_warning(self, message: str):
        """Print warning message with color"""
        print(f"{Colors.YELLOW}[WARNING]{Colors.NC} {message}")
        
    def print_error(self, message: str):
        """Print error message with color"""
        print(f"{Colors.RED}[ERROR]{Colors.NC} {message}")
        
    def check_unified_framework(self) -> bool:
        """Check if unified testing framework is available"""
        unified_runner = self.project_root / "tests_unified" / "runners" / "run_unified_tests.py"
        return unified_runner.exists()
        
    def check_python_executable(self) -> str:
        """Get the appropriate Python executable for the platform"""
        # Try common Python executable names
        python_names = ["python3", "python", "py"]
        
        for name in python_names:
            if shutil.which(name):
                try:
                    # Verify it's Python 3
                    result = subprocess.run(
                        [name, "--version"], 
                        capture_output=True, 
                        text=True, 
                        timeout=10
                    )
                    if result.returncode == 0 and "Python 3" in result.stdout:
                        return name
                except (subprocess.TimeoutExpired, subprocess.SubprocessError):
                    continue
        
        # Fallback
        return "python"
        
    def run_command(self, cmd: List[str], cwd: Optional[Path] = None) -> Tuple[int, str, str]:
        """Run a command with cross-platform handling"""
        try:
            result = subprocess.run(
                cmd,
                cwd=cwd or self.project_root,
                capture_output=True,
                text=True,
                timeout=300  # 5 minute timeout
            )
            return result.returncode, result.stdout, result.stderr
        except subprocess.TimeoutExpired:
            return 1, "", "Command timed out"
        except Exception as e:
            return 1, "", str(e)
            
    def install_dependencies(self) -> bool:
        """Install test dependencies"""
        self.print_status("Installing test dependencies...")
        
        python_exe = self.check_python_executable()
        
        # Install test requirements
        test_requirements = self.project_root / "test-requirements.txt"
        if test_requirements.exists():
            self.print_status("Installing from test-requirements.txt...")
            retcode, stdout, stderr = self.run_command([python_exe, "-m", "pip", "install", "-r", "test-requirements.txt"])
            if retcode != 0:
                self.print_error(f"Failed to install test requirements: {stderr}")
                return False
                
        # Install project in editable mode
        pyproject_toml = self.project_root / "pyproject.toml"
        setup_py = self.project_root / "setup.py"
        
        if pyproject_toml.exists() or setup_py.exists():
            self.print_status("Installing project in editable mode...")
            retcode, stdout, stderr = self.run_command([python_exe, "-m", "pip", "install", "-e", "."])
            if retcode != 0:
                self.print_warning(f"Could not install project in editable mode: {stderr}")
                # Don't fail for this as it's not always critical
                
        # Try to install colorama for Windows color support
        if self.system == "Windows":
            self.print_status("Installing Windows color support...")
            retcode, stdout, stderr = self.run_command([python_exe, "-m", "pip", "install", "colorama"])
            if retcode != 0:
                self.print_warning("Could not install colorama for color support")
                
        self.print_success("Dependencies installed")
        return True
        
    def run_unified_tests(self, mode: str) -> bool:
        """Run unified tests in specified mode"""
        self.print_status(f"Running unified test suite in {mode} mode...")
        
        python_exe = self.check_python_executable()
        unified_runner = self.project_root / "tests_unified" / "runners" / "run_unified_tests.py"
        
        if not unified_runner.exists():
            self.print_error("Unified testing framework not found!")
            self.print_error("Please ensure tests_unified/runners/run_unified_tests.py exists")
            return False
        
        # Build command based on mode
        cmd = [python_exe, str(unified_runner)]
        
        if mode == "quick":
            cmd.extend(["--quick", "--verbose"])
        elif mode == "full":
            cmd.extend(["--all-levels", "--verbose"])
        elif mode == "requirements":
            # Run requirements validation
            retcode1, stdout1, stderr1 = self.run_command(cmd + ["--validate-requirements"])
            if retcode1 != 0:
                self.print_error(f"Requirements validation failed: {stderr1}")
                return False
            print(stdout1)
            
            # Run coverage report
            retcode2, stdout2, stderr2 = self.run_command(cmd + ["--report-requirements-coverage"])
            if retcode2 != 0:
                self.print_warning(f"Coverage report failed: {stderr2}")
            print(stdout2)
            return True
        elif mode == "performance":
            cmd.extend(["--level", "performance", "--performance-benchmarks"])
        elif mode == "ci":
            cmd.extend(["--mode", "ci"])
        elif mode == "pc":
            # PC tests - run all levels but exclude android-specific tests  
            cmd.extend(["--level", "unit"])
        elif mode == "android":
            # Enhanced Android testing with comprehensive device testing
            self.print_status("Running comprehensive Android device tests...")
            android_demo_script = self.project_root / "android_device_testing_demo.py"
            if android_demo_script.exists():
                # Use the new comprehensive Android testing demo
                retcode, stdout, stderr = self.run_command([
                    python_exe, str(android_demo_script), "--mode", "full"
                ])
                if retcode == 0:
                    self.print_success("Android comprehensive testing completed")
                else:
                    self.print_warning("Comprehensive testing failed, falling back to standard Android tests")
                    cmd.extend(["--category", "android"])
            else:
                # Fallback to standard Android category tests
                cmd.extend(["--category", "android"])
        elif mode == "android_comprehensive":
            self.print_status("Running comprehensive Android device tests...")
            android_demo_script = self.project_root / "android_device_testing_demo.py"
            if android_demo_script.exists():
                cmd = [python_exe, str(android_demo_script), "--mode", "comprehensive", "--comprehensive"]
            else:
                self.print_warning("Android comprehensive testing script not found")
                cmd = [python_exe, "-m", "pytest", str(self.unified_tests_dir / "test_android_device_comprehensive.py"), 
                       "--tb=short", "-v"]
        elif mode == "android_quick":
            self.print_status("Running quick Android device validation...")
            android_demo_script = self.project_root / "android_device_testing_demo.py"
            if android_demo_script.exists():
                cmd = [python_exe, str(android_demo_script), "--mode", "quick"]
            else:
                cmd = [python_exe, "-m", "pytest", str(self.unified_tests_dir / "test_android_device_comprehensive.py"), 
                       "-k", "quick", "--tb=short", "-v"]
        elif mode == "android_ide":
            self.print_status("Running Android IDE integration tests...")
            android_demo_script = self.project_root / "android_device_testing_demo.py"
            if android_demo_script.exists():
                cmd = [python_exe, str(android_demo_script), "--mode", "ide"]
            else:
                cmd = [python_exe, "-m", "pytest", str(self.unified_tests_dir / "test_android_device_comprehensive.py"), 
                       "-k", "ide", "--tb=short", "-v"]
        elif mode == "gui":
            cmd.extend(["--category", "visual"])
        else:
            self.print_error(f"Unknown mode: {mode}")
            self.print_status("Available modes: quick, full, requirements, performance, ci, pc, android, gui")
            return False
            
        retcode, stdout, stderr = self.run_command(cmd)
        
        if stdout:
            print(stdout)
        if stderr:
            print(stderr, file=sys.stderr)
            
        if retcode != 0:
            self.print_error("Test execution failed")
            return False
            
        return True
        
    def run_legacy_tests(self) -> bool:
        """Run legacy tests as fallback"""
        self.print_warning("Unified testing framework not found, falling back to legacy tests")
        
        python_exe = self.check_python_executable()
        
        # Check for pytest
        if not shutil.which("pytest"):
            self.print_error("pytest not available and no unified framework found")
            return False
            
        # Run tests directory if it exists
        tests_dir = self.project_root / "tests"
        if tests_dir.exists():
            self.print_status("Running pytest on tests/ directory...")
            retcode, stdout, stderr = self.run_command([
                python_exe, "-m", "pytest", "tests/", "-v", "--tb=short"
            ])
            
            if stdout:
                print(stdout)
            if stderr:
                print(stderr, file=sys.stderr)
                
            if retcode != 0:
                self.print_error("Legacy tests failed")
                return False
        else:
            self.print_warning("No tests/ directory found")
            
        return True
        
    def check_project_root(self) -> bool:
        """Check if we're in the right directory"""
        required_files = ["pyproject.toml", "setup.py"]
        required_dirs = ["PythonApp"]
        
        has_file = any((self.project_root / f).exists() for f in required_files)
        has_dir = any((self.project_root / d).exists() for d in required_dirs)
        
        return has_file or has_dir
        
    def show_usage(self):
        """Show usage information"""
        print("Universal Cross-Platform Test Runner for Multi-Sensor Recording System")
        print("")
        print("Usage: python run_local_tests.py [MODE] [OPTIONS]")
        print("")
        print("MODES:")
        print("  quick        Run quick test suite (default, ~2 minutes)")
        print("  full         Run complete test suite (all levels)")
        print("  requirements Validate functional and non-functional requirements")
        print("  performance  Run performance benchmarks")
        print("  ci           Run CI/CD mode tests")
        print("  pc           Run PC/desktop application tests")
        print("  android      Run comprehensive Android device tests with IDE integration")
        print("  android_comprehensive  Run full Android testing suite (UI, functional, requirements)")
        print("  android_quick          Run quick Android device validation")
        print("  android_ide            Run Android IDE integration tests")
        print("  gui          Run GUI/UI tests for both platforms")
        print("")
        print("OPTIONS:")
        print("  --install-deps    Install test dependencies before running")
        print("  --help, -h        Show this help message")
        print("")
        print("EXAMPLES:")
        print("  python run_local_tests.py                     # Quick test suite")
        print("  python run_local_tests.py full                # Complete test suite")
        print("  python run_local_tests.py requirements        # Requirements validation")
        print("  python run_local_tests.py quick --install-deps # Install deps and run quick tests")
        print("  python run_local_tests.py pc                  # PC application tests only")
        print("  android             # Comprehensive Android device tests")
        print("  python run_local_tests.py android_quick       # Quick Android validation")
        print("  python run_local_tests.py android_ide         # Android IDE integration tests")
        print("  python run_local_tests.py gui                 # GUI tests for both platforms")
        print("")
        print(f"PLATFORM INFO:")
        print(f"  Current OS: {platform.system()} {platform.release()}")
        print(f"  Python: {sys.version.split()[0]}")
        print(f"  Color support: {'Yes' if HAS_COLOR else 'No'}")
        print("")
        print("UNIFIED FRAMEWORK USAGE:")
        print("  # Direct usage of unified framework")
        python_exe = self.check_python_executable()
        if self.system == "Windows":
            print(f"  {python_exe} tests_unified\\runners\\run_unified_tests.py --help")
            print("")
            print("  # Specific test levels")
            print(f"  {python_exe} tests_unified\\runners\\run_unified_tests.py --level unit")
            print(f"  {python_exe} tests_unified\\runners\\run_unified_tests.py --level integration")
            print("")
            print("  # Specific categories")
            print(f"  {python_exe} tests_unified\\runners\\run_unified_tests.py --category android")
            print(f"  {python_exe} tests_unified\\runners\\run_unified_tests.py --category hardware")
        else:
            print(f"  {python_exe} tests_unified/runners/run_unified_tests.py --help")
            print("")
            print("  # Specific test levels")
            print(f"  {python_exe} tests_unified/runners/run_unified_tests.py --level unit")
            print(f"  {python_exe} tests_unified/runners/run_unified_tests.py --level integration")
            print("")
            print("  # Specific categories")
            print(f"  {python_exe} tests_unified/runners/run_unified_tests.py --category android")
            print(f"  {python_exe} tests_unified/runners/run_unified_tests.py --category hardware")
        print("")
        
    def run(self, mode: str = "quick", install_deps: bool = False) -> bool:
        """Main execution method"""
        self.print_status("Multi-Sensor Recording System - Universal Test Runner")
        self.print_status("======================================================")
        self.print_status(f"Platform: {platform.system()} {platform.release()}")
        
        # Check if we're in the right directory
        if not self.check_project_root():
            self.print_error("Not in project root directory. Please run from the repository root.")
            return False
            
        # Install dependencies if requested
        if install_deps:
            if not self.install_dependencies():
                return False
                
        # Check for unified framework and run tests
        if self.check_unified_framework():
            self.print_success("Unified testing framework found")
            return self.run_unified_tests(mode)
        else:
            self.print_error("Unified testing framework not found!")
            self.print_error("The project has been consolidated to use only the unified framework.")
            self.print_error("Please ensure tests_unified/runners/run_unified_tests.py exists")
            return False

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description="Universal Cross-Platform Test Runner for Multi-Sensor Recording System",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python run_local_tests.py                      # Quick test suite
  python run_local_tests.py full                 # Complete test suite
  python run_local_tests.py requirements         # Requirements validation
  python run_local_tests.py quick --install-deps # Install deps and run quick tests
  python run_local_tests.py pc                   # PC application tests only
  python run_local_tests.py android              # Android application tests only
  python run_local_tests.py gui                  # GUI tests for both platforms
        """
    )
    
    parser.add_argument(
        "mode",
        nargs="?",
        default="quick",
        choices=["quick", "full", "requirements", "performance", "ci", "pc", "android", "android_comprehensive", "android_quick", "android_ide", "gui"],
        help="Test mode to run (default: quick)"
    )
    
    parser.add_argument(
        "--install-deps",
        action="store_true",
        help="Install test dependencies before running"
    )
    
    args = parser.parse_args()
    
    runner = CrossPlatformTestRunner()
    
    try:
        success = runner.run(args.mode, args.install_deps)
        if success:
            runner.print_success("Test execution completed")
            sys.exit(0)
        else:
            runner.print_error("Test execution failed")
            sys.exit(1)
    except KeyboardInterrupt:
        runner.print_warning("Test execution interrupted by user")
        sys.exit(1)
    except Exception as e:
        runner.print_error(f"Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()