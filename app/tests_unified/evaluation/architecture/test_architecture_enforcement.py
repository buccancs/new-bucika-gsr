import unittest
import os
import ast
from pathlib import Path
from typing import List, Set
import pytest

@pytest.mark.unit
class PythonArchitectureTest(unittest.TestCase):
    def setUp(self):
        self.project_root = Path(__file__).parent.parent
        self.python_app_root = self.project_root / "PythonApp"
    def test_gui_layer_should_not_import_low_level_services(self):
        gui_files = self._get_python_files_in_package("gui")
        forbidden_imports = [
            "network.",
            "device.",
            "shimmer_manager",
            "calibration.calibration_processor",
        ]
        for file_path in gui_files:
            imports = self._extract_imports_from_file(file_path)
            for forbidden_import in forbidden_imports:
                self.assertNotIn(
                    forbidden_import,
                    imports,
                    f"GUI file {file_path} should not directly import {forbidden_import}"
                )
    def test_managers_should_not_import_gui_components(self):
        manager_files = self._get_python_files_with_pattern("*manager*.py")
        if not manager_files:
            self.skipTest("No manager files found")
        forbidden_imports = [
            "PyQt5.QtWidgets",
            "tkinter",
            "gui.",
        ]
        for file_path in manager_files:
            # Allow stimulus manager to use GUI components for display purposes
            if "stimulus_manager" in str(file_path):
                continue
            imports = self._extract_imports_from_file(file_path)
            for forbidden_import in forbidden_imports:
                self.assertNotIn(
                    forbidden_import,
                    imports,
                    f"Manager {file_path} should not import GUI components {forbidden_import}"
                )
    def test_network_layer_independence(self):
        network_files = self._get_python_files_in_package("network")
        forbidden_imports = [
            "gui.",
            "calibration.calibration_manager",
            "session.",
        ]
        for file_path in network_files:
            imports = self._extract_imports_from_file(file_path)
            for forbidden_import in forbidden_imports:
                self.assertNotIn(
                    forbidden_import,
                    imports,
                    f"Network file {file_path} should not import {forbidden_import}"
                )
    def test_infrastructure_utilities_usage(self):
        all_python_files = self._get_all_python_files()
        for file_path in all_python_files:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            if "print(" in content and "test" not in str(file_path).lower():
                lines = content.split('\n')
                print_lines = [i for i, line in enumerate(lines) if "print(" in line and "debug" not in line.lower()]
                if len(print_lines) > 3:
                    self.fail(f"File {file_path} has excessive print() usage ({len(print_lines)} lines). Consider using logging.")
    def test_dependency_injection_patterns(self):
        manager_files = self._get_python_files_with_pattern("*manager*.py")
        for file_path in manager_files:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            if "class" in content and "Manager" in content:
                self.assertIn(
                    "__init__",
                    content,
                    f"Manager {file_path} should have proper constructor for DI"
                )
    def test_error_handling_consistency(self):
        all_python_files = self._get_all_python_files()
        for file_path in all_python_files:
            if "test" in str(file_path).lower():
                continue
            try:
                tree = self._parse_python_file(file_path)
                bare_except_count = 0
                for node in ast.walk(tree):
                    if isinstance(node, ast.ExceptHandler) and node.type is None:
                        bare_except_count += 1
                if bare_except_count > 2:
                    self.fail(f"File {file_path} has excessive bare except clauses ({bare_except_count}). Consider specific exception handling.")
            except SyntaxError:
                continue
    def test_cross_cutting_concerns_centralization(self):
        all_python_files = self._get_all_python_files()
        logging_files = []
        threading_files = []
        for file_path in all_python_files:
            imports = self._extract_imports_from_file(file_path)
            if "logging" in imports:
                logging_files.append(file_path)
            if "threading" in imports or "concurrent.futures" in imports:
                threading_files.append(file_path)
        self.assertGreater(
            len(logging_files), 0,
            "System should use centralized logging"
        )
    def test_session_management_layer_separation(self):
        session_files = self._get_python_files_in_package("session")
        forbidden_imports = [
            "gui.",
            "PyQt5.QtWidgets",
        ]
        for file_path in session_files:
            imports = self._extract_imports_from_file(file_path)
            for forbidden_import in forbidden_imports:
                self.assertNotIn(
                    forbidden_import,
                    imports,
                    f"Session file {file_path} should not directly access GUI layer"
                )
    def _get_python_files_in_package(self, package_name: str) -> List[Path]:
        package_path = self.python_app_root / package_name
        if not package_path.exists():
            return []
        return list(package_path.rglob("*.py"))
    def _get_python_files_with_pattern(self, pattern: str) -> List[Path]:
        return list(self.python_app_root.rglob(pattern))
    def _get_all_python_files(self) -> List[Path]:
        return [f for f in self.python_app_root.rglob("*.py") if f.is_file()]
    def _extract_imports_from_file(self, file_path: Path) -> Set[str]:
        try:
            tree = self._parse_python_file(file_path)
            imports = set()
            for node in ast.walk(tree):
                if isinstance(node, ast.Import):
                    for alias in node.names:
                        imports.add(alias.name)
                elif isinstance(node, ast.ImportFrom):
                    if node.module:
                        imports.add(node.module)
            return imports
        except Exception as e:
            self.fail(f"Failed to parse file {file_path}: {e}")
            return set()
    def _parse_python_file(self, file_path: Path) -> ast.AST:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        return ast.parse(content)
class PythonArchitectureValidationSuite:
    @staticmethod
    def run_validation():
        loader = unittest.TestLoader()
        suite = loader.loadTestsFromTestCase(PythonArchitectureTest)
        runner = unittest.TextTestRunner(verbosity=2)
        result = runner.run(suite)
        return result.wasSuccessful()
if __name__ == "__main__":
    unittest.main()