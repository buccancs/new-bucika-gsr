"""Command-line interface for the evaluation suite."""

import argparse
import json
import sys
from pathlib import Path
import numpy as np

from .core import EvaluationSuite
from .metrics import classification, regression, clustering, ranking


def load_data(file_path: str):
    """Load data from JSON file."""
    try:
        with open(file_path, 'r') as f:
            data = json.load(f)
        return np.array(data['y_true']), np.array(data['y_pred']), data.get('X')
    except Exception as e:
        print(f"Error loading data from {file_path}: {e}")
        sys.exit(1)


def create_suite_with_metrics(metric_type: str) -> EvaluationSuite:
    """Create evaluation suite with specific metric types."""
    suite = EvaluationSuite()
    
    if metric_type == "classification":
        for metric in classification.get_all_classification_metrics():
            suite.add_metric(metric)
    elif metric_type == "regression":
        for metric in regression.get_all_regression_metrics():
            suite.add_metric(metric)
    elif metric_type == "clustering":
        for metric in clustering.get_all_clustering_metrics():
            suite.add_metric(metric)
    elif metric_type == "ranking":
        for metric in ranking.get_all_ranking_metrics():
            suite.add_metric(metric)
    elif metric_type == "all":
        # Add all metrics
        for metric in classification.get_all_classification_metrics():
            suite.add_metric(metric)
        for metric in regression.get_all_regression_metrics():
            suite.add_metric(metric)
        for metric in clustering.get_all_clustering_metrics():
            suite.add_metric(metric)
        for metric in ranking.get_all_ranking_metrics():
            suite.add_metric(metric)
    else:
        print(f"Unknown metric type: {metric_type}")
        sys.exit(1)
    
    return suite


def main():
    """Main CLI function."""
    parser = argparse.ArgumentParser(
        description="Bucika GSR Evaluation Suite",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Evaluate classification metrics
  bucika-eval --type classification --data data.json
  
  # List available metrics
  bucika-eval --list --type classification
  
  # Evaluate specific metrics only
  bucika-eval --type classification --data data.json --metrics accuracy f1_binary
        """
    )
    
    parser.add_argument(
        "--type", 
        choices=["classification", "regression", "clustering", "ranking", "all"],
        default="classification",
        help="Type of metrics to evaluate (default: classification)"
    )
    
    parser.add_argument(
        "--data",
        type=str,
        help="Path to JSON file containing y_true and y_pred arrays"
    )
    
    parser.add_argument(
        "--metrics",
        nargs="+",
        help="Specific metrics to evaluate (default: all for selected type)"
    )
    
    parser.add_argument(
        "--output",
        type=str,
        help="Output file path for results (default: stdout)"
    )
    
    parser.add_argument(
        "--list",
        action="store_true",
        help="List available metrics for the specified type"
    )
    
    args = parser.parse_args()
    
    # Create evaluation suite
    suite = create_suite_with_metrics(args.type)
    
    # List metrics if requested
    if args.list:
        print(f"Available {args.type} metrics:")
        for metric_name in suite.list_metrics():
            info = suite.get_metric_info(metric_name)
            direction = "↑" if info["higher_is_better"] else "↓"
            print(f"  {metric_name} {direction}")
        return
    
    # Require data file for evaluation
    if not args.data:
        print("Error: --data argument is required for evaluation")
        sys.exit(1)
    
    if not Path(args.data).exists():
        print(f"Error: Data file {args.data} not found")
        sys.exit(1)
    
    # Load data
    y_true, y_pred, X = load_data(args.data)
    
    # Evaluate metrics
    kwargs = {}
    if X is not None:
        kwargs['X'] = np.array(X)
    
    try:
        results = suite.evaluate(y_true, y_pred, metrics=args.metrics, **kwargs)
        
        # Format output
        output = {
            "metric_type": args.type,
            "data_file": args.data,
            "results": results,
            "summary": {
                "total_metrics": len(results),
                "successful_metrics": len([v for v in results.values() if not isinstance(v, str)])
            }
        }
        
        # Output results
        if args.output:
            with open(args.output, 'w') as f:
                json.dump(output, f, indent=2)
            print(f"Results saved to {args.output}")
        else:
            print(json.dumps(output, indent=2))
    
    except Exception as e:
        print(f"Error during evaluation: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()