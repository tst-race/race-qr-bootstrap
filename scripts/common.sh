#!/usr/bin/env bash

export ARTIFACT_DIR=".gitlab_artifacts"

export SETUP_ARTIFACTS_COMMAND="cp $ARTIFACT_DIR/include/* /usr/local/include/ && cp $ARTIFACT_DIR/build/* /usr/local/lib/"
