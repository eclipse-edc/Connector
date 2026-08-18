# Data Plane Signaling - OAuth 2 Token Exchange authorization profile

Implements the [
`oauth2_token_exchange`](https://eclipse-dataplane-signaling.github.io/profiles/HEAD/#oauth-2-token-exchange)
authorization profile of the Data Plane Signaling specification.

Unlike `oauth2_client_credentials`, the issuing party does not run a self-signing authorization server. It presents a
workload-layer credential - a projected Kubernetes ServiceAccount token or a SPIFFE JWT-SVID - to a **token exchange
broker**, which validates it, applies policy, and mints a short-lived scoped JWT
([RFC 8693](https://datatracker.ietf.org/doc/html/rfc8693)). The workload credential never travels to the receiving
party.

## The `authorization` object

```json
{
  "authorization": {
    "type": "oauth2_token_exchange",
    "tokenExchangeEndpoint": "https://broker.example.com/token",
    "issuer": "https://broker.example.com",
    "jwksUri": "https://broker.example.com/.well-known/jwks.json",
    "resource": "urn:principal:consumer-controlplane",
    "audience": "dps-signaling",
    "scope": "signaling:dataflow"
  }
}
```

| Property                | Required       | Description                                                                                                                |
|-------------------------|----------------|----------------------------------------------------------------------------------------------------------------------------|
| `tokenExchangeEndpoint` | yes            | the broker's RFC 8693 token exchange endpoint                                                                              |
| `issuer`                | yes            | the expected `iss` claim of tokens minted by the broker                                                                    |
| `jwksUri`               | yes            | the broker's JWKS endpoint. Inline `jwks` is **not** permitted by this profile and signature verification is never skipped |
| `resource`              | yes (outbound) | the `id` URI of the Principal Resource the exchanged token speaks for, sent as the RFC 8707 `resource` parameter           |
| `audience`              | no             | the audience the exchanged token is requested for. Falls back to `edc.dps.oauth2.tokenexchange.audience`                   |
| `scope`                 | no             | falls back to `edc.dps.oauth2.tokenexchange.scope`. Outbound requests fail when neither is present                         |

## Configuration

| Setting                                         | Description                                                                                                                                               |
|-------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `edc.dps.oauth2.tokenexchange.subjecttokenpath` | path to the file holding the workload credential. Defaults to the projected ServiceAccount token at `/var/run/secrets/kubernetes.io/serviceaccount/token` |
| `edc.dps.oauth2.tokenexchange.scope`            | default scope requested when the profile carries none                                                                                                     |
| `edc.dps.oauth2.tokenexchange.audience`         | default audience requested when the profile carries none. Must match the audience the token exchange service is configured with                           |

Exchanged tokens are not cached: every outbound signaling request performs a fresh exchange. The workload credential is
re-read from disk each time, so rotation by the kubelet or the SPIFFE agent is picked up.

## Caller identity: `client_id` then `sub`

Data Plane Signaling in EDC matches the caller identity of an incoming request against the `dataplaneId`
(respectively the `controlplaneId`) of the party the token speaks for, whereas the profile requires `sub` to be an
absolute Principal Resource URI. Those two cannot generally be the same string, so the caller identity is taken from
the [RFC 8693 `client_id` claim](https://datatracker.ietf.org/doc/html/rfc8693#name-client_id-client-identifier),
falling back to `sub` when the broker does not mint one:

```json
{
  "iss": "https://broker.example.com",
  "sub": "urn:dps:principal:provider-data-plane",
  "client_id": "provider-data-plane",
  "aud": "dps-signaling",
  "scope": "signaling:dataflow"
}
```

Configure the broker to set `client_id` to the `dataplaneId` / `controlplaneId`. If it cannot, the fallback applies and
the Principal Resource `id` must itself equal that identifier.

