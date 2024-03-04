#!/usr/bin/env bash

# Script to pull artifacts from Gitlab.
# Note that you will need to set your Gitlab API token either:
#     1. In an environment variable named GITLAB_ARTIFACT_TOKEN
#     2. In a file located in ~/.race/gitlab/api_token

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR/..

source ./scripts/common.sh

GITLAB_API_TOKEN_FILE="$HOME/.race/gitlab/api_token"

if [ -z "$GITLAB_ARTIFACT_TOKEN" ] && [ -f "$GITLAB_API_TOKEN_FILE" ]; then
    echo "setting Gitlab API token from file"
    GITLAB_ARTIFACT_TOKEN="$(cat $GITLAB_API_TOKEN_FILE)"
fi

if [ -z $GITLAB_ARTIFACT_TOKEN ]; then
    echo "Gitlab API token has no been set. Please export it in an environment variable named GITLAB_ARTIFACT_TOKEN or write it to the file $GITLAB_API_TOKEN_FILE"
    exit 1
fi

rm -rf $ARTIFACT_DIR

ARTIFACT_ZIP_FILE="artifacts.zip"
GITLAB_API_URL="https://gitlab.race.twosixlabs.com/api/v4"

ARTIFACT_BRANCH="develop"
RACESDK_COMMON_PROJECT_ID="83"
echo "pulling artifacts for racesdk-common branch=$ARTIFACT_BRANCH"
curl --header "PRIVATE-TOKEN: $GITLAB_ARTIFACT_TOKEN" -o $ARTIFACT_ZIP_FILE ${GITLAB_API_URL}/projects/$RACESDK_COMMON_PROJECT_ID/jobs/artifacts/$ARTIFACT_BRANCH/download?job=package
unzip -o $ARTIFACT_ZIP_FILE -d $ARTIFACT_DIR || $(cat $ARTIFACT_ZIP_FILE && exit 1)

rm -rf $ARTIFACT_ZIP_FILE
