#!/bin/bash

# Quality Gate Enforcement Script
# Validates quality metrics and enforces thresholds automatically
# For use in CI/CD pipelines and pre-commit hooks

set -e

# Configuration
REPORT_DIR="build/reports/quality"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
QUALITY_GATE_LOG="$REPORT_DIR/quality_gates_$TIMESTAMP.log"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}🚧 BucikaGSR Quality Gates Enforcement${NC}" | tee "$QUALITY_GATE_LOG"
echo "=======================================" | tee -a "$QUALITY_GATE_LOG"

# Create reports directory
mkdir -p "$REPORT_DIR"

# Initialize gate status
GATE_FAILURES=0
GATE_WARNINGS=0

echo -e "\n${YELLOW}📋 Running Quality Checks...${NC}" | tee -a "$QUALITY_GATE_LOG"

# 1. Build Configuration Complexity Gate
BUILD_GRADLE_LINES=$(wc -l app/build.gradle 2>/dev/null | awk '{print $1}' || echo "0")
echo "Build Configuration Lines: $BUILD_GRADLE_LINES" | tee -a "$QUALITY_GATE_LOG"

if [ $BUILD_GRADLE_LINES -gt 200 ]; then
    echo -e "${RED}❌ GATE FAILURE: Build config too complex ($BUILD_GRADLE_LINES lines > 200 threshold)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_FAILURES=$((GATE_FAILURES + 1))
elif [ $BUILD_GRADLE_LINES -gt 150 ]; then
    echo -e "${YELLOW}⚠️  GATE WARNING: Build config approaching complexity limit ($BUILD_GRADLE_LINES lines)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_WARNINGS=$((GATE_WARNINGS + 1))
else
    echo -e "${GREEN}✅ GATE PASSED: Build config complexity under control ($BUILD_GRADLE_LINES lines)${NC}" | tee -a "$QUALITY_GATE_LOG"
fi

# 2. Code Complexity Gate
KOTLIN_FILES=$(find . -name "*.kt" -not -path "./build/*" | wc -l)
JAVA_FILES=$(find . -name "*.java" -not -path "./build/*" | wc -l)
TOTAL_FILES=$((KOTLIN_FILES + JAVA_FILES))
COMPLEX_FILES=$(find . -name "*.kt" -o -name "*.java" | grep -v "/build/" | xargs wc -l | awk '$1 > 100 {count++} END {print count+0}')
COMPLEXITY_PERCENTAGE=$((COMPLEX_FILES * 100 / (TOTAL_FILES > 0 ? TOTAL_FILES : 1)))

echo "Complex Files Percentage: $COMPLEXITY_PERCENTAGE%" | tee -a "$QUALITY_GATE_LOG"

if [ $COMPLEXITY_PERCENTAGE -gt 50 ]; then
    echo -e "${RED}❌ GATE FAILURE: Too many complex files ($COMPLEXITY_PERCENTAGE% > 50%)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_FAILURES=$((GATE_FAILURES + 1))
elif [ $COMPLEXITY_PERCENTAGE -gt 45 ]; then
    echo -e "${YELLOW}⚠️  GATE WARNING: Complexity approaching limit ($COMPLEXITY_PERCENTAGE%)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_WARNINGS=$((GATE_WARNINGS + 1))
else
    echo -e "${GREEN}✅ GATE PASSED: Code complexity under control ($COMPLEXITY_PERCENTAGE%)${NC}" | tee -a "$QUALITY_GATE_LOG"
fi

# 3. Test Coverage Gate
TEST_FILES=$(find . -name "*Test*.kt" -o -name "*Test*.java" | grep -v "/build/" | wc -l)
TEST_RATIO=$(echo "scale=2; $TEST_FILES * 100 / $TOTAL_FILES" | bc -l 2>/dev/null || echo "0")

echo "Test Coverage Ratio: $TEST_RATIO%" | tee -a "$QUALITY_GATE_LOG"

if [ $TEST_FILES -lt 20 ]; then
    echo -e "${RED}❌ GATE FAILURE: Insufficient test files ($TEST_FILES < 20)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_FAILURES=$((GATE_FAILURES + 1))
elif [ $TEST_FILES -lt 25 ]; then
    echo -e "${YELLOW}⚠️  GATE WARNING: Test coverage could be improved ($TEST_FILES test files)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_WARNINGS=$((GATE_WARNINGS + 1))
else
    echo -e "${GREEN}✅ GATE PASSED: Adequate test coverage ($TEST_FILES test files)${NC}" | tee -a "$QUALITY_GATE_LOG"
fi

# 4. Security Gate (Basic Check)
POTENTIAL_SECRETS=$(grep -r -i "password\|secret\|token\|key" --include="*.kt" --include="*.java" . | grep -v "/build/" | grep -v "Test" | wc -l || echo "0")
echo "Potential Security Findings: $POTENTIAL_SECRETS" | tee -a "$QUALITY_GATE_LOG"

if [ $POTENTIAL_SECRETS -gt 1000 ]; then
    echo -e "${RED}❌ GATE FAILURE: Excessive security findings ($POTENTIAL_SECRETS > 1000)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_FAILURES=$((GATE_FAILURES + 1))
elif [ $POTENTIAL_SECRETS -gt 700 ]; then
    echo -e "${YELLOW}⚠️  GATE WARNING: High security findings need review ($POTENTIAL_SECRETS)${NC}" | tee -a "$QUALITY_GATE_LOG"
    GATE_WARNINGS=$((GATE_WARNINGS + 1))
else
    echo -e "${GREEN}✅ GATE PASSED: Security findings within acceptable range ($POTENTIAL_SECRETS)${NC}" | tee -a "$QUALITY_GATE_LOG"
fi

# Final Assessment
echo -e "\n${YELLOW}📊 Quality Gates Summary${NC}" | tee -a "$QUALITY_GATE_LOG"
echo "=========================" | tee -a "$QUALITY_GATE_LOG"
echo "Gate Failures: $GATE_FAILURES" | tee -a "$QUALITY_GATE_LOG"
echo "Gate Warnings: $GATE_WARNINGS" | tee -a "$QUALITY_GATE_LOG"

if [ $GATE_FAILURES -gt 0 ]; then
    echo -e "${RED}🚨 QUALITY GATES FAILED - $GATE_FAILURES critical issues found${NC}" | tee -a "$QUALITY_GATE_LOG"
    echo "Build should not proceed to production." | tee -a "$QUALITY_GATE_LOG"
    exit 1
elif [ $GATE_WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Quality gates passed with $GATE_WARNINGS warnings${NC}" | tee -a "$QUALITY_GATE_LOG"
    echo "Consider addressing warnings before release." | tee -a "$QUALITY_GATE_LOG"
    exit 0
else
    echo -e "${GREEN}✅ ALL QUALITY GATES PASSED - Build ready for production${NC}" | tee -a "$QUALITY_GATE_LOG"
    exit 0
fi