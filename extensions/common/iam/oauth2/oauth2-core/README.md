# OAuth 2 Identity Service

This extension provides an `IdentityService` implementation based on the OAuth2 protocol for authorization.

The setting parameters of this extension are listed below:

| Parameter name                    | Description                                                                                | Mandatory | Default value                   |
|:----------------------------------|:-------------------------------------------------------------------------------------------|:----------|:--------------------------------|
| `edc.oauth.token.url`             | URL of the authorization server                                                            | true      | null                            |
| `edc.oauth.provider.audience`     | Provider audience                                                                          | false     | id of the connector             |
| `edc.oauth.provider.jwks.url`     | URL from which well-known public keys of Authorization server can be fetched               | false     | http://localhost/empty_jwks_url | 
| `edc.oauth.public.key.alias`      | Alias of public associated with client certificate                                         | true      | null                            |
| `edc.oauth.private.key.alias`     | Alias of private key (used to sign the token)                                              | true      | null                            |
| `edc.oauth.provider.jwks.refresh` | Interval at which public keys are refreshed from Authorization server (in minutes)         | false     | 5                               |
| `edc.oauth.client.id`             | Public identifier of the client                                                            | true      | null                            |
| `edc.oauth.validation.nbf.leeway` | Leeway in seconds added to current time to remedy clock skew on notBefore claim validation | false     | 10                              |
