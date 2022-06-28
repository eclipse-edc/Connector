# JSON Web Tokens for Decentralized Identity

## Decision

[Json Web Tokens (JWT)](https://datatracker.ietf.org/doc/html/rfc7519) are used to authenticate between EDC connectors when using the Decentralized Identity module. Tokens contain the following claims:

| Claim           | Value                                                        |
| --------------- | ------------------------------------------------------------ |
| Issuer          | The request source connector [did:web](https://w3c-ccg.github.io/did-method-web/) identifier (example: `did:web:edc.example.com`). This allows the server to verify the JWT signature against the source's public key. |
| Subject         | The fixed string `verifiable-credential`.                    |
| Audience        | The request destination connector IDS endpoint (example: `http://edc.example.com:8181/api/v1/ids/data`). This allows the server to verify the intended audience. |
| JWT ID          | A random UUID.                                               |
| Expiration Time | A time set in the near future.                               |

## Rationale

### Audience claim

A JWT token sent to a participant other than the one it was initially intended for must be rejected. The IDS endpoint URL of the message receiver is used as the JWT *audience* claim to achieve this.

The JWT spec defines the [audience](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3) claim to identify the JWT target of a token to protect from a malicious entity impersonating the originator (in particular, the owner of the request destination server).

The IDS endpoint URL is known to both the consumer and the provider (as the `ids.webhook.address` property).