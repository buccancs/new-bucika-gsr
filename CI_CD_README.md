# BucikaGSR CI/CD Documentation

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline implemented for the BucikaGSR project.

## 🏗️ CI/CD Architecture

The CI/CD system consists of three main GitHub Actions workflows:

### 1. **CI Pipeline** (`.github/workflows/ci.yml`)
- **Triggers**: Push to `master`/`main`/`develop`, Pull Requests
- **Purpose**: Automated testing, linting, and quality checks
- **Jobs**:
  - `lint`: Code quality and linting checks
  - `test`: Unit tests across different variants
  - `build`: APK building and compilation
  - `security`: Security vulnerability scanning
  - `dependency-check`: Dependency analysis
  - `status-check`: Overall CI status summary

### 2. **Pre-commit Checks** (`.github/workflows/pre-commit.yml`)
- **Triggers**: Pull Request events
- **Purpose**: Lightweight quality gates for PR validation
- **Features**:
  - Fast lint checks
  - Code style validation
  - Static analysis
  - Build file validation
  - Automated PR commenting with results

### 3. **Deploy & Release** (`.github/workflows/deploy.yml`)
- **Triggers**: Version tags, manual deployment
- **Purpose**: Multi-environment deployments and releases
- **Features**:
  - Environment-specific deployments (dev/beta/prod)
  - APK signing and release management
  - GitHub releases automation
  - Rollback capabilities

### 4. **Performance Testing** (`.github/workflows/performance.yml`) 🆕
- **Triggers**: Push to main branches, PRs, nightly schedule
- **Purpose**: Comprehensive performance benchmarking and regression analysis
- **Features**:
  - Android emulator performance tests
  - Memory profiling and leak detection
  - Build performance analysis
  - Performance regression detection for PRs

### 5. **Advanced Security** (`.github/workflows/security.yml`) 🆕
- **Triggers**: Push/PR, weekly comprehensive scans
- **Purpose**: Enterprise-grade security analysis and compliance
- **Features**:
  - Static Application Security Testing (SAST) with CodeQL
  - OWASP dependency vulnerability scanning
  - Secrets detection with TruffleHog and GitLeaks
  - Security compliance reporting

### 6. **Analytics Dashboard** (`.github/workflows/analytics.yml`) 🆕
- **Triggers**: Daily metrics collection, push/PR events
- **Purpose**: Project metrics visualization and GitHub Pages dashboard
- **Features**:
  - Interactive HTML dashboards with Chart.js
  - Code, Git, Build, and Test metrics collection
  - GitHub Pages deployment for team visibility
  - CI/CD pipeline status monitoring

### 7. **Mobile Device Testing** (`.github/workflows/device-testing.yml`) 🆕
- **Triggers**: Push/PR, nightly device testing
- **Purpose**: Multi-device compatibility and integration testing
- **Features**:
  - Android emulator testing across multiple API levels
  - Firebase Test Lab integration for physical devices
  - Device compatibility analysis
  - Screenshot capture and logcat collection
- **Triggers**: Push to main branches, version tags, manual dispatch
- **Purpose**: Automated building and deployment
- **Features**:
  - Multi-environment deployment (dev/beta/prod)
  - Release APK building
  - GitHub release creation
  - Deployment notifications

## 🔧 Local Development Setup

### Quick Setup
```bash
# Clone the repository
git clone https://github.com/buccancs/new-bucika-gsr.git
cd new-bucika-gsr

# Run the setup script
./setup-dev.sh
```

### Manual Setup
1. **Install Pre-commit Hooks**:
   ```bash
   cp .githooks/pre-commit .git/hooks/pre-commit
   chmod +x .git/hooks/pre-commit
   git config core.hooksPath .githooks
   ```

2. **Validate Environment**:
   ```bash
   ./gradlew validateBuild
   ```

3. **Test Pre-commit Hook**:
   ```bash
   # Make a small change and commit to test
   echo "# Test" >> README.md
   git add README.md
   git commit -m "Test commit"
   ```

## 📋 Pre-commit Quality Gates

The pre-commit hook (`.githooks/pre-commit`) performs the following checks:

### ✅ **Automated Checks**
- **Lint Analysis**: Runs Android lint and attempts auto-fix
- **Code Compilation**: Ensures code compiles successfully
- **Build Validation**: Validates Gradle configuration changes
- **Unit Tests**: Runs quick unit tests (for small changesets)

### 🔍 **Code Quality Checks**
- **Logging Standards**: Detects `android.util.Log` usage (should use XLog)
- **TODO/FIXME Format**: Ensures proper format (`TODO: description`)
- **Security Scan**: Checks for hardcoded credentials
- **Error Handling**: Validates exception handling patterns
- **Accessibility**: Checks for missing `contentDescription` in XML

### 🎯 **Smart Validation**
- **File Type Detection**: Only runs relevant checks for changed files
- **Performance Optimization**: Skips expensive checks for large changesets
- **Build File Changes**: Extra validation when Gradle files are modified

## 🚀 CI/CD Pipeline Details

### Lint Job
```yaml
# Runs on every push and PR
- Checkout code
- Setup JDK 17
- Run lint checks
- Upload reports as artifacts
- Comment results on PRs
```

### Test Job
```yaml
# Matrix strategy for multiple variants
- DevDebug and BetaDebug variants
- Unit test execution
- Jacoco coverage reports
- Test result artifacts
```

### Build Job  
```yaml
# Multi-variant APK building
- DevDebug and BetaDebug builds
- APK artifact upload
- Build summaries
```

### Security Job
```yaml
# Security scanning
- Trivy vulnerability scanner
- SARIF report upload to GitHub Security
- File system security analysis
```

## 🔄 Deployment Workflow

### Environment Strategy
- **`dev`**: Development builds (feature branches)
- **`beta`**: Beta testing (main branch)  
- **`prod`**: Production releases (version tags)

### Deployment Process
1. **Environment Detection**: Automatically determines target environment
2. **Version Extraction**: Gets version from tags or gradle files
3. **Pre-deployment Validation**: Runs tests and builds
4. **Multi-flavor Building**: Creates appropriate APK variants
5. **Environment Deployment**: Deploys to target environment
6. **Post-deployment Validation**: Verifies deployment success
7. **Notifications**: Status updates and summaries

### Release Creation
- Automatic GitHub releases for version tags
- Release notes generation from changelog or git commits
- APK asset attachment to releases

## 🛠️ Available Gradle Tasks

### Standard Tasks
```bash
./gradlew assembleDevDebug        # Build debug APK
./gradlew testDevDebugUnitTest    # Run unit tests
./gradlew lintDevDebug           # Run lint checks
./gradlew lintFix               # Auto-fix lint issues
```

### Custom BucikaGSR Tasks
```bash
./gradlew validateBuild         # Validate all configurations
./gradlew assembleAllFlavors    # Build all product flavors
./gradlew testAllModules       # Run all module tests
```

### Validation Scripts
```bash
./validate_setup.sh            # Comprehensive setup validation
./setup-dev.sh                # Development environment setup
```

## 📊 Quality Gates

### Code Coverage
- **Target**: Minimum 80% test coverage
- **Reports**: Jacoco coverage reports in CI artifacts
- **Enforcement**: Warning for low coverage, blocking for critical issues

### Performance Benchmarks
- **Memory Usage**: < 100MB for debug builds
- **Build Time**: < 5 minutes for debug builds
- **APK Size**: Monitoring and reporting in CI

### Security Standards
- **Vulnerability Scanning**: Trivy security scanner
- **Dependency Analysis**: Regular dependency audits  
- **Code Analysis**: Static analysis for security patterns

## 🚨 Error Handling & Debugging

### CI Failure Debugging
1. **Check Job Logs**: View detailed logs in GitHub Actions
2. **Download Artifacts**: Test reports, lint results, APKs
3. **Local Reproduction**: Use same commands locally
4. **Incremental Fixes**: Use `continue-on-error` for non-blocking issues

### Common Issues & Solutions

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew validateBuild
./gradlew assembleDevDebug
```

#### Lint Issues
```bash
# Auto-fix common issues
./gradlew lintFix

# Manual review
./gradlew lintDevDebug
# Check app/build/reports/lint-results-devDebug.html
```

#### Test Failures
```bash
# Run specific test
./gradlew testDevDebugUnitTest --tests "com.example.TestClass"

# Debug mode
./gradlew testDevDebugUnitTest --debug-jvm
```

#### Pre-commit Hook Issues
```bash
# Skip hooks temporarily (not recommended)
git commit --no-verify

# Fix issues and recommit
./gradlew lintFix
git add -A
git commit
```

## 🔧 Configuration Files

### Key Files
- `.github/workflows/ci.yml` - Main CI pipeline
- `.github/workflows/pre-commit.yml` - PR validation  
- `.github/workflows/deploy.yml` - Deployment workflow
- `.githooks/pre-commit` - Local pre-commit hook
- `setup-dev.sh` - Development environment setup
- `validate_setup.sh` - Project validation script

### Environment Variables
The workflows use the following environment variables:
- `JAVA_VERSION`: JDK version (17)
- `JAVA_DISTRIBUTION`: JDK distribution (temurin)
- `KEYSTORE_PASSWORD`: Release signing (CI only)
- `KEY_PASSWORD`: Release key password (CI only)

## 📈 Monitoring & Metrics

### CI/CD Metrics
- **Build Success Rate**: Track via GitHub Actions
- **Build Duration**: Monitor job execution times  
- **Test Coverage**: Jacoco reports
- **Security Alerts**: GitHub Security tab
- **Dependency Vulnerabilities**: Regular scans

### Artifacts & Reports
- **Lint Reports**: HTML/XML format
- **Test Results**: JUnit XML + HTML
- **Coverage Reports**: Jacoco HTML/XML  
- **APK Files**: Debug and release builds
- **Security Reports**: SARIF format

## 🎯 Best Practices

### Development Workflow
1. **Feature Branches**: Always work in feature branches
2. **Small Commits**: Make focused, atomic commits
3. **Pre-commit Testing**: Let hooks catch issues early
4. **PR Reviews**: Use pre-commit workflow for quick feedback

### Code Quality
1. **Lint Compliance**: Fix lint issues before committing
2. **Test Coverage**: Maintain good test coverage
3. **Security Awareness**: Review security scan results
4. **Documentation**: Update docs for significant changes

### CI/CD Maintenance  
1. **Regular Updates**: Keep actions and dependencies updated
2. **Performance Monitoring**: Watch build times and resource usage
3. **Failure Analysis**: Investigate and fix recurring failures
4. **Security Patches**: Apply security updates promptly

## 📚 Additional Resources

- [Android Development Guidelines](docs/GSR_DEVELOPMENT_SETUP.md)
- [Gradle Build Setup](GRADLE_SETUP.md) 
- [Troubleshooting Guide](docs/GSR_TROUBLESHOOTING_GUIDE.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Lint Documentation](https://developer.android.com/studio/write/lint)