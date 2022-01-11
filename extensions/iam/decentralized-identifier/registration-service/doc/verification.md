## Preconditions, assumptions, terminology

The first two steps of the verification process must already be in place. Those are:

1. Verify the JWT containing the DID Key
2. Extract claims from the IdentityHub

This flow adds one step to the flow, i.e. verifying the claims extracted from the `IdentityHub`. The DID key of
the `RegistrationService` and its host URL are public knowledge, e.g. through configuration values. If Web-DIDs are
used, one can be inferred from the other.

In order for this flow to work, the RegistrationService need not provide an `IdentityHub`, and its DID need not contain
an `IdentityHubUrl`.

## Verification Flow

_Detailing step 1 and 2 is not part of this document!_

- ...
- Provider fetches claims from Consumer's `IdentityHub`. They are stored in the form of a signed-JWT and were signed
  with the RegistrationService's private key. See [the onboarding flow](onboarding_flow.puml) for details.
- Provider obtains the RegistrationService's DID (or fetches it from an internal cache)
- Provider extracts PublicKey from RS's DID
- Provider verifies the signature of the Consumer's signed-JWT

## Possible Improvements

- instead of contacting the RegistrationService on every request, each connector caches the RS's DID in order to
  increase resilience