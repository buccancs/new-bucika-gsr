# new-bucika-gsr
This repository is currently a minimal template/starting point with no actual source code, build system, or functional application. It contains only basic setup files (README.md, LICENSE, .gitignore) configured for Android development.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Current Repository State
**CRITICAL**: This repository is essentially empty - there is no code to build, test, or run currently.

The repository contains only:
- `README.md` - Minimal placeholder content
- `LICENSE` - Apache 2.0 license
- `.gitignore` - Configured for Android/Gradle projects (indicates intended use)

## Working Effectively
Since this is an empty repository, traditional build/test commands will not work:

### Repository Validation
- Verify repository contents: `ls -la`
- Check for hidden files: `find . -name ".*" -type f | head -10`
- Confirm no build files exist: `find . -name "*.gradle" -o -name "build.xml" -o -name "package.json" -o -name "Makefile"`

**Expected Results**: Only README.md, LICENSE, and .gitignore should be present.

### Development Setup (If Adding Code)
Based on the .gitignore configuration, this repository is intended for Android development:

- Install Android SDK and tools (if developing Android apps)
- Install Gradle (for Android build system)
- Install Java Development Kit (JDK 8 or higher)

**WARNING**: Do not attempt to run `gradle build`, `./gradlew build`, or similar commands - they will fail as no build.gradle files exist.

## What You Cannot Do (Empty Repository)
- **Cannot build**: No build scripts or configuration files exist
  - `gradle build` fails: "Directory does not contain a Gradle build"
  - `./gradlew build` fails: "No such file or directory"
  - `npm run build` fails: "Unknown command" or "no package.json"
  - `make` fails: "No targets specified and no makefile found"
- **Cannot test**: No test framework or test files exist
- **Cannot run**: No executable code or application exists
- **Cannot install dependencies**: No dependency management files exist

## Scenario Validation
After any changes to the repository, always run these complete scenarios to ensure the repository remains in its expected minimal state:

### Scenario 1: Fresh Developer Onboarding
Simulate a new developer's first interaction:
```bash
# Step 1: Verify clean repository state
ls -la
git status
find . -name "*.java" -o -name "*.kt" -o -name "*.gradle" | wc -l

# Step 2: Attempt common build commands (should all fail appropriately)
gradle build 2>&1 | head -5 || echo "Expected: No Gradle build found"
./gradlew build 2>&1 || echo "Expected: gradlew not found"
npm run build 2>&1 || echo "Expected: npm command not found or package.json missing"
make 2>&1 || echo "Expected: No makefile found"

# Step 3: Confirm repository purpose from .gitignore
head -10 .gitignore
```
**Expected result**: All commands complete within 15 seconds total, build commands fail with expected error messages.

### Scenario 2: Code Change Impact Assessment
Before making any changes, validate current baseline:
```bash
# Document current state
find . -type f -not -path "./.git/*" | sort
git log --oneline -5
git branch -a
```
**Expected result**: Exactly 4 files, minimal commit history, current branch visible.

## Validation Steps
Always run these commands to verify the repository state:

1. **List repository contents** (takes < 1 second):
   ```bash
   ls -la
   ```
   **Expected output**: .git/, .github/, .gitignore, LICENSE, README.md

2. **Check for any source files** (takes < 1 second):
   ```bash
   find . -name "*.java" -o -name "*.kt" -o -name "*.xml" -o -name "*.gradle" | wc -l
   ```
   **Expected output**: 0

3. **Verify repository core files** (takes < 1 second):
   ```bash
   find . -type f -not -path "./.git/*" | wc -l
   ```
   **Expected output**: 4 (README.md, LICENSE, .gitignore, .github/copilot-instructions.md)

## Adding New Code
If you need to add code to this repository:

1. **For Android development** (based on .gitignore):
   - Create standard Android project structure: `app/`, `gradle/`, `build.gradle`, etc.
   - Add source files in `app/src/main/java/` or `app/src/main/kotlin/`
   - Add Android manifest in `app/src/main/AndroidManifest.xml`

2. **For other development**:
   - Update `.gitignore` to match your technology stack
   - Add appropriate build files (package.json, build.gradle, etc.)
   - Create source directory structure

## Common Tasks
### Repository Status Check
```bash
git status
```
**Expected**: Clean working directory (no untracked files except any you've added)

### View Repository History
```bash
git log --oneline
```
**Expected**: Minimal commit history showing initial setup

## Important Notes
- This repository is not broken - it's intentionally minimal
- Do not expect any build, test, or run commands to work
- Always validate the empty state before assuming something is missing
- The .gitignore suggests Android development intent, but no Android project exists yet

## Common Developer Scenarios

### "Why can't I build anything?"
**Answer**: This repository contains no buildable code. It's a template/starting point.

### "Are there any tests to run?"
**Answer**: No tests exist. This is expected for an empty repository.

### "Where is the main application code?"
**Answer**: No application code exists yet. This repository is waiting for initial development.

### "Should I run npm install or gradle build?"
**Answer**: No - these will fail because no package.json or build.gradle exists. This is expected behavior.

## Timing Expectations and Warnings
**CRITICAL**: Since this is an empty repository, all commands execute extremely quickly:

- **All validation commands**: Complete in < 1 second
- **Repository exploration**: Complete in < 5 seconds  
- **Failed build attempts**: Complete in < 15 seconds (including error messages)
- **Git operations**: Complete in < 1 second

**NO LONG-RUNNING OPERATIONS EXIST** - If any command takes more than 30 seconds, something is wrong.

**NEVER CANCEL WARNING**: Not applicable to this repository - all operations are instantaneous due to minimal content.

## Quick Reference Commands
Copy-paste these validated commands for immediate use:

```bash
# Complete repository validation (runs in < 5 seconds)
echo "=== Repository Contents ===" && ls -la
echo "=== File Count ===" && find . -type f -not -path "./.git/*" | wc -l  
echo "=== Source File Count ===" && find . -name "*.java" -o -name "*.kt" -o -name "*.xml" -o -name "*.gradle" | wc -l
echo "=== Git Status ===" && git status --porcelain
echo "=== Repository Type ===" && head -5 .gitignore
```

```bash
# Verify build commands fail as expected (runs in < 15 seconds)
echo "Testing gradle..." && (gradle build 2>&1 | head -3 || echo "EXPECTED FAILURE")
echo "Testing gradlew..." && (./gradlew build 2>&1 | head -3 || echo "EXPECTED FAILURE") 
echo "Testing npm..." && (npm run build 2>&1 | head -3 || echo "EXPECTED FAILURE")
echo "Testing make..." && (make 2>&1 | head -3 || echo "EXPECTED FAILURE")
```

## Troubleshooting
- **"No build.gradle found"**: Expected - this repository has no build system
- **"Nothing to build"**: Expected - this repository contains no source code  
- **"No main method/entry point"**: Expected - this repository contains no executable code
- **Gradle/Maven commands fail**: Expected - no build configuration exists
- **Commands complete too quickly**: Expected - minimal repository has no heavy operations
## Next Steps for Development
If you're tasked with adding functionality:

1. **Determine the intended technology stack** based on .gitignore (suggests Android)
2. **Initialize appropriate project structure**:
   - For Android: Use Android Studio's "New Project" wizard or `android create project`  
   - For other stacks: Update .gitignore and add appropriate build files
3. **Add source files and build configuration**
4. **Update these instructions** with actual build/test/run commands once code exists
5. **Add CI/CD workflows** in `.github/workflows/` as needed

### If Adding Android Development:
```bash
# Example Android project initialization (after installing Android SDK)
# This will create proper build.gradle files and project structure
android create project --target android-29 --name BucikaGsr --path . --activity MainActivity --package com.buccancs.bucikagsr
```

### If Adding Different Technology:
- Update `.gitignore` to match your technology (Node.js, Python, Java, etc.)
- Add appropriate dependency files (package.json, requirements.txt, pom.xml, etc.)
- Add build scripts and configuration files
- Update these instructions accordingly

**Remember**: After adding actual code, update these Copilot instructions with real build commands, test procedures, and running instructions that work for the new codebase.