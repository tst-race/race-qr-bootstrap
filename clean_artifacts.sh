#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Clean previously built artifact dirs to ensure clean build
# -----------------------------------------------------------------------------


set -e


###
# Helper functions
###


# Load Helper Functions
CURRENT_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) >/dev/null 2>&1 && pwd)
. ${CURRENT_DIR}/helper_functions.sh


###
# Arguments
###


HELP=\
"Clean previously built artifact dirs to ensure clean build

Build Arguments:
    N/A

Help Arguments:
    -h, --help
        Print this message

Examples:
    ./clean_artifacts.sh
"

while [ $# -gt 0 ]
do
    key="$1"

    case $key in
        -h|--help)
        printf "%s" "${HELP}"
        shift
        exit 1;
        ;;
        *)
        echo "${CALL_NAME} unknown argument \"$1\""
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


formatlog "INFO" "Removing previous build artifacts"
rm -rf ${CURRENT_DIR}/build/*
rm -rf ${CURRENT_DIR}/plugin/target/*

formatlog "INFO" "Cleaning artifacts in the plugin dir"
rm -rf ${CURRENT_DIR}/plugin/artifacts/*
