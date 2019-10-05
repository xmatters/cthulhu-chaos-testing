FROM java:8 AS BUILD_IMAGE
ENV APP_HOME=/root/dev/cthulhu/
RUN mkdir -p $APP_HOME/src/main/java
WORKDIR $APP_HOME
COPY build.gradle gradlew gradlew.bat $APP_HOME
COPY gradle $APP_HOME/gradle
COPY . .
RUN ./gradlew prepareDocker -Penvironment=docker


FROM java:8

WORKDIR /opt/cthulhu/

COPY ./docker/* /opt/cthulhu/
COPY --from=BUILD_IMAGE /root/dev/cthulhu/docker/* /opt/cthulhu/

ENV SCENARIO /etc/cthulhu/scenario.yaml
ENV GCP_ACCOUNT_JSON /etc/secrets/gcp-account.json
ENV KUBE_CONFIG /etc/secrets/kube.config

ENTRYPOINT ./entrypoint.sh