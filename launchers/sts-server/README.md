# Simple STS standalone launcher

This is a simple launcher demonstrating how to assemble and run an STS server. This is a minimal STS
running as a separated process with only one client configured via extension.

## How to build the STS

To build the STS just run this command in the root of the connector project:

```shell
./gradlew launchers:sts-server:build
```

Once the build end, the STS jar should be available in the build directory `aunchers/sts-server/build/libs/sts-server.jar`

### How to run the STS

To run the STS, just run the following command:

```shell
java -Dedc.keystore=launchers/sts-server/certs/cert.pfx -Dedc.keystore.password=123456 -Dedc.vault=launchers/sts-server/sts-vault.properties -Dedc.fs.config=launchers/sts-server/config.properties -jar launchers/sts-server/build/libs/sts-server.jar
 ```

The STS will be available on `9292` port.

### Fetching tokens

The only client configured is `testClient` one via `config.properties` file. The STS for now supports only 
OAuth2 client credentials with some custom parameters.

To fetch a token run this curl command:

```shell
curl --request POST \
  --url http://localhost:9292/api/v1/sts/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id=testClient \
  --data client_secret=clientSecret \
  --data audience=test10 
```

For attaching the `bearer_access_scope` as described in the IATP [spec](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/identity.protocol.base.md#6-using-the-oauth-2-client-credential-grant-to-obtain-access-tokens-from-an-sts) :

```shell
curl --request POST \
  --url http://localhost:9292/api/v1/sts/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=client_credentials \
  --data client_id=testClient \
  --data client_secret=clientSecret \
  --data audience=test10 \
  --data bearer_access_scope=test
```

It will generate an additional claim `access_token` with the scopes inside.