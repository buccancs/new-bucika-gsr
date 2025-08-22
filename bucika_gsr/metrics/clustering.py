"""Clustering metrics."""

import numpy as np
from typing import List
from sklearn.metrics import (
    silhouette_score,
    adjusted_rand_score,
    adjusted_mutual_info_score,
    calinski_harabasz_score,
    davies_bouldin_score
)
from ..core import BaseMetric


class SilhouetteScore(BaseMetric):
    """Silhouette Score metric for clustering."""
    
    def __init__(self):
        super().__init__("silhouette_score", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute silhouette score. Requires X (features) in kwargs."""
        X = kwargs.get('X')
        if X is None:
            raise ValueError("Silhouette score requires feature matrix X in kwargs")
        return float(silhouette_score(X, y_pred))


class AdjustedRandScore(BaseMetric):
    """Adjusted Rand Index metric for clustering."""
    
    def __init__(self):
        super().__init__("adjusted_rand_score", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute adjusted rand index."""
        return float(adjusted_rand_score(y_true, y_pred))


class AdjustedMutualInfoScore(BaseMetric):
    """Adjusted Mutual Information Score metric for clustering."""
    
    def __init__(self):
        super().__init__("adjusted_mutual_info_score", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute adjusted mutual information score."""
        return float(adjusted_mutual_info_score(y_true, y_pred))


class CalinskiHarabaszScore(BaseMetric):
    """Calinski-Harabasz Score metric for clustering."""
    
    def __init__(self):
        super().__init__("calinski_harabasz_score", higher_is_better=True)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute Calinski-Harabasz score. Requires X (features) in kwargs."""
        X = kwargs.get('X')
        if X is None:
            raise ValueError("Calinski-Harabasz score requires feature matrix X in kwargs")
        return float(calinski_harabasz_score(X, y_pred))


class DaviesBouldinScore(BaseMetric):
    """Davies-Bouldin Score metric for clustering."""
    
    def __init__(self):
        super().__init__("davies_bouldin_score", higher_is_better=False)
    
    def compute(self, y_true: np.ndarray, y_pred: np.ndarray, **kwargs) -> float:
        """Compute Davies-Bouldin score. Requires X (features) in kwargs."""
        X = kwargs.get('X')
        if X is None:
            raise ValueError("Davies-Bouldin score requires feature matrix X in kwargs")
        return float(davies_bouldin_score(X, y_pred))


def get_all_clustering_metrics() -> List[BaseMetric]:
    """Get all available clustering metrics."""
    return [
        SilhouetteScore(),
        AdjustedRandScore(),
        AdjustedMutualInfoScore(),
        CalinskiHarabaszScore(),
        DaviesBouldinScore()
    ]