# Embedded Secure Token Service (STS) Extension

## Overview

This module implements the `SecureTokenService` spi, which will be used for generating the Self-Issued ID Token
in the `DCP` protocol flow. This is an embeddable implementation, which can be used in the same process of 
the EDC control-plane runtime.

## Self-Issued ID Token Contents

As outlined in the [DCP](https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/identity.protocol.base.md#41-self-issued-id-token-contents) spec
the token includes the following claims:

- The `iss` and `sub` claims MUST be equal and set to the bearer's (participant's) DID.
- The `sub_jwk` claim is not used
- The `aud` set to the `participant_id` of the relying party (RP)
- The `jti` claim that is used to mitigate against replay attacks
- The `exp` expiration time of the token
- The `access_token` VP Access Token (Optional)

Additionally, when generating the Self-Issued ID Token the `bearerAccessScope` parameter is passed the additional claim
`access_token` claim is added.

## VP Access Token format

The `DCP` protocol does not specify the format of the VP Access Token, which it's up to the specific STS implementation. 
In this  implementation the VP access token is still a JWT token with the following claims:

- The `iss` is the same of the SI token (participant's DID)
- The `sub` set to the `participant_id`/`alias (DID)` of the relying party (RP)
- The `aud` set to the `participant_id` of the participant
- The `jti` claim that is used to mitigate against replay attacks
- The `exp` expiration time of the token

`CredentialService` implementors, should verify that the `sub` of the Self-Issued ID token and the `sub` of
the VP access token matches.