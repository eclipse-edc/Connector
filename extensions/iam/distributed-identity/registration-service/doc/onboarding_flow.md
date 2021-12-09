## Preconditions, assumptions, terminology

- It is assumed that a DID is already in place and publicly available. The generation or deployment of a DID is not part
  of this flow.
- The terms "connector" and "participant agent" are to be used interchangeably. Please note that both terms refer to a "
  logical" connector - several replicas in a K8s cluster would share the same identity and thus count as one Participant
  Agent.
- `NewParticipant` in the diagram refers to the participant agent, which is to be onboarded in the dataspace
- The flow presented here is _synchronous_, i.e. the verified claims are returned immediately
- The registration service's public key is available through its DID
- The registration service's DID key is public knowledge
- Participant agent may be abbreviated by "PA"
- Registration service may be abbreviated by "RS"

## Onboarding flow

- The new participant agent calls up the registration endpoint at `/register` and presents the following parameters:
    - `participantAgentId`: a string uniquely identifying the PA in the dataspace
    - `didKey`: the ID of the PA's DID, can be a Web-DID, ION-DID, etc. Must include the method, e.g. `did:web:...`
    - `claims`: a list of key-value-pairs, each of which represents a claim, which shall be verified by the registration
      service.
- The registration service verifies the claims one by one and signs each with its private key
- The RS returns the signed claims in the form of a signed JWT to the participant agent.
- The PA stores the signed claims in its `IdentityHub`
- If the claims **cannot be verified** the registration service returns HTTP 403

## Future improvements

- The registration service writes the verified claims directly in the participant agent's `IdentityHub`. This has been
  intentionally left out for now for reasons of simplicity, modularity and testability.
- Async flow: in case verifying the claims takes a long time, for example when human interaction is required, the
  participant agent must supply a callback URL through which the registration service may announce that the verification
  has completed (either successfully or with errors). This would be especially powerful if the RS writes directly to
  the `IdentityHub`