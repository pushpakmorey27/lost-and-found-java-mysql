#!/usr/bin/env bash
# Linux/macOS launcher. Run: bash launch.sh
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1; then
    echo 'Install Java JDK 21 or newer, then open a new terminal.'
    exit 1
fi
mkdir -p bin
javac --release 21 -encoding UTF-8 -d bin src/start/*.java src/items/*.java src/database/*.java src/website/*.java src/console/*.java
java -cp 'bin:lib/*' Main
