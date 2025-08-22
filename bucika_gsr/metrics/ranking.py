"""Ranking metrics."""

import numpy as np
from typing import List, Optional
from ..core import BaseMetric


class NDCG(BaseMetric):
    """Normalized Discounted Cumulative Gain metric for ranking."""
    
    def __init__(self, k: Optional[int] = None):
        name = f"ndcg@{k}" if k else "ndcg"
        super().__init__(name, higher_is_better=True)
        self.k = k
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """
        Compute NDCG score.
        
        Args:
            y_true: True relevance scores
            y_pred: Predicted scores (used for ranking)
        """
        if len(y_true) == 0:
            return 0.0
            
        # Sort by predicted scores (descending)
        order = np.argsort(y_pred)[::-1]
        y_true_sorted = y_true[order]
        
        # Apply k cutoff if specified
        if self.k is not None:
            y_true_sorted = y_true_sorted[:self.k]
        
        # Compute DCG
        dcg = self._compute_dcg(y_true_sorted)
        
        # Compute IDCG (ideal DCG)
        y_true_ideal = np.sort(y_true)[::-1]
        if self.k is not None:
            y_true_ideal = y_true_ideal[:self.k]
        idcg = self._compute_dcg(y_true_ideal)
        
        if idcg == 0:
            return 0.0
        
        return float(dcg / idcg)
    
    def _compute_dcg(self, relevance_scores: np.ndarray) -> float:
        """Compute Discounted Cumulative Gain."""
        if len(relevance_scores) == 0:
            return 0.0
        
        dcg = relevance_scores[0]
        for i in range(1, len(relevance_scores)):
            dcg += relevance_scores[i] / np.log2(i + 1)
        
        return float(dcg)


class MAP(BaseMetric):
    """Mean Average Precision metric for ranking."""
    
    def __init__(self):
        super().__init__("map", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """
        Compute Mean Average Precision.
        
        Args:
            y_true: Binary relevance labels (1 for relevant, 0 for non-relevant)
            y_pred: Predicted scores (used for ranking)
        """
        if len(y_true) == 0:
            return 0.0
        
        # Sort by predicted scores (descending)
        order = np.argsort(y_pred)[::-1]
        y_true_sorted = y_true[order]
        
        # Compute average precision
        num_relevant = np.sum(y_true)
        if num_relevant == 0:
            return 0.0
        
        precision_sum = 0.0
        relevant_found = 0
        
        for i, is_relevant in enumerate(y_true_sorted):
            if is_relevant:
                relevant_found += 1
                precision_at_i = relevant_found / (i + 1)
                precision_sum += precision_at_i
        
        return float(precision_sum / num_relevant)


class PrecisionAtK(BaseMetric):
    """Precision at K metric for ranking."""
    
    def __init__(self, k: int):
        super().__init__(f"precision@{k}", higher_is_better=True)
        self.k = k
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """
        Compute Precision at K.
        
        Args:
            y_true: Binary relevance labels
            y_pred: Predicted scores (used for ranking)
        """
        if len(y_true) == 0 or self.k == 0:
            return 0.0
        
        # Sort by predicted scores (descending)
        order = np.argsort(y_pred)[::-1]
        y_true_sorted = y_true[order]
        
        # Take top k
        k_actual = min(self.k, len(y_true_sorted))
        top_k = y_true_sorted[:k_actual]
        
        return float(np.sum(top_k) / k_actual)


class RecallAtK(BaseMetric):
    """Recall at K metric for ranking."""
    
    def __init__(self, k: int):
        super().__init__(f"recall@{k}", higher_is_better=True)
        self.k = k
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """
        Compute Recall at K.
        
        Args:
            y_true: Binary relevance labels
            y_pred: Predicted scores (used for ranking)
        """
        num_relevant = np.sum(y_true)
        if num_relevant == 0:
            return 0.0
        
        if len(y_true) == 0 or self.k == 0:
            return 0.0
        
        # Sort by predicted scores (descending)
        order = np.argsort(y_pred)[::-1]
        y_true_sorted = y_true[order]
        
        # Take top k
        k_actual = min(self.k, len(y_true_sorted))
        top_k = y_true_sorted[:k_actual]
        
        return float(np.sum(top_k) / num_relevant)


def get_all_ranking_metrics() -> List[BaseMetric]:
    """Get all available ranking metrics."""
    return [
        NDCG(),
        NDCG(5),
        NDCG(10),
        MAP(),
        PrecisionAtK(1),
        PrecisionAtK(5),
        PrecisionAtK(10),
        RecallAtK(5),
        RecallAtK(10)
    ]