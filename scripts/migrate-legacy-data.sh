#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <path-to-legacy-xlsx>"
  exit 1
fi

XLSX_PATH="$1"

if [ ! -f "$XLSX_PATH" ]; then
  echo "File not found: $XLSX_PATH"
  exit 1
fi

# classpathScope=test pulls in both the runtime-scope postgresql driver and the
# provided-scope poi-ooxml lib, neither of which alone covers both dependencies.
mvn exec:java \
  -Dexec.mainClass=com.sharemoney.migration.LegacyDataMigrationTool \
  -Dexec.classpathScope=test \
  -Dexec.args="\"$XLSX_PATH\""
