# DAPS module deprecation

## Decision

We will stop publishing DAPS related modules.

## Rationale

Shifting toward decentralized identity model though the adoption of Decentralized Claims Protocol as the protocol to be
used, makes DAPS obsolete, and maintaining it is an unneeded effort by the EDC committer group.

## Approach

In EDC version 0.10.0 we deprecated:
- module `oauth2-daps`
- class `Oauth2ServiceImpl`

they will be removed without further warnings in the subsequent versions.
