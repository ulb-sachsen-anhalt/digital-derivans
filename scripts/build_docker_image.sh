#!/bin/bash


THIS_SCRIPT=$(realpath "${BASH_SOURCE[0]}")
THIS_SCRIPT_DIR="$(dirname "$THIS_SCRIPT")"
ROOT_DIR=$(realpath "$THIS_SCRIPT_DIR/..")

PROJECT_NAME=$("$THIS_SCRIPT_DIR/read_artifact_id_from_pom_xml.sh" "$ROOT_DIR")

VERSION=$("$THIS_SCRIPT_DIR/read_version_from_pom_xml.sh" "$ROOT_DIR")
IMAGE_NAME="$PROJECT_NAME:$VERSION"
IMAGE_NAME_LATEST="$PROJECT_NAME:latest"
JAR_FILE_NAME="$PROJECT_NAME-$VERSION.jar"
JAR_FILE="$ROOT_DIR/target/$JAR_FILE_NAME"
JAR_FILE_RELATIVE_PATH=$(realpath --relative-to="$ROOT_DIR" "$JAR_FILE")

cd "$ROOT_DIR" || exit 1

if ! [ -f "$JAR_FILE" ]; then
    mvn clean package
fi

docker build \
    --build-arg VERSION="$VERSION" \
    --build-arg JAR_FILE_NAME="$JAR_FILE_NAME" \
    --build-arg JAR_FILE="$JAR_FILE_RELATIVE_PATH" \
    -t "$IMAGE_NAME" .

docker tag "$IMAGE_NAME" "$IMAGE_NAME_LATEST"

echo created image "$IMAGE_NAME"
