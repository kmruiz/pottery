#!/bin/bash

set -e

DEFAULT_VERSION=0.2.0
VERSION_TO_DOWNLOAD=${POTTERY_VERSION:-"$DEFAULT_VERSION"}
DOWNLOAD_URL=https://github.com/kmruiz/pottery/releases/download/${VERSION_TO_DOWNLOAD}/pottery-${VERSION_TO_DOWNLOAD}-fat.jar
POTTERY_JAR=.pottery/pottery.jar

if ! [ -f "$POTTERY_JAR" ]; then
  echo "Downloading pottery for the first time. Version ${VERSION_TO_DOWNLOAD} from ${DOWNLOAD_URL}."
  mkdir -p .pottery
  curl -L -s "${DOWNLOAD_URL}" > "${POTTERY_JAR}"
fi

java -jar ${POTTERY_JAR} "$@"