FROM apache/nifi:latest as nifi

COPY ./processors/build/libs/processors*.nar /opt/nifi/nifi-current/lib/edc-processors.nar

ENTRYPOINT ["/opt/nifi/scripts/start.sh"]
