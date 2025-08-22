"""Classification metrics."""

import numpy as np
from typing import Optional, List
from sklearn.metrics import (
    accuracy_score, 
    precision_score, 
    recall_score, 
    f1_score,
    roc_auc_score,
    confusion_matrix,
    classification_report
)
from ..core import BaseMetric


class Accuracy(BaseMetric):
    """Accuracy metric for classification."""
    
    def __init__(self):
        super().__init__("accuracy", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute accuracy score."""
        return float(accuracy_score(y_true, y_pred))


class Precision(BaseMetric):
    """Precision metric for classification."""
    
    def __init__(self, average: str = "binary"):
        super().__init__(f"precision_{average}", higher_is_better=True)
        self.average = average
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute precision score."""
        return float(precision_score(y_true, y_pred, average=self.average, zero_division=0))


class Recall(BaseMetric):
    """Recall metric for classification."""
    
    def __init__(self, average: str = "binary"):
        super().__init__(f"recall_{average}", higher_is_better=True)
        self.average = average
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute recall score."""
        return float(recall_score(y_true, y_pred, average=self.average, zero_division=0))


class F1Score(BaseMetric):
    """F1 score metric for classification."""
    
    def __init__(self, average: str = "binary"):
        super().__init__(f"f1_{average}", higher_is_better=True)
        self.average = average
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute F1 score."""
        return float(f1_score(y_true, y_pred, average=self.average, zero_division=0))


class ROCAUC(BaseMetric):
    """ROC AUC metric for classification."""
    
    def __init__(self, average: str = "macro"):
        super().__init__(f"roc_auc_{average}", higher_is_better=True)
        self.average = average
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute ROC AUC score."""
        try:
            # Handle binary and multiclass cases
            if len(np.unique(y_true)) == 2:
                return float(roc_auc_score(y_true, y_pred))
            else:
                return float(roc_auc_score(y_true, y_pred, multi_class='ovr', average=self.average))
        except ValueError as e:
            # Handle cases where ROC AUC cannot be computed
            return 0.0


def get_all_classification_metrics() -> List[BaseMetric]:
    """Get all available classification metrics."""
    return [
        Accuracy(),
        Precision("binary"),
        Precision("macro"),
        Precision("micro"),
        Recall("binary"),
        Recall("macro"), 
        Recall("micro"),
        F1Score("binary"),
        F1Score("macro"),
        F1Score("micro"),
        ROCAUC("macro")
    ]