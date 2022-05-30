# IDS Connector Launcher

This launcher includes all extensions that are required for an IDS Connector deployment. That 
includes communication via an IDS protocol, currently IDS Multipart Messages, as well as using an 
IDS DAPS as the Identity Provider.

## Prerequisites

As the connector defined in this launcher connects to an IDS DAPS, a running and reachable DAPS is 
required for the connector to be able to communicate via IDS protocol. In addition, you need a valid 
certificate (located in a keystore, e.g. `.p12` format) provided by this DAPS that can be used to
uniquely identify the connector.

In the case that you do not have access to a publicly available DAPS or do not have a certificate 
for one, you can set up and configure a local DAPS instance for testing. To do so, please follow 
[this](#setting-up-a-local-daps-instance) guide.

## Modules

The following modules are used for this launcher:

### Core
| Name | Description                                                                                                   |
|------|---------------------------------------------------------------------------------------------------------------|
| core | all core modules, including e.g. the BaseRuntime as well as the modules for transfer and contract negotiation |

### Extensions

| Name                                   | Description                                                                        |
|----------------------------------------|------------------------------------------------------------------------------------|
| core:defaults                          | provides default (in-memory) implementations for various data stores               | 
| extensions:data-protocols:ids          | contains all IDS modules, e.g. for dispatching and handling IDS multipart messages | 
| extensions:filesystem:configuration-fs | reads configuration properties from a file in the file-system                      | 
| extensions:filesystem:vault-fs         | file-system based vault, required for using a certificate from the file-system     | 
| extensions:iam:oauth2:oauth2-core      | provides OAuth2 authentication, required as DAPS is OAuth2 based                   | 
| extensions:iam:daps                    | provides the DAPS specific extension for OAuth2                                    | 
| extensions:api:data-management         | provides endpoints e.g. for initiating a contract negotiation or a data transfer   |

All stores used in this launcher are in-memory implementations, meaning **all data will be lost 
once the connector is shut down**. If you want data to be persisted even after the connector shuts 
down, you may want to exchange the in-memory implementations for e.g. `CosmosDB` backed implementations.

## Configuration

Some extensions used in this launcher require certain configuration values to be provided at 
application start. Since the `filesystem:configuration-fs` extension is used, we can provide these 
values in a `.properties` file. You can find an example [config.properties](./config.properties) in 
this launcher's directory. Please adjust this for your setup as follows:

* `web.http.port`: The EDC's port defaults to 8181. Adjust this property to run it on a different port.
* `web.http.path`: The default path prefix under which endpoints are available.
* `web.http.ids.port`: The port on which IDS endpoints (currently only the Multipart endpoint) are available.
* `web.http.ids.path`: The path prefix under which IDS endpoints (currently only the Multipart endpoint) are available.
* `ids.webhook.address`: Set this to the address at which another connector can reach your connector, 
  as it is used as a callback address during the contract negotiation, where messages are exchanged 
  asynchronously. If you change the IDS API port, make sure to adjust the webhook address accordingly.
* `edc.api.auth.key`: Value of the header used for authentication when calling 
  endpoints of the data management API.
* `edc.oauth.token.url`: Set this to the URL of the DAPS you want to use followed by `/token` or 
  `/v2/token`, depending on the DAPS used.
* `edc.oauth.client.id`: Identifier from the certificate for the DAPS. You can find instructions on 
  how to get the identifier from the certificate [below](#getting-the-certificate-identifier).
* `edc.oauth.provider.audience`: Audience used when requesting a token from the DAPS. This feature 
  can be used to limit the validity of the token to certain connectors, but is currently not 
  supported by the DAPS. Therefore, this property has to be set to `idsc:IDS_CONNECTORS_ALL`.
* `edc.oauth.provider.jwks.url`: Set this to the URL of the DAPS you want to use followed by 
  `/.well-known/jwks.json`.
* `edc.oauth.public.key.alias`: Set this to your certificate's `alias` in the keystore.
* `edc.oauth.private.key.alias`: Set this to your certificate's `alias` in the keystore.

### Getting the Certificate Identifier

You can get the identifier of the certificate by using `openssl`. For this, a `.cert` file is 
required (not the `.p12`). If you only have the `.p12` file available, you can extract the 
certificate by running:

```shell
openssl pkcs12 -in <your-keystore>.p12 -out <output-name-of-your-cert>.cert -nodes
```

When you have the `.cert` file available, you next need to extract the `Subject Key Identifier` and 
the `Authority Key Identifier`, as these two compose the complete identifier.

#### 1. Getting the Subject Key Identifier

To get the `Subject Key Identifier`, run the following command:

```shell
openssl x509 -in <your-cert>.cert -noout -text | grep -A1 "Subject Key Identifier"
```

This will return output similar to the following, where the second line is the `Subject Key Identifier`:

```shell
X509v3 Subject Key Identifier: 
    52:71:9A:45:C9:78:EB:A3:0C:B5:57:25:87:35:3A:BF:94:46:A3:B8
```

#### 2. Getting the Authority Key Identifier

To get the `Authority Key Identifier`, run the following command:

```shell
openssl x509 -in <your-cert>.cert -noout -text | grep -A1 "Authority Key Identifier"
```

This will return output similar to the following, where the second line is the 
`Authority Key Identifier`:

```shell
X509v3 Authority Key Identifier: 
    keyid:52:71:9A:45:C9:78:EB:A3:0C:B5:57:25:87:35:3A:BF:94:46:A3:B8
```

#### 3. Composing the Identifier

The `Subject Key Identifier` and the `Authority Key Identifier`, separated by a colon, compose the 
certificate identifier. So your resulting identifier should look as follows:

```
52:71:9A:45:C9:78:EB:A3:0C:B5:57:25:87:35:3A:BF:94:46:A3:B8:keyid:52:71:9A:45:C9:78:EB:A3:0C:B5:57:25:87:35:3A:BF:94:46:A3:B8
```

Set this identifier in the `config.properties` for `edc.oauth.client.id`.

## Running the launcher

After the configuration has been adjusted, the launcher can be run. As a `Dockerfile` is provided, 
you can either run the connector locally or as a Docker container.

When running the connector, some additional properties have to be provided as system properties:

* `edc.fs.config`: The path to the `config.properties` file.
* `edc.vault`: The path to the `vault.properties` file (required by the `vault-fs` extension, can be 
  set to point to the `config.properties` file).
* `edc.keystore`: The path to the keystore.
* `edc.keystore.password`: The password for the keystore.

**Note, that in case you are using an external DAPS or running your local DAPS under HTTPS, the DAPS 
is likely to use a self-signed SSL certificate, which will not be trusted by the connector by 
default. In that case, supply a custom truststore and password via the system properties 
`javax.net.ssl.trustStore` and `javax.net.ssl.trustStorePassword` in the same way as the other 
system properties.**

### Local setup

To run the connector locally, build the `.jar` using the Gradle wrapper and then run it using Java. 
In the run command, be sure to provide the aforementioned system properties. Run the following 
commands in the root directory of the project:

```shell
./gradlew clean :launchers:ids-connector:build
java -Dedc.fs.config=<path-to-config.properties> \
    -Dedc.vault=<path-to-config.properties> \
    -Dedc.keystore=<path-to-keystore> \
    -Dedc.keystore.password=<keystore-password> \
    -jar launchers/ids-connector/build/libs/dataspace-connector.jar
```

### Docker

This launcher provides a [Dockerfile](./Dockerfile), which builds the connector and uses environment 
variables for setting the system properties from the `java` command. Thus, the image only has to be 
built once and can then be used for different deployments. By default, no custom truststore is supplied in the
Dockerfile. If you need to use a custom truststore, please have a look at [this section](#custom-truststore). 

To build the image, run the following command in the root directory of the project:

```shell
docker build -t edc-ids-connector -f launchers/ids-connector/Dockerfile .
```

Before running the image, you need to create an `.env` file supplying the system properties. You can 
adjust the [ids-connector.env](./ids-connector.env) supplied in this sample. The paths to the 
properties and keystore files should not point to your local environment this time, but to the 
location where you mount the files in the container. Therefore, make sure that the paths match the mount paths in the
command below.

In the following command, adjust the port if you changed it in your `config.properties` and adjust 
the mounted volumes to match your environment. The mounted volumes should contain the 
`config.properties` file as well as the keystore. If you added the system properties for a custom 
truststore to the `Dockerfile`, make sure to mount the truststore as well.

```shell
docker run -p 8181:8181 -p 8282:8282 \
    --env-file ./launchers/ids-connector/ids-connector.env \
    -v '/directory/with/properties:/config/config.properties' \
    -v '/directory/with/keystore:/config/keystore.p12' \
    edc-ids-connector
```

#### Custom truststore

If you need to use a custom truststore, add the properties `-Djavax.net.ssl.trustStore` and
`-Djavax.net.ssl.trustStorePassword` to the `ENTRYPOINT` in the Dockerfile:

```Dockerfile
ENTRYPOINT java \
    -Djava.security.edg=file:/dev/.urandom \
    -Dedc.ids.id="urn:connector:edc-connector-24" \
    -Dedc.ids.title="Eclipse Dataspace Connector" \
    -Dedc.ids.description="Eclipse Dataspace Connector with IDS extensions" \
    -Dedc.ids.maintainer="https://example.maintainer.com" \
    -Dedc.ids.curator="https://example.maintainer.com" \
    -Djavax.net.ssl.trustStore=$JAVA_TRUSTSTORE \
    -Djavax.net.ssl.trustStorePassword=$JAVA_TRUSTSTORE_PASSWORD \
    -jar dataspace-connector.jar
```

The corresponding values are added to the `env` file:

```env
JAVA_TRUSTSTORE=/config/truststore.p12
JAVA_TRUSTSTORE_PASSWORD=<truststore-password>
```

When running the image, make sure to mount the truststore:

```shell
docker run -p 8181:8181 -p 8282:8282 \
    --env-file ./launchers/ids-connector/ids-connector.env \
    -v '/directory/with/properties:/config/config.properties' \
    -v '/directory/with/keystore:/config/keystore.p12' \
    -v '/directory/with/truststore:/config/truststore.p12' \
    edc-ids-connector
```

---

### Setting up a local DAPS instance

If you do not have access to an external DAPS, you can set up your own local instance for testing. 
To do so, follow these steps:

1. Checkout the [Omejdn DAPS repository](https://github.com/International-Data-Spaces-Association/omejdn-daps)
2. Retrieve the submodules: 
   > git submodule update --init --remote
3. Generate a key and a certificate for the DAPS instance:
   > openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout keys/omejdn/omejdn.key -out daps.cert
4. Modify `.env` in the project root: set `DAPS_DOMAIN` to the URL your DAPS instance will be running at.
5. Register a connector (the security profile is optional and will default to *idsc:BASE_SECURITY_PROFILE* if not
   specified):
   > scripts/register_connector.sh <client-name-for-connector> <security-profile>
6. Optionally, you can register more connectors by running step 5 multiple times with different client names.
7. Run the DAPS:
   > docker compose -f compose-development.yml up
8. When you see `omejdn-server_1  | == Sinatra (v2.1.0) has taken the stage on 4567 for development with backup from Thin`
   in the logs, the DAPS is ready to accept requests.
   
The URL under which the connector can reach the DAPS is `http://localhost:80` due to the `NGINX` used in the 
`docker-compose` file.

#### Creating the keystore for the connector

After running step 5 from the above list, two files named `<client-name-from-step-5>.cert` and
`<client-name-from-step-5>.key` have been added in the `keys` directory. Using `openssl` and these 
files, a keystore can be created. The following command will create the keystore in the root 
directory of the DAPS repository. To create it in a specific directory, precede `<client-name>.p12` 
with the desired output path.

```bash
openssl pkcs12 -export -in keys/<client-name>.cert -inkey keys/<client-name>.key -out <client-name>.p12
```

In the resulting keystore, the certificate will have alias `1`.
