# Local Test Runner for Multi-Sensor Recording System (PowerShell)
# Provides easy local testing with the unified testing framework

param(
    [string]$Mode = "quick",
    [switch]$InstallDeps,
    [switch]$Help
)

# Color support for PowerShell
if ($Host.UI.RawUI.ForegroundColor) {
    $Colors = @{
        Info = "Cyan"
        Success = "Green" 
        Warning = "Yellow"
        Error = "Red"
        Reset = "White"
    }
} else {
    $Colors = @{
        Info = "White"
        Success = "White"
        Warning = "White" 
        Error = "White"
        Reset = "White"
    }
}

function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Colors.Info
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Colors.Success
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Colors.Warning
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Colors.Error
}

function Test-UnifiedFramework {
    return Test-Path "tests_unified\runners\run_unified_tests.py"
}

function Install-Dependencies {
    Write-Status "Installing test dependencies..."
    
    try {
        if (Test-Path "test-requirements.txt") {
            Write-Status "Installing from test-requirements.txt..."
            $result = Start-Process -FilePath "pip" -ArgumentList "install", "-r", "test-requirements.txt" -Wait -PassThru
            if ($result.ExitCode -ne 0) {
                Write-Error "Failed to install test requirements"
                return $false
            }
        }
        
        if (Test-Path "pyproject.toml") {
            Write-Status "Installing project in editable mode..."
            $result = Start-Process -FilePath "pip" -ArgumentList "install", "-e", "." -Wait -PassThru
            if ($result.ExitCode -ne 0) {
                Write-Warning "Could not install project in editable mode"
                # Don't fail for this as it's not always critical
            }
        }
        
        # Install colorama for better Windows color support
        Write-Status "Installing Windows color support..."
        $result = Start-Process -FilePath "pip" -ArgumentList "install", "colorama" -Wait -PassThru -WindowStyle Hidden
        if ($result.ExitCode -ne 0) {
            Write-Warning "Could not install colorama for color support"
        }
        
        Write-Success "Dependencies installed"
        return $true
    }
    catch {
        Write-Error "Failed to install dependencies: $_"
        return $false
    }
}

function Invoke-UnifiedTests {
    param([string]$TestMode)
    
    Write-Status "Running unified test suite in $TestMode mode..."
    
    $pythonCmd = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pythonCmd) {
        $pythonCmd = Get-Command python3 -ErrorAction SilentlyContinue
    }
    if (-not $pythonCmd) {
        $pythonCmd = Get-Command py -ErrorAction SilentlyContinue
    }
    
    if (-not $pythonCmd) {
        Write-Error "Python not found in PATH"
        return $false
    }
    
    $runnerScript = "tests_unified\runners\run_unified_tests.py"
    
    try {
        switch ($TestMode) {
            "quick" {
                & $pythonCmd.Source $runnerScript --quick --verbose
            }
            "full" {
                & $pythonCmd.Source $runnerScript --all-levels --verbose
            }
            "requirements" {
                & $pythonCmd.Source $runnerScript --validate-requirements
                & $pythonCmd.Source $runnerScript --report-requirements-coverage
            }
            "performance" {
                & $pythonCmd.Source $runnerScript --level performance --performance-benchmarks
            }
            "ci" {
                & $pythonCmd.Source $runnerScript --mode ci
            }
            "pc" {
                & $pythonCmd.Source $runnerScript --category pc --level system
            }
            "android" {
                & $pythonCmd.Source $runnerScript --category android --level system
            }
            "gui" {
                & $pythonCmd.Source $runnerScript --category gui
            }
            default {
                Write-Error "Unknown mode: $TestMode"
                Write-Status "Available modes: quick, full, requirements, performance, ci, pc, android, gui"
                return $false
            }
        }
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Test execution failed"
            return $false
        }
        
        return $true
    }
    catch {
        Write-Error "Failed to run tests: $_"
        return $false
    }
}

function Invoke-LegacyTests {
    Write-Warning "Unified testing framework not found, falling back to legacy tests"
    
    $pytestCmd = Get-Command pytest -ErrorAction SilentlyContinue
    if (-not $pytestCmd) {
        Write-Error "pytest not available and no unified framework found"
        return $false
    }
    
    if (Test-Path "tests") {
        Write-Status "Running pytest on tests\ directory..."
        try {
            & $pytestCmd.Source "tests\" -v --tb=short
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Legacy tests failed"
                return $false
            }
        }
        catch {
            Write-Error "Failed to run legacy tests: $_"
            return $false
        }
    }
    else {
        Write-Warning "No tests\ directory found"
    }
    
    return $true
}

function Show-Usage {
    Write-Host "Local Test Runner for Multi-Sensor Recording System (PowerShell)"
    Write-Host ""
    Write-Host "Usage: .\run_local_tests.ps1 [-Mode <MODE>] [-InstallDeps] [-Help]"
    Write-Host ""
    Write-Host "MODES:"
    Write-Host "  quick        Run quick test suite (default, ~2 minutes)"
    Write-Host "  full         Run complete test suite (all levels)"
    Write-Host "  requirements Validate functional and non-functional requirements"
    Write-Host "  performance  Run performance benchmarks"
    Write-Host "  ci           Run CI/CD mode tests"
    Write-Host "  pc           Run PC/desktop application tests"
    Write-Host "  android      Run Android application tests"
    Write-Host "  gui          Run GUI/UI tests for both platforms"
    Write-Host ""
    Write-Host "OPTIONS:"
    Write-Host "  -InstallDeps     Install test dependencies before running"
    Write-Host "  -Help            Show this help message"
    Write-Host ""
    Write-Host "EXAMPLES:"
    Write-Host "  .\run_local_tests.ps1                        # Quick test suite"
    Write-Host "  .\run_local_tests.ps1 -Mode full             # Complete test suite"
    Write-Host "  .\run_local_tests.ps1 -Mode requirements     # Requirements validation"
    Write-Host "  .\run_local_tests.ps1 -Mode quick -InstallDeps # Install deps and run quick tests"
    Write-Host "  .\run_local_tests.ps1 -Mode pc               # PC application tests only"
    Write-Host "  .\run_local_tests.ps1 -Mode android          # Android application tests only"
    Write-Host "  .\run_local_tests.ps1 -Mode gui              # GUI tests for both platforms"
    Write-Host ""
    Write-Host "UNIFIED FRAMEWORK USAGE:"
    Write-Host "  # Direct usage of unified framework"
    Write-Host "  python tests_unified\runners\run_unified_tests.py --help"
    Write-Host ""
    Write-Host "  # Specific test levels"
    Write-Host "  python tests_unified\runners\run_unified_tests.py --level unit"
    Write-Host "  python tests_unified\runners\run_unified_tests.py --level integration"
    Write-Host ""
    Write-Host "  # Specific categories"
    Write-Host "  python tests_unified\runners\run_unified_tests.py --category android"
    Write-Host "  python tests_unified\runners\run_unified_tests.py --category hardware"
    Write-Host ""
}

function Test-ProjectRoot {
    $requiredItems = @("pyproject.toml", "setup.py", "PythonApp")
    foreach ($item in $requiredItems) {
        if (Test-Path $item) {
            return $true
        }
    }
    return $false
}

# Main execution
function Main {
    if ($Help) {
        Show-Usage
        exit 0
    }
    
    Write-Status "Multi-Sensor Recording System - Local Test Runner (PowerShell)"
    Write-Status "================================================================"
    
    # Check if we're in the right directory
    if (-not (Test-ProjectRoot)) {
        Write-Error "Not in project root directory. Please run from the repository root."
        exit 1
    }
    
    # Install dependencies if requested
    if ($InstallDeps) {
        if (-not (Install-Dependencies)) {
            exit 1
        }
    }
    
    # Check for unified framework and run appropriate tests
    if (Test-UnifiedFramework) {
        Write-Success "Unified testing framework found"
        if (-not (Invoke-UnifiedTests $Mode)) {
            exit 1
        }
    }
    else {
        if (-not (Invoke-LegacyTests)) {
            exit 1
        }
    }
    
    Write-Success "Test execution completed"
    exit 0
}

# Execute main function
Main