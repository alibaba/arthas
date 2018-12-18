FROM openjdk:8-jdk-alpine

# https://github.com/docker-library/openjdk/issues/76
RUN apk add --no-cache tini
# Tini is now available at /sbin/tini
ENTRYPOINT ["/sbin/tini", "--"]

RUN wget -qO- -O /tmp/arthas.zip https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/3.0.5/arthas-packaging-3.0.5-bin.zip && mkdir -p /opt/arthas && unzip /tmp/arthas.zip -d /opt/arthas && rm /tmp/arthas.zip

