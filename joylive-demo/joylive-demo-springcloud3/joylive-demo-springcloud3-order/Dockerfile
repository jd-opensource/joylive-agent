FROM ghcr.io/meshware/ubuntu-oracle-jdk:11.0.21
USER root

ENV TZ Asia/Shanghai

WORKDIR /export/servers/
COPY ./target/joylive-demo-springcloud3-order.jar /export/servers/

VOLUME ["/export/servers/logs", "/export/servers/config"]
EXPOSE 18081
ENTRYPOINT ["/bin/bash", "-c", "java ${SGM_OPTS} ${JAVA_OPTS} -jar /export/servers/joylive-demo-springcloud3-order.jar"]