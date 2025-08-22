@echo off
rem Local Test Runner for Multi-Sensor Recording System (Windows)
rem Provides easy local testing with the unified testing framework

setlocal enabledelayedexpansion

rem Color codes for Windows (limited support)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "WARNING=[WARNING]"
set "ERROR=[ERROR]"

rem Default values
set "MODE=quick"
set "INSTALL_DEPS=false"

rem Function to print status messages
:print_status
echo %INFO% %~1
goto :eof

:print_success
echo %SUCCESS% %~1
goto :eof

:print_warning
echo %WARNING% %~1
goto :eof

:print_error
echo %ERROR% %~1
goto :eof

rem Function to check if unified testing framework is available
:check_unified_framework
if exist "tests_unified\runners\run_unified_tests.py" (
    exit /b 0
) else (
    exit /b 1
)

rem Function to install dependencies
:install_dependencies
call :print_status "Installing test dependencies..."

if exist "test-requirements.txt" (
    pip install -r test-requirements.txt
    if errorlevel 1 (
        call :print_error "Failed to install test requirements"
        exit /b 1
    )
)

if exist "pyproject.toml" (
    pip install -e .
    if errorlevel 1 (
        call :print_error "Failed to install project in editable mode"
        exit /b 1
    )
)

call :print_success "Dependencies installed"
exit /b 0

rem Function to run unified tests
:run_unified_tests
set "test_mode=%~1"
if "%test_mode%"=="" set "test_mode=quick"

call :print_status "Running unified test suite in %test_mode% mode..."

if "%test_mode%"=="quick" (
    python tests_unified\runners\run_unified_tests.py --quick --verbose
) else if "%test_mode%"=="full" (
    python tests_unified\runners\run_unified_tests.py --all-levels --verbose
) else if "%test_mode%"=="requirements" (
    python tests_unified\runners\run_unified_tests.py --validate-requirements
    python tests_unified\runners\run_unified_tests.py --report-requirements-coverage
) else if "%test_mode%"=="performance" (
    python tests_unified\runners\run_unified_tests.py --level performance --performance-benchmarks
) else if "%test_mode%"=="ci" (
    python tests_unified\runners\run_unified_tests.py --mode ci
) else if "%test_mode%"=="pc" (
    python tests_unified\runners\run_unified_tests.py --category pc --level system
) else if "%test_mode%"=="android" (
    python tests_unified\runners\run_unified_tests.py --category android --level system
) else if "%test_mode%"=="gui" (
    python tests_unified\runners\run_unified_tests.py --category gui
) else (
    call :print_error "Unknown mode: %test_mode%"
    call :print_status "Available modes: quick, full, requirements, performance, ci, pc, android, gui"
    exit /b 1
)

if errorlevel 1 (
    call :print_error "Test execution failed"
    exit /b 1
)

exit /b 0

rem Function to run legacy tests as fallback
:run_legacy_tests
call :print_warning "Unified testing framework not found, falling back to legacy tests"

rem Check for pytest
where pytest >nul 2>&1
if errorlevel 1 (
    call :print_error "pytest not available and no unified framework found"
    exit /b 1
)

if exist "tests" (
    call :print_status "Running pytest on tests\ directory..."
    pytest tests\ -v --tb=short
    if errorlevel 1 (
        call :print_error "Legacy tests failed"
        exit /b 1
    )
) else (
    call :print_warning "No tests\ directory found"
)

exit /b 0

rem Function to show usage information
:show_usage
echo Local Test Runner for Multi-Sensor Recording System (Windows)
echo.
echo Usage: %~nx0 [MODE] [OPTIONS]
echo.
echo MODES:
echo   quick        Run quick test suite (default, ~2 minutes)
echo   full         Run complete test suite (all levels)
echo   requirements Validate functional and non-functional requirements
echo   performance  Run performance benchmarks
echo   ci           Run CI/CD mode tests
echo   pc           Run PC/desktop application tests
echo   android      Run Android application tests
echo   gui          Run GUI/UI tests for both platforms
echo.
echo OPTIONS:
echo   --install-deps    Install test dependencies before running
echo   --help, -h        Show this help message
echo.
echo EXAMPLES:
echo   %~nx0                           # Quick test suite
echo   %~nx0 full                      # Complete test suite
echo   %~nx0 requirements              # Requirements validation
echo   %~nx0 quick --install-deps      # Install deps and run quick tests
echo   %~nx0 pc                        # PC application tests only
echo   %~nx0 android                   # Android application tests only
echo   %~nx0 gui                       # GUI tests for both platforms
echo.
echo UNIFIED FRAMEWORK USAGE:
echo   # Direct usage of unified framework
echo   python tests_unified\runners\run_unified_tests.py --help
echo.
echo   # Specific test levels
echo   python tests_unified\runners\run_unified_tests.py --level unit
echo   python tests_unified\runners\run_unified_tests.py --level integration
echo.
echo   # Specific categories
echo   python tests_unified\runners\run_unified_tests.py --category android
echo   python tests_unified\runners\run_unified_tests.py --category hardware
echo.
exit /b 0

rem Parse command line arguments
:parse_args
if "%~1"=="" goto :main_execution
if "%~1"=="--install-deps" (
    set "INSTALL_DEPS=true"
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    call :show_usage
    exit /b 0
)
if "%~1"=="-h" (
    call :show_usage
    exit /b 0
)
if "%~1"=="quick" (
    set "MODE=quick"
    shift
    goto :parse_args
)
if "%~1"=="full" (
    set "MODE=full"
    shift
    goto :parse_args
)
if "%~1"=="requirements" (
    set "MODE=requirements"
    shift
    goto :parse_args
)
if "%~1"=="performance" (
    set "MODE=performance"
    shift
    goto :parse_args
)
if "%~1"=="ci" (
    set "MODE=ci"
    shift
    goto :parse_args
)
if "%~1"=="pc" (
    set "MODE=pc"
    shift
    goto :parse_args
)
if "%~1"=="android" (
    set "MODE=android"
    shift
    goto :parse_args
)
if "%~1"=="gui" (
    set "MODE=gui"
    shift
    goto :parse_args
)
call :print_error "Unknown argument: %~1"
call :show_usage
exit /b 1

rem Main execution
:main_execution
call :print_status "Multi-Sensor Recording System - Local Test Runner (Windows)"
call :print_status "============================================================="

rem Check if we're in the right directory
if not exist "pyproject.toml" (
    if not exist "setup.py" (
        if not exist "PythonApp" (
            call :print_error "Not in project root directory. Please run from the repository root."
            exit /b 1
        )
    )
)

rem Install dependencies if requested
if "%INSTALL_DEPS%"=="true" (
    call :install_dependencies
    if errorlevel 1 exit /b 1
)

rem Check for unified framework and run appropriate tests
call :check_unified_framework
if errorlevel 1 (
    call :run_legacy_tests
) else (
    call :print_success "Unified testing framework found"
    call :run_unified_tests "%MODE%"
)

if errorlevel 1 (
    call :print_error "Test execution failed"
    exit /b 1
)

call :print_success "Test execution completed"
exit /b 0

rem Start parsing arguments and execution
call :parse_args %*