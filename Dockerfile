FROM java:8

WORKDIR /opt/cthulhu/

COPY ./docker/* /opt/cthulhu/

ENV SCENARIO /etc/cthulhu/scenario.yaml
ENV GCP_ACCOUNT_JSON /etc/secrets/gcp-account.json
ENV KUBE_CONFIG /etc/secrets/kube.config

ENTRYPOINT ./entrypoint.sh