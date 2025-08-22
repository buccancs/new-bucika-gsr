#!/usr/bin/env python3
"""
Requirements Coverage Analysis for Multi-Sensor Recording System

This script analyzes the test coverage of Functional Requirements (FR) and 
Non-Functional Requirements (NFR) as defined in docs/thesis_report/final/latex/3.tex

Performs traceability analysis to ensure all requirements are tested or evaluated.
"""

import json
import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple, Set
from dataclasses import dataclass

@dataclass
class Requirement:
    """Represents a single requirement"""
    id: str
    type: str  # 'FR' or 'NFR'
    title: str
    description: str
    tested: bool = False
    test_files: List[str] = None
    coverage_notes: str = ""

class RequirementsCoverageAnalyzer:
    """Analyzes test coverage for FR and NFR requirements"""
    
    def __init__(self):
        self.requirements: Dict[str, Requirement] = {}
        self.test_files: List[Path] = []
        self.repo_root = Path(__file__).parent.parent.parent
        
    def extract_requirements_from_tex(self) -> None:
        """Extract FR and NFR requirements from 3.tex"""
        tex_file = self.repo_root / "docs" / "thesis_report" / "final" / "latex" / "3.tex"
        
        if not tex_file.exists():
            print(f"Warning: Requirements file not found at {tex_file}")
            return
            
        with open(tex_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Extract FR requirements
        fr_pattern = r'\\item\s+(FR\d+):\s+([^-]+)\s+--\s+([^\\]+)'
        fr_matches = re.findall(fr_pattern, content, re.MULTILINE | re.DOTALL)
        
        for match in fr_matches:
            req_id, title, description = match
            title = title.strip()
            description = description.strip()
            
            self.requirements[req_id] = Requirement(
                id=req_id,
                type="FR",
                title=title,
                description=description,
                test_files=[]
            )
        
        # Extract NFR requirements
        nfr_pattern = r'\\item\s+(NFR\d+):\s+([^-]+)\s+--\s+([^\\]+)'
        nfr_matches = re.findall(nfr_pattern, content, re.MULTILINE | re.DOTALL)
        
        for match in nfr_matches:
            req_id, title, description = match
            title = title.strip()
            description = description.strip()
            
            self.requirements[req_id] = Requirement(
                id=req_id,
                type="NFR",
                title=title,
                description=description,
                test_files=[]
            )
        
        print(f"Extracted {len([r for r in self.requirements.values() if r.type == 'FR'])} FR and "
              f"{len([r for r in self.requirements.values() if r.type == 'NFR'])} NFR requirements")
    
    def find_test_files(self) -> None:
        """Find all test files in the unified testing framework"""
        tests_dir = self.repo_root / "tests_unified"
        
        if not tests_dir.exists():
            print(f"Warning: Unified tests directory not found at {tests_dir}")
            return
        
        # Find all Python test files
        self.test_files = list(tests_dir.rglob("*.py"))
        print(f"Found {len(self.test_files)} test files")
    
    def analyze_test_coverage(self) -> None:
        """Analyze which requirements are covered by tests"""
        
        # Define mapping of requirements to test patterns and keywords
        requirement_mappings = {
            'FR1': {
                'keywords': ['multi.*device', 'sensor.*integration', 'shimmer', 'bluetooth', 'simulation'],
                'test_paths': ['unit/sensors', 'hardware', 'integration/device'],
                'description': 'Multi-Device Sensor Integration'
            },
            'FR2': {
                'keywords': ['synchron.*recording', 'multi.*modal', 'session.*start', 'recording.*control'],
                'test_paths': ['system', 'integration', 'evaluation'],
                'description': 'Synchronised Multi-Modal Recording'
            },
            'FR3': {
                'keywords': ['time.*sync', 'clock.*sync', 'ntp', 'timestamp'],
                'test_paths': ['integration', 'system'],
                'description': 'Time Synchronisation Service'
            },
            'FR4': {
                'keywords': ['session.*manage', 'session.*create', 'metadata'],
                'test_paths': ['system', 'integration'],
                'description': 'Session Management'
            },
            'FR5': {
                'keywords': ['data.*record', 'storage', 'csv', 'video.*record'],
                'test_paths': ['system', 'integration'],
                'description': 'Data Recording and Storage'
            },
            'FR6': {
                'keywords': ['gui', 'interface', 'monitor', 'control'],
                'test_paths': ['visual', 'browser', 'system'],
                'description': 'User Interface for Monitoring & Control'
            },
            'FR7': {
                'keywords': ['device.*sync', 'signal', 'command.*protocol', 'json'],
                'test_paths': ['integration', 'system'],
                'description': 'Device Synchronisation and Signals'
            },
            'FR8': {
                'keywords': ['fault.*toleran', 'recovery', 'disconnect', 'offline'],
                'test_paths': ['integration', 'system'],
                'description': 'Fault Tolerance and Recovery'
            },
            'FR9': {
                'keywords': ['calibrat', 'checkerboard', 'pattern', 'thermal.*align'],
                'test_paths': ['unit/calibration', 'hardware'],
                'description': 'Calibration Utilities'
            },
            'FR10': {
                'keywords': ['data.*transfer', 'file.*transfer', 'aggregat'],
                'test_paths': ['integration', 'system'],
                'description': 'Data Transfer and Aggregation'
            },
            'NFR1': {
                'keywords': ['performance', 'real.*time', 'throughput', 'latency'],
                'test_paths': ['performance', 'evaluation/metrics'],
                'description': 'Performance (Real-Time Data Handling)'
            },
            'NFR2': {
                'keywords': ['temporal.*accuracy', 'sync.*accuracy', 'millisecond'],
                'test_paths': ['performance', 'integration'],
                'description': 'Temporal Accuracy'
            },
            'NFR3': {
                'keywords': ['reliability', 'fault.*toleran', 'robust'],
                'test_paths': ['system', 'integration'],
                'description': 'Reliability and Fault Tolerance'
            },
            'NFR4': {
                'keywords': ['data.*integrity', 'validation', 'corruption', 'checksum'],
                'test_paths': ['system', 'evaluation'],
                'description': 'Data Integrity and Validation'
            },
            'NFR5': {
                'keywords': ['security', 'encrypt', 'tls', 'authentication'],
                'test_paths': ['system', 'integration'],
                'description': 'Security'
            },
            'NFR6': {
                'keywords': ['usability', 'user.*interface', 'intuitive'],
                'test_paths': ['visual', 'browser', 'system'],
                'description': 'Usability'
            },
            'NFR7': {
                'keywords': ['scalability', 'multiple.*device', 'duration'],
                'test_paths': ['performance', 'system'],
                'description': 'Scalability'
            },
            'NFR8': {
                'keywords': ['maintainability', 'modular', 'configuration'],
                'test_paths': ['evaluation', 'unit'],
                'description': 'Maintainability and Modularity'
            }
        }
        
        # Analyze each test file for requirement coverage
        for test_file in self.test_files:
            if test_file.name == '__init__.py':
                continue
                
            try:
                with open(test_file, 'r', encoding='utf-8') as f:
                    content = f.read().lower()
                
                # Check each requirement
                for req_id, mapping in requirement_mappings.items():
                    if req_id not in self.requirements:
                        continue
                        
                    requirement = self.requirements[req_id]
                    
                    # Check if file path matches requirement
                    path_match = any(path_pattern in str(test_file).lower() 
                                   for path_pattern in mapping['test_paths'])
                    
                    # Check if content matches keywords
                    keyword_match = any(re.search(keyword, content) 
                                      for keyword in mapping['keywords'])
                    
                    if path_match or keyword_match:
                        requirement.tested = True
                        requirement.test_files.append(str(test_file.relative_to(self.repo_root)))
                        
                        if path_match and keyword_match:
                            requirement.coverage_notes += f"Strong coverage: {test_file.name}; "
                        elif path_match:
                            requirement.coverage_notes += f"Path coverage: {test_file.name}; "
                        elif keyword_match:
                            requirement.coverage_notes += f"Content coverage: {test_file.name}; "
                            
            except Exception as e:
                print(f"Warning: Could not analyze {test_file}: {e}")
                continue
    
    def generate_coverage_report(self) -> Dict:
        """Generate a comprehensive coverage report"""
        report = {
            'summary': {
                'total_requirements': len(self.requirements),
                'tested_requirements': len([r for r in self.requirements.values() if r.tested]),
                'untested_requirements': len([r for r in self.requirements.values() if not r.tested]),
                'coverage_percentage': 0
            },
            'functional_requirements': {},
            'non_functional_requirements': {},
            'gaps': [],
            'recommendations': []
        }
        
        if report['summary']['total_requirements'] > 0:
            report['summary']['coverage_percentage'] = (
                report['summary']['tested_requirements'] / 
                report['summary']['total_requirements'] * 100
            )
        
        # Organize requirements by type
        for req in self.requirements.values():
            req_data = {
                'id': req.id,
                'title': req.title,
                'tested': req.tested,
                'test_files': req.test_files,
                'coverage_notes': req.coverage_notes.strip('; ')
            }
            
            if req.type == 'FR':
                report['functional_requirements'][req.id] = req_data
            else:
                report['non_functional_requirements'][req.id] = req_data
            
            if not req.tested:
                report['gaps'].append({
                    'requirement_id': req.id,
                    'title': req.title,
                    'type': req.type,
                    'description': req.description[:100] + '...' if len(req.description) > 100 else req.description
                })
        
        # Generate recommendations
        if report['gaps']:
            report['recommendations'].extend([
                f"Implement tests for {len(report['gaps'])} untested requirements",
                "Focus on system-level tests for FR2, FR4, FR5 if missing",
                "Ensure performance tests cover NFR1, NFR2, NFR7",
                "Add security validation tests for NFR5",
                "Include usability tests for NFR6"
            ])
        else:
            report['recommendations'].append("Excellent coverage! Consider adding more edge case tests.")
        
        return report
    
    def print_coverage_summary(self, report: Dict) -> None:
        """Print a human-readable coverage summary"""
        print("\n" + "="*60)
        print("REQUIREMENTS COVERAGE ANALYSIS SUMMARY")
        print("="*60)
        
        summary = report['summary']
        print(f"Total Requirements: {summary['total_requirements']}")
        print(f"Tested Requirements: {summary['tested_requirements']}")
        print(f"Untested Requirements: {summary['untested_requirements']}")
        print(f"Coverage Percentage: {summary['coverage_percentage']:.1f}%")
        
        if report['gaps']:
            print(f"\n[ALERT] TESTING GAPS ({len(report['gaps'])} requirements):")
            for gap in report['gaps']:
                print(f"  - {gap['requirement_id']}: {gap['title']}")
        else:
            print("\n[PASS] ALL REQUIREMENTS HAVE TEST COVERAGE!")
        
        print(f"\n[NOTE] RECOMMENDATIONS:")
        for rec in report['recommendations']:
            print(f"  - {rec}")
        
        print("\n" + "="*60)

def main():
    """Main analysis function"""
    analyzer = RequirementsCoverageAnalyzer()
    
    # Extract requirements from thesis document
    analyzer.extract_requirements_from_tex()
    
    # Find all test files
    analyzer.find_test_files()
    
    # Analyze coverage
    analyzer.analyze_test_coverage()
    
    # Generate and display report
    report = analyzer.generate_coverage_report()
    analyzer.print_coverage_summary(report)
    
    # Save detailed report
    output_file = Path(__file__).parent / "requirements_coverage_report.json"
    with open(output_file, 'w') as f:
        json.dump(report, f, indent=2)
    
    print(f"\nDetailed report saved to: {output_file}")
    
    # Return exit code based on coverage
    if report['summary']['coverage_percentage'] < 80:
        print(f"\n[WARN]  Coverage below 80% threshold: {report['summary']['coverage_percentage']:.1f}%")
        return 1
    else:
        print(f"\n[PASS] Coverage meets 80% threshold: {report['summary']['coverage_percentage']:.1f}%")
        return 0

if __name__ == "__main__":
    sys.exit(main())