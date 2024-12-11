FROM ghcr.io/meshware/ubuntu-oracle-jdk:11.0.21
USER root

ENV TZ Asia/Shanghai

WORKDIR /export/servers/
COPY ./target/joylive-demo-grpc-provider.jar /export/servers/

VOLUME ["/export/servers/logs", "/export/servers/config"]
EXPOSE 9898
EXPOSE 9899
ENTRYPOINT ["/bin/bash", "-c", "java ${JAVA_OPTS} -jar /export/servers/joylive-demo-grpc-provider.jar"]