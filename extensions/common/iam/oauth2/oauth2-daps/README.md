# Dynamic Attribute Provisioning Service (DAPS)

## How to run integration tests
Run omejdn server:
```
export DAPS_RESOURCES=$PWD/extensions/common/iam/oauth2/oauth2-daps/src/test/resources
docker run --rm -p 4567:4567 -v $DAPS_RESOURCES/config:/opt/config -v $DAPS_RESOURCES/keys:/opt/keys ghcr.io/fraunhofer-aisec/omejdn-server:1.4.2
```