version=1.0.0-$(shell git rev-parse --short HEAD)
#version=1.0.0
repo ?= ghcr.io/jd-opensource

all: clean build image-joylive push-joylive

image-all: image-joylive push-joylive

clean:
	mvn clean -f ../pom.xml

build:
	mvn package -f ../pom.xml -DskipTests=true package

image-joylive:
	docker build --platform linux/amd64 -t ${repo}/joylive-agent:${version}-AMD64 .

push-joylive:
	docker push ${repo}/joylive-agent:${version}-AMD64
