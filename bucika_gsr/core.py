"""Core evaluation framework."""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Union, Optional
import numpy as np


class BaseMetric(ABC):
    """Base class for all metrics."""
    
    def __init__(self, name: str, higher_is_better: bool = True):
        """
        Initialize metric.
        
        Args:
            name: Name of the metric
            higher_is_better: Whether higher values indicate better performance
        """
        self.name = name
        self.higher_is_better = higher_is_better
    
    @abstractmethod
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """
        Compute the metric.
        
        Args:
            y_true: True values
            y_pred: Predicted values
            **kwargs: Additional arguments specific to metric
            
        Returns:
            Computed metric value
        """
        pass
    
    def __call__(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Make metric callable."""
        return self.compute(y_true, y_pred, **kwargs)


class EvaluationSuite:
    """Main evaluation suite for computing multiple metrics."""
    
    def __init__(self):
        """Initialize evaluation suite."""
        self.metrics = {}
    
    def add_metric(self, metric: BaseMetric) -> None:
        """
        Add a metric to the suite.
        
        Args:
            metric: Metric instance to add
        """
        self.metrics[metric.name] = metric
    
    def remove_metric(self, name: str) -> None:
        """
        Remove a metric from the suite.
        
        Args:
            name: Name of metric to remove
        """
        if name in self.metrics:
            del self.metrics[name]
    
    def evaluate(
        self, 
        y_true: np.ndarray, 
        y_pred: np.ndarray, 
        metrics: Optional[List[str]] = None,
        **kwargs
    ) -> Dict[str, float]:
        """
        Evaluate all or specified metrics.
        
        Args:
            y_true: True values
            y_pred: Predicted values
            metrics: List of metric names to compute. If None, compute all.
            **kwargs: Additional arguments passed to metrics
            
        Returns:
            Dictionary mapping metric names to computed values
        """
        if metrics is None:
            metrics = list(self.metrics.keys())
        
        results = {}
        for metric_name in metrics:
            if metric_name not in self.metrics:
                raise ValueError(f"Metric '{metric_name}' not found in suite")
            
            try:
                results[metric_name] = self.metrics[metric_name](y_true, y_pred, **kwargs)
            except Exception as e:
                results[metric_name] = f"Error: {str(e)}"
        
        return results
    
    def list_metrics(self) -> List[str]:
        """List all available metrics."""
        return list(self.metrics.keys())
    
    def get_metric_info(self, name: str) -> Dict[str, Any]:
        """
        Get information about a metric.
        
        Args:
            name: Metric name
            
        Returns:
            Dictionary with metric information
        """
        if name not in self.metrics:
            raise ValueError(f"Metric '{name}' not found")
        
        metric = self.metrics[name]
        return {
            "name": metric.name,
            "higher_is_better": metric.higher_is_better,
            "class": type(metric).__name__
        }