#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR/..

GOOGLE_JAVA_FORMAT_JAR="google-java-format-1.8-all-deps.jar"
CPP_SOURCE=$(find loader test -type f \( -iname \*.h -o -iname \*.cpp \))
CONTAINER_CODE_MOUNT_POINT="/code/project"
IMAGE="gitlab.race.twosixlabs.com:4567/race-ta3/racesdk/race-sdk:develop"
CONTAINER_NAME="race-code-formatter-java-comms"
FORMAT_CODE_COMMAND=" \
if [ ! -f $GOOGLE_JAVA_FORMAT_JAR ]; then \
    wget https://github.com/google/google-java-format/releases/download/google-java-format-1.8/$GOOGLE_JAVA_FORMAT_JAR; \
fi; \
echo \"Formatting Java source files...\"; \
find . -name \"*.java\" | xargs java -jar $GOOGLE_JAVA_FORMAT_JAR --replace --aosp; \
echo \"Formatting C++ source files:\"; \
for FILE in \$CPP_SOURCE; do \
  echo \$FILE; \
  clang-format-10 -i \$FILE; \
done"

docker run --rm \
    -v $(pwd):$CONTAINER_CODE_MOUNT_POINT \
    -w="${CONTAINER_CODE_MOUNT_POINT}" \
    --env CPP_SOURCE="${CPP_SOURCE}" \
    --name=$CONTAINER_NAME \
    $IMAGE \
    bash -c "${FORMAT_CODE_COMMAND}"
