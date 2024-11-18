version=1.5.0-$(shell git rev-parse --short HEAD)
#version=1.0.0
repo ?= ghcr.io/jd-opensource

clean:
	mvn clean -f ../pom.xml

build:
	mvn package -f ../pom.xml -DskipTests=true package

image-joylive-demo-springcloud2021-consumer:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud2021-consumer:${version}-AMD64 ./joylive-demo-springcloud2021/joylive-demo-springcloud2021-consumer

push-joylive-demo-springcloud2021-consumer:
	docker push ${repo}/joylive-demo-springcloud2021-consumer:${version}-AMD64

image-joylive-demo-springcloud2021-provider:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud2021-provider:${version}-AMD64 ./joylive-demo-springcloud2021/joylive-demo-springcloud2021-provider

push-joylive-demo-springcloud2021-provider:
	docker push ${repo}/joylive-demo-springcloud2021-provider:${version}-AMD64

image-joylive-demo-springcloud2021-provider-reactive:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud2021-provider-reactive:${version}-AMD64 ./joylive-demo-springcloud2021/joylive-demo-springcloud2021-provider-reactive

push-joylive-demo-springcloud2021-provider-reactive:
	docker push ${repo}/joylive-demo-springcloud2021-provider-reactive:${version}-AMD64

image-joylive-demo-springcloud2021-order:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud2021-order:${version}-AMD64 ./joylive-demo-springcloud2021/joylive-demo-springcloud2021-order

push-joylive-demo-springcloud2021-order:
	docker push ${repo}/joylive-demo-springcloud2021-order:${version}-AMD64

image-joylive-demo-springcloud2021-gateway:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-springcloud2021-gateway:${version}-AMD64 ./joylive-demo-springcloud2021/joylive-demo-springcloud2021-gateway

push-joylive-demo-springcloud2021-gateway:
	docker push ${repo}/joylive-demo-springcloud2021-gateway:${version}-AMD64

image-joylive-demo-rocketmq:
	docker build --platform linux/amd64 -t ${repo}/joylive-demo-rocketmq:${version}-AMD64 ./joylive-demo-rocketmq

push-joylive-demo-rocketmq:
	docker push ${repo}/joylive-demo-rocketmq:${version}-AMD64