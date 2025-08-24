#!/bin/bash

# BucikaGSR UI Test Runner Script
# Comprehensive UI testing automation for all activities and user flows

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
PROJECT_DIR="$(dirname "$0")/.."
RESULTS_DIR="$PROJECT_DIR/build/reports/androidTests"
COVERAGE_DIR="$PROJECT_DIR/build/reports/coverage"

echo -e "${BLUE}🔧 BucikaGSR UI Test Suite Runner${NC}"
echo "=========================================="
echo ""

# Function to check if device is connected
check_device() {
    echo -e "${YELLOW}Checking for connected Android device...${NC}"
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}❌ No Android device connected. Please connect a device or start an emulator.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Android device detected${NC}"
}

# Function to install APK
install_app() {
    echo -e "${YELLOW}Installing BucikaGSR app...${NC}"
    ./gradlew installDebug
    ./gradlew installDebugAndroidTest
    echo -e "${GREEN}✅ App installed successfully${NC}"
}

# Function to run specific test suite
run_test_suite() {
    local suite_name=$1
    local class_name=$2
    
    echo -e "${YELLOW}Running $suite_name...${NC}"
    ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$class_name
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $suite_name passed${NC}"
        return 0
    else
        echo -e "${RED}❌ $suite_name failed${NC}"
        return 1
    fi
}

# Function to run all UI tests
run_all_tests() {
    echo -e "${YELLOW}Running comprehensive UI test suite...${NC}"
    ./gradlew connectedAndroidTest
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ All UI tests passed${NC}"
        return 0
    else
        echo -e "${RED}❌ Some UI tests failed${NC}"
        return 1
    fi
}

# Function to generate test report
generate_report() {
    echo -e "${YELLOW}Generating test reports...${NC}"
    
    if [ -d "$RESULTS_DIR" ]; then
        echo -e "${GREEN}📊 Test results available at: $RESULTS_DIR${NC}"
        
        # Count test results
        local total_tests=$(find "$RESULTS_DIR" -name "*.xml" -exec grep -c "testcase" {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
        local failed_tests=$(find "$RESULTS_DIR" -name "*.xml" -exec grep -c "failure\|error" {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
        local passed_tests=$((total_tests - failed_tests))
        
        echo ""
        echo -e "${BLUE}📋 Test Summary:${NC}"
        echo -e "   Total tests: $total_tests"
        echo -e "   ${GREEN}Passed: $passed_tests${NC}"
        echo -e "   ${RED}Failed: $failed_tests${NC}"
        
        if [ "$failed_tests" -eq 0 ]; then
            echo -e "${GREEN}🎉 All UI tests passed successfully!${NC}"
        else
            echo -e "${YELLOW}⚠️  Some tests failed. Check the detailed report.${NC}"
        fi
    else
        echo -e "${YELLOW}No test results found${NC}"
    fi
}

# Function to clean up
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    ./gradlew clean
}

# Main execution flow
main() {
    case "${1:-all}" in
        "check")
            check_device
            ;;
        "install")
            check_device
            install_app
            ;;
        "comprehensive")
            check_device
            install_app
            run_test_suite "Comprehensive UI Test Suite" "com.topdon.tc001.ui.ComprehensiveUITestSuite"
            generate_report
            ;;
        "manager-integration")
            check_device
            install_app
            run_test_suite "Manager Pattern UI Integration" "com.topdon.tc001.ui.ManagerPatternUIIntegrationSuite"
            generate_report
            ;;
        "performance")
            check_device
            install_app
            run_test_suite "Performance & Accessibility Tests" "com.topdon.tc001.ui.PerformanceAccessibilityUITestSuite"
            generate_report
            ;;
        "main")
            check_device
            install_app
            run_test_suite "Main Activity Tests" "com.topdon.tc001.ui.MainActivityUITest"
            generate_report
            ;;
        "thermal")
            check_device
            install_app
            run_test_suite "Thermal Activity Tests" "com.topdon.tc001.thermal.ui.IRThermalNightActivityUITest"
            generate_report
            ;;
        "gsr")
            check_device
            install_app
            run_test_suite "GSR Tests" "com.topdon.tc001.gsr.ui.GSRActivityUITest"
            generate_report
            ;;
        "recording")
            check_device
            install_app
            run_test_suite "Recording Tests" "com.topdon.tc001.recording.ui.EnhancedRecordingActivityUITest"
            generate_report
            ;;
        "clean")
            cleanup
            ;;
        "all"|*)
            check_device
            install_app
            run_all_tests
            generate_report
            ;;
    esac
}

# Help function
show_help() {
    echo "BucikaGSR UI Test Runner"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  all                    Run all UI tests (default)"
    echo "  comprehensive          Run comprehensive UI test suite"
    echo "  manager-integration    Run manager pattern integration tests"
    echo "  performance            Run performance and accessibility tests"
    echo "  main                   Run main activity tests only"
    echo "  thermal                Run thermal activity tests only"
    echo "  gsr                    Run GSR tests only"
    echo "  recording              Run recording tests only"
    echo "  check                  Check device connection"
    echo "  install                Install app and test APKs"
    echo "  clean                  Clean build files"
    echo "  help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                     # Run all tests"
    echo "  $0 comprehensive       # Run comprehensive test suite"
    echo "  $0 thermal             # Run only thermal camera tests"
    echo "  $0 clean               # Clean build files"
}

# Check for help flag
if [ "$1" = "help" ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
    exit 0
fi

# Execute main function
main "$1"