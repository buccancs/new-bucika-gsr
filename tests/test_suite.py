"""Tests for the evaluation suite."""

import pytest
import numpy as np
from bucika_gsr.core import EvaluationSuite
from bucika_gsr.metrics import classification, regression, clustering, ranking


class TestEvaluationSuite:
    """Test cases for the EvaluationSuite class."""
    
    def test_suite_creation(self):
        """Test creating evaluation suite."""
        suite = EvaluationSuite()
        assert len(suite.list_metrics()) == 0
    
    def test_add_remove_metrics(self):
        """Test adding and removing metrics."""
        suite = EvaluationSuite()
        metric = classification.Accuracy()
        
        suite.add_metric(metric)
        assert "accuracy" in suite.list_metrics()
        
        suite.remove_metric("accuracy")
        assert "accuracy" not in suite.list_metrics()
    
    def test_get_metric_info(self):
        """Test getting metric information."""
        suite = EvaluationSuite()
        metric = classification.Accuracy()
        suite.add_metric(metric)
        
        info = suite.get_metric_info("accuracy")
        assert info["name"] == "accuracy"
        assert info["higher_is_better"] is True
        assert "Accuracy" in info["class"]
    
    def test_evaluate_metrics(self):
        """Test evaluating metrics."""
        suite = EvaluationSuite()
        suite.add_metric(classification.Accuracy())
        
        y_true = np.array([0, 1, 1, 0])
        y_pred = np.array([0, 1, 0, 0])
        
        results = suite.evaluate(y_true, y_pred)
        assert "accuracy" in results
        assert 0.0 <= results["accuracy"] <= 1.0


class TestClassificationMetrics:
    """Test cases for classification metrics."""
    
    def setup_method(self):
        """Set up test data."""
        self.y_true_binary = np.array([0, 1, 1, 0, 1])
        self.y_pred_binary = np.array([0, 1, 0, 0, 1])
        
        self.y_true_multi = np.array([0, 1, 2, 1, 0])
        self.y_pred_multi = np.array([0, 1, 1, 1, 0])
    
    def test_accuracy(self):
        """Test accuracy metric."""
        metric = classification.Accuracy()
        score = metric.compute(self.y_true_binary, self.y_pred_binary)
        assert 0.0 <= score <= 1.0
        # Known result: 4/5 = 0.8
        assert score == 0.8
    
    def test_precision(self):
        """Test precision metric."""
        metric = classification.Precision("binary")
        score = metric.compute(self.y_true_binary, self.y_pred_binary)
        assert 0.0 <= score <= 1.0
    
    def test_recall(self):
        """Test recall metric."""
        metric = classification.Recall("binary")
        score = metric.compute(self.y_true_binary, self.y_pred_binary)
        assert 0.0 <= score <= 1.0
    
    def test_f1_score(self):
        """Test F1 score metric."""
        metric = classification.F1Score("binary")
        score = metric.compute(self.y_true_binary, self.y_pred_binary)
        assert 0.0 <= score <= 1.0
    
    def test_roc_auc(self):
        """Test ROC AUC metric."""
        metric = classification.ROCAUC()
        # Use probability scores for ROC AUC
        y_pred_proba = np.array([0.1, 0.9, 0.3, 0.2, 0.8])
        score = metric.compute(self.y_true_binary, y_pred_proba)
        assert 0.0 <= score <= 1.0
    
    def test_get_all_classification_metrics(self):
        """Test getting all classification metrics."""
        metrics = classification.get_all_classification_metrics()
        assert len(metrics) > 0
        assert all(hasattr(m, 'compute') for m in metrics)


class TestRegressionMetrics:
    """Test cases for regression metrics."""
    
    def setup_method(self):
        """Set up test data."""
        self.y_true = np.array([1.0, 2.0, 3.0, 4.0, 5.0])
        self.y_pred = np.array([1.1, 2.2, 2.9, 3.8, 5.2])
    
    def test_mae(self):
        """Test Mean Absolute Error."""
        metric = regression.MeanAbsoluteError()
        score = metric.compute(self.y_true, self.y_pred)
        assert score >= 0.0
    
    def test_mse(self):
        """Test Mean Squared Error."""
        metric = regression.MeanSquaredError()
        score = metric.compute(self.y_true, self.y_pred)
        assert score >= 0.0
    
    def test_rmse(self):
        """Test Root Mean Squared Error."""
        metric = regression.RootMeanSquaredError()
        score = metric.compute(self.y_true, self.y_pred)
        assert score >= 0.0
    
    def test_r2_score(self):
        """Test R-squared score."""
        metric = regression.R2Score()
        score = metric.compute(self.y_true, self.y_pred)
        # R² can be negative for very poor predictions
        assert score <= 1.0
    
    def test_mape(self):
        """Test Mean Absolute Percentage Error."""
        metric = regression.MeanAbsolutePercentageError()
        score = metric.compute(self.y_true, self.y_pred)
        assert score >= 0.0
    
    def test_mean_error(self):
        """Test Mean Error (bias)."""
        metric = regression.MeanError()
        score = metric.compute(self.y_true, self.y_pred)
        # Bias can be positive or negative
        assert isinstance(score, float)
    
    def test_get_all_regression_metrics(self):
        """Test getting all regression metrics."""
        metrics = regression.get_all_regression_metrics()
        assert len(metrics) > 0
        assert all(hasattr(m, 'compute') for m in metrics)


class TestClusteringMetrics:
    """Test cases for clustering metrics."""
    
    def setup_method(self):
        """Set up test data."""
        # Create simple 2D data for clustering
        np.random.seed(42)
        self.X = np.random.rand(20, 2)
        self.y_true = np.array([0]*10 + [1]*10)  # Two clusters
        self.y_pred = np.array([0]*8 + [1]*2 + [1]*8 + [0]*2)  # Imperfect clustering
    
    def test_silhouette_score(self):
        """Test Silhouette Score."""
        metric = clustering.SilhouetteScore()
        score = metric.compute(self.y_true, self.y_pred, X=self.X)
        assert -1.0 <= score <= 1.0
    
    def test_adjusted_rand_score(self):
        """Test Adjusted Rand Index."""
        metric = clustering.AdjustedRandScore()
        score = metric.compute(self.y_true, self.y_pred)
        assert -1.0 <= score <= 1.0
    
    def test_adjusted_mutual_info_score(self):
        """Test Adjusted Mutual Information Score."""
        metric = clustering.AdjustedMutualInfoScore()
        score = metric.compute(self.y_true, self.y_pred)
        assert score >= 0.0
    
    def test_calinski_harabasz_score(self):
        """Test Calinski-Harabasz Score."""
        metric = clustering.CalinskiHarabaszScore()
        score = metric.compute(self.y_true, self.y_pred, X=self.X)
        assert score >= 0.0
    
    def test_davies_bouldin_score(self):
        """Test Davies-Bouldin Score."""
        metric = clustering.DaviesBouldinScore()
        score = metric.compute(self.y_true, self.y_pred, X=self.X)
        assert score >= 0.0
    
    def test_get_all_clustering_metrics(self):
        """Test getting all clustering metrics."""
        metrics = clustering.get_all_clustering_metrics()
        assert len(metrics) > 0
        assert all(hasattr(m, 'compute') for m in metrics)


class TestRankingMetrics:
    """Test cases for ranking metrics."""
    
    def setup_method(self):
        """Set up test data."""
        # Relevance scores (higher = more relevant)
        self.y_true = np.array([3, 2, 0, 1, 2])
        # Predicted scores for ranking
        self.y_pred = np.array([0.9, 0.8, 0.1, 0.6, 0.7])
        
        # Binary relevance for some metrics
        self.y_true_binary = np.array([1, 1, 0, 0, 1])
    
    def test_ndcg(self):
        """Test NDCG metric."""
        metric = ranking.NDCG()
        score = metric.compute(self.y_true, self.y_pred)
        assert 0.0 <= score <= 1.0
    
    def test_ndcg_at_k(self):
        """Test NDCG@k metric."""
        metric = ranking.NDCG(k=3)
        score = metric.compute(self.y_true, self.y_pred)
        assert 0.0 <= score <= 1.0
    
    def test_map(self):
        """Test Mean Average Precision."""
        metric = ranking.MAP()
        score = metric.compute(self.y_true_binary, self.y_pred)
        assert 0.0 <= score <= 1.0
    
    def test_precision_at_k(self):
        """Test Precision@K."""
        metric = ranking.PrecisionAtK(k=3)
        score = metric.compute(self.y_true_binary, self.y_pred)
        assert 0.0 <= score <= 1.0
    
    def test_recall_at_k(self):
        """Test Recall@K."""
        metric = ranking.RecallAtK(k=3)
        score = metric.compute(self.y_true_binary, self.y_pred)
        assert 0.0 <= score <= 1.0
    
    def test_get_all_ranking_metrics(self):
        """Test getting all ranking metrics."""
        metrics = ranking.get_all_ranking_metrics()
        assert len(metrics) > 0
        assert all(hasattr(m, 'compute') for m in metrics)