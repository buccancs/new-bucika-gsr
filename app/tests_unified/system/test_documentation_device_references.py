"""
Test module to validate correct device references in documentation.

This module ensures that documentation files contain correct Topdon TC001 specifications
instead of incorrect FLIR device references.
"""

import os
import pytest
from pathlib import Path


class TestDocumentationDeviceReferences:
    """Test class for validating device references in documentation."""
    
    @pytest.fixture
    def repo_root(self):
        """Get the repository root path."""
        return Path(__file__).parent.parent.parent

    def test_architecture_md_device_reference(self, repo_root):
        """Test that architecture.md contains correct Topdon TC001 reference."""
        architecture_file = repo_root / "architecture.md"
        assert architecture_file.exists(), "architecture.md file not found"
        
        content = architecture_file.read_text()
        
        # Should contain Topdon TC001 reference
        assert "Topdon TC001" in content, "architecture.md should reference Topdon TC001"
        
        # Should NOT contain FLIR references in thermal camera context
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if "Thermal Camera" in line and "FLIR" in line:
                # Check if this is specifically the problematic line around line 75
                if i >= 70 and i <= 80:
                    pytest.fail(f"Found FLIR reference in thermal camera section at line {i+1}: {line}")

    def test_chapter3_diagrams_device_specs(self, repo_root):
        """Test that chapter3_diagrams.md contains correct Topdon TC001 specifications."""
        diagrams_file = repo_root / "docs" / "diagrams" / "chapter3_diagrams.md"
        assert diagrams_file.exists(), "chapter3_diagrams.md file not found"
        
        content = diagrams_file.read_text()
        
        # Should contain correct specifications
        assert "256x192" in content, "chapter3_diagrams.md should contain correct resolution 256x192"
        assert "25fps" in content, "chapter3_diagrams.md should contain correct frame rate 25fps"
        assert "Topdon TC001" in content, "chapter3_diagrams.md should reference Topdon TC001"
        
        # Should NOT contain incorrect specifications
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if "THERMAL[" in line and ("320x240" in line or "15fps" in line or "FLIR Lepton" in line):
                pytest.fail(f"Found incorrect thermal camera specs at line {i+1}: {line}")

    def test_mermaid_appendices_device_specs(self, repo_root):
        """Test that mermaid_appendices.md contains correct Topdon TC001 specifications."""
        appendices_file = repo_root / "docs" / "diagrams" / "mermaid_appendices.md"
        assert appendices_file.exists(), "mermaid_appendices.md file not found"
        
        content = appendices_file.read_text()
        
        # Should contain correct specifications
        assert "256x192" in content, "mermaid_appendices.md should contain correct resolution 256x192"
        assert "25fps" in content, "mermaid_appendices.md should contain correct frame rate 25fps"
        assert "Topdon TC001" in content, "mermaid_appendices.md should reference Topdon TC001"
        
        # Should NOT contain incorrect specifications
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if "THERMAL[" in line and ("320x240" in line or "15fps" in line or "FLIR Lepton" in line):
                pytest.fail(f"Found incorrect thermal camera specs at line {i+1}: {line}")

    def test_no_remaining_flir_references_in_thermal_context(self, repo_root):
        """Test that no FLIR references remain in thermal camera documentation contexts."""
        documentation_files = [
            repo_root / "architecture.md",
            repo_root / "docs" / "diagrams" / "chapter3_diagrams.md", 
            repo_root / "docs" / "diagrams" / "mermaid_appendices.md"
        ]
        
        problematic_patterns = [
            ("320x240", "15fps"),  # Incorrect resolution and frame rate
            ("FLIR Lepton", "Thermal"),  # FLIR device in thermal context
            ("FLIR/Seek", "Thermal"),   # FLIR integration in thermal context
        ]
        
        for file_path in documentation_files:
            if file_path.exists():
                content = file_path.read_text()
                for pattern1, pattern2 in problematic_patterns:
                    if pattern1 in content and pattern2 in content:
                        # Check if they appear together in thermal camera context
                        lines = content.split('\n')
                        for i, line in enumerate(lines):
                            if (pattern1 in line and pattern2 in line) or \
                               (pattern1 in line and any(pattern2 in lines[j] for j in range(max(0, i-2), min(len(lines), i+3)))):
                                pytest.fail(f"Found problematic pattern '{pattern1}' + '{pattern2}' in {file_path.name} around line {i+1}")