"""
Logging configuration for the unified test framework.

This module provides logging utilities compatible with the test environment.
"""

import logging
import sys
from typing import Optional

def get_logger(name: Optional[str] = None) -> logging.Logger:
    """
    Get a logger instance for testing.
    
    Args:
        name: Logger name, defaults to calling module
        
    Returns:
        Configured logger instance
    """
    if name is None:
        # Get caller's module name
        frame = sys._getframe(1)
        name = frame.f_globals.get('__name__', 'test_logger')
    
    logger = logging.getLogger(name)
    
    # Only configure if not already configured
    if not logger.handlers:
        handler = logging.StreamHandler()
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        handler.setFormatter(formatter)
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)
    
    return logger

def configure_test_logging(level: str = "INFO") -> None:
    """
    Configure logging for test environment.
    
    Args:
        level: Logging level (DEBUG, INFO, WARNING, ERROR)
    """
    numeric_level = getattr(logging, level.upper(), logging.INFO)
    logging.basicConfig(
        level=numeric_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        force=True
    )

# Default logger for tests
test_logger = get_logger('tests_unified')