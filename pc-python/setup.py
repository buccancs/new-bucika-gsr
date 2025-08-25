"""
Setup script for Bucika GSR PC Orchestrator Python implementation
"""

from setuptools import setup, find_packages
from pathlib import Path

# Read the README file
readme_path = Path(__file__).parent / "README.md"
long_description = readme_path.read_text() if readme_path.exists() else ""

setup(
    name="bucika-gsr-pc-orchestrator",
    version="1.0.0",
    description="PC orchestrator for coordinating GSR data collection from Android devices",
    long_description=long_description,
    long_description_content_type="text/markdown",
    author="Bucika GSR Team",
    author_email="support@bucika-gsr.com",
    url="https://github.com/buccancs/new-bucika-gsr",
    package_dir={"": "src"},
    packages=find_packages(where="src"),
    python_requires=">=3.8",
    install_requires=[
        "websockets>=12.0",
        "aiofiles>=23.2.1",
        "zeroconf>=0.131.0", 
        "pydantic>=2.5.0",
        "loguru>=0.7.2",
    ],
    extras_require={
        "dev": [
            "black>=23.12.0",
            "pytest>=7.4.3",
            "pytest-asyncio>=0.23.2",
        ],
        "gui": [
            "tkinter-tooltip>=2.1.0",
            "pillow>=10.1.0",
        ]
    },
    entry_points={
        "console_scripts": [
            "bucika-gsr-orchestrator=bucika_gsr_pc:main",
        ],
    },
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Healthcare Industry",
        "Intended Audience :: Science/Research",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Topic :: Scientific/Engineering :: Medical Science Apps.",
        "Topic :: System :: Networking",
    ],
    keywords="gsr physiological-data websocket android medical research",
)