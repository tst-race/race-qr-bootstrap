#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Script to build artifacts for the plugin in all possible environments: 
# android client, linux client, and linux server. Once built, move the artifacts
# to the plugin/artifacts dir for publishing to Jfrog Artifactory
# -----------------------------------------------------------------------------


set -e
CALL_NAME="$0"


###
# Helper functions
###


# Load Helper Functions
BASE_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) >/dev/null 2>&1 && pwd)
. ${BASE_DIR}/helper_functions.sh


###
# Arguments
###

# Version values
RACE_VERSION="2.1.0"
PLUGIN_REVISION="r1"

# Build Arguments
CMAKE_ARGS=""
VERBOSE=""

HELP=\
"Script to build artifacts for the plugin for all possible environments: 
android client, linux client, and linux server. Once built, move the artifacts
to the plugin/artifacts dir for publishing to Jfrog Artifactory

Build Arguments:
    -c [value], --cmake_args [value], --cmake_args=[value]
        Additional arguments to pass to cmake.
    --race-version [value], --race-version=[value]
        Specify the RACE version. Defaults to '${RACE_VERSION}'.
    --plugin-revision [value], --plugin-revision=[value]
        Specify the Plugin Revision Number. Defaults to '${PLUGIN_REVISION}'.
    --verbose
        Make everything very verbose.

Help Arguments:
    -h, --help
        Print this message

Examples:
    ./build_artifacts.sh --race=2.0.0
"

while [ $# -gt 0 ]
do
    key="$1"

    case $key in
        --race-version)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "missing RACE version number" >&2
            exit 1
        fi
        RACE_VERSION="$2"
        shift
        shift
        ;;
        --race-version=*)
        RACE_VERSION="${1#*=}"
        shift
        ;;
        
        --plugin-revision)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "missing revision number" >&2
            exit 1
        fi
        PLUGIN_REVISION="$2"
        shift
        shift
        ;;
        --plugin-revision=*)
        PLUGIN_REVISION="${1#*=}"
        shift
        ;;

        --verbose)
        VERBOSE="-DCMAKE_VERBOSE_MAKEFILE=ON"
        shift
        ;;

        -h|--help)
        printf "%s" "${HELP}"
        shift
        exit 1;
        ;;
        *)
        formatlog "ERROR" "${CALL_NAME} unknown argument \"$1\""
        exit 1
        ;;
    esac
done

if [ ! -z "${VERBOSE}" ] ; then
    set -x
fi

###
# Main Execution
###

formatlog "INFO" "Cleaning plugin/artifacts Before Building Artifacts"
bash ${BASE_DIR}/clean_artifacts.sh

formatlog "INFO" "Building Android x86_64 Client"
cmake -B${BASE_DIR}/build/ANDROID_x86_64 -H./ \
    -DBUILD_VERSION="${RACE_VERSION}-${PLUGIN_REVISION}" \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/android-x86_64.toolchain.cmake \
    -DCMAKE_INSTALL_PREFIX=/android/x86_64 \
    -DTARGET_ARCHITECTURE=ANDROID_x86_64
# This will copy the output to plugin/artifacts/android-x86_64-client
cmake --build ${BASE_DIR}/build/ANDROID_x86_64

formatlog "INFO" "Building Android arm64-v8a Client"
cmake -B${BASE_DIR}/build/ANDROID_arm64-v8a -H./ \
    -DBUILD_VERSION="${RACE_VERSION}-${PLUGIN_REVISION}" \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/android-arm64-v8a.toolchain.cmake \
    -DCMAKE_INSTALL_PREFIX=/android/arm64-v8a \
    -DTARGET_ARCHITECTURE=ANDROID_arm64-v8a    
# This will copy the output to plugin/artifacts/android-arm64-v8a-client
cmake --build ${BASE_DIR}/build/ANDROID_arm64-v8a
