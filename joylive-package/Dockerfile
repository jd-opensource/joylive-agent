FROM alpine:latest

ENV TZ Asia/Shanghai

WORKDIR /joylive/
COPY ./target/*.zip /joylive/

RUN /bin/sh -c 'for f in /joylive/*; do if [ ${f##*.} == 'zip' ];then unzip -q -d /joylive/ $f; fi; done'
RUN rm -f /joylive/*.zip
RUN mkdir /joylive/log
RUN mkdir /joylive/output

VOLUME ["/joylive/log", "/joylive/config"]
CMD ["true"]