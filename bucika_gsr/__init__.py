"""
Bucika GSR - Comprehensive Evaluation Suite

A comprehensive evaluation suite for machine learning metrics including
classification, regression, clustering, and ranking metrics.
"""

__version__ = "0.1.0"
__author__ = "buccancs"

from .core import EvaluationSuite
from .metrics import (
    classification,
    regression, 
    clustering,
    ranking
)

__all__ = [
    "EvaluationSuite",
    "classification",
    "regression", 
    "clustering",
    "ranking"
]