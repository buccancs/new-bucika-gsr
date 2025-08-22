"""Regression metrics."""

import numpy as np
from typing import List
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    r2_score,
    mean_absolute_percentage_error
)
from ..core import BaseMetric


class MeanAbsoluteError(BaseMetric):
    """Mean Absolute Error metric for regression."""
    
    def __init__(self):
        super().__init__("mae", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute mean absolute error."""
        return float(mean_absolute_error(y_true, y_pred))


class MeanSquaredError(BaseMetric):
    """Mean Squared Error metric for regression."""
    
    def __init__(self):
        super().__init__("mse", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute mean squared error."""
        return float(mean_squared_error(y_true, y_pred))


class RootMeanSquaredError(BaseMetric):
    """Root Mean Squared Error metric for regression."""
    
    def __init__(self):
        super().__init__("rmse", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute root mean squared error."""
        return float(np.sqrt(mean_squared_error(y_true, y_pred)))


class R2Score(BaseMetric):
    """R-squared (coefficient of determination) metric for regression."""
    
    def __init__(self):
        super().__init__("r2", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute R-squared score."""
        return float(r2_score(y_true, y_pred))


class MeanAbsolutePercentageError(BaseMetric):
    """Mean Absolute Percentage Error metric for regression."""
    
    def __init__(self):
        super().__init__("mape", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute mean absolute percentage error."""
        return float(mean_absolute_percentage_error(y_true, y_pred))


class MeanError(BaseMetric):
    """Mean Error (Bias) metric for regression."""
    
    def __init__(self):
        super().__init__("me", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute mean error (bias)."""
        return float(np.mean(y_pred - y_true))


def get_all_regression_metrics() -> List[BaseMetric]:
    """Get all available regression metrics."""
    return [
        MeanAbsoluteError(),
        MeanSquaredError(),
        RootMeanSquaredError(),
        R2Score(),
        MeanAbsolutePercentageError(),
        MeanError()
    ]