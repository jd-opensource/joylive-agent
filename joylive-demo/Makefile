version=1.0.0-$(shell git rev-parse --short HEAD)
#version=1.0.0
repo ?= ghcr.io/jd-opensource

clean:
	mvn clean -f ../pom.xml

build:
	mvn package -f ../pom.xml -DskipTests=true package

image-joylive-demo-springcloud3-consumer:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud3-consumer:${version}-AMD64 ./joylive-demo-springcloud3/joylive-demo-springcloud3-consumer

push-joylive-demo-springcloud3-consumer:
	docker push ${repo}/joylive-demo-springcloud3-consumer:${version}-AMD64

image-joylive-demo-springcloud3-provider:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud3-provider:${version}-AMD64 ./joylive-demo-springcloud3/joylive-demo-springcloud3-provider

push-joylive-demo-springcloud3-provider:
	docker push ${repo}/joylive-demo-springcloud3-provider:${version}-AMD64

image-joylive-demo-springcloud3-gateway:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud3-gateway:${version}-AMD64 ./joylive-demo-springcloud3/joylive-demo-springcloud3-gateway

push-joylive-demo-springcloud3-gateway:
	docker push ${repo}/joylive-demo-springcloud3-gateway:${version}-AMD64

image-joylive-demo-rocketmq:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-rocketmq:${version}-AMD64 ./joylive-demo-rocketmq

push-joylive-demo-rocketmq:
	docker push ${repo}/joylive-demo-rocketmq:${version}-AMD64