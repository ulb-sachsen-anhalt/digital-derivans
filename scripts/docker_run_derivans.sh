#!/bin/bash

# Minimum 1 param is required
if [ $# -lt 1 ]; then
    echo "Error: At least one parameter is required: \"path\""
    exit 1
fi

THIS_SCRIPT=$(realpath "${BASH_SOURCE[0]}")
THIS_SCRIPT_DIR="$(dirname "$THIS_SCRIPT")"
ROOT_DIR=$(realpath "$THIS_SCRIPT_DIR/..")

# Create an array to store all arguments
ARGS=()

ARG_PATH_INPUT_DATA="$1"
PATH_INPUT_DATA_ABS=$(realpath "$ARG_PATH_INPUT_DATA")
ARGS+=("$PATH_INPUT_DATA_ABS")
shift 1 # Shift the positional parameters to remove the input path

# check if config param is given
while [ $# -gt 0 ]; do
    if [ "$1" == "--config" ] || [ "$1" == "-c" ]; then
        if [ -z "$2" ]; then
            echo "Error: The config parameter requires a value."
            exit 1
        fi
        ARG_PATH_CONFIG="$2"
        PATH_CONFIG_ABS=$(realpath "$ARG_PATH_CONFIG")
        ARGS+=("$1")
        ARGS+=("$PATH_CONFIG_ABS")
        shift 2 # Consume both the parameter and its value
    else
        # Handle other parameters as needed
        ARGS+=("$1")
        shift 1 # Consume only the parameter
    fi
done

IMAGE_NAME=$("$THIS_SCRIPT_DIR/read_artifact_id_from_pom_xml.sh" "$ROOT_DIR")

MOUNTS=("--mount type=bind,source=$PATH_INPUT_DATA_ABS,target=$PATH_INPUT_DATA_ABS")

if [ -n "$ARG_PATH_CONFIG" ]; then
    PATH_CONFIG_DIR=$(dirname "$PATH_CONFIG_ABS")
    MOUNTS+=("--mount type=bind,source=$PATH_CONFIG_DIR,target=$PATH_CONFIG_DIR")
fi

RUN_CMD="docker run ${MOUNTS[*]} --rm $IMAGE_NAME ${ARGS[*]}"

echo "$RUN_CMD"

eval "$RUN_CMD"
