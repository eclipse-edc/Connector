# Kafka transfer

## How to run Kafka integration tests

We provide a [docker-compose.yml](docker-compose.yaml) file which enables to deploy a Zookeeper instance and a Kafka broker.

```bash
docker-compose -f system-tests/e2e-transfer-test/resources/kafka/docker-compose.yaml up -d
```