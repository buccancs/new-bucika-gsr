#!/bin/bash
#
# Local Test Runner for Multi-Sensor Recording System
# Provides easy local testing with the unified testing framework
#

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if unified testing framework is available
check_unified_framework() {
    if [ -f "tests_unified/runners/run_unified_tests.py" ]; then
        return 0
    else
        return 1
    fi
}

# Function to install dependencies
install_dependencies() {
    print_status "Installing test dependencies..."
    
    if [ -f "test-requirements.txt" ]; then
        pip install -r test-requirements.txt
    fi
    
    if [ -f "pyproject.toml" ]; then
        pip install -e .
    fi
    
    print_success "Dependencies installed"
}

# Function to run unified tests
run_unified_tests() {
    local mode="${1:-quick}"
    
    print_status "Running unified test suite in $mode mode..."
    
    case "$mode" in
        "quick")
            python tests_unified/runners/run_unified_tests.py --quick --verbose
            ;;
        "full")
            python tests_unified/runners/run_unified_tests.py --all-levels --verbose
            ;;
        "requirements")
            python tests_unified/runners/run_unified_tests.py --validate-requirements
            python tests_unified/runners/run_unified_tests.py --report-requirements-coverage
            ;;
        "performance")
            python tests_unified/runners/run_unified_tests.py --level performance --performance-benchmarks
            ;;
        "ci")
            python tests_unified/runners/run_unified_tests.py --mode ci
            ;;
        "pc")
            python tests_unified/runners/run_unified_tests.py --category pc --level system
            ;;
        "android")
            python tests_unified/runners/run_unified_tests.py --category android --level system
            ;;
        "gui")
            python tests_unified/runners/run_unified_tests.py --category gui
            ;;
        *)
            print_error "Unknown mode: $mode"
            print_status "Available modes: quick, full, requirements, performance, ci, pc, android, gui"
            exit 1
            ;;
    esac
}


# Function to show usage information
show_usage() {
    echo "Local Test Runner for Multi-Sensor Recording System"
    echo ""
    echo "Usage: $0 [MODE] [OPTIONS]"
    echo ""
    echo "MODES:"
    echo "  quick        Run quick test suite (default, ~2 minutes)"
    echo "  full         Run complete test suite (all levels)"
    echo "  requirements Validate functional and non-functional requirements"
    echo "  performance  Run performance benchmarks"
    echo "  ci           Run CI/CD mode tests"
    echo "  pc           Run PC/desktop application tests"
    echo "  android      Run Android application tests"
    echo "  gui          Run GUI/UI tests for both platforms"
    echo ""
    echo "OPTIONS:"
    echo "  --install-deps    Install test dependencies before running"
    echo "  --help, -h        Show this help message"
    echo ""
    echo "EXAMPLES:"
    echo "  $0                           # Quick test suite"
    echo "  $0 full                      # Complete test suite"
    echo "  $0 requirements              # Requirements validation"
    echo "  $0 quick --install-deps      # Install deps and run quick tests"
    echo "  $0 pc                        # PC application tests only"
    echo "  $0 android                   # Android application tests only"
    echo "  $0 gui                       # GUI tests for both platforms"
    echo ""
    echo "UNIFIED FRAMEWORK USAGE:"
    echo "  # Direct usage of unified framework"
    echo "  python tests_unified/runners/run_unified_tests.py --help"
    echo ""
    echo "  # Specific test levels"
    echo "  python tests_unified/runners/run_unified_tests.py --level unit"
    echo "  python tests_unified/runners/run_unified_tests.py --level integration"
    echo ""
    echo "  # Specific categories"
    echo "  python tests_unified/runners/run_unified_tests.py --category android"
    echo "  python tests_unified/runners/run_unified_tests.py --category hardware"
    echo ""
}

# Main execution
main() {
    local mode="quick"
    local install_deps=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --install-deps)
                install_deps=true
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            quick|full|requirements|performance|ci|pc|android|gui)
                mode="$1"
                shift
                ;;
            *)
                print_error "Unknown argument: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_status "Multi-Sensor Recording System - Local Test Runner"
    print_status "=================================================="
    
    # Check if we're in the right directory
    if [ ! -f "pyproject.toml" ] && [ ! -f "setup.py" ] && [ ! -d "PythonApp" ]; then
        print_error "Not in project root directory. Please run from the repository root."
        exit 1
    fi
    
    # Install dependencies if requested
    if [ "$install_deps" = true ]; then
        install_dependencies
    fi
    
    # Check for unified framework and run tests
    if check_unified_framework; then
        print_success "Unified testing framework found"
        run_unified_tests "$mode"
    else
        print_error "Unified testing framework not found!"
        print_error "The project has been consolidated to use only the unified framework."
        print_error "Please ensure tests_unified/runners/run_unified_tests.py exists"
        exit 1
    fi
    
    print_success "Test execution completed"
}

# Run main function with all arguments
main "$@"