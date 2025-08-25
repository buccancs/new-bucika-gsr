#!/usr/bin/env python3
"""
Console demo for the Bucika GSR PC Orchestrator (headless mode)
"""

import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from bucika_gsr_pc import main

if __name__ == "__main__":
    # Force headless mode
    sys.argv.append('--headless')
    main()