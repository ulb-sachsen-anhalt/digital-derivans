.ONESHELL:
SHELL:=/bin/bash

IMAGE_NAME=digital-derivans

all: build

build: FORCE build-jar docker-build

build-jar: FORCE
	mvn clean package

docker: FORCE docker-build docker-run-derivans

docker-build: FORCE
	scripts/build_docker_image.sh

docker-run-bash: FORCE
	docker run -it --rm --entrypoint /bin/bash ${IMAGE_NAME}

docker-run-derivans: FORCE
	PATH_INPUT_DATA="./temp/1981185920_57076"
	PATH_CFG="./temp/derivans.ini"
	scripts/docker_run_derivans.sh "$$PATH_INPUT_DATA" -c "$$PATH_CFG"


.PHONY: FORCE
FORCE:
