#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Script to build dex with android dependencies
# -----------------------------------------------------------------------------

BASE_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) >/dev/null 2>&1 && pwd)

set -e
${BASE_DIR}/gradlew build -x test



cd ${BASE_DIR}/build/outputs/aar/

unzip plugin-comms-twosix-java-debug.aar

/opt/android/build-tools/default/d8 --min-api 29 classes.jar

cd ${BASE_DIR}