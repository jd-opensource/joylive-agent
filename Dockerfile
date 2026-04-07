# Build an image using JDK 17 and Maven
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Set up a working directory
WORKDIR /app

# Copy the project file to the working directory
COPY . .

# Build the project using mvn directly (mvn is available in the maven image)
RUN mvn clean package -DskipTests=true

# Use a smaller base image
FROM alpine

# Set the time zone and locale
ARG TZ="Asia/Shanghai"
ENV TZ=${TZ}
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV LANGUAGE=en_US:en

RUN set -ex \
    && apk add --no-cache bash tzdata unzip ca-certificates \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo ${TZ} > /etc/timezone

# Create a target directory
RUN mkdir -p /joylive

# Copy the zip file generated during the build phase and extract it
COPY --from=builder /app/joylive-package/target/*.zip /joylive/
RUN unzip /joylive/*.zip -d /joylive/ \
    && rm /joylive/*.zip

# Set up a working directory
WORKDIR /joylive

RUN mkdir /joylive/log
RUN mkdir /joylive/output

VOLUME ["/joylive/log", "/joylive/config"]
CMD ["true"]
