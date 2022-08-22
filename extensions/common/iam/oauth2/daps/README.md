# Dynamic Attribute Provisioning Service (DAPS)

## How to run integration tests
Run omejdn server:
```
docker run --rm -p 4567:4567 -v $PWD/extensions/common/iam/daps/src/test/resources/config:/opt/config -v $PWD/extensions/common/iam/daps/src/test/resources/keys:/opt/keys ghcr.io/fraunhofer-aisec/omejdn-server:1.4.2
```