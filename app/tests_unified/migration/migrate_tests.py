#!/usr/bin/env python3
"""
Migration script for moving to unified testing framework.

This script helps developers migrate from the old fragmented testing
structure to the new unified framework.
"""

import os
import shutil
import sys
from pathlib import Path
from typing import Dict, List, Tuple
import argparse

# Mapping of old test locations to new unified structure
MIGRATION_MAPPING = {
    # Original tests directory
    "tests/browser/": "tests_unified/browser/",
    "tests/e2e/": "tests_unified/system/workflows/",
    "tests/gui/": "tests_unified/unit/python/",
    "tests/hardware/": "tests_unified/hardware/",
    "tests/integration/": "tests_unified/integration/device_coordination/",
    "tests/visual/": "tests_unified/visual/",
    "tests/web/": "tests_unified/unit/python/",
    "tests/load/": "tests_unified/performance/load/",
    
    # Evaluation suite
    "evaluation_suite/foundation/": "tests_unified/evaluation/foundation/",
    "evaluation_suite/integration/": "tests_unified/integration/device_coordination/",
    "evaluation_suite/performance/": "tests_unified/performance/benchmarks/",
    "evaluation_suite/framework/": "tests_unified/evaluation/metrics/",
    
    # PythonApp scattered tests
    "PythonApp/test_*.py": "tests_unified/unit/python/",
    "PythonApp/system_test.py": "tests_unified/system/workflows/",
    "PythonApp/production/": "tests_unified/performance/endurance/",
    
    # Root level tests
    "test_*.py": "tests_unified/unit/python/",
}

# Test categories and their appropriate markers
TEST_MARKERS = {
    "unit": ["unit"],
    "integration": ["integration"],
    "system": ["system", "e2e"],
    "performance": ["performance", "load", "stress"],
    "evaluation": ["evaluation", "research"],
    "android": ["android"],
    "hardware": ["hardware", "hardware_loop"],
    "browser": ["browser"],
    "visual": ["visual"],
    "gui": ["gui"]
}

class TestMigrator:
    """Handles migration from old test structure to unified framework"""
    
    def __init__(self, project_root: Path, dry_run: bool = True):
        self.project_root = project_root
        self.dry_run = dry_run
        self.migration_log = []
        
    def scan_existing_tests(self) -> Dict[str, List[Path]]:
        """Scan for existing test files in old structure"""
        test_files = {}
        
        # Scan original tests directory
        tests_dir = self.project_root / "tests"
        if tests_dir.exists():
            for test_file in tests_dir.rglob("test_*.py"):
                category = self._categorize_test_file(test_file)
                if category not in test_files:
                    test_files[category] = []
                test_files[category].append(test_file)
        
        # Scan evaluation suite
        eval_dir = self.project_root / "evaluation_suite"
        if eval_dir.exists():
            for test_file in eval_dir.rglob("*.py"):
                if test_file.name != "__init__.py":
                    category = "evaluation"
                    if category not in test_files:
                        test_files[category] = []
                    test_files[category].append(test_file)
        
        # Scan PythonApp
        python_dir = self.project_root / "PythonApp"
        if python_dir.exists():
            for test_file in python_dir.rglob("test_*.py"):
                category = self._categorize_test_file(test_file)
                if category not in test_files:
                    test_files[category] = []
                test_files[category].append(test_file)
                
            # Check for system_test.py
            system_test = python_dir / "system_test.py"
            if system_test.exists():
                if "system" not in test_files:
                    test_files["system"] = []
                test_files["system"].append(system_test)
        
        # Scan for root level test files
        for test_file in self.project_root.glob("test_*.py"):
            category = self._categorize_test_file(test_file)
            if category not in test_files:
                test_files[category] = []
            test_files[category].append(test_file)
        
        return test_files
    
    def _categorize_test_file(self, test_file: Path) -> str:
        """Categorize test file based on path and name"""
        path_str = str(test_file.relative_to(self.project_root))
        
        # Check path-based categorization
        if "browser" in path_str:
            return "browser"
        elif "visual" in path_str:
            return "visual"
        elif "hardware" in path_str:
            return "hardware"
        elif "android" in path_str:
            return "android"
        elif "integration" in path_str:
            return "integration"
        elif "performance" in path_str or "endurance" in path_str or "load" in path_str:
            return "performance"
        elif "e2e" in path_str or "system" in path_str:
            return "system"
        elif "evaluation" in path_str:
            return "evaluation"
        elif "gui" in path_str:
            return "unit"
        else:
            return "unit"
    
    def generate_migration_plan(self, test_files: Dict[str, List[Path]]) -> List[Tuple[Path, Path]]:
        """Generate migration plan (source -> destination mappings)"""
        migration_plan = []
        
        for category, files in test_files.items():
            target_dir = self._get_target_directory(category)
            
            for source_file in files:
                # Skip files that are already in unified structure
                if "tests_unified" in str(source_file):
                    continue
                    
                target_file = target_dir / source_file.name
                migration_plan.append((source_file, target_file))
        
        return migration_plan
    
    def _get_target_directory(self, category: str) -> Path:
        """Get target directory for a test category"""
        unified_root = self.project_root / "tests_unified"
        
        mapping = {
            "unit": unified_root / "unit" / "python",
            "integration": unified_root / "integration" / "device_coordination",
            "system": unified_root / "system" / "workflows",
            "performance": unified_root / "performance" / "benchmarks",
            "evaluation": unified_root / "evaluation" / "foundation",
            "android": unified_root / "unit" / "android",
            "hardware": unified_root / "hardware",
            "browser": unified_root / "browser",
            "visual": unified_root / "visual"
        }
        
        return mapping.get(category, unified_root / "unit" / "python")
    
    def execute_migration(self, migration_plan: List[Tuple[Path, Path]]):
        """Execute the migration plan"""
        
        for source, target in migration_plan:
            self._log(f"{'DRY RUN: ' if self.dry_run else ''}Migrating {source} -> {target}")
            
            if not self.dry_run:
                # Ensure target directory exists
                target.parent.mkdir(parents=True, exist_ok=True)
                
                # Copy file (don't move to preserve original during transition)
                shutil.copy2(source, target)
                
                # Update imports in the copied file
                self._update_imports(target)
    
    def _update_imports(self, test_file: Path):
        """Update imports in migrated test files"""
        try:
            content = test_file.read_text()
            
            # Update common import patterns
            updates = [
                ("from tests.", "from tests_unified."),
                ("import tests.", "import tests_unified."),
                ("from evaluation_suite.", "from tests_unified.evaluation."),
                ("import evaluation_suite.", "import tests_unified.evaluation."),
            ]
            
            for old_import, new_import in updates:
                content = content.replace(old_import, new_import)
            
            test_file.write_text(content)
            self._log(f"Updated imports in {test_file}")
            
        except Exception as e:
            self._log(f"Warning: Could not update imports in {test_file}: {e}")
    
    def generate_migration_report(self, test_files: Dict[str, List[Path]], 
                                migration_plan: List[Tuple[Path, Path]]) -> str:
        """Generate migration report"""
        
        report = """# Test Migration Report

## Summary
"""
        
        total_files = sum(len(files) for files in test_files.values())
        migration_files = len(migration_plan)
        
        report += f"""
- **Total test files found:** {total_files}
- **Files to migrate:** {migration_files}
- **Files already in unified structure:** {total_files - migration_files}

## Files by Category
"""
        
        for category, files in test_files.items():
            report += f"\n### {category.title()}\n"
            for file_path in files:
                status = "[PASS] Already migrated" if "tests_unified" in str(file_path) else "[CLIPBOARD] To migrate"
                report += f"- {status} `{file_path}`\n"
        
        report += "\n## Migration Plan\n"
        for source, target in migration_plan:
            report += f"- `{source}` -> `{target}`\n"
        
        report += f"""
## Next Steps

1. **Review the migration plan** above
2. **Run migration:** `python tests_unified/migration/migrate_tests.py --execute`
3. **Update CI/CD workflows** to use unified test runner
4. **Update documentation** with new test structure
5. **Test the migration** by running unified test suite

## Migration Commands

```bash
# Dry run (default)
python tests_unified/migration/migrate_tests.py

# Execute migration
python tests_unified/migration/migrate_tests.py --execute

# Generate report only
python tests_unified/migration/migrate_tests.py --report-only
```

## Unified Test Runner Usage

After migration, use the unified test runner:

```bash
# Run all tests
python tests_unified/runners/run_unified_tests.py

# Run specific categories
python tests_unified/runners/run_unified_tests.py --category android
python tests_unified/runners/run_unified_tests.py --level unit

# Quick validation
python tests_unified/runners/run_unified_tests.py --quick
```
"""
        
        return report
    
    def _log(self, message: str):
        """Log migration actions"""
        print(message)
        self.migration_log.append(message)

def main():
    parser = argparse.ArgumentParser(description="Migrate tests to unified framework")
    parser.add_argument("--execute", action="store_true", 
                       help="Execute migration (default is dry run)")
    parser.add_argument("--report-only", action="store_true",
                       help="Generate report only, don't migrate")
    parser.add_argument("--project-root", type=Path, default=Path.cwd(),
                       help="Project root directory")
    
    args = parser.parse_args()
    
    # Initialize migrator
    migrator = TestMigrator(args.project_root, dry_run=not args.execute)
    
    print(f"[INFO] Scanning for test files in {args.project_root}")
    test_files = migrator.scan_existing_tests()
    
    print(f"[CLIPBOARD] Found {sum(len(files) for files in test_files.values())} test files")
    
    # Generate migration plan
    migration_plan = migrator.generate_migration_plan(test_files)
    
    # Generate report
    report = migrator.generate_migration_report(test_files, migration_plan)
    
    # Save report
    report_file = args.project_root / "test_migration_report.md"
    report_file.write_text(report)
    print(f"[CHART] Migration report saved to {report_file}")
    
    if args.report_only:
        print("[PASS] Report generation complete")
        return
    
    if migration_plan:
        if args.execute:
            print(f"[DEPLOY] Executing migration of {len(migration_plan)} files...")
            migrator.execute_migration(migration_plan)
            print("[PASS] Migration complete!")
        else:
            print(f"[INFO] DRY RUN: Would migrate {len(migration_plan)} files")
            migrator.execute_migration(migration_plan)
            print("[PASS] Dry run complete. Use --execute to perform actual migration.")
    else:
        print("[PASS] No files need migration - all tests are already in unified structure")

if __name__ == "__main__":
    main()