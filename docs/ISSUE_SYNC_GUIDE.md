# GitHub Issue Synchronization Guide

## Overview

This guide explains how to keep GitHub issues synchronized with repository documentation files, specifically for issue #20 which contains the implementation backlog.

## Issue #20 Synchronization Problem

**Problem**: The GitHub issue #20 "Backlog" content is missing text that contains angle brackets (`<`, `>`) due to HTML parsing when the BACKLOG.md content was copied to the issue.

**Affected Content**:
- Line 73: Missing `<0.1%` in "30‑min run @128 Hz with missing samples" (should be "with <0.1% missing samples")
- Line 201: Missing `<15` in "New user records a 2‑device session in min using docs" (should be "in <15 min using docs")

## Solution

To properly sync the BACKLOG.md content with GitHub issue #20:

### Method 1: Manual HTML Entity Encoding
Replace angle brackets in the content before pasting to GitHub:
- `<` → `&lt;`
- `>` → `&gt;`

### Method 2: Code Block Formatting
Wrap sections containing angle brackets in backticks or code blocks to prevent HTML parsing.

### Method 3: Alternative Characters
Use alternative characters that won't be parsed as HTML:
- `<0.1%` → `less than 0.1%` 
- `<15 min` → `under 15 min`

## Verification

After updating the GitHub issue, verify these specific lines contain the correct content:
1. **E4 DoD**: "30‑min run @128 Hz with <0.1% missing samples"
2. **E16 DoD**: "New user records a 2‑device session in <15 min using docs"

## Automation Consideration

For future updates, consider:
- Using GitHub API with proper HTML entity encoding
- Creating a script that converts BACKLOG.md to GitHub issue format
- Using GitHub's issue templates with proper escaping