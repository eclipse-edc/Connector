# OAuth2 Provision Core

ATTENTION:
please prefer the `data-plane-http-oauth2` module to this as it is more flexible.
This package is now deprecated and it will be removed soon.

This extension can be used when there's a data endpoint that requires OAuth2 authentication through the
[*client credentials flow*](https://auth0.com/docs/get-started/authentication-and-authorization-flow/client-credentials-flow)
It can be used both on **source** and **sink** side of the data transfer:

- **source**: when the source data address contains the `oauth2` related properties, the **provider**'s provisioner
  will request a token and store it in the vault, then the `data-plane-http` extension will get the token
  to authenticate the data request call.
- **sink**: when the destination data address contains the `oauth2` related properties, the provisioner of the
  participant that will store the data will request a token and store it in the vault, then the
  `data-plane-http` extension will get the token to authenticate the data request call.

Please note that this extension doesn't currently support neither expiration nor refresh tokens, as they are not
mandatory specifications that are up to the OAuth2 server implementation used.

## How it works

![](docs/diagram.png)

The token will be requested in the provision phase and stored in the vault, then it will be used by the `data-plane-http`
extension and put in the request. This can happen either on source side (by the provisioner) or on the sink side (either
by consumer or provider).

## How to use it

The extension works for all the `HttpData` addresses that contain the "oauth2" properties (defined in
[Oauth2DataAddressSchema](src/main/java/org/eclipse/edc/connector/provision/oauth2/Oauth2DataAddressSchema.java)).
It supports [both types of client credential](https://connect2id.com/products/server/docs/guides/oauth-client-authentication#credential-types):
shared secret and private-key based.

### Common properties

- `oauth2:tokenUrl`: the url where the token will be requested
- `oauth2:scope`: (optional) the requested scope

### Private-key based client credential

This type of client credential is used when the `HttpData` address contains the `oauth2:privateKeyName` property. This type of client
credential is considered as more secured as described [here](https://connect2id.com/products/server/docs/guides/oauth-client-authentication#private-key-auth-is-more-secure).
The mandatory for working with type of client credentials are:

- `oauth2:privateKeyName`: the name of the private key used to sign the JWT sent to the Oauth2 server
  `oauth2:validity`: the validity of the JWT token sent to the Oauth2 server (in seconds)

### Shared secret client credential

This type of client credential is used when the `HttpData` address DOES not contain the `oauth2:privateKeyName` property.
The mandatory for working with type of client credentials are:

- `oauth2:clientId`: the client id
- `oauth2:clientSecret`: (deprecated) shared secret for authenticating to the Oauth2 server
- `oauth2:clientSecretKey`: the key with which the shared secret for authenticating to the Oauth2 server is stored into the `Vault`

