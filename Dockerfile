FROM openjdk:8-jdk-alpine

MAINTAINER arthas anjia0532

ARG ARTHAS_VERSION="3.0.5"

# https://github.com/docker-library/openjdk/issues/76
RUN apk add --no-cache tini

# Tini is now available at /sbin/tini
ENTRYPOINT ["/sbin/tini", "--"]

RUN wget -qO- -O /tmp/arthas.zip https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/${ARTHAS_VERSION}/arthas-packaging-${ARTHAS_VERSION}-bin.zip && \
			mkdir -p /opt/arthas && \
			unzip /tmp/arthas.zip -d /opt/arthas && \
			rm /tmp/arthas.zip
