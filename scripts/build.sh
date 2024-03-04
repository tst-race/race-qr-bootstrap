#!/usr/bin/env bash

# Script for verifying that the project will build. Creates a container and runs the build commands. Should be run from your host machine.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR/..

CONTAINER_CODE_MOUNT_POINT="/code/project"
BUILD_DIR=".temp_build_script_dir"
BUILD_COMMAND="set -e; apt-get install -y maven openjdk-8-jdk ant ca-certificates-java; cmake -B./$BUILD_DIR -H./ -DBUILD_VERSION=build-script; cmake --build $BUILD_DIR"
IMAGE="gitlab.race.twosixlabs.com:4567/race-ta3/racesdk/race-sdk:latest"
CONTAINER_NAME="race-builder-plugin-comms-qrwifi-java"

docker pull $IMAGE

docker run -it --rm \
    -v $(pwd):$CONTAINER_CODE_MOUNT_POINT \
    -w="${CONTAINER_CODE_MOUNT_POINT}" \
    --name=$CONTAINER_NAME \
    $IMAGE \
    bash -c "$BUILD_COMMAND"
