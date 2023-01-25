FROM openjdk:11-jdk as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:11-jdk as runtime
ARG USER=tmpuser
ARG HOME=/home/$USER
RUN addgroup --gid 1000 "$USER" \
  && adduser -u 1000 -G "$USER" -h "$HOME" "$USER" --disabled-password
RUN chown $USER:$USER $HOME
USER $USER
WORKDIR $HOME
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader ./
COPY --from=builder application/organization-dependencies ./
COPY --from=builder application/modules-dependencies ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ADD src/main/docker/entrypoint.sh bin/entrypoint.sh

ENV RUN_SLEEP 1
ENV JAVA_OPTS '-Xmx512m -Xms512m -Xss256k'
ENV JAVA_AGENT ''

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' >/etc/timezone \
    && mkdir logs \
    && cd logs \
    && touch entrypoint.out \
    && ln -sf /dev/stdout entrypoint.out \
    && ln -sf /dev/stderr entrypoint.out

RUN chmod +x bin/entrypoint.sh
ENTRYPOINT [ "sh", "-c", "sh bin/entrypoint.sh"]
