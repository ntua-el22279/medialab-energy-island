#!/usr/bin/env bash
# Compiles the project and runs the scenario validation regression check.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -B compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt 2>&1 \
  | grep -v -E 'sun.misc.Unsafe|AbstractFuture|terminally deprecated|future release' || true
exec jshell -q --class-path "target/classes:$(cat target/classpath.txt)" tools/validation-check.jsh < /dev/null
