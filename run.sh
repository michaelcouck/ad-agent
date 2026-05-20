#!/usr/bin/env bash
set -euo pipefail

JAR="${1:-target/google-marketing-mcp-0.1.0-SNAPSHOT.jar}"

if [ ! -f "${JAR}" ]; then
  echo "Jar not found: ${JAR}" >&2
  echo "Build first with: mvn -q -DskipTests package" >&2
  exit 1
fi

exec java -jar "${JAR}"
