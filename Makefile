MAVEN=mvn
DOCKER_IMAGE?=omnition/synthetic-load-generator
BUILD_NUMBER?=latest

.PHONY: all build publish
all: build publish
build: java-jars docker-build

.PHONY: java-jars
java-jars: # Create jars without running tests.
	@echo "\n===== $@ ======"
	# Ensure required dependencies are met
	@$(call --check-dependencies,${MAVEN})
	$(MAVEN) package -DskipTests

.PHONY: docker-build
docker-build:
	@echo "\n===== $@ ======"
	envsubst < Dockerfile | docker build --pull -t ${DOCKER_IMAGE}:${BUILD_NUMBER} -f - .

