#!/bin/bash

# BucikaGSR Development Setup Script
# Sets up pre-commit hooks and validates the development environment

set -e

echo "🚀 Setting up BucikaGSR development environment..."

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
    print_status $RED "❌ Error: Not in the root directory of BucikaGSR project"
    print_status $YELLOW "Please run this script from the root directory of the project."
    exit 1
fi

print_status $BLUE "📋 Development Environment Setup Checklist:"
echo

# 1. Setup Git Hooks
print_status $YELLOW "1. Setting up Git hooks..."
if [ -f ".githooks/pre-commit" ]; then
    # Copy hooks to .git/hooks/
    cp .githooks/pre-commit .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    print_status $GREEN "   ✅ Pre-commit hook installed"
    
    # Test the hook
    if [ -x ".git/hooks/pre-commit" ]; then
        print_status $GREEN "   ✅ Pre-commit hook is executable"
    else
        print_status $RED "   ❌ Failed to make pre-commit hook executable"
        exit 1
    fi
else
    print_status $RED "   ❌ Pre-commit hook not found in .githooks/"
    exit 1
fi

# 2. Validate Gradle setup
print_status $YELLOW "2. Validating Gradle setup..."
if [ ! -x "./gradlew" ]; then
    chmod +x ./gradlew
    print_status $GREEN "   ✅ Made gradlew executable"
fi

# Test Gradle
if ./gradlew validateBuild --quiet --no-daemon; then
    print_status $GREEN "   ✅ Gradle build validation passed"
else
    print_status $RED "   ❌ Gradle build validation failed"
    print_status $YELLOW "   💡 Run './gradlew validateBuild' for detailed error information"
    exit 1
fi

# 3. Check Java version
print_status $YELLOW "3. Checking Java environment..."
JAVA_VERSION=$(./gradlew -q javaToolchains 2>/dev/null | grep "17" || echo "")
if [ -n "$JAVA_VERSION" ] || java -version 2>&1 | grep -q "17"; then
    print_status $GREEN "   ✅ Java 17 is available"
else
    print_status $YELLOW "   ⚠️  Java 17 not detected. Please ensure Java 17 is installed"
    print_status $YELLOW "      The project may still work with other Java versions"
fi

# 4. Setup IDE configurations (optional)
print_status $YELLOW "4. Setting up IDE configurations..."
if [ -d ".idea" ]; then
    print_status $GREEN "   ✅ IntelliJ IDEA configuration detected"
else
    print_status $YELLOW "   💡 No IntelliJ IDEA configuration found"
    print_status $YELLOW "      Open the project in Android Studio/IntelliJ IDEA to generate configuration"
fi

# 5. Check for required tools
print_status $YELLOW "5. Checking development tools..."

# Check for git
if command -v git &> /dev/null; then
    print_status $GREEN "   ✅ Git is installed"
    
    # Setup git configuration for the project
    if [ "$(git config --get core.hooksPath)" != ".githooks" ]; then
        git config core.hooksPath .githooks
        print_status $GREEN "   ✅ Git hooks path configured"
    fi
else
    print_status $RED "   ❌ Git not found"
    exit 1
fi

# 6. Verify CI/CD workflows
print_status $YELLOW "6. Verifying CI/CD workflows..."
if [ -d ".github/workflows" ] && [ -f ".github/workflows/ci.yml" ]; then
    print_status $GREEN "   ✅ GitHub Actions CI/CD workflows configured"
    
    workflow_count=$(ls -1 .github/workflows/*.yml | wc -l)
    print_status $GREEN "   ✅ Found $workflow_count workflow file(s)"
else
    print_status $RED "   ❌ CI/CD workflows not found"
    exit 1
fi

# 7. Setup development branch (optional)
print_status $YELLOW "7. Git branch setup..."
current_branch=$(git branch --show-current)
print_status $GREEN "   ✅ Current branch: $current_branch"

if [ "$current_branch" = "master" ] || [ "$current_branch" = "main" ]; then
    print_status $YELLOW "   💡 You're on the main branch. Consider creating a feature branch for development:"
    print_status $YELLOW "      git checkout -b feature/your-feature-name"
fi

# 8. Test the setup
print_status $YELLOW "8. Testing the setup..."

# Quick compilation test
print_status $YELLOW "   Testing compilation..."
if ./gradlew compileDevDebugKotlin --quiet --no-daemon; then
    print_status $GREEN "   ✅ Code compiles successfully"
else
    print_status $YELLOW "   ⚠️  Compilation issues detected"
    print_status $YELLOW "      Run './gradlew compileDevDebugKotlin' for details"
fi

# Test pre-commit hook (dry run)
print_status $YELLOW "   Testing pre-commit hook..."
if [ -x ".git/hooks/pre-commit" ]; then
    print_status $GREEN "   ✅ Pre-commit hook is ready"
else
    print_status $RED "   ❌ Pre-commit hook setup failed"
    exit 1
fi

echo
print_status $GREEN "🎉 Development environment setup completed!"
echo
print_status $BLUE "📖 Next steps:"
print_status $YELLOW "1. Start development with: git checkout -b feature/your-feature"
print_status $YELLOW "2. Make your changes and commit - pre-commit hooks will run automatically"
print_status $YELLOW "3. Push changes - CI/CD pipeline will run automatically"
print_status $YELLOW "4. Create a Pull Request for code review"
echo
print_status $BLUE "🔧 Useful commands:"
print_status $YELLOW "• ./gradlew assembleDevDebug     - Build debug APK"
print_status $YELLOW "• ./gradlew testDevDebugUnitTest - Run unit tests"
print_status $YELLOW "• ./gradlew lintDevDebug         - Run lint checks"
print_status $YELLOW "• ./gradlew validateBuild        - Validate build configuration"
print_status $YELLOW "• ./validate_setup.sh            - Run full project validation"
echo
print_status $GREEN "✨ Happy coding!"