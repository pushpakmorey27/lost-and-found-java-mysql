#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ $# -ne 0 ]] && [[ $# -ne 2 || "$1" != "--mysql" || "$2" != "--reset-test-database" ]]; then
  echo "Usage: bash test.sh [--mysql --reset-test-database]" >&2
  exit 2
fi
if ! command -v javac >/dev/null 2>&1; then
  echo "Java JDK 21 or newer is required." >&2
  exit 1
fi

mkdir -p build/tests
javac --release 21 -encoding UTF-8 -cp 'lib/*' -d build/tests \
  src/start/*.java src/items/*.java src/database/*.java \
  src/console/*.java src/website/*.java tests/*.java
java -cp 'build/tests:lib/*' CoreChecks
if [[ $# -eq 2 ]]; then
  java -cp 'build/tests:lib/*' IntegrationChecks --reset-test-database
fi
