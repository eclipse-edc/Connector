# OAuth 2 Identity Service

This extension provides an `IdentityService` implementation based on the OAuth2 protocol for authorization.

## Configuration

| Parameter name                     | Description                                                                                                          | Mandatory | Default value                       |
|:-----------------------------------|:---------------------------------------------------------------------------------------------------------------------|:----------|:------------------------------------|
| `edc.oauth.token.url`              | URL of the authorization server                                                                                      | true      | null                                |
| `edc.oauth.provider.audience`      | Provider audience to be put in the outgoing token as 'aud' claim                                                     | false     | id of the connector                 |
| `edc.oauth.endpoint.audience`      | Endpoint audience to verify incoming token 'aud' claim                                                               | false     | `edc.oauth.provider.audience` value |
| `edc.oauth.provider.jwks.url`      | URL from which well-known public keys of Authorization server can be fetched                                         | false     | http://localhost/empty_jwks_url     | 
| `edc.oauth.certificate.alias`      | Alias of public associated with client certificate                                                                   | true      | null                                |
| `edc.oauth.private.key.alias`      | Alias of private key (used to sign the token)                                                                        | true      | null                                |
| `edc.oauth.provider.jwks.refresh`  | Interval at which public keys are refreshed from Authorization server (in minutes)                                   | false     | 5                                   |
| `edc.oauth.client.id`              | Public identifier of the client                                                                                      | true      | null                                |
| `edc.oauth.validation.nbf.leeway`  | Leeway in seconds added to current time to remedy clock skew on notBefore claim validation                           | false     | 10                                  |
| `edc.oauth.token.resource.enabled` | Adds `resource` form parameter in the access token request. Allows to specify an audience as defined in the RFC-8707 | false     | false                               |

## Extensions

### CredentialsRequestAdditionalParametersProvider

An instance of the `CredentialsRequestAdditionalParametersProvider` service interface can be provided to have the
possibility to enrich the form parameters of the client credentials token request 
