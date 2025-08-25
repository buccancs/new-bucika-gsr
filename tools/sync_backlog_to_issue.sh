#!/bin/bash

# Script to convert BACKLOG.md content for safe GitHub issue updating
# This script handles HTML entity encoding to prevent content loss

set -e

BACKLOG_FILE="BACKLOG.md"
OUTPUT_FILE="backlog_for_github_issue.md"

if [[ ! -f "$BACKLOG_FILE" ]]; then
    echo "Error: $BACKLOG_FILE not found"
    exit 1
fi

echo "Converting BACKLOG.md for GitHub issue compatibility..."

# Create a copy with HTML entities properly encoded
sed -e 's/</\&lt;/g' -e 's/>/\&gt;/g' "$BACKLOG_FILE" > "$OUTPUT_FILE"

echo "✓ Created $OUTPUT_FILE with HTML entities encoded"
echo ""
echo "Content preview of affected lines:"
echo "=================================="
grep -n "&lt;\|&gt;" "$OUTPUT_FILE" | head -5

echo ""
echo "Instructions:"
echo "1. Copy the content from $OUTPUT_FILE"
echo "2. Paste into GitHub issue #20 body"
echo "3. The angle brackets will be preserved as intended"
echo "4. Delete $OUTPUT_FILE when done"